package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.common.collect.ImmutableList;
import com.raceyourself.raceyourself.R;

import java.util.LinkedList;
import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunnableEmptyAdapter extends ArrayFeedListAdapter<RunnableEmptyBean> {

    private Context context;
    private RunnableEmptyBean runnableEmptyBean = new RunnableEmptyBean();

    public static RunnableEmptyAdapter create(@NonNull Context context, int resource, String titleText, long headerId) {
        return new RunnableEmptyAdapter(context, resource, titleText, new LinkedList<RunnableEmptyBean>() {{ add(new RunnableEmptyBean()); }}, headerId);
    }

    private RunnableEmptyAdapter(@NonNull Context context,
                                 int resource,
                                 @NonNull String titleText,
                                 @NonNull List<RunnableEmptyBean> items,
                                 long headerId) {
        super(context, titleText, headerId, resource, items);
        this.context = context;
    }

    public void hide() {
        if (isEmpty()) return;
        this.getDelegate().clear();
    }

    public void show() {
        if (!isEmpty()) return;
        this.getDelegate().add(runnableEmptyBean);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View cachedView = null;
        if (convertView instanceof RelativeLayout) cachedView = convertView;

        RelativeLayout view;
        if(cachedView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = (RelativeLayout) inflater.inflate(R.layout.fragment_runnable_empty, null);
        }
        else {
            view = (RelativeLayout) cachedView;
        }

        return view;
    }
}
