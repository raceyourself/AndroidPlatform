package com.raceyourself.raceyourself.home;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import lombok.val;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyFriends {

    //public static List<DummyItem> ITEMS = new ArrayList<DummyItem>();
    /**
     * An array of sample (dummy) items.
     */
    public static SortedSet<UserBean> ITEMS = new TreeSet<UserBean>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static Map<Integer, UserBean> ITEM_MAP = new HashMap<Integer, UserBean>();

    static {
        Random rand = new Random();
        Calendar now = Calendar.getInstance();
        List<String> firstNames = Arrays.asList("Joe", "Sue", "George", "Stuart", "Jane");
        List<String> surnames = Arrays.asList("Smith", "Jones", "Longbottom", "Stevens", "Bolton");
        UserBean.JoinStatus[] joinStatuses = UserBean.JoinStatus.values();

        for (int i = 0; i < 200; i++) {
            // Make up a user
            UserBean user = new UserBean();
            user.setName(firstNames.get(rand.nextInt(firstNames.size())) + " " + surnames.get(rand.nextInt(surnames.size())));
            user.setJoinStatus(joinStatuses[i % joinStatuses.length]);

            addItem(user);
        }
    }

    private static void addItem(UserBean item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.getId(), item);
    }
}
