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
import android.util.Log;
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

import com.raceyourself.platform.auth.AuthenticationActivity;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.AutoMatches;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.utils.Utils;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BaseActivity;
import com.raceyourself.raceyourself.home.HomeActivity;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * A login screen that offers login via email/password.

 */
@Slf4j
public class LoginActivity extends BaseActivity implements LoaderCallbacks<Cursor>{

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private TextView mLoginNotice;
    private Button mEmailSignInButton;
    private boolean isSyncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

//        mEmailView.setText("Foo=" + getFoo());

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        TextView mForgottenPassword = (TextView) findViewById(R.id.forgotten_password);
        mForgottenPassword.setMovementMethod(LinkMovementMethod.getInstance());

        mProgressView = findViewById(R.id.login_progress);

        mLoginNotice = (TextView)findViewById(R.id.loginNotice);

        // Skip login if already authenticated
        AccessToken ud = AccessToken.get();
        if (ud != null && ud.getApiAccessToken() != null) {
            // start a background sync
            SyncHelper syncHelper = SyncHelper.getInstance(LoginActivity.this);
            syncHelper.init();
            syncHelper.requestSync();

            mEmailSignInButton.setEnabled(false);
            mEmailSignInButton.setText(getString(R.string.login_syncing));
            showProgress(true);
            User user = User.get(AccessToken.get().getUserId());
            mEmailView.setText(user.email);
            mPasswordView.setText("*********");
            mEmailView.setEnabled(false);
            mPasswordView.setEnabled(false);
            isSyncing = true;
            Long lastSync = syncHelper.getLastSync(Utils.SYNC_GPS_DATA);
            if(lastSync != null && lastSync > 0) {
                Intent homeScreenIntent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(homeScreenIntent);
                finish();
            } else {
                ((MobileApplication)getApplication()).addCallback("Platform", "OnSynchronization", new MobileApplication.Callback<String>() {

                    @Override
                    public boolean call(String result) {
                        if("full".equalsIgnoreCase(result) || "partial".equalsIgnoreCase(result)) {
                            Intent homeScreenIntent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(homeScreenIntent);
                            finish();
                            return true;
                        } else {
                            Log.i("LoginActivity", "Sync failed");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Failed to contact the Race Yourself server. Please make sure you are connected to the internet", Toast.LENGTH_SHORT).show();
                                    mEmailSignInButton.setText(getString(R.string.login_retrying_syncing));
                                }
                            });

                            return false;
                        }
                    }
                });
            }
        }
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

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mLoginNotice.setText("");

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        log.debug("Attempting login. {}", email);

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            final String mEmail = email;
            ((MobileApplication)getApplication()).addCallback("Platform", "OnAuthentication", new MobileApplication.Callback<String>() {
                @Override
                public boolean call(final String s) {
                    LoginActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            if ("Success".equalsIgnoreCase(s)) {
                                log.info(mEmail + " logged in successfully");
                                // start a background sync
                                SyncHelper syncHelper = SyncHelper.getInstance(LoginActivity.this);
                                syncHelper.init();
                                syncHelper.requestSync();
                                isSyncing = true;

                                mEmailSignInButton.setEnabled(false);
                                mEmailSignInButton.setText(getString(R.string.login_syncing));
                                if (AutoMatches.requiresUpdate()) mLoginNotice.setText(getString(R.string.login_first_sync_notice));
                                mEmailView.setEnabled(false);
                                mPasswordView.setEnabled(false);
                                showProgress(true);
                                ((MobileApplication)getApplication()).addCallback("Platform", "OnSynchronization", new MobileApplication.Callback<String>() {

                                    @Override
                                    public boolean call(String result) {
                                        if("full".equalsIgnoreCase(result) || "partial".equalsIgnoreCase(result)) {
                                            Intent homeScreenIntent = new Intent(LoginActivity.this, HomeActivity.class);
                                            startActivity(homeScreenIntent);
                                            finish();
                                            return true;
                                        } else {
                                            Log.i("LoginActivity", "Sync failed");
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(LoginActivity.this, "Failed to contact the Race Yourself server. Please make sure you are connected to the internet", Toast.LENGTH_SHORT).show();
                                                    mEmailSignInButton.setText(getString(R.string.login_retrying_syncing));
                                                }
                                            });

                                            return false;
                                        }
                                    }
                                });

                            } else if ("Failure".equalsIgnoreCase(s)) {
                                log.info("Login failed for " + mEmail);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mPasswordView.setError(getString(R.string.error_login_failed));
                                        mPasswordView.requestFocus();
                                        mEmailView.setEnabled(true);
                                        mPasswordView.setEnabled(true);
                                        isSyncing = false;
                                        showProgress(false);
                                    }
                                });

                            } else if ("Network error".equalsIgnoreCase(s)) {
                                log.info("Login failed with " + s + " for " + mEmail);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mLoginNotice.setText(getString(R.string.error_login_network_error));
                                        mPasswordView.requestFocus();
                                        mEmailView.setEnabled(true);
                                        mPasswordView.setEnabled(true);
                                        isSyncing = false;
                                        showProgress(false);
                                    }
                                });

                            } else {
                                log.info("Login failed with " + s + " for " + mEmail);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mLoginNotice.setText(String.format(getString(R.string.error_login_error), s));
                                        mPasswordView.requestFocus();
                                        mEmailView.setEnabled(true);
                                        mPasswordView.setEnabled(true);
                                        isSyncing = false;
                                        showProgress(false);
                                    }
                                });

                            }
                        }
                    });
                    return true;
                }
            });

            // attempt authentication (spawns new thread)
            AuthenticationActivity.login(mEmail, password);
        }
    }
    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                                                                     .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }
}
