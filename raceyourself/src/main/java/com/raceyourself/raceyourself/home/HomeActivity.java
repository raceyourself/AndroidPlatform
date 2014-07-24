package com.raceyourself.raceyourself.home;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AtomicDouble;
import com.raceyourself.platform.auth.AuthenticationActivity;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Authentication;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Friend;
import com.raceyourself.platform.models.Invite;
import com.raceyourself.platform.models.Mission;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.Transaction;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.points.PointsHelper;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BaseActivity;
import com.raceyourself.raceyourself.base.ParticleAnimator;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.feed.ChallengeListAdapter;
import com.raceyourself.raceyourself.home.feed.ChallengeNotificationBean;
import com.raceyourself.raceyourself.home.feed.HomeFeedFragment;
import com.raceyourself.raceyourself.home.feed.HomeFeedFragment_;
import com.raceyourself.raceyourself.home.feed.HorizontalMissionListAdapter;
import com.raceyourself.raceyourself.home.feed.MissionBean;
import com.raceyourself.raceyourself.home.feed.VerticalMissionListWrapperAdapter;
import com.raceyourself.raceyourself.home.sendchallenge.FriendFragment;
import com.raceyourself.raceyourself.home.sendchallenge.FriendView;
import com.raceyourself.raceyourself.home.sendchallenge.SetChallengeView;
import com.raceyourself.raceyourself.home.sendchallenge.SetChallengeView_;
import com.raceyourself.raceyourself.matchmaking.MatchmakingPopupController;
import com.raceyourself.raceyourself.shop.ShopActivity_;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import it.sephiroth.android.library.widget.HListView;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EActivity
public class HomeActivity extends BaseActivity implements ActionBar.TabListener,
        FriendView.OnFriendAction,
        HomeFeedFragment.OnFragmentInteractionListener,
        HorizontalMissionListAdapter.OnFragmentInteractionListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    @Getter
    private HomePagerAdapter pagerAdapter;

    public volatile static boolean challengeDisplayed = false;

    ChallengeDetailBean activeChallengeFragment;

    private View mProgressView;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager viewPager;

    private boolean paused;

    private View loginButton;

    private PopupWindow matchmakingPopup;

    private EditText emailFriendEdit;
    private Button sendInviteBtn;

    private MatchmakingPopupController matchmakingPopupController;

    private TutorialOverlay tutorialOverlay;

    private UiLifecycleHelper facebookUiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    private List<ParticleAnimator> coinAnimators = new LinkedList<ParticleAnimator>();
    private PopupWindow notYetRunPopup;
    private SetChallengeView setChallengeView;

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
                        else {
                            log.error("TODO: error handling (Facebook me request failed");
                        }
                    }
                });
                Request.executeBatchAsync(request);
            }
            else if (state.isClosed())
                showFacebookLogin(true);
            else
                log.error("Unknown FB Session state - neither open nor closed? Is Schroedinger's cat both alive and dead?");
        }
    }

    private void showFacebookLogin(boolean show) {
        Button fbButton = (Button) findViewById(R.id.facebook_connect_button);
        fbButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void onMatchClick(View view) {
        matchmakingPopupController.onMatchClick();
    }

    public void onRaceClick(View view) {
        matchmakingPopupController.onRaceClick();
    }

    public void onSearchAgainClick(View view) {
        // TODO: remove old finding popup before displaying the new one
        matchmakingPopupController.displayFindingPopup();
    }

    public void onCancel(View view) {
        onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log.info("onCreate called");
        log.info("challenge - setting displayed false, currently is " + challengeDisplayed);
        challengeDisplayed = false;

        facebookUiHelper = new UiLifecycleHelper(this, callback);
        facebookUiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        LayoutInflater li = LayoutInflater.from(this);
        View customView = li.inflate(R.layout.action_bar_home, null);
        actionBar.setCustomView(customView);

        ImageView settingsIcon = (ImageView)customView.findViewById(R.id.action_settings);
        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Settings screen coming soon", Toast.LENGTH_SHORT).show();
            }
        });
        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        pagerAdapter = new HomePagerAdapter(getFragmentManager());

        activeChallengeFragment = new ChallengeDetailBean();

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.home_pager);
        viewPager.setAdapter(pagerAdapter);

