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
public class ChallengeNotificationBean {
    private UserBean user;
    private Calendar expiry;
    private ChallengeBean challenge;
}
