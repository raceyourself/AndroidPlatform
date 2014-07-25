package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by Duncan on 17/07/2014.
 */
@Slf4j
public abstract class FeedListAdapter<T> implements StickyListHeadersAdapter {
    private final Context context;
    @Setter @Getter
    private int stickyHeaderBackgroundColor = Color.WHITE;
    @Setter @Getter
    private int stickyHeaderTextColor = 0xff969487;
    private String titleText;
    private long headerId;

    /**
     * @param context The usual Context God object...
     * @param titleText Text that appears in sticky header.
     * @param headerId UID for a section header. Give the same headerId iff two instances should appear under a common
     *                 header.
     */
    public FeedListAdapter(@NonNull Context context, @NonNull String titleText, long headerId) {
        this.context = context;
        this.titleText = titleText;
        this.headerId = headerId;
    }

    /**
     * The section heading this challenge falls under.
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.fragment_header, parent, false);
        }

        convertView.setBackgroundColor(getStickyHeaderBackgroundColor());

        TextView title = (TextView) convertView.findViewById(R.id.textView);
        title.setText(titleText);
        title.setTextColor(getStickyHeaderTextColor());

        View missions = convertView.findViewById(R.id.missionsProgress);
        missions.setVisibility(View.GONE);

        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        return headerId;
    }
}
