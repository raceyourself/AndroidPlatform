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
 * Created by Duncan on 16/07/2014.
 */
@Slf4j
public class HorizontalMissionListAdapter extends ArrayAdapter<MissionBean> {
    private Map<String, MissionBean> byId = Maps.newHashMap();
    private Context context;

    public HorizontalMissionListAdapter(@NonNull Context context, int textViewResourceId, List<MissionBean> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        for (MissionBean mission : items) {
            byId.put(mission.getId(), mission);
        }
    }

    @Override
    public View getView(int groupPosition, View convertView, ViewGroup parent) {
        MissionView missionView;
        if (convertView == null) {
            missionView = MissionView_.build(context);
        }
        else {
            missionView = (MissionView) convertView;
        }

        MissionBean currentMission = byId.get(groupPosition);
        missionView.bind(currentMission);

        return missionView;
    }
}
