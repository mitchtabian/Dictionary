package com.codingwithmitch.dictionary.threading;

import com.codingwithmitch.dictionary.models.Word;

import java.util.ArrayList;

public interface TaskDelegate {

    void onWordsRetrieved(ArrayList<Word> words);

    void onRowsRetrieved(int numRows);
}
