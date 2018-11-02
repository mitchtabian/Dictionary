package com.codingwithmitch.dictionary;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
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


import com.codingwithmitch.dictionary.adapters.WordsRecyclerAdapter;
import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.threading.DeleteWordAsyncTask;
import com.codingwithmitch.dictionary.threading.DeleteWordRunnable;
import com.codingwithmitch.dictionary.threading.MyThread;
import com.codingwithmitch.dictionary.threading.RetrieveRowsAsyncTask;
import com.codingwithmitch.dictionary.threading.RetrieveWordsAsyncTask;
import com.codingwithmitch.dictionary.threading.RetrieveWordsRunnable;
import com.codingwithmitch.dictionary.threading.TaskDelegate;
import com.codingwithmitch.dictionary.threading.ThreadPoolRunnable;
import com.codingwithmitch.dictionary.util.Constants;
import com.codingwithmitch.dictionary.util.FakeData;
import com.codingwithmitch.dictionary.util.VerticalSpacingItemDecorator;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DictionaryActivity extends AppCompatActivity implements
        WordsRecyclerAdapter.OnWordListener,
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        TaskDelegate,
        Handler.Callback
{

    private static final String TAG = "DictionaryActivity";

    //ui components
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefresh;

    //vars
    private ArrayList<Word> mWords = new ArrayList<>();
    private WordsRecyclerAdapter mWordRecyclerAdapter;
    private FloatingActionButton mFab;
    private String mSearchQuery = "";
    private DeleteWordAsyncTask mDeleteWordAsyncTask;
    private RetrieveRowsAsyncTask mRetrieveRowsAsyncTask;
    private ExecutorService mExecutorService = null;
    private int mNumRows = 0;
    private Handler mMainThreadHandler;

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

        mMainThreadHandler = new Handler(this);
        initExecutorThreadPool();

        setupRecyclerView();
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
    protected void onStart() {
        Log.d(TAG, "onStart: called.");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called.");
        super.onStop();

        if(mDeleteWordAsyncTask != null){
            mDeleteWordAsyncTask.cancel(true);
        }

        if(mRetrieveRowsAsyncTask != null){
            mRetrieveRowsAsyncTask.cancel(true);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mSearchQuery.length() > 2){
            onRefresh();
        }
    }

    private void retrieveWords(String title) {
        Log.d(TAG, "retrieveWords: called.");

        if(mRetrieveRowsAsyncTask != null){
            mRetrieveRowsAsyncTask.cancel(true);
        }
        mRetrieveRowsAsyncTask = new RetrieveRowsAsyncTask(this,this);
        mRetrieveRowsAsyncTask.execute();
    }


    public void deleteWord(Word word) {
        Log.d(TAG, "deleteWord: called.");
        mWords.remove(word);
        mWordRecyclerAdapter.getFilteredWords().remove(word);
        mWordRecyclerAdapter.notifyDataSetChanged();

        if(mDeleteWordAsyncTask != null){
            mDeleteWordAsyncTask.cancel(true);
        }
        mDeleteWordAsyncTask = new DeleteWordAsyncTask(this);
        mDeleteWordAsyncTask.execute(word);
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
                if(query.length() > 2){
                    mSearchQuery = query;
                    retrieveWords(mSearchQuery);
                }
                else{
                    clearWords();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                if(query.length() > 2){
                    mSearchQuery = query;
                    retrieveWords(mSearchQuery);
                }
                else{
                    clearWords();
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void clearWords(){
        if(mWords != null){
            if(mWords.size() > 0){
                mWords.clear();
            }
        }
        mWordRecyclerAdapter.getFilter().filter(mSearchQuery);
    }

    @Override
    public void onRefresh() {
        retrieveWords(mSearchQuery);
        mSwipeRefresh.setRefreshing(false);
    }


    @Override
    public void onWordsRetrieved(ArrayList<Word> words) {
        clearWords();

        mWords.addAll(words);
        mWordRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRowsRetrieved(int numRows) {
        Log.d(TAG, "onRowsRetrieved: num rows: " + numRows);
        mNumRows = numRows;

    }


    @Override
    public boolean handleMessage(Message message) {

        switch (message.what){

            case Constants.MSG_THREAD_POOL_TASK_COMPLETE:{
                ArrayList<Word> words = message.getData().getParcelableArrayList("word_data_from_thread_pool");
                mWords.addAll(words);
                mWordRecyclerAdapter.getFilter().filter(mSearchQuery);

                Log.d(TAG, "handleMessage: recieved some words: " + words.size());
                Log.d(TAG, "handleMessage: total words: " + mWords.size());
                break;
            }

        }
        return false;
    }
}









