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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
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
  private Thread currentlyWritingThread = null;
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
  public synchronized SQLiteDatabase getDatabase() {
    SQLiteDatabase db = mDatabases.get(Thread.currentThread());
    if (db == null || !db.isOpen()) {
      while (true) {
        try {
          // need write lock as cannot open connection when a write is in progress
          getWriteLock();
          db = openOrCreateDatabase(getDatabaseName(), 0, null);
          mDatabases.remove(Thread.currentThread());
          mDatabases.put(Thread.currentThread(), db);
          Log.d("ORM", "Connection opened for thread ID " + Thread.currentThread().getId() + ", which makes " + mDatabases.keySet().size() + " connections in total");
          break;
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (SQLiteDatabaseLockedException e) {
          // shouldn't be here, only thing we can do is crash or wait, but wait might hang...
          Log.e("ORM", "Cannot open connection for thread ID " + Thread.currentThread().getId() + ", the database is locked.");
          throw e;
//          try {
//            this.wait();
//          } catch (InterruptedException e1) {
//            e1.printStackTrace();
//          }
        }
      }
    }
    return db;
  }
  
  public synchronized void getWriteLock() throws InterruptedException {
    while (true) {
      if (currentlyWritingThread == null && currentlyReadingThreads.isEmpty()) {
          currentlyWritingThread = Thread.currentThread();
          Log.v("ORM", "Write lock given to thread ID " + currentlyWritingThread.getId());
          return;
      } else if (currentlyWritingThread == Thread.currentThread()) {
          //Log.v("ORM", "Write lock refreshed by thread ID " + currentlyWritingThread.getId());
          return;
      } else {
          this.wait();
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
    if (currentlyWritingThread == Thread.currentThread()) {
      Log.v("ORM", "Write lock released by thread ID " + currentlyWritingThread.getId());
      currentlyWritingThread = null;
      this.notifyAll();
    }
  }
  
  public synchronized void getReadLock() throws InterruptedException {
      Thread thisThread = Thread.currentThread();
      while (true) {
        if (currentlyWritingThread == null) {
            currentlyReadingThreads.add(thisThread);
            Log.v("ORM", "Read lock given to thread ID " + thisThread.getId());
            return;
        } else {
            this.wait();
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
      if (currentlyReadingThreads.contains(thisThread)) {
        currentlyReadingThreads.remove(thisThread);
        Log.v("ORM", "Read lock released by thread ID " + thisThread.getId());
        this.notifyAll();
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
