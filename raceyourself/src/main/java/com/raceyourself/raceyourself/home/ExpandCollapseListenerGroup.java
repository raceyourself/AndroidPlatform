package com.raceyourself.raceyourself.home;

import com.nhaarman.listviewanimations.itemmanipulation.ExpandCollapseListener;

import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 21/07/2014.
 */
public class ExpandCollapseListenerGroup implements ExpandCollapseListener {
    private List<? extends ExpandCollapseListener> listeners;

    public ExpandCollapseListenerGroup(@NonNull List<? extends ExpandCollapseListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onItemExpanded(int position) {
        for (ExpandCollapseListener listener : listeners) {
            listener.onItemExpanded(position);
        }
    }

    @Override
    public void onItemCollapsed(int position) {
        for (ExpandCollapseListener listener : listeners) {
            listener.onItemCollapsed(position);
        }
    }
}
