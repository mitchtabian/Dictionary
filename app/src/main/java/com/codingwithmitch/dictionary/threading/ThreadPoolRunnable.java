package com.codingwithmitch.dictionary.threading;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;
import com.codingwithmitch.dictionary.util.Constants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ThreadPoolRunnable implements Runnable {

    private static final String TAG = "ThreadPoolRunnable";

    private int mStartingIndex;
    private int mChunkSize;
    private AppDatabase mDb;
    private WeakReference<Handler> mMainThreadHandler;

    public ThreadPoolRunnable(Context context, Handler mainThreadHandler, int startingIndex, int chunkSize ) {
        mDb = AppDatabase.getDatabase(context);
        mChunkSize = chunkSize;
        mStartingIndex = startingIndex;
        mMainThreadHandler = new WeakReference<>(mainThreadHandler);
    }

    @Override
    public void run() {
        Log.d(TAG, "retrieveSomeWords: retrieving some words. This is from thread: " + Thread.currentThread().getName());
        ArrayList<Word> words = new ArrayList<>(mDb.wordDataDao().getSomeWords(mStartingIndex, mChunkSize));
        Message message = Message.obtain(null, Constants.MSG_THREAD_POOL_TASK_COMPLETE);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("word_data_from_thread_pool", words);
        message.setData(bundle);
        mMainThreadHandler.get().sendMessage(message);
    }
}














