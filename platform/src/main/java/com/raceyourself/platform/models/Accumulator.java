package com.raceyourself.platform.models;

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
        return query(Challenge.class).where(and(eql("type", "cumulative"), eql("counter", this.id))).executeMulti();
    }

    public List<Challenge> getCompletedChallenges() {
        return query(Challenge.class).where(and(eql("type", "cumulative"),
                                                eql("counter", this.id),
                                                geq("value", this.value))).executeMulti();
    }


    @Override
    public int save() {
        log.info(this.id + " is " + this.value);
        return super.save();
    }
}
