package com.raceyourself.raceyourself.game;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 30/06/2014.
 */
@Slf4j
public class GameConfiguration {

    @Getter
    private final GameType gameType;
    @Getter
    private final double targetDistance;
    @Getter
    private final long targetTime;
    @Getter
    private final long countdown;

    /**
     * Private constructor - use GameStrategy.GameStrategyBuilder to create instances
     */
    private GameConfiguration(GameStrategyBuilder builder) {
        this.gameType = builder.gameType;
        this.targetDistance = builder.targetDistance;
        this.targetTime = builder.targetTime;
        this.countdown = builder.countdown;
    }

    public enum GameType {
        TIME_CHALLENGE ("M", "TIME REMAINING", "mins"),
        DISTANCE_CHALLENGE("M", "DISTANCE REMAINING", "meters");

        @Getter
        String aheadBehindUnit;
        @Getter
        String remainingText;
        @Getter
        String targetUnitMedium;

        GameType(String aheadBehindUnit, String remainingText, String targetUnitMedium) {
            this.aheadBehindUnit = aheadBehindUnit;
            this.remainingText = remainingText;
            this.targetUnitMedium = targetUnitMedium;
        }

    }

    public static class GameStrategyBuilder {
        private final GameType gameType;
        private double targetDistance = 0;
        private long targetTime = 0;
        private long countdown = 0;

        public GameStrategyBuilder(GameType gameType) {
            this.gameType = gameType;
        }

        public GameStrategyBuilder targetDistance(double targetDistance) {
            if (gameType != GameType.DISTANCE_CHALLENGE) throw new RuntimeException("Cannot set targetDistance unless gameType is DISTANCE_CHALLENGE");
            this.targetDistance = targetDistance;
            return this;
        }

        public GameStrategyBuilder targetTime(long targetTime) {
            if (gameType != GameType.TIME_CHALLENGE) throw new RuntimeException("Cannot set targetTime unless gameType is TIME_CHALLENGE");
            this.targetTime = targetTime;
            return this;
        }

        public GameStrategyBuilder countdown(long countdownTime) {
            this.countdown = countdownTime;
            return this;
        }

        public GameConfiguration build() {
            if (targetTime == 0 && targetDistance == 0) throw new RuntimeException("Must set targetTime or targetDistance to build valid GameStrategy");
            return new GameConfiguration(this);
        }

    }


}
