/*
 * Copyright 2012 Ross Bamford
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.roscopeco.ormdroid;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * <p>Provides static-initialization for the ORMDroid framework.
 * The {@link #initialize(Context)} method must be called with
 * a valid {@link Context} prior to using any framework methods
 * that reference the default database.</p>
 * 
 * <p>Note that this class extends {@link android.app.Application},
 * allowing you to set it as the Application class in your manifest
 * to have this initialization handled automatically.</p>
 */
public class ORMDroidApplication extends Application {
  private static ORMDroidApplication singleton;  
  private Context mContext;
  private String mDBName;
  private ConcurrentHashMap<Thread, SQLiteDatabase> mDatabases = new ConcurrentHashMap<Thread, SQLiteDatabase>(5);
  private Thread currentlyWritingThread = null; // lock for individual writes
  private Thread currentTransactionOwner = null; // lock held for entire transactions
  private Set<Thread> currentlyReadingThreads = new HashSet<Thread>();
  
  /**
   * <p>Intialize the ORMDroid framework. This <strong>must</strong> be called before
   * using any of the methods that use the default database.</p>
   * 
   * <p>If your application doesn't use the default database (e.g. you pass in your
   * own {@link SQLiteDatabase} handle to the {@link Query#execute(SQLiteDatabase)} and
   * {@link Entity#save(SQLiteDatabase)} methods) the you don't <i>technically</i>
   * need to call this, but it doesn't hurt.</p>
   * 
   * <p>This method may be called multiple times - subsequent calls are simply 
   * ignored.</p>
   * 
   * @param ctx A {@link Context} within the application to initialize.
   */
  public static void initialize(Context ctx) {
    if (singleton == null) {
        singleton = new ORMDroidApplication();
        singleton.mContext = ctx;
        singleton.attachBaseContext(ctx);
    }
  }

  /**
   * Obtain the singleton instance of this class.
   * 
   * @return the singleton instance.
   */
  public static ORMDroidApplication getInstance() {
    if (singleton == null) {
      Log.e("ORM", "ORMDroid is not initialized");
      throw new ORMDroidException("ORMDroid is not initialized - You must call ORMDroidApplication.initialize");
    }
    return singleton;
  }
  
  @Override
  public void onCreate() {
    if (singleton != null) {
      throw new IllegalStateException("ORMDroidApplication already initialized!");
    }
    singleton = this;
    mContext = getApplicationContext();
    //initInstance(this, getApplicationContext());
  }
  
  private void initDatabaseConfig() {
    try {
      ApplicationInfo ai = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
      mDBName = ai.metaData.get("ormdroid.database.name").toString();
    } catch (Exception e) {
      throw new ORMDroidException("ORMDroid database configuration not found; Did you set properties in your app manifest?", e);
    }
  }
  
  /**
   * Get the database name used by the framework in this application.
   * 
   * @return The database name.
   */
  public String getDatabaseName() {
    if (mDBName == null) {
      initDatabaseConfig();
    }
    return mDBName;
  }
  
    /**
     * Get a connection to the SQLite database. Returns a separate connection
     * per calling thread to avoid transactions on different threads
     * overlapping. SQLite handles multiple simultaneous connections internally
     * by blocking when necessary. (Though the SQLite docs do suggest that
     * threads are evil!). Try not to keep forking new threads for database
     * operations as this class will keep a connection open for each until you
     * close it explicitly.
     * 
     * @return connection to the database unique to the thread.
     */
  private synchronized SQLiteDatabase getDatabase() {
    SQLiteDatabase db = mDatabases.get(Thread.currentThread());
    try {
      
      // open a connection if we don't have one
      if (db == null || !db.isOpen()) {
          if (hasWriteLock()) {
              db = openOrCreateDatabase(getDatabaseName(), 0, null);
              mDatabases.remove(Thread.currentThread());
              mDatabases.put(Thread.currentThread(), db);
              Log.d("ORM", "Thread ID " + Thread.currentThread().getId() + " was granted a connection, which makes " + mDatabases.keySet().size() + " connections in total");
          } else {
              Arrays.toString(Thread.currentThread().getStackTrace());
              throw new RuntimeException("ORM: Thread ID " + Thread.currentThread().getId() + " thied to open a connection to the database without first requesting a write lock.");
          }
      }
      
      // test the connection
      db.rawQuery("Select 1", null);
      return db;
      
    } catch (SQLiteDatabaseLockedException e) {
      // the database has an internal lock that we need to clear
      // in theory this shouldn't really happen...
      Log.d("ORM", "Thread ID " + Thread.currentThread().getId() + " connection failed, database is internally locked");
      try {
        Log.d("ORM", "Thread ID " + Thread.currentThread().getId() + " closing and re-opening its database connection to (try to) clear internal lock");
        db.close();
        mDatabases.remove(Thread.currentThread());
        return getDatabase();
      } catch (Exception e1) {
        e1.printStackTrace();
        throw new RuntimeException("ORM: Thread ID " + Thread.currentThread().getId() + " giving up, clearing lock failed.");
      }
    }
  }
  
