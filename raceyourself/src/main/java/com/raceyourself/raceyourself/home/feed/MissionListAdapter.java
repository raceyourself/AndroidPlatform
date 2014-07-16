package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 10/07/2014.
 */
@Slf4j
public class MissionListAdapter extends ArrayAdapter<MissionBean> {

    //private final String DISTANCE_LABEL = NonSI.MILE.toString();
    //private final UnitConverter metresToMiles = SI.METER.getConverterTo(NonSI.MILE);
    private Map<Integer, MissionBean> byId = Maps.newHashMap();
    private final Context context;

    public MissionListAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<MissionBean> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        for (MissionBean notif : items) {
            byId.put(notif.getId(), notif);
        }
    }

    public MissionBean get(int id) {
        return byId.get(id);
    }

    @Override
    public View getView(int groupPosition, View convertView, ViewGroup parent) {
        MissionView missionView;
        if(convertView == null) {
            missionView = MissionView_.build(context);
        }
        else {
            missionView = (MissionView) convertView;
        }

        MissionBean currentMission = get(groupPosition);
        missionView.bind(currentMission);

        return missionView;
    }
}
