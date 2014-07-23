package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.utils.Format;
import com.raceyourself.platform.utils.UnitConversion;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.home.UserBean;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 15/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.fragment_inbox_expanded)
public class ChallengeDetailView extends ScrollView {
    private Context context;

    @ViewById
    TextView trackDistance;

    @ViewById
    TextView ascentText;

    @ViewById
    TextView descentText;

    @ViewById
    TextView trackLength;

    @ViewById
    Button ignoreBtn;

    @ViewById
    Button acceptBtn;

    @ViewById
    ImageView dividerLine3;

    @ViewById
    ImageView dividerCircle4;

    @ViewById
    ImageView rankIcon;

    @Setter
    private OnInboxChallengeAction onInboxChallengeAction;

    private Integer notificationId = null;

    public ChallengeDetailView(Context context) {
        super(context);
        this.context = context;
    }

    public ChallengeDetailView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public ChallengeDetailView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void bind(@NonNull final ChallengeNotificationBean currentChallenge) {
        this.notificationId = currentChallenge.getId();

        if (currentChallenge.getDetails() == null) {
            User player = SyncHelper.getUser(AccessToken.get().getUserId());
            final UserBean playerBean = new UserBean(player);

            final ChallengeDetailBean activeChallengeFragment = new ChallengeDetailBean();
            activeChallengeFragment.setOpponent(currentChallenge.getOpponent());
            activeChallengeFragment.setPlayer(playerBean);
            activeChallengeFragment.setChallenge(currentChallenge.getChallenge());
            activeChallengeFragment.setNotificationId(currentChallenge.getId());
            currentChallenge.setDetails(activeChallengeFragment);
        }

        String duration = StringFormattingUtils.ACTIVITY_PERIOD_FORMAT.print(
                currentChallenge.getDetails().getChallenge().getDuration().toPeriod());
        trackLength.setText(duration);

        int buttonVisibility = currentChallenge.isInbox() ? View.VISIBLE : View.GONE;
        ignoreBtn.setVisibility(buttonVisibility);
        acceptBtn.setVisibility(buttonVisibility);
        dividerLine3.setVisibility(buttonVisibility);
        dividerCircle4.setVisibility(buttonVisibility);

        if (currentChallenge.isInbox()) {
            ignoreBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onInboxChallengeAction.onIgnore(currentChallenge);
                }
            });
            acceptBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onInboxChallengeAction.onAccept(currentChallenge);
                }
            });
        }

        if (currentChallenge.getDetails().getOpponentTrack() == null) retrieveChallengeDetail(currentChallenge.getDetails());
        else drawChallengeDetail(currentChallenge.getDetails());
    }

    @Background
    void retrieveChallengeDetail(@NonNull ChallengeDetailBean activeChallengeFragment) {
        if (this.notificationId != activeChallengeFragment.getNotificationId()) return; // view has been recycled
        log.debug("retrieveChallengeDetail");

        Challenge challenge = SyncHelper.getChallenge(
                activeChallengeFragment.getChallenge().getDeviceId(),
                activeChallengeFragment.getChallenge().getChallengeId());
        if (this.notificationId != activeChallengeFragment.getNotificationId()) return; // view has been recycled
        Boolean playerFound = false;
        Boolean opponentFound = false;
        if (challenge != null) {
            for (Challenge.ChallengeAttempt attempt : challenge.getAttempts()) {
                if (attempt.user_id == activeChallengeFragment.getPlayer().getId() && !playerFound) {
                    playerFound = true;
                    Track playerTrack = SyncHelper.getTrack(attempt.track_device_id, attempt.track_id);
                    if (playerTrack != null) activeChallengeFragment.setPlayerTrack(new TrackSummaryBean(playerTrack));
                } else if (attempt.user_id == activeChallengeFragment.getOpponent().getId() && !opponentFound) {
                    opponentFound = true;
                    Track opponentTrack = SyncHelper.getTrack(attempt.track_device_id, attempt.track_id);
                    if (opponentTrack != null) activeChallengeFragment.setOpponentTrack(new TrackSummaryBean(opponentTrack));
                }
                if (playerFound && opponentFound) {
                    break;
                }
            }
            if (this.notificationId != activeChallengeFragment.getNotificationId()) return; // view has been recycled
        }
        drawChallengeDetail(activeChallengeFragment);
    }

    @UiThread
    void drawChallengeDetail(@NonNull ChallengeDetailBean activeChallengeFragment) {
        if (this.notificationId != activeChallengeFragment.getNotificationId()) return; // view has been recycled
        log.debug("drawChallengeDetail");

        TrackSummaryBean opponentTrack = activeChallengeFragment.getOpponentTrack();

        if(opponentTrack != null) {
            trackDistance.setText(Format.twoDp(UnitConversion.miles(opponentTrack.getDistanceRan())) + "mi");
            ascentText.setText(Format.twoDp(opponentTrack.getTotalUp()) + " m");
            descentText.setText(Format.twoDp(opponentTrack.getTotalDown()) + " m");
        }

        UserBean opponent = activeChallengeFragment.getOpponent();
        if (opponent.getRank() != null) {
            rankIcon.setImageDrawable(getResources().getDrawable(opponent.getRankDrawable()));
            rankIcon.setVisibility(VISIBLE);
        } else {
            rankIcon.setVisibility(INVISIBLE);
        }
    }

    public interface OnInboxChallengeAction {
        public void onIgnore(ChallengeNotificationBean challengeNotificationBean);
        public void onAccept(ChallengeNotificationBean challengeNotificationBean);
    }
}
