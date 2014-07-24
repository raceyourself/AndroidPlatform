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
public class RaceYourselfAdapter extends ArrayFeedListAdapter<RaceYourselfBean> {

    // TODO this copy feels ugly... needs to be same because they're both in run section, but could be cleaner.
    public static final long HEADER_ID = AutomatchAdapter.HEADER_ID;

    private Context context;
    private RaceYourselfBean raceYourselfBean;

    public static RaceYourselfAdapter create(@NonNull Context context, int resource) {
        String titleText = context.getString(R.string.home_feed_title_run);
        return new RaceYourselfAdapter(context, resource, titleText, ImmutableList.of(new RaceYourselfBean()));
    }

    private RaceYourselfAdapter(@NonNull Context context,
                                int resource,
                                @NonNull String titleText,
                                @NonNull List<RaceYourselfBean> items) {
        super(context, titleText, HEADER_ID, resource, items);
        this.context = context;
        this.raceYourselfBean = items.get(0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChallengeTitleView challengeTitleView;
        if (convertView == null) {
            challengeTitleView = ChallengeTitleView_.build(context);
        }
        else {
            challengeTitleView = (ChallengeTitleView) convertView;
        }

        challengeTitleView.bind(raceYourselfBean);

        return challengeTitleView;
    }
}
