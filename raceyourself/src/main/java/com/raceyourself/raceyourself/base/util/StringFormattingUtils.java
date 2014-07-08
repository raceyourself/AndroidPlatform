package com.raceyourself.raceyourself.base.util;

/**
 * Created by Amerigo on 07/07/2014.
 */
public class StringFormattingUtils {

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
        String finalName = forename + " " + initial;
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

