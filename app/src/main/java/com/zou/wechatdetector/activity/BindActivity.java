package com.zou.wechatdetector.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.zou.wechatdetector.R;
import com.zou.wechatdetector.bean.Device;
import com.zou.wechatdetector.bean.GsonAddDeviceBean;
import com.zou.wechatdetector.bean.GsonBindUserBean;
import com.zou.wechatdetector.bean.GsonGetDeviceListBean;
import com.zou.wechatdetector.utils.Constants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.InputStream;
import java.util.ArrayList;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by zou on 2018/5/3.
 */
//TODO 华为包和普通包的打包脚本不同
//TODO 动态修改AndroidManifest的研究
//TODO 项目日志传到服务器
//TODO 添加GPS功能
//TODO 线程优化 使用封装的线程池
public class BindActivity extends Activity{
    private SharedPreferences sp;
    private EditText et_username,et_devicename;
    private Button btn_bind;
    private TextInputLayout textInputLayout_username,textInputLayout_devicename;
    private BindService bindService;
    private ImageButton ib_scan,ib_close;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind);
        initData();
        initView();
        setListener();
        sp = getSharedPreferences("detector",0);
        if(!sp.getBoolean("fristTime",true)){
            Intent intent = new Intent(BindActivity.this,MainActivity.class);
            startActivity(intent);
        }
    }



    private void initView() {
        ib_scan = findViewById(R.id.ib_scan);
        ib_close = findViewById(R.id.ib_close);
        et_username = findViewById(R.id.et_username);
        btn_bind = findViewById(R.id.btn_bind);
        textInputLayout_username = findViewById(R.id.textInputLayout_username);
        textInputLayout_devicename = findViewById(R.id.textInputLayout_devicename);
        et_devicename = findViewById(R.id.et_devicename);

        String brand = android.os.Build.BRAND;
        String model = android.os.Build.MODEL;
        et_devicename.setText(brand+" "+model);
    }
    private void initData() {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(Constants.BASE_URL)
                .build();
        bindService = retrofit.create(BindService.class);
        EventBus.getDefault().register(this);
    }

    private void setListener() {
        btn_bind.setOnClickListener(v -> {
            if(et_username.getText().toString().isEmpty()){
                textInputLayout_username.setError("openId不能为空！");
                return;
            }
            if(et_devicename.getText().toString().isEmpty()){
                textInputLayout_devicename.setError("设备名称不能为空！");
                return;
            }
            bindUser();
        });
        ib_scan.setOnClickListener(v -> {
            //扫描二维码
            IntentIntegrator intentIntegrator = new IntentIntegrator(BindActivity.this);
            intentIntegrator.initiateScan();
        });
        ib_close.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 获取解析结果
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                et_username.setText(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Subscribe
    public void onEvent(Intent data){
        //有些手机使用这种方式隐藏图标会将service杀死
//        // 将app图标隐藏：
//        PackageManager p = getPackageManager();
//        p.setComponentEnabledSetting(getComponentName(),
//                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//                PackageManager.DONT_KILL_APP);
    }

    private void bindUser(){
        bindService.bindUser(et_username.getText().toString(),et_devicename.getText().toString())
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<GsonBindUserBean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(BindActivity.this,"服务器错误",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(GsonBindUserBean gsonBindUserBean) {
                        switch (gsonBindUserBean.getCode()){
                            case 0:
                                Toast.makeText(BindActivity.this,"绑定成功",Toast.LENGTH_SHORT).show();
                                sp.edit().putString("user_name",gsonBindUserBean.getUser_name()).apply();
                                sp.edit().putInt("deviceId",gsonBindUserBean.getDeviceId()).apply();
                                //第一次进入应用
                                sp.edit().putBoolean("fristTime",false).apply();
                                Intent intent = new Intent(BindActivity.this,MainActivity.class);
                                startActivity(intent);
                                break;
                            case 1:
                                showAlreadyBindDialog();
                                break;
                            case -1:
                                Toast.makeText(BindActivity.this,"绑定失败，请稍后重试",Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
    }

    private void selectDevice(){
        bindService.selectDevice(et_username.getText().toString())
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<GsonGetDeviceListBean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(BindActivity.this,"服务器错误",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(GsonGetDeviceListBean gsonGetDeviceListBean) {
                        switch (gsonGetDeviceListBean.getCode()){
                            case 0:
                                ArrayList<Device> devices = gsonGetDeviceListBean.getDeviceList();
                                showDeviceListDialog(devices);
                                break;
                            case -1:
                                Toast.makeText(BindActivity.this,"获取设备列表失败",Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
    }

    private void addDevice(){
        bindService.addDevice(et_username.getText().toString(),et_devicename.getText().toString())
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<GsonAddDeviceBean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(BindActivity.this,"服务器错误",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(GsonAddDeviceBean gsonAddDeviceBean) {
                        switch (gsonAddDeviceBean.getCode()){
                            case 0:
                                Toast.makeText(BindActivity.this,"设备添加成功",Toast.LENGTH_SHORT).show();
                                sp.edit().putString("user_name",gsonAddDeviceBean.getUser_name()).apply();
                                sp.edit().putInt("deviceId",gsonAddDeviceBean.getDeviceId()).apply();
                                //第一次进入应用
                                sp.edit().putBoolean("fristTime",false).apply();
                                Intent intent = new Intent(BindActivity.this,MainActivity.class);
                                startActivity(intent);
                                break;
                            case 1:
                                showDeviceNameRepeatDialog();
                                break;
                            case -1:
                                Toast.makeText(BindActivity.this,"设备添加失败",Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
    }

    private void showBindDialog(){
        new AlertDialog.Builder(this).setTitle("绑定openId").setMessage("您要绑定的openId为"+et_username.getText().toString()+"。一旦绑定，无法更改。确定要使用该openid进行绑定吗？")
                .setPositiveButton("绑定", (dialog, which) -> bindUser()).setNegativeButton("重新输入", null).show();
    }

    private void showAlreadyBindDialog(){
        new AlertDialog.Builder(this).setTitle("该openId已绑定").setMessage(et_username.getText().toString()+"已经绑定过，您可以选择添加新设备 "+et_devicename.getText().toString()+" ，或者覆盖以前的设备。")
                .setPositiveButton("添加新设备", (dialog, which) -> addDevice()).setNegativeButton("覆盖设备", (dialog, which) -> selectDevice()).show();
    }

    private void showDeviceListDialog(ArrayList<Device> devices){
        final Device[] selectedDevice = new Device[1];
        String[] singleChoiceItems = new String[devices.size()];
        for(int i=0;i<singleChoiceItems.length;i++){
            singleChoiceItems[i] = devices.get(i).getDeviceName();
        }

        int itemSelected = 0;
        selectedDevice[0] = devices.get(itemSelected);
        new AlertDialog.Builder(this)
                .setTitle("覆盖已有设备")
                .setSingleChoiceItems(singleChoiceItems, itemSelected, (dialog, which) -> selectedDevice[0] = devices.get(which))
                .setPositiveButton("确定", (dialog, which) -> {
                    sp.edit().putString("user_name",et_username.getText().toString()).apply();
                    sp.edit().putInt("deviceId", selectedDevice[0].getDeviceId()).apply();
                    //第一次进入应用
                    sp.edit().putBoolean("fristTime",false).apply();
                    Intent intent = new Intent(BindActivity.this,MainActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDeviceNameRepeatDialog(){
        new AlertDialog.Builder(this).setTitle("设备名称重复").setMessage("该设备已存在，请修改设备名称。")
                .setPositiveButton("确定", (dialog, which) -> {
                    et_devicename.requestFocus();
                }).show();
    }



    interface BindService{
        @POST("bindUser")
        Observable<GsonBindUserBean> bindUser(@Query("user_name") String user_name, @Query("device_name")String device_name);
        @POST("selectDevice")
        Observable<GsonGetDeviceListBean> selectDevice(@Query("user_name")String user_name);
        @POST("addDevice")
        Observable<GsonAddDeviceBean> addDevice(@Query("user_name")String user_name, @Query("device_name")String device_name);
    }
}
