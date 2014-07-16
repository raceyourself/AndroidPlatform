package com.raceyourself.raceyourself.home.feed;

import com.raceyourself.platform.models.Mission;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 16/07/2014.
 */
@Slf4j
@Data
public class MissionBean {
    private int id;
    private LevelBean nextLevel;

    public MissionBean() {}

    public MissionBean(int id, LevelBean nextLevel) {
        this.id = id;
        this.nextLevel = nextLevel;
    }

    public MissionBean(Mission mission) {
<<<<<<< HEAD

=======
        
>>>>>>> added missions; changed package layout
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
