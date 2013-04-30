package net.trinu.networkplayer;

import net.trinu.networkplayer.MusicService.LocalBinder;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
public class PlayerFragment extends Fragment {
	private String tag = "PlayerFragment";
	private MusicService ms;
	private boolean bound = false;
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			updateMetas();
		}
	};
	
	
	public void enqueue(String add) {
		ms.enqueue(add);
	}
			
	private void updateMetas() {
		TextView title = (TextView) getActivity().findViewById(R.id.title);
		TextView artist = (TextView) getActivity().findViewById(R.id.artist);
		title.setText(ms.getTitle());
		artist.setText(ms.getArtist());
		ImageButton playpause = (ImageButton) getActivity().findViewById(R.id.play);
		if (ms.isPlaying())
			playpause.setImageResource(android.R.drawable.ic_media_pause);
		else
			playpause.setImageResource(android.R.drawable.ic_media_play);
	}
		
	@Override
	public void onStart() {
		super.onStart();
		Intent intent = new Intent(getActivity(), MusicService.class);
		intent.putExtra("handler", new Messenger(handler));
		getActivity().startService(intent);
		getActivity().bindService(intent, conn, Context.BIND_AUTO_CREATE);
		
		LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.metadataLayout);
		layout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(tag, "clicked");
				
				QueueFragment queue = QueueFragment.newInstance(ms.getQueue());
				queue.show(getActivity().getSupportFragmentManager(), "queueFragment");
			}
		});
				
		ImageButton playpause = (ImageButton) getActivity().findViewById(R.id.play);
		playpause.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (ms.isPlaying()) {
					ms.pause();
				} else {
					ms.play();
				}
			}
		});
		ImageButton next = (ImageButton) getActivity().findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ms.playNext();
			}
		});

	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (bound) {
			getActivity().unbindService(conn);
			bound = false;
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
				
	public void play() {
		ms.play();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_player, container, false);
	}
	
	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			ms = binder.getService();
			bound = true;
			if (ms.isPlaying())
				updateMetas();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			bound = false;
		}
	};
}
