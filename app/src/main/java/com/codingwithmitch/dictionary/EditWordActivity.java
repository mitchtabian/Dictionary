package com.codingwithmitch.dictionary;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.threading.MyThread;
import com.codingwithmitch.dictionary.util.Constants;
import com.codingwithmitch.dictionary.util.LinedEditText;
import com.codingwithmitch.dictionary.util.Utility;

public class EditWordActivity extends AppCompatActivity implements
        View.OnClickListener,
        View.OnTouchListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        TextWatcher,
        Handler.Callback
{

    private static final String TAG = "EditWordActivity";
    private static final int EDIT_MODE_ENABLED = 1;
    private static final int EDIT_MODE_DISABLED = 0;

    //ui components
    private CoordinatorLayout mCoordinatorLayout;
    private LinedEditText mLinedEditText;
    private ImageButton mCheck, mBackArrow;
    private RelativeLayout mCheckContainer, mBackArrowContainer;
    private EditText mEditTitle;
    private TextView mViewTitle;

    //vars
    private GestureDetector mGestureDetector;
    private int mEditModeState = EDIT_MODE_DISABLED;
    private boolean mIsNewWord = false;
    private Word mWordInitial = new Word();
    private Word mWordFinal = new Word();
    private MyThread mMyThread;
    private Handler mMainThreadHandler = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_word);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        mLinedEditText = findViewById(R.id.word_text);
        mCheck = findViewById(R.id.toolbar_check);
        mBackArrow = findViewById(R.id.toolbar_back_arrow);
        mCheckContainer = findViewById(R.id.check_container);
        mBackArrowContainer = findViewById(R.id.back_arrow_container);
        mEditTitle = findViewById(R.id.word_edit_title);
        mViewTitle = findViewById(R.id.word_text_title);

        mBackArrow.setOnClickListener(this);
        mCheck.setOnClickListener(this);
        mViewTitle.setOnClickListener(this);
        mGestureDetector = new GestureDetector(this, this);
        mLinedEditText.setOnTouchListener(this);
        mEditTitle.addTextChangedListener(this);

        mMainThreadHandler = new Handler(this);

        getSupportActionBar().hide();
    }


    @Override
    protected void onStart() {
        super.onStart();
        mMyThread = new MyThread(this, mMainThreadHandler);
        mMyThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMyThread.quitThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getIncomingIntent()){
            setWordProperties();
            disableContentInteraction();
        }
        else{
            setNewWordProperties();
            enableEditMode();
        }
    }

    private void saveChanges(){
        if(mIsNewWord){
            saveNewWord();
        }else{
            updateWord();
        }
    }

    public void saveNewWord() {
        Message message = Message.obtain(null, Constants.WORD_INSERT_NEW);
        Bundle bundle = new Bundle();
        bundle.putParcelable("word_new", mWordFinal);
        message.setData(bundle);
        mMyThread.sendMessageToBackgroundThread(message);
    }

    public void updateWord() {
        Message message = Message.obtain(null, Constants.WORD_UPDATE);
        Bundle bundle = new Bundle();
        bundle.putParcelable("word_update", mWordFinal);
        message.setData(bundle);
        mMyThread.sendMessageToBackgroundThread(message);
    }


    private void setNewWordProperties(){
        mViewTitle.setText("Word");
        mEditTitle.setText("Word");
        appendNewLines();
    }

    private void setWordProperties(){
        mViewTitle.setText(mWordInitial.getTitle());
        mEditTitle.setText(mWordInitial.getTitle());
        mLinedEditText.setText(mWordInitial.getContent());
    }


    private boolean getIncomingIntent(){
        if(getIntent().hasExtra("selected_word")){
            Word incomingWord = getIntent().getParcelableExtra("selected_word");
            mWordInitial.setTitle(incomingWord.getTitle());
            mWordInitial.setTimestamp(incomingWord.getTimestamp());
            mWordInitial.setContent(incomingWord.getContent());
            mWordInitial.setUid(incomingWord.getUid());

            mWordFinal.setTitle(incomingWord.getTitle());
            mWordFinal.setTimestamp(incomingWord.getTimestamp());
            mWordFinal.setContent(incomingWord.getContent());
            mWordFinal.setUid(incomingWord.getUid());

            mIsNewWord = false;
            return true;
        }
        mIsNewWord = true;
        return false;
    }

    private void appendNewLines(){
        String text = mLinedEditText.getText().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(text);
        for(int i = 0; i < 20; i++){
            sb.append("\n");
        }
        mLinedEditText.setText(sb.toString());
    }


    private void enableEditMode(){
        mBackArrowContainer.setVisibility(View.GONE);
        mCheckContainer.setVisibility(View.VISIBLE);

        mViewTitle.setVisibility(View.GONE);
        mEditTitle.setVisibility(View.VISIBLE);

        enableContentInteraction();

        showSoftkeyboard();

        mEditModeState = EDIT_MODE_ENABLED;
    }


    private void disableEditMode(){
        Log.d(TAG, "disableEditMode: called.");
        hideSoftkeyboard();

        mBackArrowContainer.setVisibility(View.VISIBLE);
        mCheckContainer.setVisibility(View.GONE);

        mViewTitle.setVisibility(View.VISIBLE);
        mEditTitle.setVisibility(View.GONE);

        disableContentInteraction();

        mEditModeState = EDIT_MODE_DISABLED;

        // Check if they typed anything into the note. Don't want to save an empty note.
        String temp = mLinedEditText.getText().toString();
        temp = temp.replace("\n", "");
        temp = temp.replace(" ", "");
        if(temp.length() > 0){
            mWordFinal.setTitle(mEditTitle.getText().toString());
            mWordFinal.setContent(mLinedEditText.getText().toString());
            String timestamp = Utility.getCurrentTimeStamp();
            mWordFinal.setTimestamp(timestamp);

            // If the note was altered, save it.
            if(!mWordFinal.getContent().equals(mWordInitial.getContent())
                    || !mWordFinal.getTitle().equals(mWordInitial.getTitle())){
                Log.d(TAG, "disableEditMode: SAVING WORD: " + mLinedEditText.getText().toString());
                saveChanges();
            }
        }
    }

    private void disableContentInteraction(){
        mLinedEditText.setKeyListener(null);
        mLinedEditText.setFocusable(false);
        mLinedEditText.setFocusableInTouchMode(false);
        mLinedEditText.setCursorVisible(false);
        mLinedEditText.clearFocus();
    }

    private void enableContentInteraction(){
        mLinedEditText.setKeyListener(new EditText(this).getKeyListener());
        mLinedEditText.setFocusable(true);
        mLinedEditText.setFocusableInTouchMode(true);
        mLinedEditText.setCursorVisible(true);
        mLinedEditText.requestFocus();
    }

    private void showSoftkeyboard(){
        InputMethodManager inputMethodManager =
                (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInputFromWindow(
                mCoordinatorLayout.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
    }

    private void hideSoftkeyboard(){
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = this.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.toolbar_back_arrow:{
                finish();
                break;
            }
            case R.id.toolbar_check:{
                disableEditMode();
                break;
            }
            case R.id.word_text_title:{
                enableEditMode();
                mEditTitle.requestFocus();
                mEditTitle.setSelection(mEditTitle.length());
                break;
            }
        }
    }


    @Override
    public void onBackPressed() {
        if(mEditModeState == EDIT_MODE_ENABLED){
            onClick(mCheck);
        }
        else{
            super.onBackPressed();
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        Log.d(TAG, "onTouch: called.");
        if(v.getId() != R.id.toolbar_back_arrow
                && v.getId() != R.id.toolbar_check){
            if(v.getId() == R.id.word_text){
                if(mEditModeState == EDIT_MODE_DISABLED){
                    return mGestureDetector.onTouchEvent(event);
                }

            }
        }
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        enableEditMode();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mViewTitle.setText(s.toString());
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){

            case Constants.WORD_INSERT_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully inserted a new word. This is from thread: " + getMainLooper().getThread().getName());

                break;
            }
            case Constants.WORD_INSERT_FAIL:{
                Log.d(TAG, "handleMessage: unable to insert a word. This is from thread: " + getMainLooper().getThread().getName());
                break;
            }
            case Constants.WORD_UPDATE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully updated a word. This is from thread: " + getMainLooper().getThread().getName());

                break;
            }
            case Constants.WORD_UPDATE_FAIL:{
                Log.d(TAG, "handleMessage: unable to update a word. This is from thread: " + getMainLooper().getThread().getName());
                break;
            }
        }
        return true;
    }

}

