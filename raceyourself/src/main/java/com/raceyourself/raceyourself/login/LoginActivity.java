package com.raceyourself.raceyourself.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.raceyourself.platform.auth.AuthenticationActivity;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.AutoMatches;
import com.raceyourself.platform.models.Preference;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.utils.Utils;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BaseActivity;
import com.raceyourself.raceyourself.home.HomeActivity_;
import com.raceyourself.raceyourself.home.TutorialOverlay;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * A login screen that offers login via email/password.
 */
@Slf4j
@EActivity(R.layout.activity_login)
public class LoginActivity extends BaseActivity implements LoaderCallbacks<Cursor> {

    @ViewById(R.id.email)
    AutoCompleteTextView emailView;
    @ViewById(R.id.password)
    EditText passwordView;
    @ViewById(R.id.login_progress)
    View progressView;
    @ViewById(R.id.loginNotice)
    TextView loginNotice;
    @ViewById(R.id.email_sign_in_button)
    Button signInButton;
    @ViewById(R.id.forgotten_password)
    TextView forgottenPassword;

    private boolean isSyncing = false;

    @AfterViews
    void afterViews() {
        // For email.
        populateAutoComplete();

        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        forgottenPassword.setMovementMethod(LinkMovementMethod.getInstance());

        // Skip login if already authenticated
        AccessToken token = AccessToken.get();
        if (token != null && token.getApiAccessToken() != null)
            skipLogin();
    }

    private void skipLogin() {
        signInButton.setEnabled(false);
        signInButton.setText(getString(R.string.login_syncing));
        showProgress(true);
        User user = User.get(AccessToken.get().getUserId());
        emailView.setText(user.email);
        passwordView.setText("*********");
        emailView.setEnabled(false);
        passwordView.setEnabled(false);
        isSyncing = true;

        SyncHelper syncHelper = SyncHelper.getInstance(LoginActivity.this);

        Long lastSync = syncHelper.getLastSync(Utils.SYNC_GPS_DATA);
        if(lastSync != null && lastSync > 0) {
            // Do background sync - don't block before passing to home screen.
            syncHelper.init();
            syncHelper.requestSync();

            startHome();
        } else {
            // Register callback before doing sync, as we block here until sync is complete.
            ((MobileApplication)getApplication()).addCallback(SyncHelper.MESSAGING_TARGET_PLATFORM,
                    SyncHelper.MESSAGING_METHOD_ON_SYNCHRONIZATION, new LoginCallback());

            syncHelper.init();
            syncHelper.requestSync();
        }
    }

    @UiThread
    void connectionFailure() {
        log.warn("Sync failed");
        Toast.makeText(LoginActivity.this, R.string.error_connection_failure, Toast.LENGTH_SHORT).show();
        signInButton.setText(getString(R.string.login_retrying_syncing));
    }

    private void startHome() {
        Intent homeScreenIntent = new Intent(LoginActivity.this, HomeActivity_.class);

        Preference.setBoolean(LoginSignupPromptActivity.PREFERENCE_SKIP_ONBOARDING, true);
        Boolean skipTutorial = Preference.getBoolean(TutorialOverlay.PREFERENCE_SKIP_TUTORIAL);

        homeScreenIntent.putExtra("displayTutorial", skipTutorial == null ? true : !skipTutorial);
        startActivity(homeScreenIntent);
        finish();
    }

    private void populateAutoComplete() {
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onBackPressed() {
        if(!isSyncing) {
            super.onBackPressed();
        }
    }

    private void resetErrors() {
        emailView.setError(null);
        passwordView.setError(null);
        loginNotice.setText("");
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form errors (invalid email,
     * missing fields, etc.), the errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        resetErrors();
        // Disable input.
        signInButton.setEnabled(false);
        emailView.setEnabled(false);
        passwordView.setEnabled(false);

        // Store values at the time of the login attempt.
        final String email = emailView.getText().toString();
        String password = passwordView.getText().toString();

        log.debug("Attempting login. {}", email);

        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
        } else if (!isEmailValid(email)) {
            emailView.setError(getString(R.string.error_invalid_email));
            focusView = emailView;
        }
        // Check for a valid password, if the user entered one.
        else if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
        }

        if (focusView != null) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to perform the user login attempt.
            showProgress(true);
            ((MobileApplication) getApplication()).addCallback(SyncHelper.MESSAGING_TARGET_PLATFORM,
                    AuthenticationActivity.MESSAGING_METHOD_ON_AUTHENTICATION,
                    new MobileApplication.Callback<String>() {
                @Override
                public boolean call(final String s) {
                    onServerResponse(s, email);
                    return true;
                }
            });

            // attempt authentication (in background)
            AuthenticationActivity.login(email, password);
        }
    }

    @UiThread
    void onServerResponse(String status, String email) {
        if (AuthenticationActivity.AUTH_SUCCESS.equals(status)) {
            log.info("{} logged in successfully. Requesting sync.", email);

            signInButton.setText(getString(R.string.login_syncing));

            ((MobileApplication) getApplication()).addCallback(SyncHelper.MESSAGING_TARGET_PLATFORM,
                    SyncHelper.MESSAGING_METHOD_ON_SYNCHRONIZATION, new LoginCallback());

            if (AutoMatches.requiresUpdate())
                loginNotice.setText(getString(R.string.login_first_sync_notice));

            // start a background sync
            SyncHelper syncHelper = SyncHelper.getInstance(LoginActivity.this);
            syncHelper.init();
            syncHelper.requestSync();
            isSyncing = true;
        } else {
            log.info("Login failed with {} for {}", status, email);
            if (AuthenticationActivity.AUTH_FAILURE.equals(status)) {
                passwordView.setError(getString(R.string.error_login_failed));
            } else {
                String error;
                if (AuthenticationActivity.AUTH_FAILURE_NETWORK.equals(status)) {
                    error = getString(R.string.error_login_network_error);
                } else {
                    error = String.format(getString(R.string.error_login_error), status);
                }
                loginNotice.setText(error);
            }
            passwordView.requestFocus();
            emailView.setEnabled(true);
            passwordView.setEnabled(true);
            signInButton.setEnabled(true);
            isSyncing = false;
            showProgress(false);
        }
    }

    private boolean isEmailValid(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8 && password.length() <= 128;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    // Justification for @TargetApi annotation: conditional clause in method ensures that Honeycomb code doesn't get
    // executed on pre-Honeycomb devices.
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Retrieve data rows for the device user's 'profile' contact.
        Uri uri = Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                ContactsContract.Contacts.Data.CONTENT_DIRECTORY);

        // Select only email addresses.
        String selection = ContactsContract.Contacts.Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE};

        // Show primary email addresses first. Note that there won't be
        // a primary email address if the user hasn't specified one.
        String sortOrder = ContactsContract.Contacts.Data.IS_PRIMARY + " DESC";

        return new CursorLoader(this, uri, ProfileQuery.PROJECTION, selection, selectionArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = Lists.newArrayList();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emails);

        emailView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private class LoginCallback implements MobileApplication.Callback<String>  {
        @Override
        public boolean call(String result) {
            if(SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_PARTIAL.equals(result) ||
                    SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_FULL.equals(result)) {
                AutoMatches.ensureAvailability();
                startHome();
                return true;
            } else {
                connectionFailure();
                return false;
            }
        }
    }

    private interface ProfileQuery {
        final static String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.IS_PRIMARY};

        final static int ADDRESS = 0;
        final static int IS_PRIMARY = 1;
    }
}
