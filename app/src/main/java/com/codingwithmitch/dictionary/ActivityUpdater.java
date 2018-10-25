package com.codingwithmitch.dictionary;

import com.codingwithmitch.dictionary.models.Word;

import java.util.ArrayList;

public interface ActivityUpdater {

    void gotWords(ArrayList<Word> words);

    void deletedWords(int[] rows);
}
