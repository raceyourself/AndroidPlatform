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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
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
      Log.e("ORMDroidApplication", "ORMDroid is not initialized");
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
        if (currentlyWritingThread != null && currentlyWritingThread != Thread.currentThread()) {
          try {
            this.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        } else {
          db = openOrCreateDatabase(getDatabaseName(), 0, null);
          mDatabases.remove(Thread.currentThread());
          mDatabases.put(Thread.currentThread(), db); 
          Log.d("ORM", "Opening new connection to database, which makes " + mDatabases.keySet().size() + " in total");
          break;
        }
      }
    }
    return db;
  }
  
  public synchronized void getWriteLock() throws InterruptedException {
    while (true) {
      if (currentlyWritingThread != null && currentlyWritingThread != Thread.currentThread()) {
          this.wait();
      } else {
        currentlyWritingThread = Thread.currentThread();
        return;
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
      currentlyWritingThread = null;
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
