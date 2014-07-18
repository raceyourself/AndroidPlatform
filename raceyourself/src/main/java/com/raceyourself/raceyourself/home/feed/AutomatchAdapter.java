package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import lombok.extern.slf4j.Slf4j;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by Duncan on 17/07/2014.
 */
@Slf4j
public class AutomatchAdapter extends BaseAdapter implements StickyListHeadersAdapter {
    private AutomatchBean automatchBean = new AutomatchBean();
    private Context context;
    private String titleText;

    public AutomatchAdapter(Context context, int resource, String titleText) {
        this.context = context;
        this.titleText = titleText;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public Object getItem(int position) {
        return automatchBean;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        log.debug("getTitleView, pos={}", position);

        ChallengeTitleView challengeTitleView;
        if (convertView == null) {
            challengeTitleView = ChallengeTitleView_.build(context);
        }
        else {
            challengeTitleView = (ChallengeTitleView) convertView;
        }

        challengeTitleView.bind(automatchBean);

        return challengeTitleView;
    }

    @Override
    public View getHeaderView(int position, View view, ViewGroup viewGroup) {
        return StickyRunHeaderAdapter.getInstance().getHeaderView(context, titleText, position, view, viewGroup);
    }

    @Override
    public long getHeaderId(int position) {
        return StickyRunHeaderAdapter.getInstance().getHeaderId(position);
    }
}
