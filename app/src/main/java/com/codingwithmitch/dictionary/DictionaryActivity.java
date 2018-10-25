package com.codingwithmitch.dictionary;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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
import android.widget.ImageButton;
import android.widget.RelativeLayout;


import com.codingwithmitch.dictionary.adapters.WordsRecyclerAdapter;
import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.persistence.AppDatabase;
import com.codingwithmitch.dictionary.threading.MyThread;
import com.codingwithmitch.dictionary.util.Constants;
import com.codingwithmitch.dictionary.util.VerticalSpacingItemDecorator;

import java.util.ArrayList;
import java.util.List;

public class DictionaryActivity extends AppCompatActivity implements
        WordsRecyclerAdapter.OnWordListener,
        Handler.Callback,
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener
{

    private static final String TAG = "NotesListActivity";

    //ui components
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefresh;

    //vars
    private ArrayList<Word> mWords = new ArrayList<>();
    private WordsRecyclerAdapter mWordRecyclerAdapter;
    private FloatingActionButton mFab;
    private Handler mMainThreadHandler = null;
    private HandlerThread mBackgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary);
        mRecyclerView = findViewById(R.id.recyclerView);
        mFab = findViewById(R.id.fab);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);

        mFab.setOnClickListener(this);
        mSwipeRefresh.setOnRefreshListener(this);
        mMainThreadHandler = new Handler(this);

        setupRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mBackgroundThread == null){
            mBackgroundThread = new HandlerThread("DictionaryActivity Background Thread");
            mBackgroundThread.start();
        }
        if(mWords.size() == 0){
            retrieveWords();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBackgroundThread != null){
            mBackgroundThread.quit();
        }
    }


    private void retrieveWords() {
        Log.d(TAG, "retrieveWords: called.");
        Handler handler = new Handler(mBackgroundThread.getLooper());
        handler.post(retrieveWordsRunnable);
    }

    private Runnable retrieveWordsRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: retrieving notes. This is from thread: " + Looper.myLooper().getThread().getName());
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            List<Word> tempWords = new ArrayList<>(db.wordDataDao().getAllWords());
            ArrayList<Word> words = new ArrayList<>(tempWords);

            Message message = null;
            if(words.size() > 0){
                message = Message.obtain(null, Constants.WORDS_RETRIEVE_SUCCESS);
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("words_retrieve", words);
                message.setData(bundle);
            }
            else{
                message = Message.obtain(null, Constants.WORDS_RETRIEVE_FAIL);
            }
            mMainThreadHandler.sendMessage(message);
        }
    };


    public void deleteWord(Word word) {
        Log.d(TAG, "deleteWord: called.");
        mWords.remove(word);
        mWordRecyclerAdapter.getFilteredWords().remove(word);
        mWordRecyclerAdapter.notifyDataSetChanged();

        Handler handler = new Handler(mBackgroundThread.getLooper());
        DeleteWordRunnable deleteWordRunnable = new DeleteWordRunnable(word);
        handler.post(deleteWordRunnable);
    }

    /**
     *  Need to create a class for this because it references a note to delete
     */
    private class DeleteWordRunnable implements Runnable{

        private Word word;

        public DeleteWordRunnable(Word word) {
            this.word = word;
        }

        @Override
        public void run() {
            Log.d(TAG, "run: deleting word on thread: " + Looper.myLooper().getThread().getName());
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            Message message = null;
            if(db.wordDataDao().delete(word) > 0){
                message = Message.obtain(null, Constants.WORD_DELETE_SUCCESS);
            }
            else{
                message = Message.obtain(null, Constants.WORD_DELETE_FAIL);
            }

            mMainThreadHandler.sendMessage(message);
        }
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
                mWordRecyclerAdapter.notifyDataSetChanged();
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
                mWordRecyclerAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
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