  public synchronized void getWriteLock() {
    Thread thisThread = Thread.currentThread();
    while (true) {
      try {
        if (currentTransactionOwner != null && currentTransactionOwner != thisThread) {
          // another thread is mid-transaction
          this.wait();
        } else if (currentlyWritingThread != null && currentlyWritingThread != thisThread) {
          // another thread is mid-write
          this.wait();
        } else if (currentlyReadingThreads.size() > 0 && (currentlyReadingThreads.size() != 1 || !currentlyReadingThreads.contains(Thread.currentThread()))) {
          // another thread is mid-read
          this.wait();
        } else {
          // grant the lock
          if (currentlyWritingThread != thisThread) {
            currentlyWritingThread = Thread.currentThread();
            Log.v("ORM", "Thread ID " + Thread.currentThread().getId() + " was granted a write lock");
          }
          return;
        } 
      } catch (InterruptedException e) {
        e.printStackTrace();
        throw new RuntimeException("ORM: Interrupted whilst waiting for database");
      }
    }
  }
  
  public synchronized boolean hasWriteLock() {
    if (currentlyWritingThread == Thread.currentThread()) {
      return true;
    } else {
      return false;
    }
  }

  public synchronized void releaseWriteLock() {
    // if we're mid-transaction, ignore the request to release (must end transaction first)
    if (currentTransactionOwner == Thread.currentThread()) {
        return;
    } else if (currentlyWritingThread == Thread.currentThread()) {
      Log.v("ORM", "Thread ID " + Thread.currentThread().getId() + " released the write lock");
      currentlyWritingThread = null;
      this.notifyAll();
    }
  }
  
  // currently doesn't work...
  public synchronized void getReadLock() {
      Thread thisThread = Thread.currentThread();
      while (true) {
        if (currentTransactionOwner == thisThread || currentlyWritingThread == thisThread || currentlyReadingThreads.contains(thisThread)) {
            // already have enough of a lock, register the read and return silently
            currentlyReadingThreads.add(thisThread);
            return;
        } else if (currentTransactionOwner == null && currentlyWritingThread == null) {
            // OK to grant a new read lock
            //Log.v("ORM", "Thread ID " + thisThread.getId() + " was granted a read lock");
            currentlyReadingThreads.add(thisThread);
            return;
        } else {
            try {
              this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException("ORM: Interrupted whilst waiting for database read lock");
            }
        }
      }
    }
    
    public synchronized boolean hasReadLock() {
      if (currentlyReadingThreads.contains(Thread.currentThread())) {
        return true;
      } else {
        return false;
      }
    }

    public synchronized void releaseReadLock() {
      Thread thisThread = Thread.currentThread();
      if (currentTransactionOwner == thisThread || currentlyWritingThread == thisThread) {
        // the thread has a transaction/write lock so release but no point notifying
        currentlyReadingThreads.remove(thisThread);
        return;
      } else if (currentlyReadingThreads.contains(thisThread)) {
        // release the lock and tell other threads the database is available
        currentlyReadingThreads.remove(thisThread);
        //Log.v("ORM", "Thread ID "+ thisThread.getId() + " released the read lock.");
        this.notifyAll();
      }
    }
    
  public synchronized void beginTransaction() {
      getWriteLock(); // need a write lock for the entire duration of transaction
      if (getDatabase().inTransaction()) {
          // must be nested - verify the current thread is the transaction owner
          if (currentTransactionOwner != Thread.currentThread()) {
              Arrays.toString(Thread.currentThread().getStackTrace());
              throw new RuntimeException("ORM: Thread ID " + Thread.currentThread().getId() + " tried to start a transaction when another thread's transaction was in progress.");
          }
          // start a nested transaction
          getDatabase().beginTransactionNonExclusive();
          //Log.v("ORM", "Thread ID " + Thread.currentThread().getId() + " started a nested transaction.");
      } else {
          // start a top-level transaction
          currentTransactionOwner = Thread.currentThread();
          getDatabase().beginTransactionNonExclusive();
          //Log.v("ORM", "Thread ID " + Thread.currentThread().getId() + " started a top-level transaction.");
      }
  }
  
