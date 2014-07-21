package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by Duncan on 15/07/2014.
 */
@Slf4j
public class HomeFeedCompositeListAdapter extends ArrayAdapter<HomeFeedRowBean> implements StickyListHeadersAdapter {
    private final Context context;
    private List<? extends StickyListHeadersAdapter> childArrayAdapters;

    public HomeFeedCompositeListAdapter(@NonNull Context context, int resource,
                                        @NonNull List<? extends StickyListHeadersAdapter> childArrayAdapters) {
        super(context, resource, (List<HomeFeedRowBean>)null);
        this.context = context;
        this.childArrayAdapters = childArrayAdapters;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Pair<StickyListHeadersAdapter, Integer> adapterPair = getAdapterAndPosition(position);

        return adapterPair.first.getView(adapterPair.second, convertView, parent);
    }

    private Pair<StickyListHeadersAdapter, Integer> getAdapterAndPosition(int position) {
        ListIterator<? extends StickyListHeadersAdapter> bIt = childArrayAdapters.listIterator();

        StickyListHeadersAdapter out = null;
        while (out == null && bIt.hasNext()) {
            StickyListHeadersAdapter adapter = bIt.next();
            int nElems = adapter.getCount();

            if (position < nElems)
                out = adapter;
            else
                position -= nElems;
        }
        if (out == null)
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "Requested position %d is beyond summed lengths of all child adapters' lists.", position));
        return new Pair<StickyListHeadersAdapter, Integer>(out, position);
    }

    @Override
    public void add(HomeFeedRowBean object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(Collection<? extends HomeFeedRowBean> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(HomeFeedRowBean... items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(HomeFeedRowBean object, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sort(Comparator<? super HomeFeedRowBean> comparator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(HomeFeedRowBean bean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getCount() {
        int sum = 0;
        for (StickyListHeadersAdapter adapter : childArrayAdapters) {
            sum += adapter.getCount();
        }
        return sum;
    }

    @Override
    public HomeFeedRowBean getItem(int position) {
        Pair<StickyListHeadersAdapter, Integer> adapterPair = getAdapterAndPosition(position);
        return (HomeFeedRowBean)adapterPair.first.getItem(adapterPair.second);
    }

    @Override
    public int getPosition(HomeFeedRowBean item) {
        int ret = -1;
        int index = 0;
        for (StickyListHeadersAdapter adapter : childArrayAdapters) {
            for (int i=0;i<adapter.getCount();i++) {
                if (adapter.getItem(i).equals(item)) {
                    ret = index;
                    break;
                }
                index++;
            }
            if (ret != -1) break;
        }
        return ret;
    }

    @Override
    public int getViewTypeCount() {
        return childArrayAdapters.size();
    }

    @Override
    public int getItemViewType(int position) {
        StickyListHeadersAdapter adapter = getAdapterAndPosition(position).first;
        return childArrayAdapters.indexOf(adapter);
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup viewGroup) {
        Pair<StickyListHeadersAdapter, Integer> pair = getAdapterAndPosition(position);

        return pair.first.getHeaderView(position, convertView, viewGroup);
    }

    @Override
    public long getHeaderId(int position) {
        Pair<StickyListHeadersAdapter, Integer> pair = getAdapterAndPosition(position);

        return pair.first.getHeaderId(pair.second);
    }

    public int getFirstPosition(Class<? extends Adapter> adapterClass) {
        int position = -1;
        for (Adapter child : childArrayAdapters) {
            if (child.getClass().equals(adapterClass))
                return position;
            position += child.getCount();
        }
        return position;
    }
}
