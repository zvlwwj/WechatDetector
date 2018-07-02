package com.zou.wechatdetector.utils;

import android.content.Context;
import android.util.Log;

/**
 * Created by zou on 2018/6/7.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";


    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;

    private Context mContext;

    private static CrashHandler sInstance = new CrashHandler();

    //构造方法私有，防止外部构造多个实例，即采用单例模式
    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return sInstance;
    }

    //这里主要完成初始化工作
    public void init(Context context) {
        //获取系统默认的异常处理器
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        //将当前实例设为系统默认的异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
        //获取Context，方便内部使用
        mContext = context.getApplicationContext();
    }
    @Override
    public void uncaughtException(Thread t, Throwable e) {

        //打印出当前调用栈信息
        e.printStackTrace();
        Log.e(TAG,"uncaughtException : "+e.getMessage());
    }
}
