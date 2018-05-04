package com.zou.wechatdetector.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.zou.wechatdetector.R;
import com.zou.wechatdetector.bean.Device;
import com.zou.wechatdetector.bean.GsonAddDeviceBean;
import com.zou.wechatdetector.bean.GsonBindUserBean;
import com.zou.wechatdetector.bean.GsonGetDeviceListBean;
import com.zou.wechatdetector.utils.Constants;

import java.util.ArrayList;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by zou on 2018/5/3.
 */

public class BindActivity extends Activity{
    private SharedPreferences sp;
    private EditText et_username,et_devicename;
    private Button btn_bind;
    private TextInputLayout textInputLayout_username,textInputLayout_devicename;
    private BindService bindService;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind);
        initData();
        initView();

        btn_bind.setOnClickListener(v -> {
            if(et_username.getText().toString().isEmpty()){
                textInputLayout_username.setError("微信号不能为空！");
                return;
            }
            if(et_devicename.getText().toString().isEmpty()){
                textInputLayout_devicename.setError("设备名称不能为空！");
                return;
            }
            showBindDialog();
        });

        sp = getSharedPreferences("detector",0);
//        if(sp.getBoolean("fristTime",true)){
            //第一次进入应用
            sp.edit().putBoolean("fristTime",false).apply();
//            new AlertDialog.Builder(this).setView(R.layout.dialog_bind).setCancelable(false).setTitle("绑定微信号").setMessage("绑定后，您可以使用该微信号关注公众号，来进行监控。").setPositiveButton("绑定", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    Intent intent = new Intent(BindActivity.this,MainActivity.class);
//                    startActivity(intent);
//                }
//            }).show();
//        }else {
//            Intent intent = new Intent(BindActivity.this,MainActivity.class);
//            startActivity(intent);
//        }
    }

    private void initView() {
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
        new AlertDialog.Builder(this).setTitle("绑定微信号").setMessage("您要绑定的微信号为"+et_username.getText().toString()+"。绑定后，您可以使用该微信号关注公众号，来进行监控。确定要使用该微信号进行绑定吗？")
                .setPositiveButton("绑定", (dialog, which) -> bindUser()).setNegativeButton("重新输入", null).show();
    }

    private void showAlreadyBindDialog(){
        new AlertDialog.Builder(this).setTitle("该微信号已绑定").setMessage("微信号"+et_username.getText().toString()+"已经绑定过，您可以选择添加新设备 "+et_devicename.getText().toString()+" ，或者覆盖以前的设备。")
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
        Observable<GsonBindUserBean> bindUser(@Query("user_name")String user_name, @Query("device_name")String device_name);
        @POST("selectDevice")
        Observable<GsonGetDeviceListBean> selectDevice(@Query("user_name")String user_name);
        @POST("addDevice")
        Observable<GsonAddDeviceBean> addDevice(@Query("user_name")String user_name, @Query("device_name")String device_name);
    }
}
