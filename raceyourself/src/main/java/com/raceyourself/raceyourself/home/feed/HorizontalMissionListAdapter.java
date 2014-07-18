package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 16/07/2014.
 */
@Slf4j
public class HorizontalMissionListAdapter extends ArrayAdapter<MissionBean> {
    private Map<String, MissionBean> byId = Maps.newHashMap();
    private Context context;

    @Setter
    private HorizontalMissionListAdapter.OnFragmentInteractionListener onFragmentInteractionListener;

    public HorizontalMissionListAdapter(@NonNull Context context, int textViewResourceId, List<MissionBean> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        for (MissionBean mission : items) {
            byId.put(mission.getId(), mission);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MissionView missionView;
        if (convertView == null) {
            missionView = MissionView_.build(context);
        }
        else {
            missionView = (MissionView) convertView;
        }

        final MissionBean currentMission = getItem(position);
        missionView.bind(currentMission);
        missionView.setOnClickListener(new MissionView.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onFragmentInteractionListener != null) onFragmentInteractionListener.onFragmentInteraction(currentMission, v);
            }
        });

        return missionView;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(MissionBean mission, View view);
    }
}
