package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
    
	// Fields
    @Column(unique = true, primaryKey=true)
    public String game_id; // Unique identifier of the game (e.g. "Zombies 2")
    public String name; // Pretty name to display to users
    public String description; // Pretty description to display to users
    public String activity; // run, cycle, gym etc
    public String state; // "Locked" or "Unlocked"
    public int tier; // which tier the game sits in (1,2,3,4 etc)
    public long price_in_points;
    public long price_in_gems;
    
    // Metadata
    @JsonIgnore
    public boolean dirty = false;
    
    public Game() {}

    public Game(String gameId, String name, String activity, String description, String state, int tier, long priceInPoints, long priceInGems) {
        this.game_id = gameId;
        this.name = name;
        this.activity = activity;
        this.description = description;
        this.state = state;
        this.tier = tier;
        this.price_in_points = priceInPoints;
        this.price_in_gems = priceInGems;
    }
    
    public static void loadDefaultGames(Context c) {
        // Delete existing games (and states!) from the database
        List<Game> games = query(Game.class).executeMulti();
        for (Game g : games) g.delete();
        
        // TODO: Read the master game list from CSV file:
        //Reader in = c.getResources().openRawResource(R.raw.MasterGameList.csv);
        //BufferedReader b = new BufferedReader(in);
        //b.readLine(); // read (and discard) headers
        //while (b.)
        
        
    }
    
    
    public static List<Game> getGames() {
        
        Log.d("Game.java", "Querying games from database...");
        List<Game> games = Entity.query(Game.class).executeMulti();
        
        // if no games exist in the database, try a server sync
        if (games.size() == 0) {
            // TODO: call sync
        }
        // if we still have no games, populate the database with a list of defaults
        if (games.size() < 6) {
            Log.d("Game.java","Inserting default ganes into database...");
            new Game("Race Yourself (run)","Race Yourself","run", "Run against an avatar that follows your previous track","unlocked",1,0,0).save();
            new Game("Challenge Mode (run)","Challenge a friend","run","Run against your friends' avatars","locked",1,1000,0).save();
            new Game("Switch to cycle mode (run)","Cycle Mode","run","Switch to cycle mode","locked",1,1000,0).save();
            new Game("Zombies 1","Zombie pursuit","run","Get chased by zombies","locked",2,50000,0).save();
            new Game("Boulder 1","Boulder Dash","run","Run against an avatar that follows your previous track","locked",1,10000,0).save();
            new Game("Dinosaur 1","Dinosaur Safari","run","Run against an avatar that follows your previous track","locked",3,100000,0).save();
            new Game("Eagle 1","Escape the Eagle","run","Run against an avatar that follows your previous track","locked",2,70000,0).save();
            new Game("Train 1","The Train Game","run","Run against an avatar that follows your previous track","locked",2,20000,0).save();
            Log.d("Game.java","...success!");
        }
        
        List<Game> allGames = Entity.query(Game.class).executeMulti();
        Log.d("Game.java", "getGames found " + allGames.size() + " games.");
        return allGames;
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

    public String getGameId() {
        return game_id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getActivity() {
        return activity;
    }

    public String getState() {
        return state;
    }

    public int getTier() {
        return tier;
    }

    public long getPriceInPoints() {
        return price_in_points;
    }

    public long getPriceInGems() {
        return price_in_gems;
    }

    public boolean isDirty() {
        return dirty;
    }
    
}
