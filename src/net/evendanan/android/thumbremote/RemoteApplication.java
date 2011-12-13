package net.evendanan.android.thumbremote;

import android.app.Application;

public class RemoteApplication extends Application {

	private static Settings msConfig;
	
	@Override
	public void onCreate() {
//		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//        .detectAll()
//        .penaltyLog()
//        .build());
//		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//        .detectAll()
//        .penaltyLog()
//        .penaltyDeath()
//        .build());

		super.onCreate();
		
		msConfig = new Settings(getApplicationContext());
	}
	
	public static Settings getConfig() { return msConfig;}
}
