package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 08/07/2014.
 */
@Slf4j
@Data
public class ChallengeExpandableListAdapter extends BaseExpandableListAdapter {

    Activity context;
    List<ChallengeNotificationBean> challengeNotifications;

    public ChallengeExpandableListAdapter(Activity context, List<ChallengeNotificationBean> challenges) {
        this.context = context;
        this.challengeNotifications = challenges;
    }

    @Override
    public int getGroupCount() {
        return challengeNotifications.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.fragment_challenge_notification, null);
        }

        ChallengeNotificationBean notif = challengeNotifications.get(groupPosition);
        DurationChallengeBean chal = (DurationChallengeBean) notif.getChallenge(); // TODO avoid cast - more generic methods in ChallengeBean? 'limit' and 'goal'?

        TextView itemView = (TextView) convertView.findViewById(R.id.challenge_notification_challenger_name);
        itemView.setText(notif.getUser().getName());

        TextView durationView = (TextView) convertView.findViewById(R.id.challenge_notification_duration);
        String durationText = context.getString(R.string.challenge_notification_duration);
        int duration = chal.getDuration().toStandardMinutes().getMinutes();
        log.debug("Duration text and value: {} / {}", durationText, duration);
        durationView.setText(String.format(durationText, duration));

        TextView expiryView = (TextView) convertView.findViewById(R.id.challenge_notification_expiry);
        expiryView.setText(PeriodFormat.getDefault().print(new Period(new DateTime(), new DateTime(notif.getExpiry()))));

        TextView subtitleView = (TextView) convertView.findViewById(R.id.challenge_notification_challenge_subtitle);
        if (notif.isFromMe()) subtitleView.setText(R.string.challenge_sent);
        else subtitleView.setText(R.string.challenge_received);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.fragment_challenge_expanded, null);
        }

        ChallengeNotificationBean currentChallenge = challengeNotifications.get(groupPosition);

        final ChallengeDetailBean activeChallengeFragment = new ChallengeDetailBean();

        UserBean opponentUserBean = currentChallenge.getUser();
        activeChallengeFragment.setOpponent(currentChallenge.getUser());
        User player = SyncHelper.getUser(AccessToken.get().getUserId());
        final UserBean playerBean = new UserBean();
        playerBean.setId(player.getId());
        playerBean.setName(player.getName());
        playerBean.setShortName(StringFormattingUtils.getForenameAndInitial(player.getName()));
        playerBean.setProfilePictureUrl(player.getImage());
        activeChallengeFragment.setPlayer(playerBean);
        activeChallengeFragment.setChallenge(currentChallenge.getChallenge());

        TextView challengeHeaderText = (TextView)convertView.findViewById(R.id.challengeHeader);
        String headerText = context.getString(R.string.challenge_notification_duration);
        DurationChallengeBean challengeAsDuration = (DurationChallengeBean)activeChallengeFragment.getChallenge();
        String formattedHeader = String.format(headerText, challengeAsDuration.getDuration().getStandardMinutes());
        challengeHeaderText.setText(formattedHeader);
        final View finalConvertView = convertView;
        Task.callInBackground(new Callable<ChallengeTrackSummaryBean>() {

            @Override
            public ChallengeTrackSummaryBean call() throws Exception {
                ChallengeTrackSummaryBean challengeTrackSummaryBean = new ChallengeTrackSummaryBean();
                Challenge challenge = SyncHelper.getChallenge(activeChallengeFragment.getChallenge().getChallengeId());
                challengeTrackSummaryBean.setChallenge(challenge);
                Boolean playerFound = false;
                Boolean opponentFound = false;
                if (challenge != null) {
                    for (Challenge.ChallengeAttempt attempt : challenge.getAttempts()) {
                        if (attempt.user_id == playerBean.getId() && !playerFound) {
                            playerFound = true;
                            Track playerTrack = SyncHelper.getTrack(attempt.device_id, attempt.track_id);
                            challengeTrackSummaryBean.setPlayerTrack(playerTrack);
                            Double init_alt = null;
                            double min_alt = Double.MAX_VALUE;
                            double max_alt = Double.MIN_VALUE;
                            double max_speed = 0;
                            for (Position position : playerTrack.getTrackPositions()) {
                                if (position.getAltitude() != null && init_alt != null)
                                    init_alt = position.altitude;
                                if (position.getAltitude() != null && max_alt < position.getAltitude())
                                    max_alt = position.getAltitude();
                                if (position.getAltitude() != null && min_alt > position.getAltitude())
                                    min_alt = position.getAltitude();
                                if (position.speed > max_speed) max_speed = position.speed;
                            }
                            TrackSummaryBean playerTrackBean = new TrackSummaryBean();
                            playerTrackBean.setAveragePace((Math.round((playerTrack.distance * 60 * 60 / 1000) / playerTrack.time) * 10) / 10);
                            playerTrackBean.setDistanceRan((int) playerTrack.distance);
                            playerTrackBean.setTopSpeed(Math.round(((max_speed * 60 * 60) / 1000) * 10) / 10);
                            playerTrackBean.setTotalUp(Math.round((max_alt - init_alt) * 100) / 100);
                            playerTrackBean.setTotalDown(Math.round((min_alt - init_alt) * 100) / 100);
                            playerTrackBean.setDeviceId(playerTrack.device_id);
                            playerTrackBean.setTrackId(playerTrack.track_id);
                            playerTrackBean.setRaceDate(playerTrack.getRawDate());
                            activeChallengeFragment.setPlayerTrack(playerTrackBean);
                        } else if (attempt.user_id == activeChallengeFragment.getOpponent().getId() && !opponentFound) {
                            opponentFound = true;
                            Track opponentTrack = SyncHelper.getTrack(attempt.device_id, attempt.track_id);
                            challengeTrackSummaryBean.setOpponentTrack(opponentTrack);
                            Double init_alt = null;
                            double min_alt = Double.MAX_VALUE;
                            double max_alt = Double.MIN_VALUE;
                            double max_speed = 0;
                            for (Position position : opponentTrack.getTrackPositions()) {
                                if (position.getAltitude() != null && init_alt != null)
                                    init_alt = position.altitude;
                                if (position.getAltitude() != null && max_alt < position.getAltitude())
                                    max_alt = position.getAltitude();
                                if (position.getAltitude() != null && min_alt > position.getAltitude())
                                    min_alt = position.getAltitude();
                                if (position.speed > max_speed) max_speed = position.speed;
                            }
                            TrackSummaryBean opponentTrackBean = new TrackSummaryBean();
                            opponentTrackBean.setAveragePace((Math.round((opponentTrack.distance * 60 * 60 / 1000) / opponentTrack.time) * 10) / 10);
                            opponentTrackBean.setDistanceRan((int) opponentTrack.distance);
                            opponentTrackBean.setTopSpeed(Math.round(((max_speed * 60 * 60) / 1000) * 10) / 10);
                            opponentTrackBean.setTotalUp(Math.round((max_alt - init_alt) * 100) / 100);
                            opponentTrackBean.setTotalDown(Math.round((min_alt - init_alt) * 100) / 100);
                            opponentTrackBean.setDeviceId(opponentTrack.device_id);
                            opponentTrackBean.setTrackId(opponentTrack.track_id);
                            opponentTrackBean.setRaceDate(opponentTrack.getRawDate());
                            activeChallengeFragment.setOpponentTrack(opponentTrackBean);
                        }
                        if (playerFound && opponentFound) {
                            break;
                        }
                    }
                }
                return challengeTrackSummaryBean;
            }
        }).continueWith(new Continuation<ChallengeTrackSummaryBean, Void>() {
            @Override
            public Void then(Task<ChallengeTrackSummaryBean> challengeTask) throws Exception {
                activeChallengeFragment.setPoints(20000);
                String durationText = context.getString(R.string.challenge_notification_duration);
                DurationChallengeBean durationChallenge = (DurationChallengeBean)activeChallengeFragment.getChallenge();

                int duration = durationChallenge.getDuration().toStandardMinutes().getMinutes();
                activeChallengeFragment.setTitle(String.format(durationText, duration));

                TextView opponentName = (TextView) finalConvertView.findViewById(R.id.opponentName);
                opponentName.setText(activeChallengeFragment.getOpponent().getShortName());

                TextView playerName = (TextView) finalConvertView.findViewById(R.id.playerName);
                playerName.setText(activeChallengeFragment.getPlayer().getShortName());

                TrackSummaryBean playerTrack = activeChallengeFragment.getPlayerTrack();
                Boolean playerComplete = false;
                if(playerTrack != null) {
                    playerComplete = true;

                    String formattedDistance = StringFormattingUtils.getDistanceInKmString(playerTrack.getDistanceRan());
                    setTextViewAndColor(R.id.playerDistance, "#269b47", formattedDistance + "KM", finalConvertView);
                    setTextViewAndColor(R.id.playerAveragePace, "#269b47", playerTrack.getAveragePace() + "", finalConvertView);
                    setTextViewAndColor(R.id.playerTopSpeed, "#269b47", playerTrack.getTopSpeed() + "", finalConvertView);
                    setTextViewAndColor(R.id.playerTotalUp, "#269b47", playerTrack.getTotalUp() + "", finalConvertView);
                    setTextViewAndColor(R.id.playerTotalDown, "#269b47", playerTrack.getTotalDown() + "", finalConvertView);

                    Button raceNowBtn = (Button)finalConvertView.findViewById(R.id.raceNowBtn);
                    raceNowBtn.setVisibility(View.INVISIBLE);
                    Button raceLaterBtn = (Button)finalConvertView.findViewById(R.id.raceLaterBtn);
                    raceLaterBtn.setVisibility(View.INVISIBLE);
                }
                TrackSummaryBean opponentTrack = activeChallengeFragment.getOpponentTrack();
                Boolean opponentComplete = false;
                if(opponentTrack != null) {
                    opponentComplete = true;

                    String formattedDistance = StringFormattingUtils.getDistanceInKmString(opponentTrack.getDistanceRan());
                    setTextViewAndColor(R.id.opponentDistance, "#269b47", formattedDistance + "KM", finalConvertView);
                    setTextViewAndColor(R.id.opponentAveragePace, "#269b47", opponentTrack.getAveragePace() + "", finalConvertView);
                    setTextViewAndColor(R.id.opponentTopSpeed, "#269b47", opponentTrack.getTopSpeed() + "", finalConvertView);
                    setTextViewAndColor(R.id.opponentTotalUp, "#269b47", opponentTrack.getTotalUp() + "", finalConvertView);
                    setTextViewAndColor(R.id.opponentTotalDown, "#269b47", opponentTrack.getTotalDown() + "", finalConvertView);
                }

                if(playerComplete && opponentComplete) {

                    if(playerTrack.getDistanceRan() > opponentTrack.getDistanceRan()) {
                        TextView opponentDistance = (TextView)finalConvertView.findViewById(R.id.opponentDistance);
                        opponentDistance.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerDistance = (TextView)finalConvertView.findViewById(R.id.playerDistance);
                        playerDistance.setTextColor(Color.parseColor("#e31f26"));
                        FrameLayout rewardIcon = (FrameLayout)finalConvertView.findViewById(R.id.reward_icon);
                        rewardIcon.setVisibility(View.INVISIBLE);
                        TextView rewardText = (TextView)finalConvertView.findViewById(R.id.rewardPoints);
                        rewardText.setVisibility(View.INVISIBLE);
                    }

                    if(playerTrack.getAveragePace() > opponentTrack.getAveragePace()) {
                        TextView opponentAveragePace = (TextView)finalConvertView.findViewById(R.id.opponentAveragePace);
                        opponentAveragePace.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerAveragePace = (TextView)finalConvertView.findViewById(R.id.playerAveragePace);
                        playerAveragePace.setTextColor(Color.parseColor("#e31f26"));
                    }

                    if(playerTrack.getTopSpeed() > opponentTrack.getTopSpeed()) {
                        TextView opponentTopSpeed = (TextView)finalConvertView.findViewById(R.id.opponentTopSpeed);
                        opponentTopSpeed.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerTopSpeed = (TextView)finalConvertView.findViewById(R.id.playerTopSpeed);
                        playerTopSpeed.setTextColor(Color.parseColor("#e31f26"));
                    }

                    if(playerTrack.getTotalUp() > opponentTrack.getTotalUp()) {
                        TextView opponentTotalUp = (TextView)finalConvertView.findViewById(R.id.opponentTotalUp);
                        opponentTotalUp.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerTotalUp = (TextView)finalConvertView.findViewById(R.id.playerTotalUp);
                        playerTotalUp.setTextColor(Color.parseColor("#e31f26"));
                    }

                    if(playerTrack.getTotalDown() > opponentTrack.getTotalDown()) {
                        TextView opponentTotalDown = (TextView)finalConvertView.findViewById(R.id.opponentTotalDown);
                        opponentTotalDown.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerTotalDown = (TextView)finalConvertView.findViewById(R.id.playerTotalDown);
                        playerTotalDown.setTextColor(Color.parseColor("#e31f26"));
                    }
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

        final ImageView playerPic = (ImageView)finalConvertView.findViewById(R.id.playerProfilePic);
        Picasso.with(context).load(player.getImage()).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                playerPic.measure(0,0);
                playerPic.setImageBitmap(PictureUtils.getRoundedBmp(bitmap, playerPic.getMeasuredWidth()));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                log.error("Bitmap failed - player pic");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        });

        final ImageView opponentPic = (ImageView)finalConvertView.findViewById(R.id.playerProfilePic);

        Picasso.with(context).load(activeChallengeFragment.getOpponent().getProfilePictureUrl()).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                opponentPic.measure(0, 0);
                opponentPic.setImageBitmap(PictureUtils.getRoundedBmp(bitmap, opponentPic.getMeasuredWidth()));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                log.error("Bitmap failed - opponent pic");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });

        return finalConvertView;
    }

    public void setTextViewAndColor(int textViewId, String color, String textViewString, View view) {
        TextView textView = (TextView)view.findViewById(textViewId);
        textView.setTextColor(Color.parseColor(color));
        textView.setText(textViewString);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
