package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.widget.ListView;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 *
 *
 * Created by Duncan on 10/07/2014.
 */
@Slf4j
public abstract class ChallengeListAdapter extends FeedListAdapter<ChallengeNotificationBean> {

    //private final String DISTANCE_LABEL = NonSI.MILE.toString();
    //private final UnitConverter metresToMiles = SI.METER.getConverterTo(NonSI.MILE);
    @Getter
    private Map<Integer, ChallengeNotificationBean> notificationsById = Maps.newHashMap();

    public ChallengeListAdapter(@NonNull Context context, @NonNull List<ChallengeNotificationBean> items,
                                @NonNull String title, long headerId) {
        super(context, title, headerId);

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
            clear();
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
                    addAll(notifications.subList(index, notifications.size()));
                    log.info("Challenge notifications: tail insertion of " + (notifications.size() - index) + " at " + index);
                    index = notifications.size();
                    break;
                }
                ChallengeNotificationBean a = notifications.get(index);
                ChallengeNotificationBean b = getItem(index);
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
                    remove(a);
                    insert(a, index);
                    log.info("Challenge notifications: " + a.getId() + " inserted at " + index);
                    index++;
                    continue;
                }
                // Items did not match and old item was earlier in the ordering, ie. removed.
                // TODO: Animate removal
                remove(b);
                log.info("Challenge notifications: removal " + b.getId() + " at " + index);
            }
            // Remove tail items not in new list
            for (; index < getCount();) {
                ChallengeNotificationBean b = this.getItem(index);
                // TODO: Animate removal
                remove(b);
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

    // NOTE: we can't simply take a delegate Adapter as a param, because there is no single class or interface that
    // all the adapters we use inherits from or extends. Subclasses should implement the methods below by delegating
    // to an appropriate Adapter implementation.

    @Override
    public abstract ChallengeNotificationBean getItem(int position);

    protected abstract void insert(ChallengeNotificationBean a, int index);

    protected abstract void remove(ChallengeNotificationBean a);

    protected abstract void addAll(List<ChallengeNotificationBean> challengeNotificationBeans);

    protected abstract void clear();

    public abstract void notifyDataSetChanged();
}
