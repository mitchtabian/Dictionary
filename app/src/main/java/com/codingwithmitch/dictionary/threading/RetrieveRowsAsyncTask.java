package com.codingwithmitch.dictionary.threading;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RetrieveRowsAsyncTask extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = "RetrieveRowsAsyncTask";

    private AppDatabase mDb;
    private WeakReference<TaskDelegate> mDelegate;

    public RetrieveRowsAsyncTask(Context context, TaskDelegate delegate) {
        super();
        mDb = AppDatabase.getDatabase(context);
        mDelegate = new WeakReference<>(delegate);
    }

    @Override
    protected Integer doInBackground(Void... voids) {

        // Done on background thread

        return retrieveRowsAsync();
    }

    @Override
    protected void onPostExecute(Integer rows) {
        super.onPostExecute(rows);
        // Executed on UI Thread

        mDelegate.get().onRowsRetrieved(rows);
    }

    private Integer retrieveRowsAsync(){
        Log.d(TAG, "retrieveWordsAsync: retrieving rows. This is from thread: " + Thread.currentThread().getName());
        return mDb.wordDataDao().getNumRows();
    }

}













