package com.codingwithmitch.dictionary.threading;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;

import java.util.ArrayList;

public class DeleteWordAsyncTask extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = "DeleteWordAsyncTask";

    private AppDatabase mDb;
    private Word mWord;

    public DeleteWordAsyncTask(Context context, Word word) {
        super();
        mDb = AppDatabase.getDatabase(context);
        mWord = word;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Executed on UI Thread
    }

    @Override
    protected Integer doInBackground(Void... voids) {

        // Done on background thread

        return deleteWord();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        // Executed on UI Thread
    }

    @Override
    protected void onPostExecute(Integer value) {
        super.onPostExecute(value);
        // Executed on UI Thread
    }

    private Integer deleteWord(){
        Log.d(TAG, "deleteWord: deleting word. This is from thread: " + Thread.currentThread().getName());
        return mDb.wordDataDao().delete(mWord);
    }

}













