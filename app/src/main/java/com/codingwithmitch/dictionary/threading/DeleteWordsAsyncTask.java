package com.codingwithmitch.dictionary.threading;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.codingwithmitch.dictionary.ActivityUpdater;
import com.codingwithmitch.dictionary.DictionaryActivity;
import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class DeleteWordsAsyncTask extends AsyncTask<Word, Integer, int[]> {

    private static final String TAG = "DeleteWordAsyncTask";

    private WeakReference<ActivityUpdater> activityUpdater;
    private AppDatabase db;

    public DeleteWordsAsyncTask(Application application, ActivityUpdater activityUpdater) {
        super();
        this.activityUpdater = new WeakReference<>(activityUpdater);
        db = AppDatabase.getDatabase(application);
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Executed on UI Thread
    }

    @Override
    protected int[] doInBackground(Word... words) {

        return deleteWordsAsync(words);
    }

    @Override
    protected void onProgressUpdate(Integer... progressValues) {
        super.onProgressUpdate(progressValues);
        // Executed on UI Thread

        activityUpdater.get().progressUpdate(progressValues[0], progressValues[1]);
    }

    @Override
    protected void onPostExecute(int[] rows) {
        super.onPostExecute(rows);
        activityUpdater.get().deletedWords(rows);
    }

    private int[] deleteWordsAsync(Word... words){
        Log.d(TAG, "deleteWordAsync: deleting words. This is from thread: " + Thread.currentThread().getName());

        int[] rows = new int[words.length];
        for(int i = 0; i < rows.length; i++){
            Integer[] progressUpdate = {i + 1, rows.length};
            onProgressUpdate(progressUpdate);
            rows[i] = db.wordDataDao().delete(words[i]);
        }

        return rows;
    }

}