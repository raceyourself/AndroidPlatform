package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.collect.Maps;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandableListItemAdapter;
import com.raceyourself.raceyourself.home.ChallengeDetailView_;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 10/07/2014.
 */
@Slf4j
class ChallengeListAdapter extends ExpandableListItemAdapter<ChallengeNotificationBean> {

    //private final String DISTANCE_LABEL = NonSI.MILE.toString();
    //private final UnitConverter metresToMiles = SI.METER.getConverterTo(NonSI.MILE);
    private Map<Integer, ChallengeNotificationBean> notificationsById = Maps.newHashMap();
    private final Context context;

    public ChallengeListAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<ChallengeNotificationBean> items) {
        super(context, items);
        this.context = context;
        for (ChallengeNotificationBean notif : items) {
            notificationsById.put(notif.getId(), notif);
        }
    }

    public ChallengeNotificationBean getChallengeNotificationBeanById(int id) {
        return notificationsById.get(id);
    }

    public synchronized void mergeItems(@NonNull List<ChallengeNotificationBean> notifications) {
        final boolean DEBUG = true;
        if (notifications.isEmpty()) {
            this.clear();
            log.info("Challenge notifications list cleared");
            return;
        }
        try {
            // Iterate over new list and old list (in sorted order), matching, adding and removing items
            int index = 0;
            while (index < notifications.size()) {
                // At end of old/current list - nothing to match against: add remaining from new list and
                // break out of loop
                if (index >= getCount()) {
                    // TODO: Animate insertion
                    this.addAll(notifications.subList(index, notifications.size()));
                    log.info("Challenge notifications: tail insertion of " + (notifications.size() - index) + " at " + index);
                    index = notifications.size();
                    break;
                }
                ChallengeNotificationBean a = notifications.get(index);
                ChallengeNotificationBean b = this.getItem(index);
                // Same notification: skip over item
                if (a.getId() == b.getId()) {
                    // TODO: Do we need to copy any data over?
                    log.info("Challenge notifications: " + a.getId() + " match at " + index);
                    index++;
                    continue;
                }
                int cmp = a.compareTo(b);
                // New notification to be placed before or in same position as current b,
                // insert above b and continue (next iteration will compare next a to same b)
                if (cmp <= 0) {
                    // TODO: Animate insertion
                    this.remove(a);
                    this.add(index, a);
                    log.info("Challenge notifications: " + a.getId() + " inserted at " + index);
                    index++;
                    continue;
                }
                // Items did not match and old item was earlier in the ordering, ie. removed.
                // TODO: Animate removal
                this.remove(b);
                log.info("Challenge notifications: removal " + b.getId() + " at " + index);
            }
            // Remove tail items not in new list
            for (; index < getCount();) {
                ChallengeNotificationBean b = this.getItem(index);
                // TODO: Animate removal
                this.remove(b);
                log.info("Challenge notifications: " + b.getId() + " tail removal at " + index);
            }
            if (DEBUG) {
                /// DEBUG
                if (notifications.size() != getCount())
                    throw new Error("ASSERT: size mismatch: " + notifications.size() + " != " + getCount());
                boolean error = false;
                for (int i = 0; i < notifications.size(); i++) {
                    if (notifications.get(i).getId() != getItem(i).getId()) {
                        log.error("Challenge notification list has not been updated correctly at index: " + i + ": " + notifications.get(i).getId() + " != " + getItem(i).getId());
                        error = true;
                    }
                    if (error) {
                        throw new Error("ASSERT: merge errors!");
                    }
                }
                /// DEBUG
            }
        } catch (Exception e) {
            log.error("Error in ChallengeListAdapter::mergeItems algorithm, attempting to recover with a clear+addAll", e);
            this.clear();
            this.addAll(notifications);
        }
        for (ChallengeNotificationBean notif : notifications) {
            notificationsById.put(notif.getId(), notif);
        }
        log.info("Updated challenge notification list. There are now {} challenges.", getCount());
    }

    @Override
    public View getTitleView(int position, View convertView, ViewGroup parent) {
        ChallengeTitleView challengeTitleView;
        if (convertView == null) {
            challengeTitleView = ChallengeTitleView_.build(context);
        }
        else {
            challengeTitleView = (ChallengeTitleView) convertView;
        }

        ChallengeNotificationBean notif = getItem(position);
        challengeTitleView.bind(notif);

        return challengeTitleView;
    }

    @Override
    public View getContentView(int groupPosition, View convertView, ViewGroup parent) {
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
