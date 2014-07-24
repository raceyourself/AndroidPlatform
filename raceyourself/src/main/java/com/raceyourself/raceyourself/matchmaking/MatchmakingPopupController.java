package com.raceyourself.raceyourself.matchmaking;

import android.animation.Animator;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.raceyourself.platform.auth.AuthenticationActivity;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.AutoMatches;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.game.GameConfiguration;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.UserBean;
import com.raceyourself.raceyourself.home.feed.ChallengeBean;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;
import com.raceyourself.raceyourself.home.sendchallenge.SetChallengeView;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

/**
 * Jesus wept.
 *
 * Created by Amerigo on 17/07/2014.
 */
@Slf4j
public class MatchmakingPopupController implements SeekBar.OnSeekBarChangeListener {
    int duration;

    TextView durationTextView;
    TextView furthestRunTextView;

    HomeActivity homeActivity;

    PopupWindow matchmakingFitnessPopup;
    PopupWindow matchmakingDurationPopup;
    PopupWindow matchmakingFindingPopup;

    LayoutInflater inflater;

    String fitness;

    View fitnessView;

    TextView matchingText;
    TextView searchingText;
    TextView matrixText;
    TextView foundText;

    ImageView heartIcon;
    ImageView globeIcon;
    ImageView wandIcon;
    ImageView tickIcon;

    Animation translateRightAnim;
    Animation rotationAnim;

    Drawable checkmarkIconDrawable;
    Drawable loadingIconDrawable;

    Button raceButton;
    Button searchAgainButton;

    User opponent;

    TextView opponentNameText;
    ImageView opponentProfilePic;

    TextView lengthWarningText;

    ChallengeDetailBean challengeDetail;

    private boolean raceYourself;

    int animationCount = 0;
    private SortedMap<Integer, Pair<Track, SetChallengeView.MatchQuality>> availableOwnTracksMap;
    private Button findBtn;
    private View durationView;

    public MatchmakingPopupController(){}

    public MatchmakingPopupController(HomeActivity homeActivity) {
        this.homeActivity = homeActivity;
        inflater = LayoutInflater.from(homeActivity);
    }

