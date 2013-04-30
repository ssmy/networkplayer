package net.trinu.networkplayer;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;

public class NetworkPlayerActivity extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		FragmentManager frag = getSupportFragmentManager();
		FragmentTransaction transaction = frag.beginTransaction();
		ServerFragment serverFragment = new ServerFragment();
		transaction.add(R.id.server_placeholder, serverFragment, "serverFragment");
		PlayerFragment playerFragment = new PlayerFragment();
		transaction.add(R.id.player_placeholder, playerFragment, "playerFragment");
		transaction.commit();
		setContentView(R.layout.activity_network_player);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.network_player, menu);
		return false;
		//return true;
	}
}
