package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glassfitgames.glassfitplatform.R;
import com.glassfitgames.glassfitplatform.models.Transaction.InsufficientFundsException;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;

/**
 * Game. Holds the metadata (name, description, cost etc) for a game.
 * 
 * Consistency model: Client populates on first load. No adds/deletes after
 * that. Server can upsert/delete using game_id as unique identifier.
 */
public class Game extends Entity {

    // Fields
    @Column(unique = true, primaryKey = true)
    public String game_id; // Unique identifier of the game (e.g. "Zombies 2")
    public String name; // Pretty name to display to users
    public String iconName;
    public String description; // Pretty description to display to users
    public String activity; // run, cycle, gym etc
    public String state; // "Locked" or "Unlocked"
    public int tier; // which tier the game sits in (1,2,3,4 etc)
    public long price_in_points;
    public int price_in_gems;
    public String type;
    public int column;
    public int row;
    
    // Metadata
    @JsonIgnore
    public boolean dirty = false;

    public Game() {
    }

    /**
     * Constructor for creating a game with the fields passed.
     * @param gameId unique string identifier for the game
     * @param name display name to be shown to the user. 'Title' might be better.
     * @param activity run, cycle, gym etc
     * @param description description of the game to be shown to the users
     * @param state locked/unlocked
     * @param tier 1,2,3 etc, probably needed for unlocking in the future
     * @param priceInPoints points required to unlock this game
     * @param priceInGems gems required to unlock this game
     */
    public Game(String gameId, String name, String iconName, String activity, String description, String state, int tier, long priceInPoints, int priceInGems, String type, int column, int row) {
        this.game_id = gameId;
        this.name = name;
        this.iconName = iconName;
        this.activity = activity;
        this.description = description;
        this.state = state;
        this.tier = tier;
        this.price_in_points = priceInPoints;
        this.price_in_gems = priceInGems;
        this.type = type;
        this.column = column;
        this.row = row;
    }

