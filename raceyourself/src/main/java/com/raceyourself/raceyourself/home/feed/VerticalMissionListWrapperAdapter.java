package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.google.common.collect.Lists;
import com.raceyourself.platform.models.Mission;
import com.raceyourself.raceyourself.R;

import java.util.List;

import it.sephiroth.android.library.widget.HListView;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The horizontally-scrolling list fills exactly one row of the master, vertical list, irrespective of how many elements
 * it contains. This one row contains a HListView, managed by a HorizontalMissionListAdapter.
 *
 * Created by Duncan on 10/07/2014.
 */
@Slf4j
public class VerticalMissionListWrapperAdapter extends ArrayAdapter<Object> {
    private final Context context;

    public static VerticalMissionListWrapperAdapter create(@NonNull Context context, int textViewResourceId) {
        List<Object> dummyItemList = Lists.newArrayList(new Object());

        return new VerticalMissionListWrapperAdapter(context, textViewResourceId, dummyItemList);
    }

    private VerticalMissionListWrapperAdapter(@NonNull Context context, int textViewResourceId, List<Object> items) {
        super(context, textViewResourceId, items);
        this.context = context;
    }

    @Override
    public View getView(int groupPosition, View convertView, ViewGroup parent) {
        LinearLayout vWrapper;
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            vWrapper = (LinearLayout) inflater.inflate(R.layout.fragment_mission_list, null);

            HListView hListView = (HListView) vWrapper.findViewById(R.id.missionList);

            List<Mission> missions = Mission.getMissions();
            List<MissionBean> missionBeans = MissionBean.from(missions);

            HorizontalMissionListAdapter adapter = new HorizontalMissionListAdapter(
                    context, R.layout.fragment_mission_list, missionBeans);

            hListView.setAdapter(adapter);
        }
        else {
            vWrapper = (LinearLayout) convertView;
        }

        // Mission list appears exactly once in horizontal list. Thus cannot be recycled by Android, thus
        // if non-null, we know it's already set up correctly.

        return vWrapper;
    }
}
