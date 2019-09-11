package com.viewsonic.vsotaupdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OtaReceiver extends BroadcastReceiver {

    private static final String TAG = "vsotaupdate/OtaReceiver";

    public OtaReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Receive action "+ action);

        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            // start service
            Intent service_intent = new Intent(context, SystemUpdateService.class);
            service_intent.setAction(Util.Action.ACTION_BOOT_COMPLETE);
            context.startService(service_intent);
        }

    }
}
