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

import java.util.ArrayList;


public class DeleteWordRunnable implements Runnable {

    private static final String TAG = "RetrieveWordsRunnable";

    private Handler mMainThreadHandler;
    private AppDatabase mDb;
    private Word mWord;

    public DeleteWordRunnable(Context context, Handler mainThreadHandler, Word word) {
        mMainThreadHandler = mainThreadHandler;
        mDb = AppDatabase.getDatabase(context);
        mWord = word;
    }

    @Override
    public void run() {
        Log.d(TAG, "run: retrieving words. This is from thread: " + Looper.myLooper().getThread().getName());
        Message message = null;
        if (mDb.wordDataDao().delete(mWord) > 0) {
            message = Message.obtain(null, Constants.WORD_DELETE_SUCCESS);
        } else {
            message = Message.obtain(null, Constants.WORD_DELETE_FAIL);
        }

        mMainThreadHandler.sendMessage(message);
    }
}










