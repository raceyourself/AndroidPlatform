package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.eql;

import java.util.List;

import android.database.sqlite.SQLiteDatabase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glassfitgames.glassfitplatform.models.Transaction.InsufficientFundsException;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;

/**
 * Game.
 * Holds the metadata (name, description, cost etc) for a game.
 *
 * Consistency model: Client populates on first load. No adds/deletes after that.
 *                    Server can upsert/delete using game_id as unique identifier.
 */
public class Game extends Entity {
	
    @JsonIgnore
	public long id = 0;  // SQLite internal, not exposed
    
	// Fields
    @Column(unique = true)
    public String game_id; // Unique identifier of the game (e.g. "Zombies 2")
    public String name; // Pretty name to display to users
    public String description; // Pretty description to display to users
    public String state; // "Locked" or "Unlocked"
    public int tier; // which tier the game sits in (1,2,3,4 etc)
    public int price_in_points;
    public int price_in_gems;
    
    // Metadata
    @JsonIgnore
    public boolean dirty = false;

    public Game() {
    }    
    
    
    public static List<Game> getGames() {
        return query(Game.class).executeMulti();
    }
    
	/**
	 * Unlocking games is handled java-side so we can handle the points/gems in
	 * a single database transaction.
	 * @return Updated Game entity to replace this one.
	 * @throws InsufficientFundsException if the user does not have enough points/gems to unlock the game
	 */
	public Game unlock() throws InsufficientFundsException {
	    
	    Game g = this;
	    
		// set up transaction to take cost off user's balance
	    Transaction t = new Transaction("Game unlock", this.game_id, "Cost: "
				+ this.price_in_points + " points", -this.price_in_points);
		
	    // apply transaction and unlock game in same database transaction to keep things thread-safe
	    SQLiteDatabase db = ORMDroidApplication.getDefaultDatabase();
		db.beginTransaction();
		try {
		    // get the latest version of this game from the database
		    g = Entity.query(Game.class).where(eql(this.game_id, "game_id")).limit(1).execute();
		    
		    // no action if already unlocked, just return latest game state
		    if (g.state.equals("Unlocked")) {
		        db.endTransaction();
		        return g;
		    }
		    
		    // unlock the game and commit the transaction
		    g.state = "unlocked";
			t.saveIfSufficientFunds();
			g.save();
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		return g;	
	}
    
	/**
	 * Unlock all games in the same tier as this game. Only possible if this game is the tier_master.
	 */
    public void unlockTier() {
    	//TODO: spec the tier system
    }
    
    /** 
     * Saves the state to the database and flags as dirty for pick-up by server-sync.
     * @return
     */
    @Override
    public int save() {
    	this.dirty = true;
    	return super.save();
    }
	
    /**
     * When records come back from the server, clear the dirty flag.
     */
	public void flush() {
		if (dirty) {
			dirty = false;
			super.save();
		}
	}
    
}
