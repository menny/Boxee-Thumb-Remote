package com.menny.android.boxeethumbremote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class HelpUiActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
	}
	
	public void onCloseClicked(View view) {
		finish();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		finish();
		startActivity(new Intent(getApplicationContext(), RemoteUiActivity.class));
	}
}
