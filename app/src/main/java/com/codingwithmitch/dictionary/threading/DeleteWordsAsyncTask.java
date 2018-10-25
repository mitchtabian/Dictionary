package com.codingwithmitch.dictionary.threading;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.codingwithmitch.dictionary.ActivityUpdater;
import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;

import java.util.ArrayList;

public class DeleteWordsAsyncTask extends AsyncTask<Word, Void, int[]> {

    private static final String TAG = "DeleteWordAsyncTask";

    private ActivityUpdater activityUpdater;
    private AppDatabase db;

    public DeleteWordsAsyncTask(Application application, ActivityUpdater activityUpdater) {
        super();
        this.activityUpdater = activityUpdater;
        db = AppDatabase.getDatabase(application);
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Executed on UI Thread
    }

    @Override
    protected int[] doInBackground(Word... words) {

        // Done on background thread
        return deleteWordsAsync(words);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        // Executed on UI Thread
    }

    @Override
    protected void onPostExecute(int[] rows) {
        super.onPostExecute(rows);
        activityUpdater.deletedWords(rows);
    }

    private int[] deleteWordsAsync(Word... words){
        Log.d(TAG, "deleteWordAsync: deleting words. This is from thread: " + Thread.currentThread().getName());

        int[] rows = new int[words.length];
        for(int i = 0; i < rows.length; i++){
            rows[i] = db.wordDataDao().delete(words[i]);
        }
        return rows;
    }

}