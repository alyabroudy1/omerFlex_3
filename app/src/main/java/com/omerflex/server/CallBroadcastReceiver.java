package com.omerflex.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "CallBroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action != null && action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.d(TAG, "Outgoing call to: " + phoneNumber);

            // You can perform actions here, such as:
            // - Logging the call
            // - Blocking the call (by aborting the broadcast, but this is highly restricted on newer Android versions)
            // - Displaying a custom UI
        }
    }
}
