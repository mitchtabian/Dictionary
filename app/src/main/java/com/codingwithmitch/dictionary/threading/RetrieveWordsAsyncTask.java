package com.codingwithmitch.dictionary.threading;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;

import java.util.ArrayList;

public class RetrieveWordsAsyncTask extends AsyncTask<String, Void, ArrayList<Word>> {

    private static final String TAG = "RetrieveWordsAsyncTask";

    private AppDatabase mDb;

    public RetrieveWordsAsyncTask(Context context) {
        super();
        mDb = AppDatabase.getDatabase(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Executed on UI Thread
    }

    @Override
    protected ArrayList<Word> doInBackground(String... strings) {

        // Done on background thread
        return retrieveWordsAsync(strings[0]);
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

    private ArrayList<Word> retrieveWordsAsync(String query){
        Log.d(TAG, "retrieveWordsAsync: retrieving words. This is from thread: " + Thread.currentThread().getName());
        return new ArrayList<>(mDb.wordDataDao().getWords(query));
    }

}













