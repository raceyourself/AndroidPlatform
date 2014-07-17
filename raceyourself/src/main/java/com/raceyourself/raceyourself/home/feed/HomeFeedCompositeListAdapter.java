package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.raceyourself.raceyourself.base.util.Threesome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 15/07/2014.
 */
@Slf4j
public class HomeFeedCompositeListAdapter extends ArrayAdapter<HomeFeedRowBean> {
    private final Context context;
    private List<Pair<HomeFeedRowBean,? extends BaseAdapter>> children = Lists.newArrayList();

    public HomeFeedCompositeListAdapter(@NonNull Context context, int resource) {
        super(context, resource, new ArrayList<HomeFeedRowBean>());
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Threesome<BaseAdapter, Integer, ?> threesome = getAdapterAndPositionAndItem(position);
        if (threesome.first != null)
            return threesome.first.getView(threesome.second, convertView, parent);

        //TODO non-nested types

        return null;
    }

    private Threesome<BaseAdapter, Integer, HomeFeedRowBean> getAdapterAndPositionAndItem(int position) {
        ListIterator<Pair<HomeFeedRowBean,? extends BaseAdapter>> it = children.listIterator();

        int traversed = 0;
        while (it.hasNext()) {
            Pair<HomeFeedRowBean,? extends BaseAdapter> pair = it.next();

            if (pair.first != null) {
                if (traversed == position)
                    return Threesome.create(null, traversed, pair.first);
                traversed++;
            }
            else {
                int nestedItems = pair.second.getCount();
                if (traversed + nestedItems > position) {
                    int nestedPos = position - traversed;
                    return Threesome.create(pair.second, nestedPos, (HomeFeedRowBean) pair.second.getItem(nestedPos));
                }
                traversed += nestedItems;
            }
        }
        throw new ArrayIndexOutOfBoundsException(String.format(
                "Requested position %d is beyond summed lengths of all child adapters' lists.", position));
    }

    @Override
    public void add(@NonNull HomeFeedRowBean bean) {
        children.add(Pair.create(bean, (BaseAdapter) null));
        super.add(bean);
    }

    public void add(@NonNull ArrayAdapter adapter) {
        children.add(Pair.create((HomeFeedRowBean) null, adapter));
    }

    public void add(@NonNull com.nhaarman.listviewanimations.ArrayAdapter adapter) {
        children.add(Pair.create((HomeFeedRowBean) null, adapter));
    }

    @Override
    public void addAll(@NonNull Collection<? extends HomeFeedRowBean> beans) {
        for (HomeFeedRowBean bean : beans)
            add(bean);
    }

    @Override
    public void addAll(@NonNull HomeFeedRowBean... items) {
        addAll(ImmutableList.copyOf(items));
    }

    @Override
    public void insert(@NonNull HomeFeedRowBean object, int index) {
        ListIterator<Pair<HomeFeedRowBean,? extends BaseAdapter>> it = children.listIterator();

        int traversed = 0;
        while (it.hasNext()) {
            Pair<HomeFeedRowBean,? extends BaseAdapter> pair = it.next();

            if (pair.first != null) {
                if (traversed == index) {
                    it.previous(); // rewind one
                    it.add(Pair.create(object, (BaseAdapter) null));
                    it.next(); // get back where we were
                }
                traversed++;
            }
            else {
                int nestedItems = pair.second.getCount();
                if (traversed + nestedItems > index) {
                    int nestedPos = index - traversed;
                    if (pair.second instanceof ArrayAdapter) {
                        ArrayAdapter adapter = (ArrayAdapter) pair.second;
                        adapter.insert(object, index);
                    }
                    else if (pair.second instanceof com.nhaarman.listviewanimations.ArrayAdapter) {
                        com.nhaarman.listviewanimations.ArrayAdapter<HomeFeedRowBean> adapter =
                                (com.nhaarman.listviewanimations.ArrayAdapter<HomeFeedRowBean>) pair.second;
                        ListIterator<HomeFeedRowBean> adIt = adapter.listIterator();
                        for (int i = 0; i < nestedPos; i++)
                            adIt.next();
                        adIt.add(object);
                    }
                }
                traversed += nestedItems;
            }
        }
        throw new ArrayIndexOutOfBoundsException(String.format(
                "Requested position %d is beyond sum of items and child adapter's items.", index));
    }

