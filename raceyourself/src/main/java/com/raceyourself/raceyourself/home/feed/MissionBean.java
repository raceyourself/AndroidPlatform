package com.raceyourself.raceyourself.home.feed;

import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Mission;
import com.raceyourself.platform.models.Notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 16/07/2014.
 */
@Slf4j
@Data
public class MissionBean {
    private String id;
    private LevelBean currentLevel;

    public MissionBean() {}

    public MissionBean(String id, LevelBean currentLevel) {
        this.id = id;
        this.currentLevel = currentLevel;
    }

    public MissionBean(Mission mission) {
        id = mission.id;
        currentLevel = new LevelBean(mission.getCurrentLevel());
    }

    public static List<MissionBean> from(List<Mission> missions) {
        List<MissionBean> beans = new ArrayList<MissionBean>(missions.size());
        for (Mission mission : missions) {
            try {
                beans.add(new MissionBean(mission));
            } catch (Throwable e) {
                log.error("Mission " + mission.id + " is malformed", e);
            }
        }
        return beans;
    }

    @Data
    public class LevelBean {
        private String id;
        private String mission;
        private String longDescription;
        private int level;
        private boolean completed;
        private boolean claimed;
        private int deviceId;
        private int challengeId;
        private double progressPct;
        private String progressText;
        private String description;


        public LevelBean() {}

        public LevelBean(Mission.MissionLevel level) {
            this.id = level.id;
            this.mission = level.mission;
            this.level = level.level;
            this.completed = level.isCompleted();
            this.claimed = level.isClaimed();
            Challenge challenge = level.getChallenge();
            this.deviceId = challenge.device_id;
            this.challengeId = challenge.challenge_id;
            this.progressPct = challenge.getProgressPercentage();
            this.progressText = challenge.getProgressString();
            this.description = challenge.name;
            this.longDescription = challenge.description;
        }

    }
}
