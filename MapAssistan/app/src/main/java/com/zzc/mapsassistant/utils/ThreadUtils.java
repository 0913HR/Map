package com.zzc.mapsassistant.utils;

import android.os.Handler;

public class ThreadUtils {
    //启动一个普通进程
    public static void runInThread(Runnable task){
        new Thread(task).start();
    }

    public static Handler handler=new Handler();
    public static void RunInUIThread(Runnable task){
        handler.post(task);
    }
}
