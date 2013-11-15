package com.glassfitgames.glassfitplatform.points;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.util.Log;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.models.Transaction;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.unity3d.player.UnityPlayer;

public class PointsHelper {
    
    private static PointsHelper pointsHelper = null;
    private GPSTracker gpsTracker = null;
    private Timer timer = new Timer();
    
    // constants to calc points/level/multipliers. May be overriden by values from database in constructor.
    private final long TIME_SINCE_LAST_ACTIVITY = 0;
    private final int BASE_POINTS_PER_METRE = 5;
    private final int BASE_MULTIPLIER_LEVELS = 4;
    private final int BASE_MULTIPLIER_PERCENT = 25;
    private final long BASE_MULTIPLIER_TIME_THRESH = 8000;  // ms
    private final long ACTIVITY_COMPLETE_MULTIPLIER_PERCENT = 30;
    private final long ACTIVITY_COMPLETE_MULTIPLIER_LEVELS= 7;
    private final long CHALLENGE_COMPLETE_MULTIPLIER_PERCNET = 100;
    
    private float baseSpeed = 0.0f;
    private long currentActivityPoints = 0;  // stored locally to reduce DB access
    private long openingPointsBalance = 0;  // stored locally to reduce DB access
    
    /**
     * Singleton class to manage user's points. Public methods are designed to be accessed from unity.              
     * @param c
     */
    private PointsHelper(Context c) {
        ORMDroidApplication.initialize(c);
        
        // initialisation for this activity
        gpsTracker = Helper.getInstance(c).getGPSTracker();
        lastTimestamp = System.currentTimeMillis();
        lastCumulativeDistance = gpsTracker.getElapsedDistance();
        
        // retrieve opening points balance & store locally to reduce DB access
        Transaction lastTransaction = Transaction.getLastTransaction();
        if (lastTransaction != null) {
            openingPointsBalance = lastTransaction.points_balance;
        }
        
        // initialise constants
        //TIME_SINCE_LAST_ACTIVITY = System.currentTimeMillis() - Position.getMostRecent().getDeviceTimestamp();
        //TODO: init the other constants from the calibration table
        
        // start checking for points to award!
        timer.scheduleAtFixedRate(task, 0, BASE_MULTIPLIER_TIME_THRESH);
    }
    
    /**
     * Get the singleton instance
     * @param c android application context
     * @return Singleton PointsHelper instance
     */
    public static PointsHelper getInstance(Context c) {
        if (pointsHelper == null) {
            pointsHelper = new PointsHelper(c);
        }
        return pointsHelper;
    }
    
    /** 
     * Set the reference speed above which we will add multipliers to the user's score.
     * @param baseSpeed in metres/sec
     */
    public void setBaseSpeed(float baseSpeed) {
        this.baseSpeed = baseSpeed;
    }

    /** 
     * User's total points before starting the current activity
     * @return
     */
    public long getOpeningPointsBalance() {
        return openingPointsBalance;
    }
    
    /**
     * Points earned during the current activity
     * @return
     */
    public long getCurrentActivityPoints() {
        return currentActivityPoints + extrapolatePoints();
    }
    
    /**
     * Flexible helper method for awarding arbitrary in-game points for e.g. custom acheivements
     * @param type: base points, bonus points etc
     * @param calc: string describing how the points were calculated (for human sense check)
     * @param source_id: which bit of code generated the points?
     * @param points_delta: the points to add/deduct from the user's balance
     */
    public void awardPoints(String type, String calc, String source_id, int points_delta) {
        Transaction t = new Transaction(type, calc, source_id, points_delta);
        t.save();
        currentActivityPoints += points_delta;
    }
    
    private int extrapolatePoints() {
        if (gpsTracker == null) {
            return 0;
        } else {
            return (int)((gpsTracker.getElapsedDistance() - lastCumulativeDistance)
                            * lastBaseMultiplier * BASE_POINTS_PER_METRE);
        } 
    }
    
    private long lastTimestamp = 0;
    private double lastCumulativeDistance = 0.0;
    private float lastBaseMultiplier = 1;
    
    private TimerTask task = new TimerTask() {
        public void run() {
            if (gpsTracker == null) return;
            
            // calculate base points
            double currentDistance = gpsTracker.getElapsedDistance();
            double awardDistance = currentDistance - lastCumulativeDistance;
            int points = (int)awardDistance*BASE_POINTS_PER_METRE;
            String calcString = points + " base";
            
            // apply base multiplier
            if (baseSpeed != 0.0) {
                
                // update points based on current multiplier
                points *= lastBaseMultiplier;
                calcString += " * " + lastBaseMultiplier*100 + "% base multiplier";
                
                // update multiplier for next time
                long awardTime = System.currentTimeMillis() - lastTimestamp;
                float awardSpeed = (float)(awardDistance*1000.0/awardTime);
                if (awardSpeed > baseSpeed) {
                    // bump up the multiplier
                    if (lastBaseMultiplier <= (1+BASE_MULTIPLIER_LEVELS*BASE_MULTIPLIER_PERCENT/100.0f)) {
                        lastBaseMultiplier += BASE_MULTIPLIER_PERCENT / 100.0f;
                        UnityPlayer.UnitySendMessage("Preset Track GUI", "NewBaseMultiplier", String.valueOf(lastBaseMultiplier));
                        Log.i("PointsHelper","New base multiplier: " + lastBaseMultiplier);
                    }
                } else if (lastBaseMultiplier != 1) {
                    // reset multiplier to 1
                    lastBaseMultiplier = 1;
                    UnityPlayer.UnitySendMessage("Preset Track GUI", "NewBaseMultiplier", String.valueOf(lastBaseMultiplier));
                    Log.i("PointsHelper","New base multiplier: " + lastBaseMultiplier);
                }
            }
            
            awardPoints("BASE POINTS", calcString, "PointsHelper.java", points);
            lastTimestamp = System.currentTimeMillis();
            lastCumulativeDistance = currentDistance;
        }
    };
}
