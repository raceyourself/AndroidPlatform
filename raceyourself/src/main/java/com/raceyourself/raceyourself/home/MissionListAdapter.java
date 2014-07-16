package com.raceyourself.raceyourself.home;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.common.collect.Maps;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandableListItemAdapter;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 10/07/2014.
 */
@Slf4j
class MissionListAdapter extends ArrayAdapter<ChallengeNotificationBean> {

    //private final String DISTANCE_LABEL = NonSI.MILE.toString();
    //private final UnitConverter metresToMiles = SI.METER.getConverterTo(NonSI.MILE);
    private Map<Integer, ChallengeNotificationBean> notificationsById = Maps.newHashMap();
    private final Context context;

    public MissionListAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<ChallengeNotificationBean> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        for (ChallengeNotificationBean notif : items) {
            notificationsById.put(notif.getId(), notif);
        }
    }

    public ChallengeNotificationBean get(int id) {
        return notificationsById.get(id);
    }

    @Override
    public View getView(int groupPosition, View convertView, ViewGroup parent) {
        ChallengeDetailView challengeDetailView;
        if(convertView == null) {
            challengeDetailView = ChallengeDetailView_.build(context);
        }
        else {
            challengeDetailView = (ChallengeDetailView) convertView;
        }

        ChallengeNotificationBean currentChallenge = get(groupPosition);
        challengeDetailView.bind(currentChallenge);

        return challengeDetailView;
    }
}
