package com.codingwithmitch.dictionary;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


import com.codingwithmitch.dictionary.adapters.WordsRecyclerAdapter;
import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;
import com.codingwithmitch.dictionary.threading.DeleteWordsAsyncTask;
import com.codingwithmitch.dictionary.threading.RetrieveWordsAsyncTask;
import com.codingwithmitch.dictionary.util.Constants;
import com.codingwithmitch.dictionary.util.VerticalSpacingItemDecorator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DictionaryActivity extends AppCompatActivity implements
        WordsRecyclerAdapter.OnWordListener,
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        ActivityUpdater,
        Handler.Callback
{

    private static final String TAG = "WordsListActivity";

    //ui components
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefresh;

    //vars
    private ArrayList<Word> mWords = new ArrayList<>();
    private WordsRecyclerAdapter mWordRecyclerAdapter;
    private FloatingActionButton mFab;
    private String mSearchQuery = "";
    private ExecutorService mExecutorService = null;
    private Handler mMainUIHandler = null;
    private long mStartTime;
    private int mNumRows;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restoreInstanceState(savedInstanceState);

        setContentView(R.layout.activity_dictionary);
        mRecyclerView = findViewById(R.id.recyclerView);
        mFab = findViewById(R.id.fab);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);

        mFab.setOnClickListener(this);
        mSwipeRefresh.setOnRefreshListener(this);

        mMainUIHandler = new Handler(this);

        setupRecyclerView();
        initExecutorThreadPool();
    }

    private void initExecutorThreadPool(){
        int numProcessors = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "initExecutorThreadPool: processors: " + numProcessors);
        mExecutorService = Executors.newFixedThreadPool(numProcessors);
    }

    private void restoreInstanceState(Bundle savedInstanceState){
        if(savedInstanceState != null){
            mWords = savedInstanceState.getParcelableArrayList("words");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("words", mWords);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void gotWords(ArrayList<Word> words) {
//        String time = String.valueOf(System.currentTimeMillis() - mStartTime) + " ms";
//        Log.d(TAG, "handleMessage: TIME ELAPSED: " + time);
    }

    @Override
    public void deletedWords(int[] rows) {
        for(int row: rows){
            Log.d(TAG, "deletedWords: row: " + row);
        }
    }

    @Override
    public void progressUpdate(int completedAmount, int totalAmount) {
        String percentageCompleted = String.valueOf(((double)completedAmount / (double)totalAmount) * 100);
        Log.d(TAG, "progressUpdate: " + percentageCompleted + "%");
    }


    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: called.");
        super.onStart();
        if(mWords.size() == 0){
            retrieveWords();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called.");
    }


    @SuppressLint("StaticFieldLeak")
    private void retrieveWords() {
        Log.d(TAG, "retrieveWords: called.");

        mStartTime = System.currentTimeMillis();

        new AsyncTask<Void, Void, Integer>(){

            @Override
            protected Integer doInBackground(Void... voids) {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());

                return db.wordDataDao().getNumRows();
            }

            @Override
            protected void onPostExecute(Integer numRows) {
                super.onPostExecute(numRows);

                mNumRows = numRows;

                Log.d(TAG, "onPostExecute: num rows: " + mNumRows);

                int numTasks = Runtime.getRuntime().availableProcessors();
                for(int i = 0; i <= numTasks; i++){
                    Log.d(TAG, "Starting query at: row#: " + (numRows / numTasks)*i);
                    DatabaseQueryRunnable runnable = new DatabaseQueryRunnable( (numRows / numTasks)*i, (numRows / numTasks));
                    mExecutorService.submit(runnable);
                }
            }

        }.execute();

//        new RetrieveWordsAsyncTask(getApplication(), this).execute();
    }


    private class DatabaseQueryRunnable implements Runnable{

        private int mStartingIndex;
        private int mChunkSize;

        public DatabaseQueryRunnable(int startingIndex, int chunkSize) {
            mStartingIndex = startingIndex;
            mChunkSize = chunkSize;
        }

        @Override
        public void run() {
            retrieveSomeWords(mStartingIndex, mChunkSize);
        }

        private void retrieveSomeWords(int startingIndex, int chunkSize){
            Log.d(TAG, "retrieveSomeWords: retrieving some notes. This is from thread: " + Thread.currentThread().getName());
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            ArrayList<Word> words = new ArrayList<>(db.wordDataDao().getSomeWords(startingIndex, chunkSize));
            Message message = Message.obtain(null, Constants.MSG_THREAD_POOL_TASK_COMPLETE);
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("word_data_from_thread_pool", words);
            message.setData(bundle);
            mMainUIHandler.sendMessage(message);
        }
    }



    public void deleteWord(Word word) {
        Log.d(TAG, "deleteWord: called.");
        mWords.remove(word);
        mWordRecyclerAdapter.getFilteredWords().remove(word);
        mWordRecyclerAdapter.notifyDataSetChanged();

        new DeleteWordsAsyncTask(getApplication(), this).execute(word);
    }



    private void setupRecyclerView(){
        Log.d(TAG, "setupRecyclerView: called.");
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        VerticalSpacingItemDecorator itemDecorator = new VerticalSpacingItemDecorator(10);
        mRecyclerView.addItemDecoration(itemDecorator);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);
        mWordRecyclerAdapter = new WordsRecyclerAdapter(mWords, this);
        mRecyclerView.setAdapter(mWordRecyclerAdapter);
    }

    @Override
    public void onWordClick(int position) {
        Intent intent = new Intent(this, EditWordActivity.class);
        intent.putExtra("selected_word", mWords.get(position));
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.fab:{
                Intent intent = new Intent(this, EditWordActivity.class);
                startActivity(intent);
                break;
            }

        }
    }


    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            deleteWord(mWords.get(mWords.indexOf(mWordRecyclerAdapter.getFilteredWords().get(viewHolder.getAdapterPosition()))));
        }
    };


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){

            case Constants.WORDS_RETRIEVE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully retrieved notes. This is from thread: " + Thread.currentThread().getName());

                if(mWords != null){
                    if(mWords.size() > 0){
                        mWords.clear();
                    }
                }

                ArrayList<Word> words = new ArrayList<>(msg.getData().<Word>getParcelableArrayList("words_retrieve"));
                mWords.addAll(words);
                mWordRecyclerAdapter.getFilter().filter(mSearchQuery);
                break;
            }

            case Constants.WORDS_RETRIEVE_FAIL:{
                Log.d(TAG, "handleMessage: unable to retrieve words. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_INSERT_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully inserted new word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_INSERT_FAIL:{
                Log.d(TAG, "handleMessage: unable to insert new word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_DELETE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully deleted a word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_DELETE_FAIL:{
                Log.d(TAG, "handleMessage: unable to delete word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.MSG_THREAD_POOL_TASK_COMPLETE:{
                ArrayList<Word> words = msg.getData().getParcelableArrayList("word_data_from_thread_pool");
                mWords.addAll(words);
                mWordRecyclerAdapter.getFilter().filter(mSearchQuery);

                String time = String.valueOf(System.currentTimeMillis() - mStartTime) + " ms";
                Log.d(TAG, "handleMessage: TIME ELAPSED: " + time);
                Log.d(TAG, "handleMessage: recieved some words: " + words.size());
                Log.d(TAG, "handleMessage: total words: " + mWords.size());
                break;
            }

        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dictionary_activity_actions, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView =
                (SearchView) searchItem.getActionView();

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                mSearchQuery = query;
                mWordRecyclerAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                mSearchQuery = query;
                mWordRecyclerAdapter.getFilter().filter(query);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onRefresh() {
        retrieveWords();
        mSwipeRefresh.setRefreshing(false);
    }
    
}


