package com.raceyourself.raceyourself.home;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.ListIterator;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 15/07/2014.
 */
@Slf4j
public class HomePageCompositeListAdapter extends ArrayAdapter<HomePageRowBean> {
    private final Context context;
    private List<? extends BaseAdapter> childArrayAdapters;
    private List<Integer> adapterListLengths;

    public HomePageCompositeListAdapter(@NonNull Context context, int resource,
                                        @NonNull List<HomePageRowBean> objects,
                                        @NonNull List<? extends BaseAdapter> childArrayAdapters,
                                        @NonNull List<Integer> adapterListLengths) {
        super(context, resource, objects);
        this.context = context;
        this.childArrayAdapters = childArrayAdapters;
        this.adapterListLengths = adapterListLengths;

        if (adapterListLengths.size() != childArrayAdapters.size())
            throw new IllegalArgumentException("List lengths should match.");

//        List<Integer> counts = Lists.newArrayList();
//        for (ArrayAdapter<? extends HomePageRowBean> adapter : childArrayAdapters) {
//            counts.add(adapter.getCount());
//        }
//        adapterListLengths = ImmutableList.copyOf(counts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BaseAdapter adapter = getAdapter(position);

        return adapter.getView(position, convertView, parent);
    }

    private BaseAdapter getAdapter(int position) {
        ListIterator<Integer> nIt = adapterListLengths.listIterator();
        ListIterator<? extends BaseAdapter> bIt = childArrayAdapters.listIterator();

        BaseAdapter out = null;
        while (out == null && nIt.hasNext()) {
            int nElems = nIt.next();
            BaseAdapter adapter = bIt.next();

            if (position < nElems)
                out = adapter;
            else
                position -= nElems;
        }
        if (out == null)
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "Requested position %d is beyond summed lengths of all child adapters' lists.", position));
        return out;
    }

    @Override
    public int getViewTypeCount() {
        return childArrayAdapters.size();
    }

    @Override
    public int getItemViewType(int position) {
        BaseAdapter adapter = getAdapter(position);
        return childArrayAdapters.indexOf(adapter);
    }
}
