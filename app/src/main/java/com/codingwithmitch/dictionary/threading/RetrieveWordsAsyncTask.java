package com.codingwithmitch.dictionary.threading;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.codingwithmitch.dictionary.ActivityUpdater;
import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RetrieveWordsAsyncTask extends AsyncTask<Void, Void, ArrayList<Word>> {

    private static final String TAG = "RetrieveWordsAsyncTask";

    private WeakReference<ActivityUpdater> activityUpdater;
    private AppDatabase db;

    public RetrieveWordsAsyncTask(Application application, ActivityUpdater activityUpdater) {
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
    protected ArrayList<Word> doInBackground(Void... voids) {

        // Done on background thread
        return retrieveWordsAsync();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        // Executed on UI Thread
    }

    @Override
    protected void onPostExecute(ArrayList<Word> words) {
        super.onPostExecute(words);
        activityUpdater.get().gotWords(words);
    }

    private ArrayList<Word> retrieveWordsAsync(){
        Log.d(TAG, "retrieveWordsAsync: retrieving words. This is from thread: " + Thread.currentThread().getName());
        return new ArrayList<>(db.wordDataDao().getAllWords());
    }


}














