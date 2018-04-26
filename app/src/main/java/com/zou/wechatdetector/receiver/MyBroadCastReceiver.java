package com.zou.wechatdetector.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by zou on 2018/4/26.
 */

public class MyBroadCastReceiver extends BroadcastReceiver {
    private static final String TAG = "MyBroadCastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"MyBroadCastReceiver onReceive");
    }
}
