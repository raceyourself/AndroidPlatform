package com.raceyourself.raceyourself;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.raceyourself.platform.gpstracker.Helper;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.Device;
import com.raceyourself.platform.utils.MessageHandler;
import com.raceyourself.platform.utils.MessagingInterface;
import com.raceyourself.platform.utils.SyncReportSender;
import com.raceyourself.platform.utils.Utils;
import com.roscopeco.ormdroid.ORMDroidApplication;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ReportsCrashes(
        formKey = "", // This is required for backward compatibility but not used
        formUri = Utils.ACRA_REPORT_URL,
        formUriBasicAuthLogin = "MobileApplication",
        formUriBasicAuthPassword = "f32T#EGg21sdf32g!ANDsoON"
)
public class MobileApplication extends Application implements MessageHandler {

    // TODO: Typed callbacks. The platform itself should probably not be sending messages as strings if we aren't using unity.
    private Table<String, String, List<Callback<String>>> callbacks = HashBasedTable.create();

    @Override
    public void onCreate() {
        super.onCreate();
        ORMDroidApplication.initialize(this);
        MessagingInterface.addHandler(this);

        ACRA.init(this);
        ACRA.getErrorReporter().setReportSender(new SyncReportSender());

        // GCM push notifications
        if (checkPlayServices()) {
            log.info("Play services available");

            String regid = getRegistrationId(getApplicationContext());
            if (regid.isEmpty()) {
                registerInBackground();
            } else {
                Device self = Device.self();
                if (self != null) {
                    self.push_id = regid;
                    self.save();
                }
            }
        }
    }

    @Override
    public void onTerminate() {
        MessagingInterface.removeHandler(this);
        super.onTerminate();
    }

    /// MessageHandler

    public void sendMessage(String method, String message) {
        sendMessage(SyncHelper.MESSAGING_TARGET_PLATFORM, method, message);
    }

    @Override
    public void sendMessage(String target, String method, String message) {
        log.debug("Message: " + target + "::" + method + ": " + message);
        List<Callback<String>> list = callbacks.get(target, method);
        if (list == null || list.isEmpty()) {
            log.debug("No handlers for " + target + "::" + method);
            return;
        }
        List<Callback<String>> completed = new LinkedList<Callback<String>>();
        for (Callback<String> callback : list) {
            if (callback.call(message)) {
                completed.add(callback);
            }
        }
        list.removeAll(completed);
    }

    public void addCallback(String method, Callback<String> callback) {
        addCallback(SyncHelper.MESSAGING_TARGET_PLATFORM, method, callback);
    }

    public void addCallback(String target, String method, Callback<String> callback) {
        List<Callback<String>> list = callbacks.get(target, method);
        if (list == null) {
            synchronized(callbacks) {
                if (callbacks.get(target, method) == null) {
                    callbacks.put(target, method, new LinkedList<Callback<String>>());
                }
                list = callbacks.get(target, method);
            }
        }
        list.add(callback);
    }

    public void removeCallback(String method, Callback<String> callback) {
        removeCallback(SyncHelper.MESSAGING_TARGET_PLATFORM, method, callback);
    }

    public void removeCallback(String target, String method, Callback<String> callback) {
        List<Callback<String>> list = callbacks.get(target, method);
        if (list != null) list.remove(callback);
    }

    public static interface Callback<T> {
        // Call callback
        // a return value of true removes the callback from the handler
        public boolean call(T t);
    }

    /// GCM Push notifications

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(Utils.GCM_REG_ID, "");
        if (registrationId.isEmpty()) {
            log.info("GCM registration not found.");
            return "";
        }
        return registrationId;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.GCM_REG_ID, regId);
        editor.commit();
        // Store in device pushId
        Device self = Device.self();
        if (self != null) {
            self.push_id = regId;
            self.save();
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(this.getClass().getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<String, String, Boolean>() {
            @Override
            protected Boolean doInBackground(final String... params) {
                try {
                    Context context = getApplicationContext();
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                    String regid = gcm.register(Utils.GCM_SENDER_ID);
                    log.info("Device registered with GCM, id=" + regid);

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (final IOException ex) {
                    log.error("Failed to register device with GCM", ex);
                    return false;
                }
                return true;
            }

        }.execute(null, null, null);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        if (Helper.onGlass()) return false; // TODO: Fix resources so this special case isn't needed.
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            log.warn("This device is not supported by Play Services");
            return false;
        }
        return true;
    }
}
