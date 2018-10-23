package com.codingwithmitch.dictionary.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;


import com.codingwithmitch.dictionary.R;
import com.codingwithmitch.dictionary.models.Word;
import com.codingwithmitch.dictionary.util.Utility;

import java.util.ArrayList;

public class WordsRecyclerAdapter extends RecyclerView.Adapter<WordsRecyclerAdapter.ViewHolder> implements Filterable {

    private static final String TAG = "WordsRecyclerAdapter";
    private ArrayList<Word> mWords = new ArrayList<>();
    private ArrayList<Word> mFilteredWords = new ArrayList<>();
    private OnWordListener mOnWordListener;


    public WordsRecyclerAdapter(ArrayList<Word> words, OnWordListener onWordListener) {
        mOnWordListener = onWordListener;
        mWords = words;
        mFilteredWords = mWords;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_word_list_item, parent, false);
        return new ViewHolder(view, mOnWordListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {


        try{
            String month = mFilteredWords.get(position).getTimestamp().substring(0,2);
            month = Utility.getMonthFromNumber(month);
            String year = mFilteredWords.get(position).getTimestamp().substring(3);
            String timestamp = month + " " + year;
            holder.timestamp.setText(timestamp);
            holder.title.setText(mFilteredWords.get(position).getTitle());
        }catch (NullPointerException e){
            Log.e(TAG, "onBindViewHolder: Null Pointer: " + e.getMessage() );
        }

    }


    @Override
    public int getItemCount() {
        return mFilteredWords.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    mFilteredWords = mWords;
                } else {
                    ArrayList<Word> filteredList = new ArrayList<>();

                    for (int i = 0; i < mWords.size(); i++) {
                        if (mWords.get(i).getTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(mWords.get(i));
                        }
                    }
                    mFilteredWords = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mFilteredWords;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mFilteredWords = (ArrayList<Word>) filterResults.values;

                // refresh the list with filtered data
                notifyDataSetChanged();
            }
        };
    }

    public void setFilteredWords(ArrayList<Word> words){
        mFilteredWords = words;
    }

    public ArrayList<Word> getFilteredWords(){
        return mFilteredWords;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener
    {
        TextView timestamp, title;
        OnWordListener listener;

        public ViewHolder(View itemView, OnWordListener listener) {
            super(itemView);
            timestamp = itemView.findViewById(R.id.word_timestamp);
            title = itemView.findViewById(R.id.word_title);
            this.listener = listener;

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick: called.");
            listener.onWordClick(mWords.indexOf(mFilteredWords.get(getAdapterPosition())));
        }


    }

    public interface OnWordListener{
        void onWordClick(int position);
    }

}
