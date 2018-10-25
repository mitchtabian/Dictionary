package com.codingwithmitch.dictionary.threading;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;
import com.codingwithmitch.dictionary.util.Constants;
import com.codingwithmitch.dictionary.util.Utility;

import java.util.ArrayList;
import java.util.List;


public class MyThread extends Thread {

    private static final String TAG = "MyThread";

    private MyThreadHandler mMyThreadHandler = null;
    private Handler mMainThreadHandler = null;
    private boolean isRunning = false;
    private AppDatabase mDb;


    public MyThread(Context context, Handler mMainThreadHandler) {
        this.mMainThreadHandler = mMainThreadHandler;
        isRunning = true;
        mDb = AppDatabase.getDatabase(context); // will not cause memory leak because 'getApplicationContext()'
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

    private long[] saveNewWord(Word word){
        long[] returnValue = mDb.wordDataDao().insertWords(word);
        if(returnValue.length > 0){
            Log.d(TAG, "saveNewWord: return value: " + returnValue.toString());
        }
        return returnValue;
    }

    private List<Word> retrieveWords(){
        return mDb.wordDataDao().getAllWords();
    }

    private int updateWord(Word word){
        return mDb.wordDataDao().updateWord(word.getTitle(), word.getContent(), Utility.getCurrentTimeStamp(), word.getUid());
    }

    private int deleteWord(Word word){
        return mDb.wordDataDao().delete(word);
    }


    private void insertTestWords(){
        for(int i = 0; i < 5000; i++){
            saveNewWord(new Word("word " + i, "some random text", Utility.getCurrentTimeStamp()));
        }
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
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
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
                    Word word = msg.getData().getParcelable("word_new");
                    Message message = null;
                    if(saveNewWord(word).length > 0){
                        message = Message.obtain(null, Constants.WORD_INSERT_SUCCESS);
                    }
                    else{
                        message = Message.obtain(null, Constants.WORD_INSERT_FAIL);
                    }
                    mMainThreadHandler.sendMessage(message);

                    break;
                }

                case Constants.WORD_UPDATE:{
                    Log.d(TAG, "handleMessage: updating word on thread: " + Thread.currentThread().getName());
                    Word word = msg.getData().getParcelable("word_update");
                    Message message = null;
                    int updateInt = updateWord(word);
                    Log.d(TAG, "handleMessage: update int: " + updateInt);
                    if(updateInt > 0){
                        message = Message.obtain(null, Constants.WORD_UPDATE_SUCCESS);
                    }
                    else{
                        message = Message.obtain(null, Constants.WORD_UPDATE_FAIL);
                    }
                    mMainThreadHandler.sendMessage(message);
                    break;
                }

                case Constants.WORDS_RETRIEVE:{
                    Log.d(TAG, "handleMessage: retrieving words on thread: " + Thread.currentThread().getName());
                    List<Word> tempWords = new ArrayList<>(retrieveWords());
                    ArrayList<Word> notes = new ArrayList<>(tempWords);
                    Message message = null;
                    if(notes.size() > 0){
                        message = Message.obtain(null, Constants.WORDS_RETRIEVE_SUCCESS);
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList("words_retrieve", notes);
                        message.setData(bundle);
                    }
                    else{
                        message = Message.obtain(null, Constants.WORDS_RETRIEVE_FAIL);
                    }

                    mMainThreadHandler.sendMessage(message);

                    break;
                }

                case Constants.WORD_DELETE:{
                    Log.d(TAG, "handleMessage: deleting word on thread: " + Thread.currentThread().getName());
                    Word word = msg.getData().getParcelable("word_delete");
                    Message message = null;
                    if(deleteWord(word) > 0){
                        message = Message.obtain(null, Constants.WORD_DELETE_SUCCESS);
                    }
                    else{
                        message = Message.obtain(null, Constants.WORD_DELETE_FAIL);
                    }

                    mMainThreadHandler.sendMessage(message);

                    break;
                }
            }
        }
    }
}
