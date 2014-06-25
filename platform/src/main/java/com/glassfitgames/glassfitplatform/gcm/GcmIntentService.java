package com.glassfitgames.glassfitplatform.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.raceyourself.platform.R;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {
    public static final String TAG = "GcmIntentService";
    
    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Log.e(TAG, "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                Log.e(TAG, "Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                String text = extras.getString("text");
                Helper.getInstance(getApplicationContext());
                if (Helper.hasPermissions("any", "login")) Helper.syncToServer(getApplicationContext());
                else text = "Log in to race";
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                Log.i(TAG, "Received: " + extras.toString());
                createNotification(extras.getString("title"), text);
            } else Log.w(TAG, "Unknown message type: " + messageType);
        
        }        
        Log.i(TAG, "Completed service @ " + SystemClock.elapsedRealtime());
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
    
    protected void createNotification(String title, String text) {
        Log.i(TAG, "Creating notification");
        
        Resources res = getApplicationContext().getResources();
        String rname = "ic_launcher";
        String rtype = "drawable";
        int icon = res.getIdentifier(rname, rtype, getPackageName());
        if (icon <= 0) {
            Log.e(TAG, "Could not find resource: " + getPackageName() + "." + rtype + "." + rname);
            return;
        }
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text);
        
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(Uri.fromParts("raceyourself", "challenge", ""));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent =
            PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        );        
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);
        // Sets an ID for the notification
        int mNotificationId = (int)(System.currentTimeMillis()%Integer.MAX_VALUE);
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr = 
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
        Log.i(TAG, "Notified user");
    }
}