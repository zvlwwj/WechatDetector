package com.zou.wechatdetector.service;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.zou.wechatdetector.utils.Tools;

/**
 * Created by zou on 2018/4/26.
 * 守护进程
 */

public class JobProtectService extends JobService {

    private static final String TAG = "ProtectService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {

//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
        boolean running = Tools.isProessRunning(this,"com.zou.wechatdetector");
        Toast.makeText(this,"doing job time :"+Tools.getTimeStamp(),Toast.LENGTH_SHORT).show();
        Log.i(TAG,"doing job time :"+Tools.getTimeStamp());
        Log.i(TAG,"process running :"+running);
        jobFinished(params, false);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
//        jobScheduler.cancelAll();
//        JobInfo.Builder builder = new JobInfo.Builder(1024, new ComponentName(getPackageName(), JobProtectService.class.getName()));
//        builder.setPeriodic(15*60*1000);
//        builder.setPersisted(true);
//        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
//        int schedule = jobScheduler.schedule(builder.build());
//        if (schedule <= 0) {
//            Log.w(TAG, "schedule error！");
//        }
    }
}
