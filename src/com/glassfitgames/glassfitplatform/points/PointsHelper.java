package com.glassfitgames.glassfitplatform.points;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import android.content.Context;
import android.util.Log;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.glassfitgames.glassfitplatform.models.Transaction;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.unity3d.player.UnityPlayer;

/**
 * Singleton class to manage user's points. 
 * Public methods are designed to be accessed from unity.          
 * 
 * The class polls GPS tracker at a fixed interval (BASE_MULTIPLIER_TIME_THRESH), awards
 * points to the user by storing Transactions in the database and messages unity (UNITY_TARGET) 
 * when the user has gained or lost point streaks (NewBaseMultiplier).
 * 
 * @author Ben Lister
 */
public class PointsHelper {
    // Singleton instance
    private static PointsHelper pointsHelper = null;
    
    private GPSTracker gpsTracker = null;
    private Timer timer = new Timer();
    
    // Constants to calculate points/level/multipliers. May be overriden by values from database in constructor.
    private final long TIME_SINCE_LAST_ACTIVITY = 0;
    private final int BASE_POINTS_PER_METRE = 5;
    private final int BASE_MULTIPLIER_LEVELS = 4;
    private final int BASE_MULTIPLIER_PERCENT = 25;
    private final long BASE_MULTIPLIER_TIME_THRESH = 8000;  // ms
    private final long ACTIVITY_COMPLETE_MULTIPLIER_PERCENT = 30;
    private final long ACTIVITY_COMPLETE_MULTIPLIER_LEVELS = 7;
    private final long CHALLENGE_COMPLETE_MULTIPLIER_PERCENT = 100;
    
    private static final String UNITY_TARGET = "Preset Track GUI";
    
    private float baseSpeed = 0.0f;
    private AtomicLong currentActivityPoints = new AtomicLong();  // stored locally to reduce DB access
    private long openingPointsBalance = 0;  // stored locally to reduce DB access
    
    /**
     * Private singleton constructor. Use getInstance()
     * @param c android application context
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
    public static synchronized PointsHelper getInstance(Context c) {
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
     * @return points
     */
    public long getOpeningPointsBalance() {
        return openingPointsBalance;
    }
    
    /**
     * Points earned during the current activity
     * @return points
     */
    public long getCurrentActivityPoints() {
        return currentActivityPoints.get() + extrapolatePoints();
    }
    
    /**
     * Flexible helper method for awarding arbitrary in-game points for e.g. custom achievements
     * @param type: base points, bonus points etc
     * @param calc: string describing how the points were calculated (for human sense check)
     * @param source_id: which bit of code generated the points?
     * @param points_delta: the points to add/deduct from the user's balance
     */
    public void awardPoints(String type, String calc, String source_id, int points_delta) {
        Transaction t = new Transaction(type, calc, source_id, points_delta);
        t.save();
        long p = currentActivityPoints.get();
        while (!currentActivityPoints.compareAndSet(p, p + points_delta)) {};
    }
    
    /**
     * Extrapolate yet to be awarded points based on elapsed distance.
     * @return points
     */
    private int extrapolatePoints() {
        if (gpsTracker == null) {
            return 0;
        } else {
            return (int)((gpsTracker.getElapsedDistance() - lastCumulativeDistance)
                            * lastBaseMultiplierPercent * BASE_POINTS_PER_METRE) / 100; //integer division floors to nearest whole point below
        } 
    }
    
    // Variables modified by TimerTask and shared with parent class
    private long lastTimestamp = 0;
    private double lastCumulativeDistance = 0.0;
    private int lastBaseMultiplierPercent = 100;
    
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
                points *= lastBaseMultiplierPercent / 100; //integer division floors to nearest whole point below
                calcString += " * " + lastBaseMultiplierPercent + "% base multiplier";
                
                // update multiplier for next time
                long awardTime = System.currentTimeMillis() - lastTimestamp;
                float awardSpeed = (float)(awardDistance*1000.0/awardTime);
                if (awardSpeed > baseSpeed) {
                    // bump up the multiplier (incremented by BASE_MULTIPLIER_PERCENT each time round this loop for BASE_MULTIPLIER_LEVELS)
                    if (lastBaseMultiplierPercent <= (1+BASE_MULTIPLIER_LEVELS*BASE_MULTIPLIER_PERCENT)) {
                        lastBaseMultiplierPercent += BASE_MULTIPLIER_PERCENT;
                        UnityPlayer.UnitySendMessage(UNITY_TARGET, "NewBaseMultiplier", String.valueOf(lastBaseMultiplierPercent/100.0f));
                        Log.i("PointsHelper","New base multiplier: " + lastBaseMultiplierPercent + "%");
                    }
                } else if (lastBaseMultiplierPercent != 100) {
                    // reset multiplier to 1
                    lastBaseMultiplierPercent = 100;
                    UnityPlayer.UnitySendMessage(UNITY_TARGET, "NewBaseMultiplier", String.valueOf(lastBaseMultiplierPercent/100.0f));
                    Log.i("PointsHelper","New base multiplier: " + lastBaseMultiplierPercent + "%");
                }
            }
            
            awardPoints("BASE POINTS", calcString, "PointsHelper.java", points);
            lastTimestamp = System.currentTimeMillis();
            lastCumulativeDistance = currentDistance;
        }
    };
}
