//package net.evendanan.android.thumbremote.receiver;
//
//import net.evendanan.android.thumbremote.ui.RemoteUiActivity;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.os.Bundle;
//
//public class NetworkTypeChangedReceiver extends BroadcastReceiver {
//	
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        Bundle bundle = intent.getExtras();
//        if(null == bundle)
//        	return;
//        
//        final NetworkInfo netInfo = (NetworkInfo)bundle.getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
//        
//        if (netInfo.isConnected())
//        	RemoteUiActivity.onNetworkAvailable();
//    }
//
//}
