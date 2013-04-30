package net.trinu.networkplayer;

import java.util.ArrayList;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class QueueFragment extends DialogFragment {
	//private final String tag = "QueueFragment";
	private ArrayAdapter<Song> aa;
	private ArrayList<Song> queue;
	
	static QueueFragment newInstance(ArrayList<Song> queue) {
		QueueFragment q = new QueueFragment();
		Bundle args = new Bundle();
		args.putSerializable("queue", queue);
		q.setArguments(args);
		return q;
	}
	
	private class QueueAdapter extends ArrayAdapter<Song> {
		private ArrayList<Song> songs;
		
		public QueueAdapter(Context context, int resource, int textViewId, ArrayList<Song> songs) {
			super(context, resource, textViewId, songs);
			this.songs = songs;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = super.getView(position, convertView, parent);
			TextView title = (TextView) row.findViewById(R.id.songTitle);
			TextView artist = (TextView) row.findViewById(R.id.songArtist);
			MediaMetadataRetriever meta = new MediaMetadataRetriever();
			meta.setDataSource(getActivity().getExternalCacheDir()+songs.get(position).getPath().substring(5));
			title.setText(meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
			artist.setText(meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
			return row;
		}
		
	}

	public QueueFragment() {
		// TODO Auto-generated constructor stub
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		queue = (ArrayList<Song>) getArguments().getSerializable("queue");
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_queue, container, false);
		ListView lv = (ListView) v.findViewById(R.id.queueList);
		aa = new QueueAdapter(getActivity(), R.layout.queue_item, R.id.songTitle, queue);
		lv.setAdapter(aa);
		getDialog().setTitle("Queue");
		
		return v;
	}
	/*
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ListView lv = (ListView) getActivity().findViewById(R.id.queueList);
		aa = new QueueAdapter(getActivity(), R.layout.queue_item, queue);
		lv.setAdapter(aa);
		getDialog().setTitle("Queue");
	}
*/
}
