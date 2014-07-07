package com.raceyourself.raceyourself.home;

import android.app.ActionBar;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.google.common.collect.ImmutableMap;
import com.raceyourself.platform.auth.AuthenticationActivity;
import com.raceyourself.platform.gpstracker.Helper;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.matchmaking.ChooseFitnessActivity;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Bolts;
import bolts.Continuation;
import bolts.Task;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HomeActivity extends Activity implements ActionBar.TabListener,
        FriendFragment.OnFragmentInteractionListener, ChallengeFragment.OnFragmentInteractionListener, ChallengeExpandedFragment.OnFragmentInteractionListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private HomePagerAdapter pagerAdapter;

    Boolean challengeDisplayed = false;

    RelativeLayout challengeHolder;

    ChallengeDetailBean activeChallengeFragment;

    private View mProgressView;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager viewPager;

    private boolean paused;

    private View loginButton;

    private UiLifecycleHelper facebookUiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (!paused) {
            // check for the OPENED state instead of session.isOpened() since for the
            // OPENED_TOKEN_UPDATED state, the selection fragment should already be showing.
            if (state.equals(SessionState.OPENED)) {
                showFacebookLogin(false);
                final String accessToken = session.getAccessToken();

                log.debug("onSessionStateChange() - FB session is open");

                Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {
                            log.debug("onCompleted() - got 'me'!");

                            final String userId = user.getId();

                            new AsyncTask<Void, Void, Void>() {
                                private IOException e;

                                @Override
                                protected Void doInBackground(Void... params) {
                                    log.debug("doInBackground() - hopefully not on a UI thread now?");
                                    try {
                                        AuthenticationActivity.linkProvider("facebook", userId, accessToken);
                                    } catch (IOException e) {
                                        this.e = e;
                                    }
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void result) {
                                    if (e != null)
                                        log.error("Unable to link Facebook provider", e);
                                }
                            }.execute();
                        }
                        else
                            throw new IllegalStateException("TODO: error handling (Facebook me request failed");
                    }
                });
                Request.executeBatchAsync(request);
            }
            else if (state.isClosed())
                showFacebookLogin(true);
            else
                throw new IllegalStateException("Unknown FB Session state - neither open nor closed? Is Schroedinger's cat both alive and dead?");
        }
    }

    private void showFacebookLogin(boolean show) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (loginButton == null)
            loginButton = inflater.inflate(R.layout.fragment_auth_fb, null);

        ViewGroup layout = (ViewGroup) findViewById(R.id.facebook_login_holder);
        if (show) {
            if (layout.getChildCount() == 0)
                layout.addView(loginButton);
        }
        else
            layout.removeAllViews();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        facebookUiHelper = new UiLifecycleHelper(this, callback);
        facebookUiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        pagerAdapter = new HomePagerAdapter(getFragmentManager());

        challengeHolder = (RelativeLayout)findViewById(R.id.challengeFragment);

        activeChallengeFragment = new ChallengeDetailBean();

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.home_pager);
        viewPager.setAdapter(pagerAdapter);

        mProgressView = findViewById(R.id.loading_challenge);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < pagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(pagerAdapter.getPageTitle(i))
                            .setTabListener(this)
            );
        }

        Button raceNowButton = (Button) findViewById(R.id.race_now_quickmatch);
        raceNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, ChooseFitnessActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        facebookUiHelper.onPause();
        paused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        facebookUiHelper.onResume();
        paused = false;

        /*
        FIXME: the following lines are adapted from FB's Android API demo 'Scrumptious', which
        has a MainActivity that extends FragmentActivity. MainActivity overrides
        onResumeFragments()... but we don't have that here, as this class directly extends
        Activity (the ViewPager holds the Fragment references instead). As such this code sits here
        for the moment, but it may result in lifecycle sequencing issues.
         */
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            // if the session is already open, try to show the selection fragment
            showFacebookLogin(false);
        } else {
            showFacebookLogin(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookUiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        facebookUiHelper.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onFragmentInteraction(UserBean user) {
        log.info("Friend selected: {}", user.getId());
        if (user.getId() > 0) {
            Helper.queueAction(String.format("{\"action\":\"challenge\", \"target\":%d,\n" +
                    "            \"taunt\" : \"Try beating my track!\",\n" +
                    "            \"challenge\" : {\n" +
                    "                    \"distance\": %d,\n" +
                    "                    \"duration\": %d,\n" +
                    "                    \"public\": true,\n" +
                    "                    \"start_time\": null,\n" +
                    "                    \"stop_time\": null,\n" +
                    "                    \"type\": \"duration\"\n" +
                    "            }}", user.getId(), 5, 1000));
        }
    }

    @Override
    public void onFragmentInteraction(ChallengeNotificationBean challengeNotification) {
        log.info("Challenge selected: {}", challengeNotification.getId());
        Notification.get(challengeNotification.getId()).setRead(true);

        challengeDisplayed = true;
        challengeHolder.setVisibility(View.VISIBLE);
        activeChallengeFragment = new ChallengeDetailBean();
        UserBean opponentUserBean = challengeNotification.getUser();
        activeChallengeFragment.setOpponent(challengeNotification.getUser());
        User player = SyncHelper.getUser(AccessToken.get().getUserId());
        final UserBean playerBean = new UserBean();
        playerBean.setId(player.getId());
        playerBean.setName(player.getName());
        playerBean.setShortName(StringFormattingUtils.getForenameAndInitial(player.getName()));
        playerBean.setProfilePictureUrl(player.getImage());
        activeChallengeFragment.setPlayer(playerBean);
        activeChallengeFragment.setChallenge(challengeNotification.getChallenge());

        TextView challengeHeaderText = (TextView)findViewById(R.id.challengeHeader);
        String headerText = getString(R.string.challenge_notification_duration);
        DurationChallengeBean challengeAsDuration = (DurationChallengeBean)activeChallengeFragment.getChallenge();
        String formattedHeader = String.format(headerText, challengeAsDuration.getDuration().getStandardMinutes());
        challengeHeaderText.setText(formattedHeader);
        Task.callInBackground(new Callable<ChallengeTrackSummaryBean>() {

            @Override
            public ChallengeTrackSummaryBean call() throws Exception {
                ChallengeTrackSummaryBean challengeTrackSummaryBean = new ChallengeTrackSummaryBean();
                Challenge challenge = SyncHelper.getChallenge(activeChallengeFragment.getChallenge().getChallengeId());
                challengeTrackSummaryBean.setChallenge(challenge);
                Boolean playerFound = false;
                Boolean opponentFound = false;
                if(challenge != null) {
                    for(Challenge.ChallengeAttempt attempt : challenge.getAttempts()) {
                        if(attempt.user_id == playerBean.getId() && !playerFound) {
                            playerFound = true;
                            Track playerTrack = SyncHelper.getTrack(attempt.device_id, attempt.track_id);
                            challengeTrackSummaryBean.setPlayerTrack(playerTrack);
                            Double init_alt = null;
                            double min_alt = Double.MAX_VALUE;
                            double max_alt = Double.MIN_VALUE;
                            double max_speed = 0;
                            for (Position position : playerTrack.getTrackPositions()) {
                                if (position.getAltitude() != null && init_alt != null) init_alt = position.altitude;
                                if (position.getAltitude() != null && max_alt < position.getAltitude()) max_alt = position.getAltitude();
                                if (position.getAltitude() != null && min_alt > position.getAltitude()) min_alt = position.getAltitude();
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
                        } else if(attempt.user_id == activeChallengeFragment.getOpponent().getId() && !opponentFound) {
                            opponentFound = true;
                            Track opponentTrack = SyncHelper.getTrack(attempt.device_id, attempt.track_id);
                            challengeTrackSummaryBean.setOpponentTrack(opponentTrack);
                            Double init_alt = null;
                            double min_alt = Double.MAX_VALUE;
                            double max_alt = Double.MIN_VALUE;
                            double max_speed = 0;
                            for (Position position : opponentTrack.getTrackPositions()) {
                                if (position.getAltitude() != null && init_alt != null) init_alt = position.altitude;
                                if (position.getAltitude() != null && max_alt < position.getAltitude()) max_alt = position.getAltitude();
                                if (position.getAltitude() != null && min_alt > position.getAltitude()) min_alt = position.getAltitude();
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
                        if(playerFound && opponentFound) {
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
                String durationText = getString(R.string.challenge_notification_duration);
                DurationChallengeBean durationChallenge = (DurationChallengeBean)activeChallengeFragment.getChallenge();

                int duration = durationChallenge.getDuration().toStandardMinutes().getMinutes();
                activeChallengeFragment.setTitle(String.format(durationText, duration));

                TextView opponentName = (TextView)findViewById(R.id.opponentName);
                opponentName.setText(activeChallengeFragment.getOpponent().getShortName());

                TextView playerName = (TextView)findViewById(R.id.playerName);
                playerName.setText(activeChallengeFragment.getPlayer().getShortName());

                TrackSummaryBean playerTrack = activeChallengeFragment.getPlayerTrack();
                Boolean playerComplete = false;
                if(playerTrack != null) {
                    playerComplete = true;

                    String formattedDistance = StringFormattingUtils.getDistanceInKmString(playerTrack.getDistanceRan());
                    setTextViewAndColor(R.id.playerDistance, "#269b47", formattedDistance + "KM");
                    setTextViewAndColor(R.id.playerAveragePace, "#269b47", playerTrack.getAveragePace() + "");
                    setTextViewAndColor(R.id.playerTopSpeed, "#269b47", playerTrack.getTopSpeed() + "");
                    setTextViewAndColor(R.id.playerTotalUp, "#269b47", playerTrack.getTotalUp() + "");
                    setTextViewAndColor(R.id.playerTotalDown, "#269b47", playerTrack.getTotalDown() + "");

                    Button raceNowBtn = (Button)findViewById(R.id.raceNowBtn);
                    raceNowBtn.setVisibility(View.INVISIBLE);
                    Button raceLaterBtn = (Button)findViewById(R.id.raceLaterBtn);
                    raceLaterBtn.setVisibility(View.INVISIBLE);
                }
                TrackSummaryBean opponentTrack = activeChallengeFragment.getOpponentTrack();
                Boolean opponentComplete = false;
                if(opponentTrack != null) {
                    opponentComplete = true;

                    String formattedDistance = StringFormattingUtils.getDistanceInKmString(opponentTrack.getDistanceRan());
                    setTextViewAndColor(R.id.opponentDistance, "#269b47", formattedDistance + "KM");
                    setTextViewAndColor(R.id.opponentAveragePace, "#269b47", opponentTrack.getAveragePace() + "");
                    setTextViewAndColor(R.id.opponentTopSpeed, "#269b47", opponentTrack.getTopSpeed() + "");
                    setTextViewAndColor(R.id.opponentTotalUp, "#269b47", opponentTrack.getTotalUp() + "");
                    setTextViewAndColor(R.id.opponentTotalDown, "#269b47", opponentTrack.getTotalDown() + "");
                }

                if(playerComplete && opponentComplete) {

                    if(playerTrack.getDistanceRan() > opponentTrack.getDistanceRan()) {
                        TextView opponentDistance = (TextView)findViewById(R.id.opponentDistance);
                        opponentDistance.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerDistance = (TextView)findViewById(R.id.playerDistance);
                        playerDistance.setTextColor(Color.parseColor("#e31f26"));
                        FrameLayout rewardIcon = (FrameLayout)findViewById(R.id.reward_icon);
                        rewardIcon.setVisibility(View.INVISIBLE);
                        TextView rewardText = (TextView)findViewById(R.id.rewardPoints);
                        rewardText.setVisibility(View.INVISIBLE);
                    }

                    if(playerTrack.getAveragePace() > opponentTrack.getAveragePace()) {
                        TextView opponentAveragePace = (TextView)findViewById(R.id.opponentAveragePace);
                        opponentAveragePace.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerAveragePace = (TextView)findViewById(R.id.playerAveragePace);
                        playerAveragePace.setTextColor(Color.parseColor("#e31f26"));
                    }

                    if(playerTrack.getTopSpeed() > opponentTrack.getTopSpeed()) {
                        TextView opponentTopSpeed = (TextView)findViewById(R.id.opponentTopSpeed);
                        opponentTopSpeed.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerTopSpeed = (TextView)findViewById(R.id.playerTopSpeed);
                        playerTopSpeed.setTextColor(Color.parseColor("#e31f26"));
                    }

                    if(playerTrack.getTotalUp() > opponentTrack.getTotalUp()) {
                        TextView opponentTotalUp = (TextView)findViewById(R.id.opponentTotalUp);
                        opponentTotalUp.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerTotalUp = (TextView)findViewById(R.id.playerTotalUp);
                        playerTotalUp.setTextColor(Color.parseColor("#e31f26"));
                    }

                    if(playerTrack.getTotalDown() > opponentTrack.getTotalDown()) {
                        TextView opponentTotalDown = (TextView)findViewById(R.id.opponentTotalDown);
                        opponentTotalDown.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerTotalDown = (TextView)findViewById(R.id.playerTotalDown);
                        playerTotalDown.setTextColor(Color.parseColor("#e31f26"));
                    }
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

        final ImageView playerPic = (ImageView)findViewById(R.id.playerProfilePic);
        Picasso.with(this).load(player.getImage()).into(new Target() {
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

        final ImageView opponentPic = (ImageView)findViewById(R.id.playerProfilePic);

        Picasso.with(this).load(activeChallengeFragment.getOpponent().getProfilePictureUrl()).into(new Target() {
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

    }

    public void onRaceLaterClick(View view) {
        onBackPressed();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void resetExpandedChallenge() {
        ImageView opponentPic = (ImageView)findViewById(R.id.opponentProfilePic);
        opponentPic.setImageDrawable(getResources().getDrawable(R.drawable.default_profile_pic));

        setTextViewAndColor(R.id.opponentName, "#1f1f1f", getString(R.string.challenge_opponent_distance));

        setTextViewAndColor(R.id.opponentDistance, "#1f1f1f", getString(R.string.challenge_opponent_distance));
        setTextViewAndColor(R.id.opponentAveragePace, "#1f1f1f", getString(R.string.challenge_opponent_distance));
        setTextViewAndColor(R.id.opponentTopSpeed, "#1f1f1f", getString(R.string.challenge_opponent_distance));
        setTextViewAndColor(R.id.opponentTotalUp, "#1f1f1f", getString(R.string.challenge_opponent_distance));
        setTextViewAndColor(R.id.opponentTotalDown, "#1f1f1f", getString(R.string.challenge_opponent_distance));

        setTextViewAndColor(R.id.playerDistance, "#1f1f1f", getString(R.string.challenge_opponent_distance));
        setTextViewAndColor(R.id.playerAveragePace, "#1f1f1f", getString(R.string.challenge_opponent_distance));
        setTextViewAndColor(R.id.playerTopSpeed, "#1f1f1f", getString(R.string.challenge_opponent_distance));
        setTextViewAndColor(R.id.playerTotalUp, "#1f1f1f", getString(R.string.challenge_opponent_distance));
        setTextViewAndColor(R.id.playerTotalDown, "#1f1f1f", getString(R.string.challenge_opponent_distance));

        FrameLayout rewardIcon = (FrameLayout)findViewById(R.id.reward_icon);
        rewardIcon.setVisibility(View.VISIBLE);
        TextView rewardText = (TextView)findViewById(R.id.rewardPoints);
        rewardText.setVisibility(View.VISIBLE);

        Button raceNowBtn = (Button)findViewById(R.id.raceNowBtn);
        raceNowBtn.setVisibility(View.VISIBLE);
        Button raceLaterBtn = (Button)findViewById(R.id.raceLaterBtn);
        raceLaterBtn.setVisibility(View.VISIBLE);
    }

    public void setTextViewAndColor(int textViewId, String color, String textViewString) {
        TextView textView = (TextView)findViewById(textViewId);
        textView.setTextColor(Color.parseColor(color));
        textView.setText(textViewString);
    }

    @Override
    public void onBackPressed() {
        if(challengeDisplayed) {
            challengeDisplayed = false;
            challengeHolder.setVisibility(View.GONE);
            resetExpandedChallenge();
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class HomePagerAdapter extends FragmentPagerAdapter {

        private Map<Integer, Fragment> fragments =
                new ImmutableMap.Builder<Integer, Fragment>()
                        .put(0, new ChallengeFragment())
                        .put(1, new FriendFragment())
                        .build();
        private Map<Integer, String> fragmentTitles =
                new ImmutableMap.Builder<Integer, String>()
                        .put(0, getString(R.string.title_challenges_page))
                        .put(1, getString(R.string.title_friends_page))
                        .build();

        public HomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = fragments.get(position);
            if (fragment != null)
                return fragment;
            throw new IllegalArgumentException(String.format("No such tab index: %d", position));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String title = fragmentTitles.get(position);
            if (title != null)
                return title;
            throw new IllegalArgumentException(String.format("No such tab index: %d", position));
        }
    }
}
