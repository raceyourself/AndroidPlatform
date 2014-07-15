package com.raceyourself.platform.models;

public class ChallengeNotification {
    public String type;
    public int from;
    public int to;
    // Challenge id tuple
    public int device_id;
    public int challenge_id;
    public String challenge_type;
    public String taunt;

    public ChallengeNotification() {}

    public ChallengeNotification(int from, int to, Challenge challenge) {
        this.type = "challenge";
        this.from = from;
        this.to = to;
        this.device_id = challenge.device_id;
        this.challenge_id = challenge.challenge_id;
        this.challenge_type = challenge.type;
    }
}
