package com.raceyourself.raceyourself.home;

import com.google.common.collect.ImmutableList;

import org.joda.time.Duration;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyChallenges {

    //public static List<DummyItem> ITEMS = new ArrayList<DummyItem>();
    /**
     * An array of sample (dummy) items.
     */
    public static SortedSet<ChallengeNotificationBean> ITEMS = new TreeSet<ChallengeNotificationBean>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static Map<Integer, ChallengeNotificationBean> ITEM_MAP = new HashMap<Integer, ChallengeNotificationBean>();

    static {
        Random rand = new Random();
        Calendar now = Calendar.getInstance();
        List<String> firstNames = ImmutableList.of("Joe", "Sue", "George", "Stuart", "Jane");
        List<String> surnames = ImmutableList.of("Smith", "Jones", "Longbottom", "Stevens", "Bolton");

        for (int i = 0; i < 30; i++) {
            ChallengeNotificationBean notif = new ChallengeNotificationBean();
            notif.setId(i);

            // Each challenge's expiry should be between yesterday and two days from now.
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -1);
            cal.add(Calendar.MINUTE, rand.nextInt(60 * 24 * 3));
            notif.setExpiry(cal);
            // Sprinkle a few read challenges among the ones in the future
            notif.setRead(i % 2 == 0 || cal.before(now));
            // Define a challenge
            ChallengeBean chal = new ChallengeBean();
            chal.setChallengeGoal(5+rand.nextInt(25/5)*5);
            chal.setType("duration");

            notif.setChallenge(chal);
            // Make up a user
            UserBean user = new UserBean();
            user.setName(firstNames.get(rand.nextInt(firstNames.size())) + " " + surnames.get(rand.nextInt(surnames.size())));
            notif.setUser(user);

            addItem(notif);
        }
    }

    private static void addItem(ChallengeNotificationBean item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.getId(), item);
    }
}
