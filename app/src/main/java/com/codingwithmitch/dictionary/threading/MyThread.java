package com.codingwithmitch.dictionary.threading;

import android.os.Handler;
import android.os.Looper;

public class MyThread extends Thread {

    private Handler mMyThreadHandler = null;

    @Override
    public void run() {
        Looper.prepare();
        mMyThreadHandler = new Handler(Looper.myLooper());
        Looper.loop();
    }
}
