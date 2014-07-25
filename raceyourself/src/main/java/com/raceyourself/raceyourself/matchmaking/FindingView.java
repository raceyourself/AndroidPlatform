package com.raceyourself.raceyourself.matchmaking;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.BuildConfig;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.home.UserBean;
import com.raceyourself.raceyourself.home.feed.ChallengeBean;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 25/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_matchmaking_finding)
public class FindingView extends RelativeLayout {

    // Four text views for finding an opponent
    @ViewById
    TextView matchingText;
    @ViewById
    TextView searchingText;
    @ViewById
    TextView matrixText;
    @ViewById
    TextView matchedText;

    // Four image views for finding an opponent
    @ViewById
    ImageView heartIcon;
    @ViewById
    ImageView globeIcon;
    @ViewById
    ImageView wandIcon;
    @ViewById
    ImageView tickIcon;

    @ViewById
    ImageView playerProfilePic;

    @ViewById
    Button quickmatch_ok_button;

    @ViewById
    Button searchAgainBtn;

    @ViewById
    TextView opponentName;

    @ViewById
    @Getter
    ImageView opponentProfilePic;

    Integer[] outOfShapeUserIds = BuildConfig.OUT_OF_SHAPE_BOT_IDS;
    Integer[] averageUserIds = BuildConfig.AVERAGE_BOT_IDS;
    Integer[] athleticUserIds = BuildConfig.ATHLETIC_BOT_IDS;
    Integer[] eliteUserIds = BuildConfig.ELITE_BOT_IDS;

    float[] speedBoundaries = {1.3f, 2.0f, 3.2f, 5.5f, 6.7f};  // m/s, oos to elite
    Random random = new Random();

    List<Integer> eligibleUserIds = new ArrayList<Integer>();
    private List<Integer> matchedIds = new ArrayList<Integer>();

    // Animations for finding an opponent
    // This translates a view from left to right
    Animation translateRightAnim;
    // This rotates an image view 360 degrees three times
    Animation rotationAnim;

    // Drawables for the spinner and checkmark when finding an opponent
    Drawable checkmarkIconDrawable;
    Drawable loadingIconDrawable;

    Context context;

    int duration;
    String fitness;

    int animationCount = 0;

    User opponent;

    @Getter
    ChallengeDetailBean challengeDetail;

    @AfterViews
    public void findOpponent() {

        final User user = User.get(AccessToken.get().getUserId());
        String url = user.getImage();

        Picasso.with(context)
                .load(url)
                .placeholder(R.drawable.default_profile_pic)
                .transform(new PictureUtils.CropCircle())
                .into(playerProfilePic);

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
            Toast toast = new Toast(context);
            toast.makeText(context, "We couldn't find any opponents that match your fitness level. Please try another.",
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
                        if (futureUser.isDone()) endImageAnimation(wandIcon, checkmarkIconDrawable, matchedText);
                        else {
                            animationCount--;
                            startImageAnimation(wandIcon);
                        }
                        break;
                    case 3:
                        tickIcon.setImageDrawable(checkmarkIconDrawable);
                        quickmatch_ok_button.setVisibility(View.VISIBLE);
                        if (eligibleUserIds.size() > 1) {
                            searchAgainBtn.setVisibility(View.VISIBLE);
                        }
                        try {
                            opponent = futureUser.get();
                            if(opponent == null) {
                                restartSearch();
                                Toast.makeText(context, "Problem getting user, trying again", Toast.LENGTH_LONG);
                                return;
                            }
                            opponentName.setText(StringFormattingUtils.getForename(opponent.name));
                            Picasso.with(context).load(opponent.getImage())
                                    .placeholder(R.drawable.default_profile_pic)
                                    .transform(new PictureUtils.CropCircle())
                                    .into(opponentProfilePic);

                            UserBean opponentBean = new UserBean(opponent);
                            challengeDetail.setOpponent(opponentBean);
                        } catch (InterruptedException e) {
                            Toast toast = new Toast(context);
                            toast.makeText(context, "Internal error, please try again.", Toast.LENGTH_SHORT).show();
                            restartSearch();
                            return;
                        } catch (ExecutionException e) {
                            Toast toast = new Toast(context);
                            toast.makeText(context, "We couldn't contact our server, please check your network connection.", Toast.LENGTH_SHORT).show();
                            restartSearch();
                            return;
                        }
                        tickIcon.setImageDrawable(checkmarkIconDrawable);
                        quickmatch_ok_button.setVisibility(View.VISIBLE);
                        searchAgainBtn.setVisibility(View.VISIBLE);
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
    }

    public void restartSearch() {
        searchingText.setVisibility(INVISIBLE);
        matrixText.setVisibility(INVISIBLE);
        matchedText.setVisibility(INVISIBLE);

        heartIcon.setImageDrawable(loadingIconDrawable);
        globeIcon.setVisibility(INVISIBLE);
        wandIcon.setVisibility(INVISIBLE);
        tickIcon.setVisibility(INVISIBLE);

        quickmatch_ok_button.setVisibility(INVISIBLE);
        searchAgainBtn.setVisibility(INVISIBLE);

        opponentName.setText(R.string.opponent_default_name);
        opponentProfilePic.setImageDrawable(context.getResources().getDrawable(R.drawable.default_profile_pic));

        animationCount = 0;

        findOpponent();
    }

    public FindingView(Context context, int duration, String fitness) {
        super(context);
        this.context = context;

        this.duration = duration;
        this.fitness = fitness;

        translateRightAnim = AnimationUtils.loadAnimation(context, R.anim.matched_text_anim);
        rotationAnim = AnimationUtils.loadAnimation(context, R.anim.rotating_icon_anim);

        checkmarkIconDrawable = context.getResources().getDrawable(R.drawable.icon_checkmark);
        loadingIconDrawable = context.getResources().getDrawable(R.drawable.icon_loading);
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
