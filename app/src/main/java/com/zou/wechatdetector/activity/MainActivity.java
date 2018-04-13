package com.zou.wechatdetector.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.zhy.m.permission.MPermissions;
import com.zhy.m.permission.PermissionDenied;
import com.zhy.m.permission.PermissionGrant;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by zou on 2018/4/9.
 */

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 101;
    private MainService mainService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        requestRecordPermission();
        Intent intent1 = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent1);
        Intent intent = new Intent(this,MainService.class);
        startService(intent);

        Log.i(TAG,"MainActivity");
    }

    private void initData() {
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
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
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    /**
     * 权限请求失败
     */
    @PermissionDenied(4)
    public void requestRecordFailed(){
        Toast.makeText(this,"权限请求失败,请重试",Toast.LENGTH_SHORT).show();
    }

    /**
     * 屏幕捕捉请求 REQUEST_CODE_SCREEN_CAPTURE
     */
    @Override
    protected void onActivityResult(int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_SCREEN_CAPTURE:
                if(resultCode == RESULT_OK) {
                    EventBus.getDefault().post(data);
                }else{
                    Toast.makeText(this,"权限请求失败,请重试",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
