package com.codingwithmitch.dictionary.threading;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.util.Constants;

public class MyThread extends Thread {

    private static final String TAG = "MyThread";

    private MyThreadHandler mMyThreadHandler = null;
    private Handler mMainThreadHandler = null;
    private boolean isRunning = false;

    public MyThread(Handler mMainThreadHandler) {
        this.mMainThreadHandler = mMainThreadHandler;
        isRunning = true;
    }

    @Override
    public void run() {
        if(isRunning){
            Looper.prepare();
            mMyThreadHandler = new MyThreadHandler(Looper.myLooper());
            Looper.loop();
        }
    }

    public void quitThread(){
        isRunning = false;
        mMainThreadHandler = null;
    }

    public void sendMessageToBackgroundThread(Message message){
        while(true){
            try{
                mMyThreadHandler.sendMessage(message);
                break;
            }catch (NullPointerException e){
                Log.e(TAG, "sendMessageToBackgroundThread: null pointer: " + e.getMessage() );
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    class MyThreadHandler extends Handler {

        public MyThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){

                case Constants.WORD_INSERT_NEW:{
                    Log.d(TAG, "handleMessage: saving word on thread: " + Thread.currentThread().getName());

//                    Message message = Message.obtain(mMainThreadHandler, Constants.WORD_INSERT_SUCCESS);
//                    message.sendToTarget();

                    Message message = Message.obtain(null, Constants.WORD_INSERT_SUCCESS);
                    mMainThreadHandler.sendMessage(message);

                    break;
                }

                case Constants.WORD_UPDATE:{
                    Log.d(TAG, "handleMessage: updating word on thread: " + Thread.currentThread().getName());


                    break;
                }

                case Constants.WORDS_RETRIEVE:{
                    Log.d(TAG, "handleMessage: retrieving words on thread: " + Thread.currentThread().getName());


                    break;
                }

                case Constants.WORD_DELETE:{
                    Log.d(TAG, "handleMessage: deleting word on thread: " + Thread.currentThread().getName());


                    break;
                }

            }
        }
    }
}
