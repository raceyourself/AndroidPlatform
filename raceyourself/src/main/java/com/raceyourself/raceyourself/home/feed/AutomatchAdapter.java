package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.collect.ImmutableList;
import com.raceyourself.raceyourself.R;

import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 17/07/2014.
 */
@Slf4j
public class AutomatchAdapter extends ArrayFeedListAdapter<AutomatchBean> {

    public static final long HEADER_ID = 8525897190003L;

    private Context context;
    private AutomatchBean automatchBean;

    public static AutomatchAdapter create(@NonNull Context context, int resource) {
        String titleText = context.getString(R.string.home_feed_title_run);
        return new AutomatchAdapter(context, resource, titleText, ImmutableList.of(new AutomatchBean()));
    }

    private AutomatchAdapter(@NonNull Context context,
                             int resource,
                             @NonNull String titleText,
                             @NonNull List<AutomatchBean> items) {
        super(context, titleText, HEADER_ID, resource, items);
        this.context = context;
        automatchBean = items.get(0);
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
}
