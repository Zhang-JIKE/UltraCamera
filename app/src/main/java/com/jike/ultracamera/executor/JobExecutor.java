package com.jike.ultracamera.executor;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jike.ultracamera.utils.Utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JobExecutor {

    private static final String TAG = Utils.getClassName(JobExecutor.class);

    private ThreadPoolExecutor mExecutor;

    private Handler mHandler;

    public JobExecutor(){
        mExecutor = new ThreadPoolExecutor(1, 4, 10,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(4),
                new ThreadPoolExecutor.DiscardOldestPolicy());

        mHandler = new Handler(Looper.getMainLooper());
    }

    public Handler getHandler() {
        return mHandler;
    }

    public ThreadPoolExecutor getExecutor() {
        return mExecutor;
    }
}
