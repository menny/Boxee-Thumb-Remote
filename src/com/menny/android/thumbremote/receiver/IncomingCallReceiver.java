package com.menny.android.thumbremote.receiver;

import com.menny.android.thumbremote.ui.RemoteUiActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class IncomingCallReceiver extends BroadcastReceiver {	
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if(null == bundle)
                return;
        
        final String state = bundle.getString(TelephonyManager.EXTRA_STATE);
                        
        if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING))
        {
            final String phonenumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            final String event = String.format("An incoming call from %s!", phonenumber);
            RemoteUiActivity.onExternalImportantEvent(event);
        }
    }

}
