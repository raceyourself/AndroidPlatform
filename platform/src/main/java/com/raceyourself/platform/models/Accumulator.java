package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.raceyourself.platform.utils.Format;
import com.raceyourself.platform.utils.UnitConversion;
import com.roscopeco.ormdroid.Entity;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.eql;
import static com.roscopeco.ormdroid.Query.geq;

/**
 * A cumulative value.
 *
 * Consistency model: Client can increment while offline.
 *                    Server can replace using id.
 */
@Slf4j
public class Accumulator extends Entity {
    @JsonProperty("name")
    public String id;
    public double value;

    public static final String DISTANCE_TRAVELLED = "distance_travelled";
    public static final String TIME_TRAVELLED = "time_travelled";
    public static final String HEIGHT_ASCENDED = "height_ascended";
    public static final String HEIGHT_DESCENDED = "height_descended";
    public static final String TRACKS_COMPLETED = "tracks_completed";
    public static final String CHALLENGES_SENT = "challenges_sent";

    public Accumulator() {}

    public Accumulator(String id) {
        this.id = id;
        this.value = 0.0;
    }

    public static double get(String id) {
        Accumulator cnt = query(Accumulator.class).where(eql("id", id)).execute();
        if (cnt == null) return 0.0;
        return cnt.value;
    }

    public static String getProgressString(String counter, int value, String locale) {
        int count = (int)Accumulator.get(counter);
        if (count >= value) return "Completed";

        double convertedCount = count;
        double convertedValue = value;

        if(counter.equals(HEIGHT_ASCENDED) || counter.equals(HEIGHT_DESCENDED)) {
            if(locale.equals(Challenge.IMPERIAL_LOCALE)) {
                convertedCount = UnitConversion.feet(count);
                convertedValue = UnitConversion.feet(value);
                String formattedCount = Format.zeroDp(convertedCount);
                String formattedValue = Format.zeroDp(convertedValue);
                return formattedCount + "ft /" + formattedValue + "ft";
            } else if(locale.equals(Challenge.METRIC_LOCALE)) {
                if(convertedValue > 1000) {
                    String formattedCount = Format.twoDp(convertedCount / 1000);
                    String formattedValue = Format.twoDp(convertedValue / 1000);
                    return formattedCount + "km / " + formattedValue + "km";
                } else {
                    String formattedCount = Format.zeroDp(convertedCount);
                    String formattedValue = Format.zeroDp(convertedValue);
                    return formattedCount + "m / " + formattedValue + "m";
                }
            }
        } else if(counter.equals(DISTANCE_TRAVELLED)) {
            if(locale.equals(Challenge.IMPERIAL_LOCALE)) {
                convertedCount = UnitConversion.miles(count);
                convertedValue = UnitConversion.miles(value);
                String formattedCount = Format.twoDp(convertedCount);
                String formattedValue = Format.twoDp(convertedValue);
                return formattedCount + "mi / " + formattedValue + "mi";
            } else if(locale.equals(Challenge.METRIC_LOCALE)) {
                if(convertedValue > 1000) {
                    String formattedCount = Format.twoDp(convertedCount / 1000);
                    String formattedValue = Format.twoDp(convertedValue / 1000);
                    return formattedCount + "km / " + formattedValue + "km";
                } else {
                    String formattedCount = Format.zeroDp(convertedCount);
                    String formattedValue = Format.zeroDp(convertedValue);
                    return formattedCount + "m / " + formattedValue + "m";
                }
            }
        } else if(counter.equals(TIME_TRAVELLED)) {
            if(value > 3600000) {
                convertedCount = UnitConversion.hours(count);
                convertedValue = UnitConversion.hours(value);
                int countAsInt = (int)convertedCount;
                String divider = countAsInt == 1 ? "hr / " : "hrs / ";
                return Format.zeroDp(convertedCount) + divider + Format.zeroDp(convertedValue) + "hrs";
            } else {
                convertedCount = UnitConversion.minutes(count);
                convertedValue = UnitConversion.minutes(value);
                int countAsInt = (int)convertedCount;
                String divider = countAsInt == 1 ? "min / " : "mins / ";
                return Format.zeroDp(convertedCount) + divider + Format.zeroDp(convertedValue) + "mins";
            }
        }
        if (convertedValue > 1000) return String.valueOf((int)getProgressPercentage(count, value)) + "%";

        return count + "/" + value;
    }

    public static double getProgressPercentage(int count, int value) {
        return Math.min(100.0, count * 100 / value);
    }

    public synchronized static double add(String id, double value) {
        Accumulator cnt = query(Accumulator.class).where(eql("id", id)).execute();
        if (cnt == null) {
            cnt = new Accumulator(id);
        }
        cnt.value += value;
        cnt.save();
        return cnt.value;
    }

    public List<Challenge> getChallenges() {
        return query(Challenge.class).where(and(eql("type", "counter"), eql("counter", this.id))).executeMulti();
    }

    public List<Challenge> getCompletedChallenges() {
        return query(Challenge.class).where(and(eql("type", "counter"),
                                                eql("counter", this.id),
                                                geq("value", this.value))).executeMulti();
    }



}
