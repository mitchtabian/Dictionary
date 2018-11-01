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


public class DeleteWordRunnable implements Runnable {

    private static final String TAG = "DeleteWordRunnable";

    private WeakReference<Handler> mMainThreadHandler;
    private AppDatabase mDb;
    private Word mWord;


    public DeleteWordRunnable(Context context, Handler mMainThreadHandler, Word word) {
        this.mMainThreadHandler = new WeakReference<>(mMainThreadHandler);
        this.mWord = word;
        mDb = AppDatabase.getDatabase(context);
    }

    @Override
    public void run() {
        Log.d(TAG, "run: deleting words. This is from thread: " + Thread.currentThread().getName());
        ArrayList<Word> words = new ArrayList<>(mDb.wordDataDao().delete(mWord));
        Message message = null;
        if (words.size() > 0) {
            message = Message.obtain(null, Constants.WORD_DELETE_SUCCESS);
        } else {
            message = Message.obtain(null, Constants.WORD_DELETE_FAIL);
        }

        mMainThreadHandler.get().sendMessage(message);
    }
}
