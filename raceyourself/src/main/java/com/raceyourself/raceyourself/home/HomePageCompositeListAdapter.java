package com.raceyourself.raceyourself.home;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import java.util.Collection;
import java.util.Comparator;
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

    public HomePageCompositeListAdapter(@NonNull Context context, int resource,
                                        @NonNull List<? extends BaseAdapter> childArrayAdapters) {
        super(context, resource, (List<HomePageRowBean>)null);
        this.context = context;
        this.childArrayAdapters = childArrayAdapters;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Pair<BaseAdapter, Integer> adapterPair = getAdapterAndPosition(position);

        return adapterPair.first.getView(adapterPair.second, convertView, parent);
    }

    private Pair<BaseAdapter, Integer> getAdapterAndPosition(int position) {
        ListIterator<? extends BaseAdapter> bIt = childArrayAdapters.listIterator();

        BaseAdapter out = null;
        while (out == null && bIt.hasNext()) {
            BaseAdapter adapter = bIt.next();
            int nElems = adapter.getCount();

            if (position < nElems)
                out = adapter;
            else
                position -= nElems;
        }
        if (out == null)
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "Requested position %d is beyond summed lengths of all child adapters' lists.", position));
        return new Pair<BaseAdapter, Integer>(out, position);
    }

    @Override
    public void add(HomePageRowBean object) {
        throw new Error("Not implemented");
    }

    @Override
    public void addAll(Collection<? extends HomePageRowBean> collection) {
        throw new Error("Not implemented");
    }

    @Override
    public void addAll(HomePageRowBean... items) {
        throw new Error("Not implemented");
    }

    @Override
    public void insert(HomePageRowBean object, int index) {
        throw new Error("Not implemented");
    }

    @Override
    public void clear() {
        throw new Error("Not implemented");
    }

    @Override
    public void sort(Comparator<? super HomePageRowBean> comparator) {
        throw new Error("Not implemented");
    }

    @Override
    public int getCount() {
        int sum = 0;
        for (BaseAdapter adapter : childArrayAdapters) {
            sum += adapter.getCount();
        }
        return sum;
    }

    @Override
    public HomePageRowBean getItem(int position) {
        Pair<BaseAdapter, Integer> adapterPair = getAdapterAndPosition(position);
        return (HomePageRowBean)adapterPair.first.getItem(adapterPair.second);
    }

    @Override
    public int getPosition(HomePageRowBean item) {
        int ret = -1;
        int index = 0;
        for (BaseAdapter adapter : childArrayAdapters) {
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
        BaseAdapter adapter = getAdapterAndPosition(position).first;
        return childArrayAdapters.indexOf(adapter);
    }
}
