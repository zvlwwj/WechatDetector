package com.zou.wechatdetector.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by zou on 2018/7/5.
 */

public class MobileInfoUtils {
    /**
     * Get Mobile Type
     *
     * @return
     */
    private static String getMobileType() {
        return Build.MANUFACTURER;
    }

    /**
     * 跳转到自启动界面
     * @param context
     */
    public static void jumpStartInterface(Activity context,int requestCode) {
        Intent intent = new Intent();
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.e("HLQ_Struggle", "******************当前手机型号为：" + getMobileType());
            ComponentName componentName = null;
            if (getMobileType().equals("Xiaomi")) {
                componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
            } else if (getMobileType().equals("Letv")) {
                intent.setAction("com.letv.android.permissionautoboot");
            } else if (getMobileType().equals("HUAWEI")) {
                if(Build.VERSION.SDK_INT >= 26) {
                    componentName = ComponentName.unflattenFromString("com.huawei.systemmanager/.appcontrol.activity.StartupAppControlActivity");
                }else{
                    componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
                }
            } else if (getMobileType().equals("vivo")) {
                componentName = ComponentName.unflattenFromString("com.iqoo.secure/.safeguard.PurviewTabActivity");
            } else if (getMobileType().equals("Meizu")) {
                componentName = ComponentName.unflattenFromString("com.meizu.safe/.permission.PermissionMainActivity");
            } else if (getMobileType().equals("OPPO")) {
                componentName = ComponentName.unflattenFromString("com.oppo.safe/.permission.startup.StartupAppListActivity");
            } else if (getMobileType().equals("ulong")) {
                componentName = new ComponentName("com.yulong.android.coolsafe", ".ui.activity.autorun.AutoRunListActivity");
            } else {
//                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
//                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                Toast.makeText(context,"该机型不需要设置",Toast.LENGTH_SHORT).show();
                return;
            }
            intent.setComponent(componentName);
            context.startActivityForResult(intent,requestCode);
        } catch (Exception e) {//抛出异常就直接打开设置页面
            intent = new Intent(Settings.ACTION_SETTINGS);
            context.startActivityForResult(intent,requestCode);
        }
    }


    /**
     * 跳转到电池管理界面
     * @param context
     */
    public static void batteryManagerInterface(Activity context) {
        Intent intent = new Intent();
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.e("HLQ_Struggle", "******************当前手机型号为：" + getMobileType());
            ComponentName componentName = null;
            if (getMobileType().equals("HUAWEI")) {
                if(Build.VERSION.SDK_INT >= 26) {
                    componentName = ComponentName.unflattenFromString("com.android.settings/.Settings$HighPowerApplicationsActivity");
                }
            }else if(getMobileType().equals("Xiaomi")){
                componentName = ComponentName.unflattenFromString("com.miui.powerkeeper/.ui.HiddenAppsContainerManagementActivity");
            }
            intent.setComponent(componentName);
            context.startActivity(intent);
        } catch (Exception e) {//抛出异常就直接打开设置页面
            intent = new Intent(Settings.ACTION_SETTINGS);
            context.startActivity(intent);
        }
    }

    /**
     * 跳转到应用管理界面
     * @param context
     */
    public static void appManagerInterface(Activity context) {
        Intent intent = new Intent();
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.e("HLQ_Struggle", "******************当前手机型号为：" + getMobileType());
            ComponentName componentName = null;
            if (getMobileType().equals("smartisan")) {
                componentName = ComponentName.unflattenFromString("com.android.settings/.applications.ManageApplicationsActivity");
            }
            intent.setComponent(componentName);
            context.startActivity(intent);
        } catch (Exception e) {//抛出异常就直接打开设置页面
            intent = new Intent(Settings.ACTION_SETTINGS);
            context.startActivity(intent);
        }
    }
}
