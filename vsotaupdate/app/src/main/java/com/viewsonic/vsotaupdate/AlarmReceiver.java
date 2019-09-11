package com.viewsonic.vsotaupdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private SystemUpdateService mService;
    private static final String TAG = "vsotaupdate/AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // connect to SystemUpdateService
        Log.d("123", "Receive Alarm");

        Intent serviceIntent = new Intent(context, SystemUpdateService.class);
        serviceIntent.setAction(Util.Action.ACTION_PERIODIC_CHECK);
        //context.bindService(serviceIntent, mConnection, context.BIND_AUTO_CREATE);
        context.startService(serviceIntent);
    }

}