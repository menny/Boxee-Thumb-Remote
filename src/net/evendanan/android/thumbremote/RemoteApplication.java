package net.evendanan.android.thumbremote;

import android.app.Application;

public class RemoteApplication extends Application {

	private static Settings msConfig;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		msConfig = new Settings(getApplicationContext());
	}
	
	public static Settings getConfig() { return msConfig;}
}
