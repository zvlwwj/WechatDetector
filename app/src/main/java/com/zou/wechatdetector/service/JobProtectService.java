package com.zou.wechatdetector.service;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.zou.wechatdetector.activity.MainActivity;
import com.zou.wechatdetector.utils.Tools;

/**
 * Created by zou on 2018/4/26.
 * 守护进程
 */

public class JobProtectService extends JobService {

    private static final String TAG = "ProtectService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }

    //隔一段时间检查进程是否在运行，如果没有运行，则重启activity
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG,"onStartJob "+Tools.getTimeStamp());
        boolean running = Tools.isServiceRun(this,"com.zou.wechatdetector.service.MainService");
//        boolean running=Tools.isExistActivity(this,MainActivity.class);
        Log.i(TAG,"service running = "+running);
//        Toast.makeText(this,"onStartJob"+Tools.getTimeStamp()+" running = "+running,Toast.LENGTH_SHORT).show();
        if(!running){
            Log.i(TAG,"startActivity");
//            Toast.makeText(this,"startActivity",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }else{
//            Toast.makeText(this,"noStartActivity",Toast.LENGTH_SHORT).show();
        }
//        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
//            scheduleRefresh();
//            jobFinished(params, false);
//            return true;
//        }else {
            jobFinished(params, false);
//        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void scheduleRefresh() {
        JobScheduler mJobScheduler = (JobScheduler)getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        //jobId可根据实际情况设定
        JobInfo.Builder mJobBuilder =
                new JobInfo.Builder(0,
                        new ComponentName(getPackageName(),
                                JobProtectService.class.getName()));

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mJobBuilder.setMinimumLatency(10 * 1000).setPersisted(true).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        }

        if (mJobScheduler != null && mJobScheduler.schedule(mJobBuilder.build())
                <= JobScheduler.RESULT_FAILURE) {
            //Scheduled Failed/LOG or run fail safe measures
            Log.d("JobSchedulerService", "7.0 Unable to schedule the service FAILURE!");
        }else{
            Log.d("JobSchedulerService", "7.0 schedule the service SUCCESS!");
        }
    }
}
