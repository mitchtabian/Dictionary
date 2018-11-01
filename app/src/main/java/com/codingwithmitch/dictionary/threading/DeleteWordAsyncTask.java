package com.codingwithmitch.dictionary.threading;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;

import java.util.ArrayList;

public class DeleteWordAsyncTask extends AsyncTask<Word, Void, Integer> {

    private static final String TAG = "DeleteWordAsyncTask";

    private AppDatabase mDb;

    public DeleteWordAsyncTask(Context context) {
        super();
        mDb = AppDatabase.getDatabase(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Executed on UI Thread
    }

    @Override
    protected Integer doInBackground(Word... words) {

        // Done on background thread

        return deleteWord(words[0]);
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

    private Integer deleteWord(Word word){
        Log.d(TAG, "deleteWord: deleting word. This is from thread: " + Thread.currentThread().getName());
        return mDb.wordDataDao().delete(word);
    }

}













