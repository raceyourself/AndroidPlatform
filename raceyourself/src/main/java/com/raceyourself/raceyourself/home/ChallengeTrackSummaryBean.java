package com.raceyourself.raceyourself.home;

import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Track;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 07/07/2014.
 */
@Slf4j
@Data
public class ChallengeTrackSummaryBean {
    Challenge challenge;
    Track playerTrack;
    TrackSummaryBean playerTrackBean;
    Track opponentTrack;
    TrackSummaryBean opponentTrackBean;
}
