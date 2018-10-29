package com.codingwithmitch.dictionary;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
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


import com.codingwithmitch.dictionary.adapters.WordsRecyclerAdapter;
import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.threading.MyThread;
import com.codingwithmitch.dictionary.util.Constants;
import com.codingwithmitch.dictionary.util.FakeData;
import com.codingwithmitch.dictionary.util.VerticalSpacingItemDecorator;
import java.util.ArrayList;
import java.util.Arrays;


public class DictionaryActivity extends AppCompatActivity implements
        WordsRecyclerAdapter.OnWordListener,
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener,
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
    private MyThread mMyThread;
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

        setupRecyclerView();
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

    private void sendTestMessageToThread(){
        Log.d(TAG, "sendTestMessageToThread: sending message from thread: " + Thread.currentThread().getName());
        Message message = Message.obtain(null, Constants.WORD_INSERT_NEW);
        mMyThread.sendMessageToBackgroundThread(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendTestMessageToThread();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: called.");
        super.onStart();
        if(mMyThread == null){
            mMyThread = new MyThread(this, mMainThreadHandler);
            mMyThread.start();
        }
        if(mWords.size() == 0){
            retrieveWords();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mMyThread != null){
            mMyThread.quitThread();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called.");
    }


    private void retrieveWords() {
        Log.d(TAG, "retrieveWords: called.");
        mWords.addAll(Arrays.asList(FakeData.words));
    }


    public void deleteWord(Word word) {
        Log.d(TAG, "deleteWord: called.");
        mWords.remove(word);
        mWordRecyclerAdapter.getFilteredWords().remove(word);
        mWordRecyclerAdapter.notifyDataSetChanged();

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

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){

            case Constants.WORDS_RETRIEVE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully retrieved notes. This is from thread: " + Thread.currentThread().getName());

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

        }
        return true;
    }
}


