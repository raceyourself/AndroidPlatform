package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nhaarman.listviewanimations.itemmanipulation.ExpandCollapseListener;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandableListItemAdapter;
import com.nhaarman.listviewanimations.util.AdapterViewUtil;

import java.util.List;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 18/07/2014.
 */
@Slf4j
public class ExpandableChallengeListAdapter extends ChallengeListAdapter {

    private Context context;
    private ExpandableDelegateAdapter expandableAdapter;

    public ExpandableChallengeListAdapter(@NonNull Context context,
                                          @NonNull List<ChallengeNotificationBean> items,
                                          @NonNull String title,
                                          long headerId) {
        super(context, items, title, headerId);
        this.context = context;
        expandableAdapter = new ExpandableDelegateAdapter(context, items);
    }

    public void setExpandCollapseListener(ExpandCollapseListener challengeVersusAnimator) {
        expandableAdapter.setExpandCollapseListener(challengeVersusAnimator);
    }

    /**
     * This nested class is necessary because we want to:
     *
     * 1. Extend functionality from FeedListAdapter/ChallengeListAdapter, and
     * 2. Extend from ExpandableListItemAdapter.
     */
    private class ExpandableDelegateAdapter extends ExpandableListItemAdapter<ChallengeNotificationBean> {
        private ListView mAbsListView;
        @Setter
        private int offset = 0;

        ExpandableDelegateAdapter(@NonNull Context context,
                                  @NonNull List<ChallengeNotificationBean> items) {
            super(context, items);

            // Only allow one expanded item at a time.
            setLimit(1);
        }

        /**
         * Unexpanded view of challenge.
         *
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        @Override
        public View getTitleView(int position, View convertView, ViewGroup parent) {
            log.debug("getTitleView, pos={}", position);

            ChallengeTitleView challengeTitleView;
            if (convertView == null) {
                challengeTitleView = ChallengeTitleView_.build(context);
            }
            else {
                challengeTitleView = (ChallengeTitleView) convertView;
            }

            ChallengeNotificationBean notif = getItem(position);
            log.debug("getTitleView, class={}", notif.toString());
            challengeTitleView.bind(notif);

            return challengeTitleView;
        }

        /**
         * Expanded part of challenge.
         *
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        @Override
        public View getContentView(int position, View convertView, ViewGroup parent) {
            ChallengeDetailView challengeDetailView;
            if(convertView == null) {
                challengeDetailView = ChallengeDetailView_.build(context);
            }
            else {
                challengeDetailView = (ChallengeDetailView) convertView;
            }

            ChallengeNotificationBean currentChallenge = get(position);
            challengeDetailView.bind(currentChallenge);

            return challengeDetailView;
        }

        public void setAbsListView(ListView view, int offset) {
            mAbsListView = view;
            this.offset = offset;
            super.setAbsListView(view);
        }

        public View getView(final int position) {
            View parentView = findViewForPosition(position+offset);

            return parentView;
        }

        private View findViewForPosition(final int position) {
            View result = null;
            for (int i = 0; i < mAbsListView.getChildCount() && result == null; i++) {
                View childView = mAbsListView.getChildAt(i);
                if (AdapterViewUtil.getPositionForView(mAbsListView, childView) == position) {
                    result = childView;
                }
            }
            return result;
        }
    }

    /**
     * The unexpanded view of the challenge.
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return expandableAdapter.getView(position, convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return expandableAdapter.isEmpty();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        expandableAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        expandableAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public int getCount() {
        return expandableAdapter.getCount();
    }

    @Override
    public ChallengeNotificationBean getItem(int position) {
        return expandableAdapter.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return expandableAdapter.getItemId(position);
    }

    @Override
    public boolean hasStableIds() {
        return expandableAdapter.hasStableIds();
    }

    @Override
    protected void insert(ChallengeNotificationBean a, int index) {
        expandableAdapter.add(index, a);
    }

    @Override
    protected void remove(ChallengeNotificationBean a) {
        expandableAdapter.remove(a);
    }

    @Override
    protected void addAll(List<ChallengeNotificationBean> challengeNotificationBeans) {
        expandableAdapter.addAll(challengeNotificationBeans);
    }

    @Override
    protected void clear() {
        expandableAdapter.clear();
    }

    @Override
    public void notifyDataSetChanged() {
        expandableAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return expandableAdapter.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        return expandableAdapter.isEnabled(position);
    }

    public void setAbsListView(ListView wrappedList, int offset) {
        expandableAdapter.setAbsListView(wrappedList, offset);
    }

    public void setListOffset(int offset) {
        expandableAdapter.setOffset(offset);
    }

    public View getView(int position) {
        return expandableAdapter.getView(position);
    }
}
