package com.raceyourself.platform.models;

import java.nio.ByteBuffer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;

/**
 * Gamification transaction.
 * Log of data resulting in a points change.
 * 
 * Consistency model: Client can add.
 *                    Server upserts latest transaction.
 * 
 * @author Ben Lister
 */
public class Transaction extends Entity {

    // Globally unique compound key (transaction, device)
    public int transaction_id;
    public int device_id;
    
	@JsonIgnore
	public long id;  // for local database only
	
	public long ts; // timestamp of transaction (millisecs since 1970 in device time) TODO: convert to server time on sync
	public String transaction_type;  // e.g. base points, bonus, game purchase
	public String transaction_calc;  // e.g. 1 point * 5 seconds * 1.25 multiplier
	public String source_id;   // game/class that generated the transaction
	public long points_delta;  // points awarded/deducted
	public long points_balance; // sum of points_deltas with timestamps <= current record
	public int gems_delta;  // gems awarded/deducted
	public int gems_balance;  // sum of gems_deltas with timestamps <= current record
	public float metabolism_delta;  // metabolism awarded (often small changes, accuracy doesn't matter much)
	public float metabolism_balance;  // metabolism_balance: decays over time, and can be awarded for exercising
	public long cash_delta;     // cash added/removed from GlassFit account
	public String currency;    // currency (e.g. GBP/USD) that the transaction was in
	
	@JsonIgnore
    public boolean dirty = false;
	
	public Transaction() {}  // public constructor with no args required by ORMdroid
	
	/**
	 * Create a transaction with the fields specified
	 * @param type base points, in-game bonus, game purchase etc
	 * @param calc why the points were awarded
	 * @param source_id the game_id or function that caused the transaction to come about
	 * @param points_delta value in points of the transaction. Positive increases the user's balance.
	 * @param gems_delta value in gems of the transaction. Positive increases the user's balance.
	 * @param metabolism_delta metabolism change of the transaction. Positive increases the user's balance. Note metabolism decays over time.
	 */
    public Transaction(String type, String calc, String source_id, long points_delta, int gems_delta, float metabolism_delta) {
        this.device_id = Device.self().getId();
        this.transaction_id = Sequence.getNext("transaction_id");
        this.transaction_type = type;
        this.transaction_calc = calc;
        this.source_id = source_id;
        this.points_delta = points_delta;
        this.gems_delta = gems_delta;
        this.metabolism_delta = metabolism_delta;
        this.cash_delta = 0;
        this.currency = null;
        this.ts = System.currentTimeMillis();
        this.dirty = true;
    }
	
    /**
     * Get the user's most recent transaction (and therefore balance at that time)
     * @return the most recent transaction
     */
    public static synchronized Transaction getLastTransaction() {
        return Entity.query(Transaction.class).orderBy("ts desc").limit(1).execute();
    }
    
    /**
     * Generates globally-unique IDs suitable for syncing with the server.
     */
    private void generateId() {
        if (id == 0) {
            ByteBuffer encodedId = ByteBuffer.allocate(8);
            encodedId.putInt(device_id);
            encodedId.putInt(transaction_id);
            encodedId.flip();
            this.id = encodedId.getLong();
        }
    }
	
    /**
     * Use instead of the standard save() if there is a risk that this transaction will take the user's funds below zero.
     * @return ID of record if a new record is created, -1 if update, -2 if something went really wrong
     * @throws InsufficientFundsException if the user doesn't have enough points/gems/metabolism to complete the transaction
     */
    public int saveIfSufficientFunds() throws InsufficientFundsException {
    	int returnValue = -2;
        
        try {
            ORMDroidApplication.getInstance().beginTransaction();
            
            Transaction t = getLastTransaction();
        	if (this.points_delta < 0) {
        		if (t == null || t.points_balance < -this.points_delta) {
        		    throw new InsufficientFundsException(t == null ? 0 : t.points_balance, 0, -this.points_delta, 0);
        		}
        	}
            if (this.gems_delta < 0) {
                if (t == null || t.gems_balance < -this.gems_delta) {
                    throw new InsufficientFundsException(t == null ? 0 : t.gems_balance, 0, -this.gems_delta, 0);
                }
            }
            if (this.metabolism_delta < 0) {
                if (t == null || t.points_balance < -this.points_delta) {
                    throw new InsufficientFundsException(t == null ? 0 : t.points_balance, 0, -this.points_delta, 0);
                }
            }
        	this.points_balance = (t == null) ? 0 : t.points_balance + this.points_delta;
        	this.gems_balance = (t == null) ? 0 : t.gems_balance + this.gems_delta;
        	this.metabolism_balance = (t == null) ? 0 : t.metabolism_balance + this.metabolism_delta;
        	generateId();
        	returnValue = super.save();
        	ORMDroidApplication.getInstance().setTransactionSuccessful();
        } finally {
            ORMDroidApplication.getInstance().endTransaction();
        }
        return returnValue;
    }
    
    /**
     * Overrides the standard Entity.save() method with a transaction-based
     * implementation, so consistency of points_balance is maintained even with
     * multiple threads. TODO: might be a risk of deadlock in the database, need
     * to look into SQLite's locking model.
     */
    @Override
    public int save() {
        int returnValue = -2;
        
        try {
            ORMDroidApplication.getInstance().beginTransaction();
            
            Transaction lastTransaction = getLastTransaction();
            this.points_balance = lastTransaction == null ? 0
                    : lastTransaction.points_balance + this.points_delta;
            this.gems_balance = (lastTransaction == null) ? 0
                    : lastTransaction.gems_balance + this.gems_delta;
            this.metabolism_balance = (lastTransaction == null) ? 0
                    : lastTransaction.metabolism_balance + this.metabolism_delta;
            generateId();
            returnValue = super.save();
            ORMDroidApplication.getInstance().setTransactionSuccessful();
        } finally {
            ORMDroidApplication.getInstance().endTransaction();
        }
        return returnValue;
    }
    
    /**
     * Store verified/replacement values.
     */
    public int store() {
        this.dirty = false;
        generateId();
        return super.save();
    }
	
	/**
	 * Remove the dirty transactions. The server will replace them.
	 */
    public void flush() {
        if (dirty) {
            dirty = false;
            delete();
        }
    }
    
    /**
     * Custom exception type as none of the standard Java ones seemed
     * appropriate Holds the available and required funds so the catching code
     * can work out the deficit. Name uses 'Funds' rather than the more game-y
     * word "Resources" so this exception doesn't get confused with e.g. an out
     * of memory problem
    
     * 
     * @author Ben Lister
     * 
     */
    public class InsufficientFundsException extends Exception {

        private static final long serialVersionUID = -8013940015226477449L;
        private long availablePoints;
        private int availableGems;
        private long requiredPoints;
        private int requiredGems;

        public InsufficientFundsException(long availablePoints,
                int availableGems, long requiredPoints, int requiredGems) {
            super("Insufficient funds available for transaction.");
            this.availablePoints = availablePoints;
            this.availableGems = availableGems;
            this.requiredPoints = requiredPoints;
            this.requiredGems = requiredGems;
        }

        public long getAvailablePoints() {
            return availablePoints;
        }

        public int getAvailableGems() {
            return availableGems;
        }

        public long getRequiredPoints() {
            return requiredPoints;
        }

        public int getRequiredGems() {
            return requiredGems;
        }

    } // end of InsufficientFundsException class

}
