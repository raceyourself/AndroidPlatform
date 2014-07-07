package com.raceyourself.raceyourself.home;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.ChallengeNotification;
import com.raceyourself.platform.models.Notification;

import org.joda.time.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

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
    private boolean fromMe;
    private Calendar expiry;
    private ChallengeBean challenge;
    private boolean read;

    public ChallengeNotificationBean() {}

    public ChallengeNotificationBean(Notification notification) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        ChallengeNotification cNotif = om.readValue(notification.getMessage(), ChallengeNotification.class);

        Challenge challenge = Challenge.get(cNotif.challenge_id);

        id = notification.id;

        // Each challenge's expiry should be between yesterday and two days from now.
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(challenge.stop_time.getTime());
        setExpiry(cal);

        setRead(notification.isRead());

        DurationChallengeBean chal = new DurationChallengeBean();
        chal.setDistanceMetres(challenge.distance);
        chal.setDuration(new Duration(challenge.duration * 1000));
        setChallenge(chal);

        if (cNotif.from == AccessToken.get().getUserId())
            fromMe = true;
        // Make up a user
        UserBean user = new UserBean();
        if (fromMe)
            user.setName("User " + cNotif.to);
        else
            user.setName("User " + cNotif.from);
        setUser(user);
    }

    public static List<ChallengeNotificationBean> from(List<Notification> notifications) {
        ArrayList<ChallengeNotificationBean> beans = new ArrayList<ChallengeNotificationBean>(notifications.size());
        for (Notification notification : notifications) {
            try {
                beans.add(new ChallengeNotificationBean(notification));
            } catch (Throwable e) {
                log.error("Notification " + notification.id + " is malformed", e);
            }
        }
        Collections.sort(beans);
        return beans;
    }

    @Override
    public int compareTo(ChallengeNotificationBean another) {
        if (read != another.read)
            return Boolean.compare(read, another.read);
        if (expiry != null && another.expiry != null)
            return expiry.compareTo(another.expiry);
        return user.getName().compareTo(another.user.getName());
    }
}