//        mProgressView = findViewById(R.id.loading_challenge);

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

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String alertText = extras.getString("alert");
            if (alertText != null) {
                Toast.makeText(this, alertText, Toast.LENGTH_LONG).show();
            }
        }

        matchmakingPopupController = new MatchmakingPopupController(this);

        //launch the tutorial
        tutorialOverlay = new TutorialOverlay(HomeActivity.this, (ViewGroup)findViewById(R.id.activity_home));
        tutorialOverlay.popup();
    }

    public void onFitnessBtn(View view) {
        matchmakingPopupController.onFitnessBtn(view);
    }

    @Override
    public void onBackPressed() {
        // TODO refactor - isDisplaying() has side-effect of dismissing if open :o
        boolean matchmaking = matchmakingPopupController.isDisplaying();

        boolean notYetRun = notYetRunPopup != null && notYetRunPopup.isShowing();
        boolean setChallenge = setChallengeView != null && setChallengeView.isShowing();
        if (notYetRun)
            notYetRunPopup.dismiss();
        if (setChallenge)
            setChallengeView.dismiss();

        if (!matchmaking && !notYetRun && !setChallenge)
            super.onBackPressed();
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
        log.info("challenge - setting displayed false, currently is " + challengeDisplayed);
        challengeDisplayed = false;
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
            showFacebookLogin(Authentication.getAuthenticationByProvider("facebook") == null);
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

        TextView pointsView = (TextView) findViewById(R.id.points_value);
        User player = User.get(AccessToken.get().getUserId());
        pointsView.setText(String.valueOf(player.getPoints()));

        ImageView store = (ImageView) findViewById(R.id.store);
        store.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shopIntent = new Intent(HomeActivity.this, ShopActivity_.class);
                startActivity(shopIntent);
            }
        });

        ImageView settings = (ImageView) findViewById(R.id.action_settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Settings menu. Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView watch = (ImageView) findViewById(R.id.watchIcon);
        watch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Smartwatch integration. Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView glass = (ImageView) findViewById(R.id.glassIcon);
        glass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Google Glass integration. Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

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
    public void onQuickmatchSelect() {
        matchmakingPopupController.displayFitnessPopup();
    }

    @Override
    public void sendChallenge(UserBean friend) {
        if (friend.getId() <= 0)
            throw new IllegalArgumentException("Friend's ID must be positive.");

        int playerUserId = AccessToken.get().getUserId();

        List<Track> tracks = Track.getTracks(playerUserId);

        // Check if they've done a run lasting at least 5 minutes.
        boolean hasRun = false;
        for (Track track : tracks) {
            if (track.getTime() > 60 * 1000 * 5) {
                hasRun = true;
            }
        }

        if (!hasRun) {
//        if (new java.util.Random().nextBoolean()) { // for easier testing.
            View popupView = LayoutInflater.from(this).inflate(R.layout.popup_race_before_challenging, null, false);

            Button findBtn = (Button) popupView.findViewById(R.id.findBtn);
            findBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notYetRunPopup.dismiss();
                }
            });

            // TODO factor out the positioning stuff - copy-pasted too many times...
            notYetRunPopup = new PopupWindow(popupView);
            notYetRunPopup.setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            notYetRunPopup.showAtLocation(getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        }
        else {
            setChallengeView = SetChallengeView_.build(this);
            setChallengeView.bind(friend);
            setChallengeView.show();
        }
    }

    @Override
    public void invite(final UserBean user) {
        final Invite invite = Invite.getFirstUnused();

        List<Invite> unused = Invite.getUnused();

        log.info("home - invite count is " + unused.size());

        Session session = Session.getActiveSession();
        if(session == null || !session.isOpened()) {
            Session.openActiveSession(this, true, new Session.StatusCallback() {

                // callback when session changes state
                @Override
                public void call(Session session, SessionState state, Exception exception) {
                    if (session.isOpened()) {
                        showFacebookInviteDialog(invite, user.getProvider(), user.getUid());
                    }
                }
            });
        } else {
            showFacebookInviteDialog(invite, user.getProvider(), user.getUid());
        }
    }

    public void bounceMissionList() {
        log.warn("Bouncing mission list..");
        final VerticalMissionListWrapperAdapter vmlwa = pagerAdapter.getHomeFeedFragment().getVerticalMissionListWrapperAdapter();
        final int missionCount = vmlwa.getMissionCount();

        // start at RH end, then animate to LH end after a short delay
        // makes users realise it's a scrolling view
        vmlwa.setMissionSelection(0);

    }

    private void showFacebookInviteDialog(final Invite invite, final String provider, final String uid) {
        if (invite != null) {
            log.info("home - invite not null");
            Bundle params = new Bundle();
            params.putString("message", "Join race yourself!");
            WebDialog requestDialog = (new WebDialog.RequestsDialogBuilder(HomeActivity.this, Session.getActiveSession(), params)).setTo(uid).setOnCompleteListener(new WebDialog.OnCompleteListener() {
                @Override
                public void onComplete(Bundle values, FacebookException error) {
                    if (error != null) {
                        if (error instanceof FacebookOperationCanceledException) {
                            // request cancelled
                            log.info("home - network error");
                            Toast.makeText(HomeActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                        } else {
                            // network error
                            log.info("home - request cancelled");
                            Toast.makeText(HomeActivity.this, "Request Cancelled", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        final String requestId = values.getString("request");
                        if (requestId != null) {
                            //request sent
                            Friend friend = Friend.getFriend(provider, uid);
                            invite.inviteFriend(friend);
                            log.info("home - invite sent");
                            Toast.makeText(HomeActivity.this, "Invite sent", Toast.LENGTH_SHORT).show();
                            refreshFriends();
                        } else {
                            //request cancelled
                            log.info("home - request cancelled");
                            Toast.makeText(HomeActivity.this, "Request cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }).build();
            requestDialog.show();
        } else {
            Toast.makeText(this, "You've run out of invites!", Toast.LENGTH_LONG).show();
        }
    }

    @Background
    void refreshFriends() {
        // TODO AndroidAnnotationsify FriendFragment to be able to @Background a method there directly.
        pagerAdapter.getFriendFragment().refreshFriends();
    }

    public void showInviteEditText(View view) {
        final Invite invite = Invite.getFirstUnused();

        if(invite != null ) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Enter email address");
            alert.setMessage("Enter the email address of your friend to invite them.");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String emailAddress = input.getText().toString();
                    invite.inviteEmail(emailAddress);
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "You have been invited to Race Yourself!");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "You have been invited to Race Yourself! Go to staging.raceyourself.com/beta_sign_up?invite_code=" + invite.code + " to sign up!");
                    try {
                        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                    } catch(ActivityNotFoundException ex) {
                        Toast.makeText(HomeActivity.this, "There are no email clients installed", Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(HomeActivity.this, "Invite sent to " + emailAddress + "!", Toast.LENGTH_SHORT).show();
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        } else {
            Toast.makeText(this, "You have no invites left! Challenge some friends to earn more", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFragmentInteraction(ChallengeNotificationBean challengeNotification) {
//        ChallengeFragment.OnFragmentInteractionListener
        log.info("challenge - checking displayed");
        if(challengeDisplayed) return;
        log.info("challenge - displayed false, setting to true");
        challengeDisplayed = true;
        log.info("Challenge selected: {}", challengeNotification.getId());

        // TODO at present we need to update both the model and the bean representations...
        Notification notification = Notification.get(challengeNotification.getId());
        ChallengeListAdapter adapter = pagerAdapter.getHomeFeedFragment().getInboxListAdapter();
        ChallengeNotificationBean challengeNotificationBean = adapter.getChallengeNotificationBeanById(challengeNotification.getId());
        if (notification != null && challengeNotificationBean != null) {
            notification.setRead(true);
            challengeNotificationBean.setRead(true);
            adapter.notifyDataSetChanged();
        } else {
            log.error("Couldn't set notification read = true. Notification ID is " + challengeNotification.getId());
        }
    }

    @Override
    public void onFragmentInteraction(MissionBean missionBean, View view) {
        final Mission mission = Mission.get(missionBean.getId());
        final Mission.MissionLevel level = mission.getCurrentLevel();

        if (level != null && level.isCompleted() && level.claim()) {
            final Challenge challenge = level.getChallenge();

            // Animation
            final ViewGroup rl = (ViewGroup) findViewById(R.id.homeFeedFragment);
            int[] parent_location = new int[2];
            rl.getLocationOnScreen(parent_location);

            int[] location = new int[2];
            view.getLocationOnScreen(location);
            location[0] = location[0] - parent_location[0] + view.getMeasuredWidth()/2;
            location[1] = location[1] - parent_location[1] + view.getMeasuredHeight()/2;

            int coins = 25;
            final double pointsPerCoin = (double)challenge.points_awarded / coins;
            List<ParticleAnimator.Particle> particles = new ArrayList<ParticleAnimator.Particle>(coins);
            for (int i=0; i<coins; i++) {
                ImageView coin = new ImageView(this);
                coin.setImageDrawable(getResources().getDrawable(R.drawable.icon_coin_small));
                coin.setX(location[0]);
                coin.setY(location[1]);
                rl.addView(coin);
                particles.add(new ParticleAnimator.Particle(coin, new Vector2D(-500+Math.random()*1000, -500+Math.random()*1000)));
            }
            final TextView pointsView = (TextView)findViewById(R.id.points_value);
            final AtomicDouble pointsCounter = new AtomicDouble(0.0);
            int[] target_location = new int[2];
            pointsView.getLocationOnScreen(target_location);
            target_location[0] = target_location[0] - parent_location[0];
            target_location[1] = target_location[1] - parent_location[1];

            final ParticleAnimator coinAnimator = new ParticleAnimator(particles, new Vector2D(target_location[0], target_location[1]), 99999, 500);
            coinAnimator.setParticleListener(new ParticleAnimator.ParticleListener() {
                @Override
                public void onTargetReached(ParticleAnimator.Particle particle, int particlesAlive) {
                    final User player = User.get(AccessToken.get().getUserId());
                    pointsView.setText(String.valueOf(player.getPoints() + (int)pointsCounter.addAndGet(pointsPerCoin)));
                    rl.removeView(particle.getView());
                    if (particlesAlive == 0) {
                        coinAnimators.remove(coinAnimator);
                        try {
                            PointsHelper.getInstance(rl.getContext()).awardPoints("MISSION CLAIM", ("[" + level.mission + "," + level.level + "]"), "HomeActivity.java", challenge.points_awarded);
                        } catch (Transaction.InsufficientFundsException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            coinAnimator.start();
            coinAnimators.add(coinAnimator);
        } else {
            if(missionBean.getCurrentLevel() != null) {
                Toast.makeText(this, missionBean.getCurrentLevel().getLongDescription(), Toast.LENGTH_LONG).show();
            }
        }
        ((MobileApplication)getApplication()).sendMessage(
                HomeFeedFragment.class.getSimpleName(), HomeFeedFragment.MESSAGING_MESSAGE_REFRESH);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class HomePagerAdapter extends FragmentPagerAdapter {
        @Getter
        private HomeFeedFragment homeFeedFragment = new HomeFeedFragment_();
        @Getter
        private FriendFragment friendFragment = new FriendFragment();
        private Map<Integer, Fragment> fragments =
                new ImmutableMap.Builder<Integer, Fragment>()
                        .put(0, homeFeedFragment)
                        .put(1, friendFragment)
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
//challengeExpanded.putExtra("previous", "home");
