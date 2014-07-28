package com.raceyourself.raceyourself.base;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Track;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.matchmaking.DurationView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.joda.time.Duration;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A DurationView wherein the user is obliged to select a distance they've already run.
 *
 * Created by Duncan on 08/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public abstract class PreviouslyRunDurationView extends DurationView {
    @ViewById
    @Getter(AccessLevel.PROTECTED)
    TextView lengthWarning;
    @Getter(AccessLevel.PROTECTED)
    private SortedMap<Integer, Pair<Track, MatchQuality>> availableOwnTracksMap;

    protected PreviouslyRunDurationView(Context context) {
        super(context);

        populateAvailableUserTracksMap();
    }

    @AfterViews
    protected void afterViews() {
        lengthWarning.setVisibility(View.VISIBLE);
        lengthWarning.setText("");
    }

    /**
     * We need to match the selected duration with an available track. Considerations:
     *
     * 1. Duration. Ideally about the same as the selected duration; failing that longer, failing that, shorter.
     * 2. Age. Favour recent tracks.
     *
     * We prioritise duration. Among similarly 'qualified' tracks, we go with the most recent.
     */
    private void populateAvailableUserTracksMap() {
        SortedMap<Integer,Pair<Track,MatchQuality>> durationToTrackId = Maps.newTreeMap();

        int playerUserId = AccessToken.get().getUserId();
        List<Track> playerTracks = Track.getTracks(playerUserId);
        for (int durationMins = MIN_DURATION_MINS; durationMins <= MAX_DURATION_MINS; durationMins += STEP_SIZE_MINS) {
            long desiredDurationMillis = durationMins * 60 * 1000;
            List<Track> matches = Lists.newArrayList();
            MatchQuality quality = null;
            for (Track candidate : playerTracks) {
                // Go 2.5 mins above desired duration.
                if (candidate.time >= (desiredDurationMillis - 60L * 1000L) &&
                        candidate.time < (desiredDurationMillis + 150L * 1000L)) {
                    matches.add(candidate);
                    quality = MatchQuality.GOOD;
                }
            }
            if (matches.isEmpty()) {
                // If they've not run the desired distance, pick a longer run (so we can truncate).
                for (Track candidate : playerTracks) {
                    if (candidate.time >= desiredDurationMillis) {
                        matches.add(candidate);
                        quality = MatchQuality.TRACK_TOO_LONG;
                    }
                }
            }
            if (matches.isEmpty()) {
                // Final fallback: shorter tracks.
                matches = playerTracks;
                quality = MatchQuality.TRACK_TOO_SHORT;
            }
            Collections.sort(matches, new Comparator<Track>() {
                @Override
                public int compare(Track lhs, Track rhs) {
                    return (int) (lhs.ts - rhs.ts); // TODO in practice, is this cast safe...?
                }
            });
            Track newestOfFiltered = matches.get(0);
            durationToTrackId.put(durationMins, new Pair(newestOfFiltered, quality));
        }
        availableOwnTracksMap = durationToTrackId;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);

        if (availableOwnTracksMap.isEmpty()) {
            log.debug("availableOwnTracksMap empty; quitting onProgressChanged()." +
                    " Legit if this occurs from onCreate(); dubious if observed repeatedly.");
            return;
        }
        Duration duration = getDuration();
        MatchQuality quality = availableOwnTracksMap.get((int) duration.getStandardMinutes()).second;

        String qualityWarning = quality.getMessageId() == null ? "" :
                String.format(context.getString(quality.getMessageId()), duration);
        getLengthWarning().setText(qualityWarning);

        final boolean enable = quality != MatchQuality.TRACK_TOO_SHORT;
        // Disable send button if no runs recorded that are long enough.
        // Having a run that's too long is fine - we can truncate it.
        Button findBtn = getFindBtn();
        findBtn.setEnabled(enable);
        findBtn.setClickable(enable);
    }

    public enum MatchQuality {
        GOOD(null),
        TRACK_TOO_LONG(R.string.send_challenge_track_too_long),
        TRACK_TOO_SHORT(R.string.send_challenge_track_too_short);

        @Getter
        private final Integer messageId;

        MatchQuality(Integer messageId) {
            this.messageId = messageId;
        }
    }
}
