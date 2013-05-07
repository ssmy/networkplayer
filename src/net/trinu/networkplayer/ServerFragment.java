package net.trinu.networkplayer;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class ServerFragment extends Fragment {
	String tag = "ServerFragment";
	ArrayAdapter<String> aa;								// connects locList to ListView
	ArrayList<String> locList = new ArrayList<String>();	// items on screen in nice form
	ArrayList<String> currentList;							// items on screen in full form
	Stack<String> prev = new Stack<String>();				// back stack of folders
	String titleText = "Choose Domain";						// name of previous folder
	boolean pastDomain = false;								// hides button when at domain screen
	
	private final class SmbRequest extends AsyncTask<SmbFile,Void,SmbFile[]> {
		@Override
		protected void onPreExecute() {
			
		}
		
		@Override
		protected SmbFile[] doInBackground(SmbFile... args) {
			try {
				if (!args[0].toString().equals("smb:////") && !args[0].toString().equals("smb://")) { // choose domain
					titleText = args[0].toString();
					titleText = titleText.split("/")[titleText.split("/").length-1];
					pastDomain = true;
				} else 
					pastDomain = false;
				if (prev.isEmpty() || !prev.peek().toString().equals(args[0].toString())) // allows to go back
					prev.push(args[0].toString());
				return args[0].listFiles();
			} catch (SmbException smbe) {
				smbe.printStackTrace();
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(SmbFile[] objs) {
			if (objs != null) {
				locList.clear();
				currentList = new ArrayList<String>();
				for(int i=0; i<objs.length; i++) {
					String[] split = objs[i].toString().split("/");
					if (checkFile(objs[i].toString())) {
						locList.add(split[split.length-1]);
						currentList.add(objs[i].toString());
					}
				}
				aa.notifyDataSetChanged();
				updateButton();
			} else {
				locList.clear();
				locList.add("No domains found. Ensure that wifi is enabled. This will display shares from computers using the Windows file sharing system");
				currentList = new ArrayList<String>();
				currentList.add("smb://");
				aa.notifyDataSetChanged();
			}
		}
	}
	
	private Boolean checkFile(String input) {
		List<String> good = Arrays.asList(".mp3", ".flac", "/", ".m4a", ".3gp", ".mp4", ".aac", ".ogg", ".mid", ".wav");
		if (input.endsWith("$/"))	// eliminate special folders
			return false;
		for(String e:good) {
			if (input.endsWith(e))
				return true;
		}
		return false;
	}
	
	private void updateButton() {
		Button back = (Button) getActivity().findViewById(R.id.back);
		back.setText(titleText);
		TextView title = (TextView) getActivity().findViewById(R.id.chooseDomain);
		if (pastDomain) {
			title.setVisibility(View.GONE);
			back.setVisibility(View.VISIBLE);
		} else {
			title.setVisibility(View.VISIBLE);
			back.setVisibility(View.GONE);
		}
	}
				
	@SuppressWarnings("unchecked")
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		aa = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, locList);
		ListView list = (ListView) getActivity().findViewById(R.id.fileList);

		if (savedInstanceState != null) {
			Log.d(tag, "Restoring state");
			prev = (Stack<String>) savedInstanceState.getSerializable("prev");
			titleText = savedInstanceState.getString("titleText");
			locList = savedInstanceState.getStringArrayList("locList");
			pastDomain = savedInstanceState.getBoolean("pastDomain");
			currentList = savedInstanceState.getStringArrayList("currentList");
			aa = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, locList);
			list.setAdapter(aa);
			aa.notifyDataSetChanged();
			Log.d(tag, locList.toString()+currentList.toString());
			updateButton();
		} else {
			try {
				new SmbRequest().execute(new SmbFile("smb://"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		list.setAdapter(aa);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(@SuppressWarnings("rawtypes") AdapterView av, View v, int pos, long id) {
				if (currentList.get(pos).endsWith("/")) {
					try {
						new SmbRequest().execute(new SmbFile(currentList.get(pos)));
					} catch (MalformedURLException e) {}
				} else {
					PlayerFragment player = (PlayerFragment) getFragmentManager().findFragmentByTag("playerFragment");
					player.enqueue(currentList.get(pos));
				}
			}
		});
		
		Button back = (Button) getActivity().findViewById(R.id.back);
		back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				prev.pop();
				Log.d(tag, locList.toString());
				try {
					new SmbRequest().execute(new SmbFile(prev.peek()));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		});

	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_server, container, false);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("prev", prev);
		outState.putString("titleText", titleText);
		outState.putStringArrayList("locList", locList);
		outState.putBoolean("pastDomain", pastDomain);
		outState.putStringArrayList("currentList", currentList);
	}
}
