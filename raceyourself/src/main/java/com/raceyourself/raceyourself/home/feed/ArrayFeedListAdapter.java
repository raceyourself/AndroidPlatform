package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 18/07/2014.
 */
@Slf4j
public class ArrayFeedListAdapter<T> extends FeedListAdapter<T> {

    @Getter
    private ArrayAdapter<T> delegate;

    public ArrayFeedListAdapter(@NonNull Context context, @NonNull String titleText,
                                long headerId, int resource, List<T> items) {
        super(context, titleText, headerId);

        delegate = new ArrayAdapter<T>(context, resource, items);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return delegate.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        return delegate.isEnabled(position);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        delegate.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        delegate.unregisterDataSetObserver(observer);
    }

    @Override
    public int getCount() {
        return delegate.getCount();
    }

    @Override
    public Object getItem(int position) {
        return delegate.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return delegate.getItemId(position);
    }

    @Override
    public boolean hasStableIds() {
        return delegate.hasStableIds();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return delegate.getView(position, convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return delegate.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return delegate.getViewTypeCount();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }
}
