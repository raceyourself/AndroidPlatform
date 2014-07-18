package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;

import lombok.extern.slf4j.Slf4j;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by Duncan on 17/07/2014.
 */
@Slf4j
public class StickyRunHeaderAdapter {
    private static StickyRunHeaderAdapter instance;

    private StickyRunHeaderAdapter() {}

    public static StickyRunHeaderAdapter getInstance() {
        if (instance == null)
            instance = new StickyRunHeaderAdapter();
        return instance;
    }

    /**
     * The section heading this challenge falls under.
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    public View getHeaderView(Context context, String titleText, int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.fragment_header, parent, false);
        }

        convertView.setBackgroundColor(Color.WHITE);

        TextView title = (TextView) convertView.findViewById(R.id.textView);
        title.setText(titleText);

        View missions = convertView.findViewById(R.id.missionsProgress);
        missions.setVisibility(View.GONE);

        return convertView;
    }

    public long getHeaderId(int i) {
        return 48234972034832998L; // must be unique
    }
}