    @Override
    public void clear() {
        for (Pair<HomeFeedRowBean,? extends BaseAdapter> pair : children) {
            if (pair.second != null) {
                if (pair.second instanceof ArrayAdapter) {
                    ArrayAdapter<?> adapter = (ArrayAdapter<?>) pair.second;
                    adapter.clear();
                }
                else if (pair.second instanceof com.nhaarman.listviewanimations.ArrayAdapter) {
                    com.nhaarman.listviewanimations.ArrayAdapter<?> adapter =
                            (com.nhaarman.listviewanimations.ArrayAdapter<?>) pair.second;
                    adapter.clear();
                }
            }
        }
        children.clear();
        super.clear();
    }

    @Override
    public void sort(Comparator<? super HomeFeedRowBean> comparator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getCount() {
        int n = 0;
        for (Pair<HomeFeedRowBean,? extends BaseAdapter> pair : children) {
            if (pair.first != null)
                n++;
            else
                n += pair.second.getCount();
        }
        return n;
    }

    @Override
    public HomeFeedRowBean getItem(int position) {
        Threesome<BaseAdapter, Integer,? extends HomeFeedRowBean> threesome = getAdapterAndPositionAndItem(position);
        if (threesome.first != null)
            return (HomeFeedRowBean) threesome.first.getItem(threesome.second);
        else {
            return threesome.third;
        }
    }

    @Override
    public int getPosition(@NonNull HomeFeedRowBean item) {
        ListIterator<Pair<HomeFeedRowBean,? extends BaseAdapter>> it = children.listIterator();

        int traversed = 0;
        while (it.hasNext()) {
            Pair<HomeFeedRowBean,? extends BaseAdapter> pair = it.next();

            if (pair.first != null) {
                if (pair.first.equals(item))
                    return traversed;
                traversed++;
            }
            else {
                int nestedItems = pair.second.getCount();
                for (int nestedPos = 0; nestedPos < nestedItems; nestedPos++) {
                    if (pair.second.getItem(nestedPos).equals(item))
                        return traversed;
                    traversed++;
                }
            }
        }
        throw new NoSuchElementException(String.format(
                "Element %d isn't in this Adapter nor in its children.", item.toString()));
    }

    private Map<Class<?>,Integer> getItemViewTypes() {
        // FIXME probably could use optimisation... will get called a lot.
        Map<Class<?>, Integer> itemViewTypes = Maps.newHashMap();
        int n = 0;
        for (Pair<HomeFeedRowBean,? extends BaseAdapter> pair : children) {
            if (pair.first != null) {
                Class<?> clazz = pair.first.getClass();
                if (!itemViewTypes.containsKey(clazz))
                    itemViewTypes.put(pair.first.getClass(), n++);
            }
            else {
                for (int i = 0; i < pair.second.getCount(); i++) {
                    Class<?> clazz = pair.second.getItem(i).getClass();
                    if (!itemViewTypes.containsKey(clazz)) {
                        itemViewTypes.put(pair.second.getItem(i).getClass(), n++);
                    }
                }
            }
        }
        return itemViewTypes;
    }

    @Override
    public int getViewTypeCount() {
        return getItemViewTypes().size();
    }

    @Override
    public int getItemViewType(int position) {
        Threesome<BaseAdapter, Integer, HomeFeedRowBean> threesome = getAdapterAndPositionAndItem(position);
        return getItemViewTypes().get(threesome.third.getClass());
    }
}
