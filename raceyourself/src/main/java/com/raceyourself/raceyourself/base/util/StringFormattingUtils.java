package com.raceyourself.raceyourself.base.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.text.SimpleDateFormat;

/**
 * Created by Amerigo on 07/07/2014.
 */
public class StringFormattingUtils {

    public final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm");

    public static String getForename(String name) {
        String forename = "";
        int i = name.indexOf(' ');
        if(i>0) {
            forename = name.substring(0, i);
        } else {
            forename = name;
        }

        return forename;
    }

    public static String getForenameAndInitial(String name) {
        String forename = getForename(name);
        String surname = name.substring(name.lastIndexOf(" ")+1);
        char initial = surname.charAt(0);
        String finalName = forename;
        if(!forename.equalsIgnoreCase(surname)) {
            finalName = forename + " " + initial;
        }

        return finalName;
    }

    public static String getDistanceInKmString(int distance) {
        float distanceInKm = distance / 1000.f;
        String formattedDistance = "";
        if(distanceInKm >= 10) {
            formattedDistance = String.format("%.1f", distanceInKm);
        } else {
            formattedDistance = String.format("%.2f", distanceInKm);
        }
        return formattedDistance;
    }
}
