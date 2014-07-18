package com.raceyourself.raceyourself.home;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.raceyourself.platform.auth.AuthenticationActivity;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Friend;
import com.raceyourself.platform.models.Invite;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BaseActivity;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.feed.ChallengeListAdapter;
import com.raceyourself.raceyourself.home.feed.ChallengeNotificationBean;
import com.raceyourself.raceyourself.home.feed.HomeFeedFragment;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;
import com.raceyourself.raceyourself.home.sendchallenge.FriendFragment;
import com.raceyourself.raceyourself.home.sendchallenge.SetChallengeActivity;
import com.raceyourself.raceyourself.matchmaking.MatchmakingPopupController;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HomeActivity extends BaseActivity implements ActionBar.TabListener,
        FriendFragment.OnFragmentInteractionListener, HomeFeedFragment.OnFragmentInteractionListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
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
        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        pagerAdapter = new HomePagerAdapter(getFragmentManager());
        pagerAdapter.getHomeFeedFragment().setOnCreateViewListener(new Runnable() {
            @Override
            public void run() {
                // Attach ChallengeVersusAnimator once challenge list is created
                final ChallengeListAdapter cadapter = pagerAdapter.getHomeFeedFragment().getInboxListAdapter();
                cadapter.setExpandCollapseListener(new ChallengeVersusAnimator(HomeActivity.this, cadapter));

                final ChallengeListAdapter rAdapter = pagerAdapter.getHomeFeedFragment().getRunListAdapter();
                rAdapter.setExpandCollapseListener(new ChallengeVersusAnimator(HomeActivity.this, rAdapter));
            }
        });

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

        ImageButton raceNowButton = (ImageButton) findViewById(R.id.raceNowImageBtn);
        raceNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                matchmakingPopupController.displayFitnessPopup();
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String alertText = extras.getString("alert");
            if (alertText != null) {
                Toast.makeText(this, alertText, Toast.LENGTH_SHORT).show();
            }
        }

        ImageView playerPic = (ImageView)findViewById(R.id.playerProfilePic);
        User player = User.get(AccessToken.get().getUserId());
        Picasso.with(this).load(player.getImage())
                .placeholder(R.drawable.default_profile_pic)
                .transform(new PictureUtils.CropCircle())
                .into(playerPic);
        ImageView rankIcon = (ImageView)findViewById(R.id.playerRank);
        rankIcon.setImageDrawable(getResources().getDrawable(UserBean.getRankDrawable(player.getRank())));

        TextView playerName = (TextView)findViewById(R.id.playerName);
        playerName.setText(StringFormattingUtils.getForename(player.getName()));

        matchmakingPopupController = new MatchmakingPopupController(this);
    }

    public void onFitnessBtn(View view) {
        matchmakingPopupController.onFitnessBtn(view);
    }

    @Override
    public void onBackPressed() {
        if(!matchmakingPopupController.isDisplaying()) {
            super.onBackPressed();
        }
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
    public void onQuickmatchSelect() {
        matchmakingPopupController.displayFitnessPopup();
    }

    @Override
    public void onFragmentInteraction(UserBean user) {
        log.info("Friend selected: {}", user.getId());

        if (user == null)
            throw new IllegalArgumentException("null friend");


        UserBean.JoinStatus status = user.getJoinStatus();
        if (status == UserBean.JoinStatus.NOT_MEMBER) {
            inviteFacebookFriend(user);
        } else if (status == UserBean.JoinStatus.INVITE_SENT) {
            // no action defined at present. maybe send reminder?
        }
        else if (status.isMember()) {
            if (user.getId() <= 0)
                throw new IllegalArgumentException("Friend's ID must be positive.");
            challengeFriend(user);
        }
        else
            throw new Error("Unrecognised UserBean.JoinStatus: " + status);
    }

    private void challengeFriend(UserBean user) {
        Intent intent = new Intent(this, SetChallengeActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("opponent", user);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void ShowFacebookInviteDialog(final Invite invite, final String provider, final String uid) {
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
                            Toast.makeText(HomeActivity.this, "Invite Sent", Toast.LENGTH_SHORT).show();
                        } else {
                            //request cancelled
                            log.info("home - request cancelled");
                            Toast.makeText(HomeActivity.this, "Request Cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }).build();
            requestDialog.show();
        } else {
            log.info("home - invite is null");
        }
    }

    private void inviteFacebookFriend(final UserBean user) {
        final Invite invite = Invite.getFirstUnused();

        List<Invite> unused = Invite.getUnused();

        log.info("home - invite count is " + unused.size());

        Session session = Session.getActiveSession();
        if(session == null || !session.isOpened()) {
            Session.openActiveSession(this, true, new Session.StatusCallback() {

                // callback when session changes state
                @Override
                public void call(Session session, SessionState state, Exception exception) {
                    if(session.isOpened()) {
                        ShowFacebookInviteDialog(invite, user.getProvider(), user.getUid());
                    }
                }
            });
        } else {
            ShowFacebookInviteDialog(invite, user.getProvider(), user.getUid());
        }
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

        // Animate versus images


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

        activeChallengeFragment = new ChallengeDetailBean();
        activeChallengeFragment.setOpponent(challengeNotification.getOpponent());
        User player = SyncHelper.getUser(AccessToken.get().getUserId());
        final UserBean playerBean = new UserBean(player);
        activeChallengeFragment.setPlayer(playerBean);
        activeChallengeFragment.setChallenge(challengeNotification.getChallenge());

        final Context context = this;

        Task.callInBackground(new Callable<ChallengeDetailBean>() {

            @Override
            public ChallengeDetailBean call() throws Exception {
                Challenge challenge = SyncHelper.getChallenge(activeChallengeFragment.getChallenge().getDeviceId(), activeChallengeFragment.getChallenge().getChallengeId());
                Boolean playerFound = false;
                Boolean opponentFound = false;
                if(challenge != null) {
                    log.info(String.format("Challenge <%d,%d>- checking attempts, there are %d attempts", challenge.device_id, challenge.challenge_id, challenge.getAttempts().size()));
                    for(Challenge.ChallengeAttempt attempt : challenge.getAttempts()) {
                        if(attempt.user_id == playerBean.getId() && !playerFound) {
                            log.info("Challenge - checking attempts, found player " + attempt.user_id);
                            playerFound = true;
                            Track playerTrack = SyncHelper.getTrack(attempt.track_device_id, attempt.track_id);
                            TrackSummaryBean playerTrackBean = new TrackSummaryBean(playerTrack);
                            activeChallengeFragment.setPlayerTrack(playerTrackBean);
                        } else if(attempt.user_id == activeChallengeFragment.getOpponent().getId() && !opponentFound) {
                            log.info("Challenge - checking attempts, found opponent " + attempt.user_id);
                            opponentFound = true;
                            Track opponentTrack = SyncHelper.getTrack(attempt.track_device_id, attempt.track_id);
                            TrackSummaryBean opponentTrackBean = new TrackSummaryBean(opponentTrack);
                            activeChallengeFragment.setOpponentTrack(opponentTrackBean);
                        }
                        if(playerFound && opponentFound) {
                            break;
                        }
                    }
                }
                return activeChallengeFragment;
            }
        }).continueWith(new Continuation<ChallengeDetailBean, Void>() {
            @Override
            public Void then(Task<ChallengeDetailBean> challengeTask) throws Exception {
                activeChallengeFragment.setPoints(20000);
                String durationText = getString(R.string.challenge_notification_duration);

                int duration = activeChallengeFragment.getChallenge().getChallengeGoal() / 60;
                activeChallengeFragment.setTitle(String.format(durationText, duration));

                Intent challengeExpanded = new Intent(context, ChallengeSummaryActivity.class);
                challengeExpanded.putExtra("challenge", activeChallengeFragment);
                challengeExpanded.putExtra("previous", "home");
                context.startActivity(challengeExpanded);

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class HomePagerAdapter extends FragmentPagerAdapter {
        @Getter
        private HomeFeedFragment homeFeedFragment = new HomeFeedFragment();
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
