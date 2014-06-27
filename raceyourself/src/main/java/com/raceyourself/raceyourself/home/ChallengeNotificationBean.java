package com.raceyourself.raceyourself.home;

import android.graphics.Bitmap;

import java.util.Calendar;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 27/06/2014.
 */
@Slf4j
@Data
public class ChallengeNotificationBean implements Comparable<ChallengeNotificationBean> {
    private int id;
    private UserBean user;
    private Calendar expiry;
    private ChallengeBean challenge;
    private boolean read;

    @Override
    public int compareTo(ChallengeNotificationBean another) {
        if (read != another.read)
            return read ? -1 : 1;
        if (expiry != null && another.expiry != null)
            return expiry.compareTo(another.expiry);
        return user.getName().compareTo(another.user.getName());
    }
}
