package com.codingwithmitch.dictionary.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.codingwithmitch.dictionary.models.Word;

import java.util.List;

@Dao
public interface WordDao {

    @Query("SELECT * FROM Word WHERE title LIKE :title || '%'")
    List<Word> getWords(String title);

    @Query("SELECT * FROM Word")
    List<Word> getAllWords();

    @Insert
    long[] insertWords(Word... words);

    @Delete
    int delete(Word word);

    @Query("UPDATE Word SET title = :title, content = :content, timestamp = :timestamp WHERE uid = :uid")
    int updateWord(String title, String content, String timestamp, int uid);

    @Query("SELECT * FROM Word LIMIT :row, :numRows ")
    public List<Word> getSomeWords(int row, int numRows);

    @Query("SELECT COUNT(*) FROM Word")
    public Integer getNumRows();
}
