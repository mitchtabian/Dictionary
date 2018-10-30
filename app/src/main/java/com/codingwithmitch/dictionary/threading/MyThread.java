package com.codingwithmitch.dictionary.threading;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.codingwithmitch.dictionary.util.Constants;


public class MyThread extends Thread {

    private static final String TAG = "MyThread";

    private Handler mMyThreadHandler = null;

    @Override
    public void run() {
        Looper.prepare();
        mMyThreadHandler = new Handler(Looper.myLooper());
        Looper.loop();
    }


}
