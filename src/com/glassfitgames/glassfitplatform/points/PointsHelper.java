package com.glassfitgames.glassfitplatform.points;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import android.content.Context;
import android.util.Log;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.glassfitgames.glassfitplatform.models.Transaction;
import com.glassfitgames.glassfitplatform.models.Transaction.InsufficientFundsException;
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
    
    private long openingPointsBalance = 0;  // stored locally to reduce DB access
    private AtomicLong currentActivityPoints = new AtomicLong();  // stored locally to reduce DB access
    private AtomicInteger currentGemBalance = new AtomicInteger();  // stored locally to reduce DB access
    private AtomicReference<Float> currentMetabolism = new AtomicReference<Float>();  // stored locally to reduce DB access
    private long currentMetabolismTimestamp;
    
    
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
            currentActivityPoints.set(0);
            currentGemBalance.set(lastTransaction.gems_balance);
            currentMetabolism.set(lastTransaction.metabolism_balance);
            currentMetabolismTimestamp = lastTransaction.ts;
        } else {
            openingPointsBalance = 0;
            currentActivityPoints.set(0);
            currentGemBalance.set(0);
            currentMetabolism.set(0.0f);
            currentMetabolismTimestamp = 0;
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
    
    public void reset() {
        // retrieve opening points balance & store locally to reduce DB access
        Transaction lastTransaction = Transaction.getLastTransaction();
        if (lastTransaction != null) {
            openingPointsBalance = lastTransaction.points_balance;
            currentActivityPoints.set(0);
            currentGemBalance.set(lastTransaction.gems_balance);
            currentMetabolism.set(lastTransaction.metabolism_balance);
            currentMetabolismTimestamp = lastTransaction.ts;
        } else {
            openingPointsBalance = 0;
            currentActivityPoints.set(0);
            currentGemBalance.set(0);
            currentMetabolism.set(0.0f);
            currentMetabolismTimestamp = 0;
        }
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
        long alreadyAwarded = currentActivityPoints.get();
        long pending = extrapolatePoints();
        return alreadyAwarded + pending;
    }
    
    /**
     * User's current gem balance, returned from local variable to reduce DB access
     * @return gems
     */
    public int getCurrentGemBalance() {
        return currentGemBalance.get();
    }
    
    /**
     * User's current metabolism
     * @return metabolism
     */
    public float getCurrentMetabolism() {
        return 100 + decayMetabolism(currentMetabolism.get(), currentMetabolismTimestamp);
    }
    
    /**
     * Flexible helper method for awarding arbitrary in-game points for e.g. custom achievements
     * @param type: base points, bonus points etc
     * @param calc: string describing how the points were calculated (for human sense check)
     * @param source_id: which bit of code generated the points?
     * @param points_delta: the points to add/deduct from the user's balance
     */
    public void awardPoints(String type, String calc, String source_id, long points_delta) throws InsufficientFundsException {
        Transaction t = new Transaction(type, calc, source_id, points_delta, 0, 0);
        t.saveIfSufficientFunds();
        currentActivityPoints.getAndAdd(points_delta);
        Log.d("glassfitplatform.points.PointsHelper","Awarded " + type + " of "+ points_delta + " points for " + calc + " in " + source_id);
    }
    
    /**
     * Flexible helper method for awarding in-game gems for e.g. finishing a race
     * @param type: race finish, challenge win etc
     * @param calc: string describing how the gems were calculated (for human sense check)
     * @param source_id: which bit of code generated the gems?
     * @param gems_delta: the gems to add/deduct from the user's balance
     */
    public void awardGems(String type, String calc, String sourceId, int gemsDelta) throws InsufficientFundsException {
        Transaction t = new Transaction(type, calc, sourceId, 0, gemsDelta, 0);
        t.saveIfSufficientFunds();
        currentGemBalance.getAndAdd(gemsDelta);
        Log.d("glassfitplatform.points.PointsHelper","Awarded " + type + " of "+ gemsDelta + " points for " + calc + " in " + sourceId);
    }
    
    /**
     * Flexible helper method for awarding metabolism for e.g. time spent exercising
     * Synchronized as decay calc and transaction save must always happen as a single transaction.
     * @param type: why are we awarding metabolism?
     * @param calc: string describing how the metabolism was calculated (for human sense check)
     * @param source_id: which bit of code generated the metabolism?
     * @param gems_delta: the metabolism to add/deduct from the user's balance
     */
    public synchronized void awardMetabolism(String type, String calc, String sourceId, float metabolismDelta) throws InsufficientFundsException {
        Transaction lastT = Transaction.getLastTransaction();
        // reduce delta by decay since last transaction. Decay calc can never let it go negative.
        metabolismDelta -= (lastT == null ? 0 : (lastT.metabolism_balance - decayMetabolism(lastT.metabolism_balance, lastT.ts)));
        Transaction newT = new Transaction(type, calc, sourceId, 0, 0, metabolismDelta);
        newT.saveIfSufficientFunds();
        Float f = currentMetabolism.get();
        currentMetabolism.getAndSet((f == null ? 0 : f) + metabolismDelta);
        currentMetabolismTimestamp = System.currentTimeMillis();
        Log.d("glassfitplatform.points.PointsHelper","Awarded " + type + " of "+ metabolismDelta + " metabolism for " + calc + " in " + sourceId);
    }
    
    /**
     * Extrapolate yet to be awarded points based on elapsed distance.
     * @return points
     */
    private int extrapolatePoints() {
        
        if (gpsTracker == null) {
            return 0;
        }
        
        if (gpsTracker.getElapsedDistance() < lastCumulativeDistance) {
            // user has probably reset/restarted the route, need to re-init pointsHelper.
            // TODO: work out how to award/save the points earned between last task.run and now. (currently they are discarded)
            lastCumulativeDistance = 0.0;
            lastTimestamp = System.currentTimeMillis();
            lastBaseMultiplierPercent = 100;
        }
    
        int points = (int)((gpsTracker.getElapsedDistance() - lastCumulativeDistance)
                        * lastBaseMultiplierPercent * BASE_POINTS_PER_METRE) / 100; //integer division floors to nearest whole point below
        return points;
    }
    
    private float decayMetabolism(float metabolism, long timeInMillis) {
        if (timeInMillis == 0) {
            return metabolism;
        } else {
            return (float)(metabolism*Math.exp((timeInMillis-System.currentTimeMillis())*0.00000001));
        }
    }
    
    // Variables modified by TimerTask and shared with parent class
    private long lastTimestamp = 0;
    private double lastCumulativeDistance = 0.0;
    private int lastBaseMultiplierPercent = 100;
    
    private TimerTask task = new TimerTask() {
        public void run() {
            if (gpsTracker == null || !gpsTracker.isTracking()) return;
            
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
                        try {
                            UnityPlayer.UnitySendMessage(UNITY_TARGET, "NewBaseMultiplier", String.valueOf(lastBaseMultiplierPercent/100.0f));
                        } catch (UnsatisfiedLinkError e) {
                            Log.i("GPSTracker","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                            Log.i("GPSTracker",e.getMessage());
                        }
                        Log.i("PointsHelper","New base multiplier: " + lastBaseMultiplierPercent + "%");
                    }
                } else if (lastBaseMultiplierPercent != 100) {
                    // reset multiplier to 1
                    lastBaseMultiplierPercent = 100;
                    try {
                        UnityPlayer.UnitySendMessage(UNITY_TARGET, "NewBaseMultiplier", String.valueOf(lastBaseMultiplierPercent/100.0f));
                    } catch (UnsatisfiedLinkError e) {
                        Log.i("GPSTracker","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                        Log.i("GPSTracker",e.getMessage());
                    }
                    Log.i("PointsHelper","New base multiplier: " + lastBaseMultiplierPercent + "%");
                }
            }
            try {
                awardPoints("BASE POINTS", calcString, "PointsHelper.java", points);
            } catch (InsufficientFundsException e) {
                // should never get here as we're trying to award positive points
                e.printStackTrace();
            }
            lastTimestamp = System.currentTimeMillis();
            lastCumulativeDistance = currentDistance;
            
            // award metabolism
            float metabolismDelta = (float)Math.exp(-(getCurrentMetabolism()-100)/20) // the more you have, the harder it is to earn
                    /(60000*BASE_MULTIPLIER_TIME_THRESH); // scale per-minute reward to trigger time of this loop
            try {
                awardMetabolism("BASE METABOLISM", calcString, "PointsHelper.java", metabolismDelta);
            } catch (InsufficientFundsException e) {
                Log.e("PointsHelper", "Failed to award base metabolism - this transaction would take it negative");
            }
        }
    };
}
