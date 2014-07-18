package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.collect.ImmutableList;

import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 17/07/2014.
 */
@Slf4j
public class AutomatchAdapter extends ArrayFeedListAdapter<AutomatchBean> {
    private static final long HEADER_ID = 8525897190003L;

    private AutomatchBean automatchBean = new AutomatchBean();
    private Context context;

    public AutomatchAdapter create(@NonNull Context context, int resource, String titleText) {
        return new AutomatchAdapter(context, resource, titleText, ImmutableList.of(new AutomatchBean()));
    }

    private AutomatchAdapter(@NonNull Context context, int resource, @NonNull String titleText,
                             @NonNull List<AutomatchBean> items) {
        super(context, titleText, HEADER_ID, resource, items);
        this.context = context;
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