  public synchronized void setTransactionSuccessful() {
      // check the current thread is the transaction owner
      if (currentTransactionOwner == null) {
          Arrays.toString(Thread.currentThread().getStackTrace());
          throw new RuntimeException("ORM: Thread ID " + Thread.currentThread().getId() + " tried to set a non-existent transaction as successful");
      }
      if (currentTransactionOwner != Thread.currentThread()) {
          Arrays.toString(Thread.currentThread().getStackTrace());
          throw new RuntimeException("ORM: Thread ID " + Thread.currentThread().getId() + " tried to set another thread's transaction as successful");
      }
      getDatabase().setTransactionSuccessful();
      //Log.v("ORM", "Thread ID " + Thread.currentThread().getId() + " marked the transaction successful.");
  }
  
  public synchronized void endTransaction() {
      // check the current thread still has a write lock
      if (currentTransactionOwner == null) {
          Arrays.toString(Thread.currentThread().getStackTrace());
          throw new RuntimeException("ORM: Thread ID " + Thread.currentThread().getId() + " tried to end a non-existent transaction");
      }
      if (currentTransactionOwner != Thread.currentThread()) {
          Arrays.toString(Thread.currentThread().getStackTrace());
          throw new RuntimeException("ORM: Thread ID " + Thread.currentThread().getId() + " tried to end another thread's transaction");
      }
      getDatabase().endTransaction();
      
      // is still in a transaction, must be nested
      if (getDatabase().inTransaction()) {
          // don't release the locks
          //Log.v("ORM", "Thread ID " + Thread.currentThread().getId() + " ended a nested transaction.");
      } else {
          // release the locks
          currentTransactionOwner = null;
          //Log.v("ORM", "Thread ID " + Thread.currentThread().getId() + " ended the top-level transaction.");
          releaseWriteLock(); // all done, can release lock
      }
      
  }
  
  public synchronized Cursor query(String sql) {
      Cursor result = null;
      try {
          getWriteLock();
          //Log.v("ORM: Q", "Thread ID " + Thread.currentThread().getId() + " " + sql);
          result = getDatabase().rawQuery(sql, null);
      } finally {
          releaseWriteLock();
      }
      return result;
  }
  
  public synchronized void execSQL(String sql) {
      try {
          getWriteLock();
          //Log.v("ORM: E", "Thread ID " + Thread.currentThread().getId() + " " + sql);
          getDatabase().execSQL(sql);
      } finally {
          releaseWriteLock();
      }
  }
  
  public void replace(String table, String nullColumnHack, ContentValues values) {
      try {
          getWriteLock();
          getDatabase().replace(table, nullColumnHack, values);
      } finally {
          releaseWriteLock();
      }
  }
  
  public void insert(String table, String nullColumnHack, ContentValues values) {
      try {
          getWriteLock();
          getDatabase().insert(table, nullColumnHack, values);
      } finally {
          releaseWriteLock();
      }
  }
  
  public void update(String table, ContentValues values, String whereClause, String[] whereArgs) {
      try {
          getWriteLock();
          getDatabase().update(table, values, whereClause, whereArgs);
      } finally {
          releaseWriteLock();
      }
  }
  
  public void delete(String table, String whereClause, String[] whereArgs) {
      try {
          getWriteLock();
          getDatabase().delete(table, whereClause, whereArgs);
      } finally {
          releaseWriteLock();
      }
  }
  
  public SQLiteStatement compileStatement(String sqlStatement) {
      try {
          getWriteLock();
          return getDatabase().compileStatement(sqlStatement);
      } finally {
          releaseWriteLock();
      }
  }
  
  public long executeInsert(SQLiteStatement stmt) {
      try {
          getWriteLock();
          Log.v("ORM: I", "Thread ID " + Thread.currentThread().getId() + " " + stmt.toString());
          return stmt.executeInsert();
      } finally {
          releaseWriteLock();
      }
  }
  
  /**
   * Reset database.
   */
  public synchronized void resetDatabase() {
      // TODO: establish write lock?
      mDatabases.clear();
      deleteDatabase(getDatabaseName());
      Entity.resetEntityMappings();
  }
}
