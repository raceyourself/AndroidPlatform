package com.raceyourself.raceyourself.home;

import com.google.common.collect.ImmutableList;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
        List<Calendar> durations = new ArrayList<Calendar>();
        durations.add(new GregorianCalendar(0, 0, 0, 0, 5));
        durations.add(new GregorianCalendar(0, 0, 0, 0, 10));
        durations.add(new GregorianCalendar(0, 0, 0, 0, 15));
        durations.add(new GregorianCalendar(0, 0, 0, 0, 20));
        durations.add(new GregorianCalendar(0, 0, 0, 0, 25));
        durations.add(new GregorianCalendar(0, 0, 0, 0, 30));

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
            DurationChallengeBean chal = new DurationChallengeBean();
            chal.setDistanceMetres(rand.nextInt(5000) + rand.nextDouble() % 1d);
            chal.setDuration(durations.get(rand.nextInt(durations.size())));
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