    public void displayFitnessPopup() {
        raceYourself = false;
        fitnessView = inflater.inflate(R.layout.activity_choose_fitness, null);
        matchmakingFitnessPopup = new PopupWindow(fitnessView);
        matchmakingFitnessPopup.setWindowLayoutMode(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        matchmakingFitnessPopup.showAtLocation(
                homeActivity.getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        AutoMatches.update();
    }

    public void onFitnessBtn(View view) {
        RadioButton outOfShapeButton = (RadioButton)fitnessView.findViewById(R.id.outOfShape);
        RadioButton averageButton = (RadioButton)fitnessView.findViewById(R.id.averageBtn);
        RadioButton athleticButton = (RadioButton)fitnessView.findViewById(R.id.athleticBtn);
        RadioButton eliteButton = (RadioButton)fitnessView.findViewById(R.id.eliteBtn);

        if (outOfShapeButton.isChecked()) {
            fitness = "out of shape";
        } else if (averageButton.isChecked()) {
            fitness = "average";
        } else if (athleticButton.isChecked()) {
            fitness = "athletic";
        } else if (eliteButton.isChecked()) {
            fitness = "elite";
        } else {
            log.error("fitness level not found");
            return;
        }

        final String finalFitness = fitness;
        Thread updateUserThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AuthenticationActivity.editUser(new AuthenticationActivity.UserDiff().profile("running_fitness", finalFitness));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        updateUserThread.start();

        displayQuickmatchDurationPopup();
        matchmakingFitnessPopup.dismiss();
    }

    public boolean isDisplaying() {
        if(matchmakingFindingPopup != null &&  matchmakingFindingPopup.isShowing()) {
            displayQuickmatchDurationPopup();
            matchmakingFindingPopup.dismiss();
            return true;
        } else if(matchmakingDurationPopup != null && matchmakingDurationPopup.isShowing()) {
            if (!raceYourself)
                displayFitnessPopup();
            matchmakingDurationPopup.dismiss();
            return true;
        } else if(matchmakingFitnessPopup != null && matchmakingFitnessPopup.isShowing()) {
            matchmakingFitnessPopup.dismiss();
            return true;
        }

        return false;
    }

    public void onMatchClick() {
        displayFindingPopup();
        matchmakingDurationPopup.dismiss();
    }

    public void displayRaceYourselfPopup() {
        displayDurationPopup(true);
    }

    public void displayQuickmatchDurationPopup() {
        raceYourself = false;
        displayDurationPopup(false);
    }

    public void displayDurationPopup(final boolean raceYourself) {
        this.raceYourself = raceYourself;

        if (raceYourself)
            availableOwnTracksMap = SetChallengeView.populateAvailableUserTracksMap();

        durationView = inflater.inflate(R.layout.activity_select_duration, null);
        matchmakingDurationPopup = new PopupWindow(durationView);
        matchmakingDurationPopup.setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        durationTextView = (TextView) durationView.findViewById(R.id.duration);
        furthestRunTextView = (TextView) durationView.findViewById(R.id.furthestRunNumber);
        lengthWarningText = (TextView) durationView.findViewById(R.id.lengthWarning);

        findBtn = (Button) durationView.findViewById(R.id.findBtn);

        if (raceYourself) {
            TextView furthestRunBeforeDurationText = (TextView) durationView.findViewById(R.id.furthestRunText);
            furthestRunBeforeDurationText.setText(R.string.duration_description_raceyourself);
            findBtn.setText(R.string.raceyourself_button);
            opponentProfilePic = (ImageView) durationView.findViewById(R.id.playerProfilePic);

            // change width to match_parent to make room for extra text
            ImageView speechBubble = (ImageView) durationView.findViewById(R.id.weatherBox);
//            speechBubble.setLayoutParams(new RelativeLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        TextView furthestRunAfterTime = (TextView) durationView.findViewById(R.id.furthestRunAfterTime);
        furthestRunAfterTime.setVisibility(raceYourself ? View.VISIBLE : View.GONE);

        lengthWarningText.setVisibility(raceYourself ? View.VISIBLE : View.GONE);

        TextView lengthWarningHidden = (TextView) durationView.findViewById(R.id.lengthWarningLongestHidden);

        SeekBar seekBar = (SeekBar)durationView.findViewById(R.id.matchmaking_distance_bar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(30);
        User user = User.get(AccessToken.get().getUserId());

        ImageView playerImage = (ImageView) durationView.findViewById(R.id.playerProfilePic);
        String url = user.getImage();
        Picasso.with(homeActivity).load(url).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerImage);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findBtn.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.matchmaking_distance_bar);
        findBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (raceYourself)
                    raceYourselfMatchClick();
                else
                    onMatchClick();
            }
        });

