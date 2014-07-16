package com.raceyourself.raceyourself.home.feed;

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
        private int level;
        private int challengeId;

        public LevelBean() {}

        public LevelBean(String id, String mission, int level, ChallengeBean challenge) {
            this.id = id;
            this.mission = mission;
            this.level = level;
            this.challengeId = challenge.getChallengeId();
        }

        public LevelBean(Mission.MissionLevel level) {
            this.id = level.id;
            this.mission = level.mission;
            this.level = level.level;
            this.challengeId = level.challenge_id;
        }

//        public ChallengeBean getChallengeBean() {
//            // TODO do lookup based on challengeId
//        }
    }
}
