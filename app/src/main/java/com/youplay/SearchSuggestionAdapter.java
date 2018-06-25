package com.youplay;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tan on 19/12/17.
 **/

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.ItemViewHolder> {

    final String TAG = "SearchSuggestionAdapter";
    private List<String> searchSuggestionList = new ArrayList<>();
    private PreferenceManager pm;
    private Activity activity;
    private FirebaseAnalytics firebaseAnalytics;

    public SearchSuggestionAdapter(Activity activity, List<String> items, PreferenceManager pm) {
        this.activity = activity;
        this.searchSuggestionList = items;
        this.pm = pm;
        this.firebaseAnalytics = FirebaseAnalytics.getInstance(activity);
    }

    @Override
    public SearchSuggestionAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_search_suggestion, null);
        return new SearchSuggestionAdapter.ItemViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final SearchSuggestionAdapter.ItemViewHolder holder, final int position) {
        String suggestion = searchSuggestionList.get(holder.getAdapterPosition());
        holder.searchSuggestionText.setText(suggestion);
        if (Utils.getSavedSearchList(pm.getSavedSearchQueries()).contains(suggestion))
            holder.searchSuggestionIcon.setText(activity.getString(R.string.ic_restore));
        else
            holder.searchSuggestionIcon.setText(activity.getString(R.string.ic_search));

        holder.searchSuggestionLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) activity).getSearchResults(searchSuggestionList.get(holder.getAdapterPosition()), true);
                firebaseAnalytics.logEvent(AppConstants.Event.SEARCH_SUGGESTION_CLICKED, new Bundle());
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchSuggestionList.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        FrameLayout searchSuggestionLayout;
        TextView searchSuggestionText, searchSuggestionIcon;

        public ItemViewHolder(View itemView) {
            super(itemView);
            searchSuggestionLayout = itemView.findViewById(R.id.search_suggestion_layout);
            searchSuggestionText = itemView.findViewById(R.id.search_suggestion_text);
            searchSuggestionIcon = itemView.findViewById(R.id.search_suggestion_icon);
            Typeface typeface = Typeface.createFromAsset(activity.getAssets(), "fonts/material.ttf");
            searchSuggestionIcon.setTypeface(typeface);

        }
    }

}