        matchmakingDurationPopup.showAtLocation(
                homeActivity.getWindow().getDecorView().getRootView(), Gravity.CENTER, 0, 0);
    }

    private void raceYourselfMatchClick() {
        User player = User.get(AccessToken.get().getUserId());
        UserBean playerBean = new UserBean(player);

        GameConfiguration gameConfiguration = new GameConfiguration.GameStrategyBuilder(
                GameConfiguration.GameType.TIME_CHALLENGE).targetTime(duration*60*1000).countdown(2999).build();

        // TODO refactor to avoid this dependency on SetChallengeView.
        Pair<Track,SetChallengeView.MatchQuality> p = availableOwnTracksMap.get(duration);

        TrackSummaryBean opponentTrack = new TrackSummaryBean(p.first, gameConfiguration);

        ChallengeBean challengeBean = new ChallengeBean(null);
        challengeBean.setType("duration");
        challengeBean.setChallengeGoal(duration * 60);
        challengeBean.setPoints(20000);

        challengeDetail = new ChallengeDetailBean();
        challengeDetail.setOpponent(playerBean);
        challengeDetail.setPlayer(playerBean);
        challengeDetail.setOpponentTrack(opponentTrack);
        challengeDetail.setChallenge(challengeBean);

        onRaceClickDelegate(true);

        matchmakingDurationPopup.dismiss();
    }

    Integer[] outOfShapeUserIds = {100, 101, 102, 107};
    Integer[] averageUserIds = {98, 105, 106, 99};
    Integer[] athleticUserIds = {96, 97, 104,};
    Integer[] eliteUserIds = {95, 103};

    float[] speedBoundaries = {1.3f, 2.0f, 3.2f, 5.5f, 6.7f};  // m/s, oos to elite
    Random random = new Random();

    List<Integer> eligibleUserIds = new ArrayList<Integer>();
    private List<Integer> matchedIds = new ArrayList<Integer>();
    public void displayFindingPopup() {
        animationCount = 0;
        View findingView = inflater.inflate(R.layout.activity_matchmaking_finding, null);

        matchingText = (TextView)findingView.findViewById(R.id.matchingText);
        searchingText = (TextView)findingView.findViewById(R.id.searchingText);
        matrixText = (TextView)findingView.findViewById(R.id.matrixText);
        foundText = (TextView)findingView.findViewById(R.id.matchedText);

        heartIcon = (ImageView)findingView.findViewById(R.id.heartIcon);
        globeIcon = (ImageView)findingView.findViewById(R.id.globeIcon);
        wandIcon = (ImageView)findingView.findViewById(R.id.wandIcon);
        tickIcon = (ImageView)findingView.findViewById(R.id.tickIcon);

        translateRightAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.matched_text_anim);
        rotationAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.rotating_icon_anim);

        checkmarkIconDrawable = homeActivity.getResources().getDrawable(R.drawable.icon_checkmark);
        loadingIconDrawable = homeActivity.getResources().getDrawable(R.drawable.icon_loading);

        raceButton = (Button)findingView.findViewById(R.id.quickmatch_ok_button);
        searchAgainButton = (Button)findingView.findViewById(R.id.searchAgainBtn);

        opponentNameText = (TextView)findingView.findViewById(R.id.opponentName);
        opponentProfilePic = (ImageView)findingView.findViewById(R.id.opponentProfilePic);

        final User user = User.get(AccessToken.get().getUserId());
        String url = user.getImage();

        final ImageView playerImage = (ImageView)findingView.findViewById(R.id.playerProfilePic);
        Picasso
            .with(homeActivity)
            .load(url)
            .placeholder(R.drawable.default_profile_pic)
            .transform(new PictureUtils.CropCircle())
            .into(playerImage);

        // pick a random user in the right fitness bucket to be the opponent

        float randomSpeed = random.nextFloat();  // will be shifted and scaled to fitness band
        float opponentSpeed = 0.0f;
        int opponentUserId = 0;

        if (fitness.equals("out of shape")) {
            opponentSpeed = randomSpeed * (speedBoundaries[1] - speedBoundaries[0]) + speedBoundaries[0];
            eligibleUserIds = new ArrayList(Arrays.asList(outOfShapeUserIds));
        } else if (fitness.equals("average")) {
            opponentSpeed = randomSpeed * (speedBoundaries[2] - speedBoundaries[1]) + speedBoundaries[1];
            eligibleUserIds = new ArrayList(Arrays.asList(averageUserIds));
        } else if (fitness.equals("athletic")) {
            opponentSpeed = randomSpeed * (speedBoundaries[3] - speedBoundaries[2]) + speedBoundaries[2];
            eligibleUserIds = new ArrayList(Arrays.asList(athleticUserIds));
        } else if (fitness.equals("elite")) {
            opponentSpeed = randomSpeed * (speedBoundaries[4] - speedBoundaries[3]) + speedBoundaries[3];
            eligibleUserIds = new ArrayList(Arrays.asList(eliteUserIds));
        }

        // remove opponents we've already shown
        eligibleUserIds.removeAll(matchedIds);
        if (eligibleUserIds.size() == 0) {
            Toast toast = new Toast(homeActivity);
            toast.makeText(homeActivity, "We couldn't find any opponents that match your fitness level. Please try another.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // choose an opponent
        opponentUserId = eligibleUserIds.get(random.nextInt(eligibleUserIds.size()));
        if (eligibleUserIds.size() == 1) {
            matchedIds.clear();  // repopulate eligible with full list for next time
        } else {
            matchedIds.add(opponentUserId);  // don't show this one if user taps choose again
        }

        // background thread to pull chosen opponent's details from server
        final int opponentUserIdFinal = opponentUserId;
        final float opponentSpeedFinal = opponentSpeed;
        ExecutorService pool = Executors.newFixedThreadPool(1);
        final Future<User> futureUser = pool.submit(new Callable<User>() {
            @Override
            public User call() throws Exception {

//                AutoMatches.update();
//                List<Track> trackList = AutoMatches.getBucket(fitness, duration);
//
//                // choose random track from list
//                Random random = new Random();
//                int trackNumber = random.nextInt(trackList.size());
//                final Track selectedTrack = trackList.get(trackNumber);
//
//                log.info("Matched track " + selectedTrack.getId() + ", distance: " + selectedTrack.getDistance() + "m, pace: " +
//                        selectedTrack.getPace() + " min/km, by user " + selectedTrack.user_id);
//
                challengeDetail = new ChallengeDetailBean();

                UserBean player = new UserBean(user);
                challengeDetail.setPlayer(player);

                TrackSummaryBean opponentTrack = new TrackSummaryBean(opponentSpeedFinal, duration*60*1000);
                challengeDetail.setOpponentTrack(opponentTrack);

                ChallengeBean challengeBean = new ChallengeBean(null);
                challengeBean.setType("duration");
                challengeBean.setChallengeGoal(duration * 60);
                challengeBean.setPoints(500);
                challengeDetail.setChallenge(challengeBean);


                return SyncHelper.get("users/" + opponentUserIdFinal, User.class);

            }
        });

        // start the funky matching animations
        translateRightAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                switch(animationCount) {
                    case 0:
                        startImageAnimation(heartIcon);
                        break;

                    case 1:
                        startImageAnimation(globeIcon);
                        break;

                    case 2:
                        startImageAnimation(wandIcon);
                        break;

                    case 3:
                        startImageAnimation(tickIcon);
                        break;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        rotationAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                switch(animationCount) {
                    case 0:
                        endImageAnimation(heartIcon, checkmarkIconDrawable, searchingText);
                        break;
                    case 1:
                        endImageAnimation(globeIcon, checkmarkIconDrawable, matrixText);
                        break;
                    case 2:
                        if (futureUser.isDone()) endImageAnimation(wandIcon, checkmarkIconDrawable, foundText);
                        else {
                            animationCount--;
                            startImageAnimation(wandIcon);
                        }
                        break;
                    case 3:
                        tickIcon.setImageDrawable(checkmarkIconDrawable);
                        raceButton.setVisibility(View.VISIBLE);
                        if (eligibleUserIds.size() > 1) {
                            searchAgainButton.setVisibility(View.VISIBLE);
                        }
                        try {
                            opponent = futureUser.get();
                            opponentNameText.setText(StringFormattingUtils.getForename(opponent.name));
                            Picasso.with(homeActivity).load(opponent.getImage())
                                    .placeholder(R.drawable.default_profile_pic)
                                    .transform(new PictureUtils.CropCircle())
                                    .into(opponentProfilePic);

                            UserBean opponentBean = new UserBean(opponent);
                            challengeDetail.setOpponent(opponentBean);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        tickIcon.setImageDrawable(checkmarkIconDrawable);
                        raceButton.setVisibility(View.VISIBLE);
                        searchAgainButton.setVisibility(View.VISIBLE);
                        break;
                }
                if(animationCount < 3) {
                    animationCount++;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        matchingText.startAnimation(translateRightAnim);

        if(matchmakingFindingPopup != null && matchmakingFindingPopup.isShowing()) matchmakingFindingPopup.dismiss();

        matchmakingFindingPopup = new PopupWindow(findingView);

        matchmakingFindingPopup.setAnimationStyle(R.style.Animation);

        matchmakingFindingPopup.setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        matchmakingFindingPopup.showAtLocation(homeActivity.getWindow().getDecorView().getRootView(),
                Gravity.CENTER, 0, 0);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int stepSize = 6;
        progress = (Math.round(progress/ stepSize))* stepSize;
        seekBar.setProgress(progress);
        duration = ((progress / stepSize) + 1) * 5;
        if(duration == 0) {
            duration = 5;
        }
        durationTextView.setText(duration + "");

        StringBuilder text = new StringBuilder();
        text.append(" ");
        text.append(duration);
        text.append(" mins");
        if (!raceYourself)
            text.append("?");
        furthestRunTextView.setText(text.toString());

        if (raceYourself) {
            SetChallengeView.MatchQuality quality = availableOwnTracksMap.get(duration).second;

            // TODO jodatime...
            String qualityWarning = quality.getMessageId() == null ? "" :
                    String.format(homeActivity.getString(quality.getMessageId()), duration + " mins");

            // TODO share code with SetChallengeView
            TextView warning = (TextView) durationView.findViewById(R.id.lengthWarning);
            warning.setText(qualityWarning);

            final boolean enable = quality != SetChallengeView.MatchQuality.TRACK_TOO_SHORT;
            // Disable send button if no runs recorded that are long enough.
            // Having a run that's too long is fine - we can truncate it.
            findBtn.setEnabled(enable);
            findBtn.setClickable(enable);
        }
    }

    public void onRaceClick() {
        onRaceClickDelegate(false);
    }

    public void onRaceClickDelegate(boolean raceYourself) {
        homeActivity.getPagerAdapter().getHomeFeedFragment().setSelectedChallenge(challengeDetail);
        TextView opponentName = (TextView) homeActivity.findViewById(R.id.opponentName);
        opponentName.setText(StringFormattingUtils.getForename(challengeDetail.getOpponent().getName()));

        // Clone profile image into root layout
        int[] location = new int[2];
        opponentProfilePic.getLocationOnScreen(location);

        final ViewGroup rl = (ViewGroup) homeActivity.findViewById(R.id.activity_home);
        int[] parent_location = new int[2];
        rl.getLocationOnScreen(parent_location);

        RelativeLayout.LayoutParams cp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        ViewGroup.LayoutParams pp = opponentProfilePic.getLayoutParams();
        cp.width = pp.width;
        cp.height = pp.height;
        final ImageView clone = new ImageView(rl.getContext());
        clone.setImageDrawable(opponentProfilePic.getDrawable());
        clone.setScaleType(opponentProfilePic.getScaleType());
        clone.setLayoutParams(cp);
        clone.setX(location[0] - parent_location[0]);
        clone.setY(location[1] - parent_location[1]);
        rl.addView(clone);

        // Animate to opponent versus location
        final ImageView opponent = (ImageView)rl.findViewById(R.id.opponentPic);
        final ImageView opponentRank = (ImageView)rl.findViewById(R.id.opponentRank);
        final Drawable rankDrawable = homeActivity.getResources().getDrawable(challengeDetail.getOpponent().getRankDrawable());
        opponent.getLocationOnScreen(location);
        clone.animate().x(location[0] - parent_location[0]).y(location[1] - parent_location[1]).setDuration(1500).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                opponent.setImageDrawable(clone.getDrawable());
                opponentRank.setImageDrawable(rankDrawable);
                opponentRank.setVisibility(View.VISIBLE);
                rl.removeView(clone);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                opponent.setImageDrawable(clone.getDrawable());
                opponentRank.setImageDrawable(rankDrawable);
                opponentRank.setVisibility(View.VISIBLE);
                rl.removeView(clone);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        if (!raceYourself)
            matchmakingFindingPopup.dismiss();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void startImageAnimation(ImageView imageView) {
        imageView.setImageDrawable(loadingIconDrawable);
        imageView.setVisibility(View.VISIBLE);
        imageView.startAnimation(rotationAnim);

    }

    public void endImageAnimation(ImageView imageView, Drawable drawable, TextView textView) {
        imageView.setImageDrawable(drawable);
        textView.setVisibility(View.VISIBLE);
        textView.startAnimation(translateRightAnim);
    }
}
