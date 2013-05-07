package net.trinu.networkplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener {
	private static final String tag = "MusicService";
	public static final String ACTION_START = "start";
	public static final String ACTION_PAUSE = "pause";
	private final IBinder binder = new LocalBinder();
	private ArrayList<Song> queue;
	private MediaPlayer mp = null;
	private WifiLock wl;
	private String artist;
	private String title;
	private Messenger msg = null;
	private NotificationManager noteManager;
	
	private void update() {
		try {
			msg.send(Message.obtain());
		} catch (RemoteException e) {
		} catch (NullPointerException e) {}
	}
	
	public int onStartCommand(Intent in, int flags, int startId) {
		if (in != null && in.getExtras().containsKey("handler"))
			msg = (Messenger) in.getExtras().get("handler");
		return START_STICKY;
	}
	
	@Override
	public void onCreate() {
		noteManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		queue = new ArrayList<Song>();
	}
	
	public void enqueue(String add) {
		Song song = new Song(add);
		queue.add(song);
		new cacheFile().execute(song);
	}
	
	public void playNext() {
		if (queue.size() > 0) {
			if (queue.get(0).isCached()) {
				playSong(queue.remove(0).getPath());
				return;
			}
		} else if (isPlaying()) {
			return;
		}
		noteManager.cancel(0);
		update();
	}
		
	private void playSong(String path) {
		if (isValid()) {
			mp.stop();
			mp.release();
		}
		path = getExternalCacheDir()+path.substring(5);
		MediaMetadataRetriever meta = new MediaMetadataRetriever();
		meta.setDataSource(path);
		title = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		artist = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
		NotificationCompat.Builder builder = 
				new NotificationCompat.Builder(this).
				setSmallIcon(R.drawable.ic_launcher).
				setContentTitle("Network Player").
				setContentText("Playing "+title).
				setOngoing(true);  // make notification
		Intent result = new Intent(this, NetworkPlayerActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(NetworkPlayerActivity.class);
		stackBuilder.addNextIntent(result);
		PendingIntent resultPending = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPending);
		noteManager.notify(0, builder.build());
		
		mp = new MediaPlayer();
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mp.setDataSource(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mp.setOnPreparedListener(this);
		mp.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				playNext();
			}
		}); 
		mp.prepareAsync();
	}
	
	public boolean isPlaying() {
		if (isValid())
			return mp.isPlaying();
		else return false;
	}
	
	public boolean isValid() {
		return (mp != null);
	}
	
	public int getPosition() {
		if (isValid()) {
			return mp.getCurrentPosition();
		} else return 0;
	}
	
	public void play() {
		if (isValid()) {
			mp.start();
			update();
		} else
			playNext();
	}
	
	public void pause() {
		if (isValid()) {
			mp.pause();
			noteManager.cancel(0);
		}
		update();
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getArtist() {
		return artist;
	}
	
	public ArrayList<Song> getQueue() {
		return queue;
	}

	@Override
	public void onPrepared(MediaPlayer mplayer) {
		update();
		mplayer.start();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
    public class LocalBinder extends Binder {
        MusicService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MusicService.this;
        }
    }
	
	@Override
	public void onDestroy() {
		if (isValid()) {
			mp.stop();
			mp.release();
		}
		NotificationManager noteManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		noteManager.cancel(0);
	}
	
	private final class cacheFile extends AsyncTask<Song, Void, String> {
		@Override
		protected String doInBackground(Song...in) {
			wl = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
					.createWifiLock(WifiManager.WIFI_MODE_FULL, "wifilock");
			wl.acquire();
			for (Song cacheSong:in) {
				SmbFile toCache = null;
				try {
					toCache = new SmbFile(cacheSong.getPath());
				} catch (MalformedURLException e2) {}
				String path = getExternalCacheDir()+toCache.toString().substring(5);
				if (!(new File(path).exists())) {
					Log.d(tag, "path is "+path);
					new File(path).mkdirs(); // makes parent directories
					new File(path).delete(); // delete one named after the file
					FileOutputStream cacheFile = null;
					try {
						cacheFile = new FileOutputStream(path);
					} catch (FileNotFoundException e1) {
						Log.e(tag, "Could not write to cache");
						e1.printStackTrace();
					}
					SmbFileInputStream inFile;
					try {
						inFile = (SmbFileInputStream) toCache.getInputStream();
						int read=0;
						byte[] bytes = new byte[1024];
						while((read = inFile.read(bytes))!= -1) {
							cacheFile.write(bytes, 0, read);
						}
						cacheFile.close();
						inFile.close();
					} catch (IOException e) {
						Log.e(tag, "Error copying file");
						e.printStackTrace();
					}
				}
				cacheSong.setCached(true);
			}
			try { wl.release(); } catch (Exception e) {};
			return getExternalCacheDir()+queue.get(0).toString().substring(5);
		}
		
		@Override
		protected void onPostExecute(String path) {
			if (!isValid()) {
				playNext();
			}
		}
	}
}