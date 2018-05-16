package com.zou.wechatdetector.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.zhy.m.permission.MPermissions;
import com.zhy.m.permission.PermissionDenied;
import com.zhy.m.permission.PermissionGrant;

import com.zou.wechatdetector.BuildConfig;
import com.zou.wechatdetector.service.JobProtectService;
import com.zou.wechatdetector.service.MainService;
import com.zou.wechatdetector.utils.Tools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by zou on 2018/4/9.
 */

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    private MediaProjectionManager projectionManager;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 101;
    private static final int REQUEST_CODE_USAGE_ACCESS = 102;
    private MainServiceConnection mainServiceConnection;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");
        initData();
        //1.请求常规权限
        requestRecordPermission();
        //启动截屏服务
        Intent intent = new Intent(this,MainService.class);
        //设置截屏服务为前台服务
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            //android o 中有限制
//            startForegroundService(intent);
//        }else {
        mainServiceConnection = new MainServiceConnection();
        bindService(intent,mainServiceConnection, Service.BIND_AUTO_CREATE);
//        startService(intent);
//        }
        Log.i(TAG,"onCreate");
        //启动守护进程
//        Intent portectService = new Intent(this,ProtectService.class);
//        startService(portectService);
        startJobScheduler();
    }

    private void startJobScheduler() {
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancelAll();
        JobInfo.Builder builder = new JobInfo.Builder(1024, new ComponentName(getPackageName(), JobProtectService.class.getName()));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //android N之后时间必须在15分钟以上
//            builder.setMinimumLatency(10 * 1000);
            builder.setPeriodic(15 * 60 * 1000);
        }else{
            builder.setPeriodic(60 * 1000);
        }
        builder.setPersisted(true);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        int schedule = jobScheduler.schedule(builder.build());
        if (schedule <= 0) {
            Log.w(TAG, "schedule error！");
        }
    }

    private void initData() {
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unbindService(mainServiceConnection);
    }

    /**
     * 请求屏幕录制的权限
     */
    private void requestRecordPermission(){
        MPermissions.requestPermissions(this, 4, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MPermissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    /**
     * 权限请求成功
     */
    @PermissionGrant(4)
    public void requestRecordSuccess(){
        //2.请求获取最近访问的进程的权限
        if(!Tools.hasUsageAccessPermission(this)) {
            Intent intent1 = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent1, REQUEST_CODE_USAGE_ACCESS);
        }else{
            //3.如果已经有了访问进程的权限，则去请求截屏权限
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
        }
    }

    /**
     * 权限请求失败
     */
    @PermissionDenied(4)
    public void requestRecordFailed(){
        Toast.makeText(this,"权限请求失败,请重试",Toast.LENGTH_SHORT).show();
        //常规权限请求失败，重新请求
        requestRecordPermission();
    }

    /**
     * 屏幕捕捉请求 REQUEST_CODE_SCREEN_CAPTURE
     */
    @Override
    protected void onActivityResult(int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_USAGE_ACCESS:
                if(Tools.hasUsageAccessPermission(this)){
                    Intent captureIntent = projectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
                }else{
                    Toast.makeText(this,"请在有权查看使用情况的应用中选择WechatDetector，并开启。",Toast.LENGTH_SHORT).show();
                    Intent intent1 = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent1, REQUEST_CODE_USAGE_ACCESS);
                }
                break;
            case REQUEST_CODE_SCREEN_CAPTURE:
                if(resultCode == RESULT_OK) {
                    EventBus.getDefault().post(data);

                    moveTaskToBack(true);
                }else{
                    Toast.makeText(this,"请点击不再提示和立即开始",Toast.LENGTH_SHORT).show();
                    Intent captureIntent = projectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
                }
                break;
        }
    }

    private class MainServiceConnection implements ServiceConnection{

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainService.SubBinder subBinder = (MainService.SubBinder) service;
            subBinder.ShowTips();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

}
