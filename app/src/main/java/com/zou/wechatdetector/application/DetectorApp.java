package com.zou.wechatdetector.application;

import android.app.Application;

import com.zou.wechatdetector.utils.CrashHandler;

/**
 * Created by zou on 2018/6/7.
 */

public class DetectorApp extends Application {
    private static DetectorApp sInstance;
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
    }

    public static DetectorApp getInstance() {
        return sInstance;
    }

}
