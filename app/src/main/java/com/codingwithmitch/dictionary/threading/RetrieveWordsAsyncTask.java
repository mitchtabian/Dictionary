package com.codingwithmitch.dictionary.threading;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;

import java.util.ArrayList;

public class RetrieveWordsAsyncTask extends AsyncTask<Void, Void, ArrayList<Word>> {

    private static final String TAG = "RetrieveWordsAsyncTask";

    private AppDatabase mDb;
    private String mQuery;

    public RetrieveWordsAsyncTask(Context context, String query) {
        super();
        mDb = AppDatabase.getDatabase(context);
        mQuery = query;
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
        // Executed on UI Thread
    }

    private ArrayList<Word> retrieveWordsAsync(){
        Log.d(TAG, "retrieveWordsAsync: retrieving words. This is from thread: " + Thread.currentThread().getName());
        return new ArrayList<>(mDb.wordDataDao().getWords(mQuery));
    }

}