    /**
     * Loads games from the master_game_list.csv file in the /res/raw/
     * directory. Note the assets directory cannot be used in an Android Library
     * project.
     * 
     * @param c current application context
     * @throws IOException when reading the CVS file fails
     */
    public static void loadDefaultGames(Context c) throws IOException {
        // Delete existing games (and states!) from the database
        List<Game> games = query(Game.class).executeMulti();
        for (Game g : games)
            g.delete();

        // Read the master game list from CSV file:
        InputStream in = c.getResources().openRawResource(
                R.raw.beta_game_list);
        BufferedReader b = new BufferedReader(new InputStreamReader(in));
        b.readLine(); // read (and discard) headers
        String line = null;
        while ((line = b.readLine()) != null) {
            try {
                String[] fields = line.split(",");
                new Game(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5],
                        Integer.valueOf(fields[6]), Long.valueOf(fields[7]),
                        Integer.valueOf(fields[8]), fields[9], Integer.valueOf(fields[10]),
                        Integer.valueOf(fields[11])).save();
                Log.d("glassfitplatform.models.Game", "Loaded " + fields[1] + " from CSV.");
            } catch (NumberFormatException e) {
                Log.w("glassfitplatform.models.Game",
                        "Failed to load game, invalid number format in CSV: " + line);
            } catch (IndexOutOfBoundsException e) {
                Log.w("glassfitplatform.models.Game",
                        "Failed to load game, not enough fields present in CSV: " + line);
            }
        }
    }
    
    public static void deleteGamesDatabase(Context c) {
    	List<Game> games = query(Game.class).executeMulti();
    	for(Game g : games)
    		g.delete();
    }

    public static List<Game> getGames(Context c) {

        Log.d("Game.java", "Querying games from database...");
        List<Game> games = Entity.query(Game.class).executeMulti();

        // if no games exist in the database, try a server sync
        if (games.size() == 0) {
            // TODO: call sync
       
        // if we still have no games, populate the database with a list of
        // defaults
            try {
                Log.d("Game.java", "Loading default games from CSV...");
                loadDefaultGames(c);
                Log.d("Game.java", "Games successfully loaded from CSV.");
            } catch (IOException e) {
                Log.d("Game.java",
                        "Couldn't read games from CSV, falling back to a small number of hard-coded games.");
                Log.d("Game.java","Couldn't read games from CSV, falling back to a small number of hard-coded games.");
                new Game("Race Yourself (run)","Race Yourself", "activity_run", "run", "Run against an avatar that follows your previous track","unlocked",1,0,0, "Race", 0, 0).save();
                new Game("Challenge Mode (run)","Challenge a friend", "activity_challenge", "run","Run against your friends' avatars","locked",1,1000,0, "Challenge", 0, 1).save();
                new Game("Switch to cycle mode (run)","Cycle Mode", "activity_bike", "run","Switch to cycle mode","locked",1,1000,0, "Race", 1, 0).save();
                new Game("Zombies 1","Zombie pursuit", "activity_zombie", "run","Get chased by zombies","locked",2,50000,0, "Pursuit", 0, -1).save();
                new Game("Boulder 1","Boulder Dash", "activity_boulder", "run","Run against an avatar that follows your previous track","locked",1,10000,0, "Pursuit", -1, 0).save();
                new Game("Dinosaur 1","Dinosaur Safari", "activity_dinosaurs","run","Run against an avatar that follows your previous track","locked",3,100000,0, "Pursuit", -1, -1).save();
                new Game("Eagle 1","Escape the Eagle","activity_eagle", "run","Run against an avatar that follows your previous track","locked",2,70000,0, "Pursuit", -1, 1).save();
                new Game("Train 1","The Train Game", "activity_train", "run","Run against an avatar that follows your previous track","locked",2,20000,0, "Pursuit", 1, 1).save();
                new Game("Mo Farah","activity_farah", "activity_farah", "run","Run against Mo Farah! See how you compare to his 2012 Olympic time!","unlocked",2,70000,0, "Celebrity", 2, 0).save();
                new Game("Paula Radcliffe","activity_paula_radcliffe","activity_paula_radcliffe","run","Run a marathon with Paula Radcliffe! Try and beat her time at the 2007 NYC Marathon!","unlocked",2,20000,0, "Celebrity", 2, 1).save();
                new Game("Chris Hoy", "activity_chris_hoy", "activity_chris_hoy","run", "Cycle with Chris Hoy, in his almost record breaking 1km cycle in 2007", "unlocked", 2, 10000, 0, "Celebrity", 2, -1).save();
                new Game("Bradley Wiggins", "activity_bradley_wiggins","activity_bradley_wiggins", "cycle", "Participate in a 4km pursuit race with Bradley Wiggins on his 2008 Olympics gold medal time", "unlocked", 2, 10000, 0, "Celebrity", 1, -1).save();
                new Game("Fire", "activity_fire", "activity_fire", "run", "Know what's good on a barbeque? Burgers. Know what isn't? You. So run before you get burned.", "unlocked", 2, 10000, 0, "Pursuit", 1, 2).save();
                new Game("Rearview", "activity_rearview","activity_rearview", "run", "Use this to activate rearview mode so that you can see what's behind you.", "unlocked", 2, 5000, 0, "Mode", 0, 2).save();
                new Game("Settings", "settings","settings", "run", "Settings for Indoor mode", "unlocked", 2, 0, 0, "Mode", -1, 2).save();
                
                Log.d("Game.java","Hard-coded games successfully loaded.");
            }
        }
        List<Game> allGames = Entity.query(Game.class).executeMulti();
        Log.d("Game.java", "getGames found " + allGames.size() + " games.");
        return allGames;
    }
    
    public static List<Game> getTempGames(Context c) {
    	//List<Game> allGames = new List<Game>();
        new Game("Race Yourself (run)","Race Yourself", "activity_run", "run", "Run against an avatar that follows your previous track","unlocked",1,0,0, "Race", 0, 0).save();
        new Game("Challenge Mode (run)","Challenge a friend", "activity_challenge", "run","Run against your friends' avatars","locked",1,1000,0, "Challenge", 0, 1).save();
        new Game("Switch to cycle mode (run)","Cycle Mode", "activity_bike", "run","Switch to cycle mode","locked",1,1000,0, "Race", 1, 0).save();
        new Game("Zombies 1","Zombie pursuit", "activity_zombie", "run","Get chased by zombies","locked",2,50000,0, "Pursuit", 0, -1).save();
        new Game("Boulder 1","Boulder Dash", "activity_boulder", "run","Run against an avatar that follows your previous track","locked",1,10000,0, "Pursuit", -1, 0).save();
        new Game("Dinosaur 1","Dinosaur Safari", "activity_dinosaurs","run","Run against an avatar that follows your previous track","locked",3,100000,0, "Pursuit", -1, -1).save();
        new Game("Eagle 1","Escape the Eagle","activity_eagle", "run","Run against an avatar that follows your previous track","locked",2,70000,0, "Pursuit", -1, 1).save();
        new Game("Train 1","The Train Game", "activity_train", "run","Run against an avatar that follows your previous track","locked",2,20000,0, "Pursuit", 1, 1).save();
        new Game("Mo Farah","activity_farah", "activity_farah", "run","Run against Mo Farah! See how you compare to his 2012 Olympic time!","unlocked",2,70000,0, "Celebrity", 2, 0).save();
        new Game("Paula Radcliffe","activity_paula_radcliffe","activity_paula_radcliffe","run","Run a marathon with Paula Radcliffe! Try and beat her time at the 2007 NYC Marathon!","unlocked",2,20000,0, "Celebrity", 2, 1).save();
        new Game("Chris Hoy", "activity_chris_hoy", "activity_chris_hoy","run", "Cycle with Chris Hoy, in his almost record breaking 1km cycle in 2007", "unlocked", 2, 10000, 0, "Celebrity", 2, -1).save();
        new Game("Bradley Wiggins", "activity_bradley_wiggins","activity_bradley_wiggins", "cycle", "Participate in a 4km pursuit race with Bradley Wiggins on his 2008 Olympics gold medal time", "unlocked", 2, 10000, 0, "Celebrity", 1, -1).save();
        new Game("Fire", "activity_fire", "activity_fire", "run", "Know what's good on a barbeque? Burgers. Know what isn't? You. So run before you get burned.", "unlocked", 2, 10000, 0, "Pursuit", 1, 2).save();
        new Game("Rearview", "activity_rearview","activity_rearview", "run", "Use this to activate rearview mode so that you can see what's behind you.", "unlocked", 2, 5000, 0, "Mode", 0, 2).save();
        new Game("Settings", "settings","settings", "run", "Settings for Indoor mode", "unlocked", 2, 0, 0, "Mode", -1, 2).save();
    		   Log.d("Game.java","Hard-coded games successfully loaded.");
        
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
	    
            // set up transaction to take cost in points off user's balance
	    Transaction t = new Transaction("Game unlock", this.game_id, 
	                    "Cost: " + this.price_in_points + " points",
	                    -this.price_in_points, 0, 0);
	    try {
    	        return unlockWith(t);
	    } catch (InsufficientFundsException e) {
	        // Ignore and attempt with gems
	    }
	    
            // set up transaction to take cost in gems off user's balance
            t = new Transaction("Game unlock", this.game_id, 
                            "Cost: " + this.price_in_gems + " gems",
                            0, -this.price_in_gems, 0);
            try {
                return unlockWith(t);
            } catch (InsufficientFundsException e) {
                // Re-throw
                throw e;
            }
	}
		
	private Game unlockWith(Transaction t) throws InsufficientFundsException {
        
	    Game g = this;
		
		try {
		    ORMDroidApplication.getInstance().beginTransaction();
		    // get the latest version of this game from the database
		    g = Entity.query(Game.class).where(eql("game_id", this.game_id)).limit(1).execute();
		    
		    if (!g.state.equals("unlocked")) {
		        // unlock the game and commit the transaction
		        g.state = "unlocked";
		        t.saveIfSufficientFunds();
		        g.save();
		    }
			ORMDroidApplication.getInstance().setTransactionSuccessful();
        } finally {
            ORMDroidApplication.getInstance().endTransaction();
		}
		
		return g;	
	}
    
	/**
	 * Unlock all games in the same tier as this game. Only possible if this game is the tier_master.
	 */
    public void unlockTier() {
        // TODO: spec the tier system
    }

    /**
     * Saves the state to the database and flags as dirty for pick-up by
     * server-sync.
     * 
     * @return row number of the new record, -1 if existing record was updated
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
    
    public String getIconName() {
    	return iconName;
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
    
    public String getType() {
    	return type;
    }
    
    public int getColumn() {
    	return column;
    }
    
    public int getRow() {
    	return row;
    }

    public boolean isDirty() {
        return dirty;
    }

}
