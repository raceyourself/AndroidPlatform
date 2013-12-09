/*    */ package com.qualcomm.QCARUnityPlayer;
/*    */ 
/*    */ import android.app.Activity;
/*    */ import android.app.Dialog;
/*    */ import android.content.res.Configuration;
/*    */ import android.os.Bundle;
/*    */ import android.view.KeyEvent;
/*    */ import android.view.Window;
/*    */ import com.unity3d.player.UnityPlayer;
/*    */ 
/*    */ public class QCARPlayerActivity extends Activity
/*    */ {
/*    */   private QCARPlayerSharedActivity mQCARShared;
/*    */   private UnityPlayer mUnityPlayer;
/*    */ 
/*    */   protected void onCreate(Bundle savedInstanceState)
/*    */   {
/* 28 */     super.onCreate(savedInstanceState);
/*    */ 
/* 30 */     setTheme(16973831);
/* 31 */     requestWindowFeature(1);
/* 32 */     getWindow().setFlags(1024, 1024);
/*    */ 
/* 35 */     this.mQCARShared = new QCARPlayerSharedActivity();
/* 36 */     this.mQCARShared.setActivity(this);
/* 37 */     this.mQCARShared.onCreate(savedInstanceState);
/*    */ 
/* 39 */     this.mUnityPlayer = this.mQCARShared.getUnityPlayer();
/*    */   }

			public UnityPlayer getUnityPlayer() {
				return mUnityPlayer;
			}
/*    */ 
/*    */   protected Dialog onCreateDialog(int id)
/*    */   {
/* 44 */     return this.mQCARShared.onCreateDialog(id);
/*    */   }
/*    */ 
/*    */   protected void onResume()
/*    */   {
/* 49 */     super.onResume();
/* 50 */     this.mQCARShared.onResume();
/*    */   }
/*    */ 
/*    */   protected void onPause()
/*    */   {
/* 55 */     super.onPause();
/* 56 */     this.mQCARShared.onPause();
/*    */   }
/*    */ 
/*    */   protected void onDestroy()
/*    */   {
/* 61 */     super.onDestroy();
/* 62 */     this.mQCARShared.onDestroy();
/*    */   }
/*    */ 
/*    */   public void onConfigurationChanged(Configuration newConfig)
/*    */   {
/* 67 */     super.onConfigurationChanged(newConfig);
/* 68 */     this.mQCARShared.onConfigurationChanged(newConfig);
/*    */   }
/*    */ 
/*    */   public void onWindowFocusChanged(boolean hasFocus)
/*    */   {
/* 73 */     super.onWindowFocusChanged(hasFocus);
/* 74 */     this.mQCARShared.onWindowFocusChanged(hasFocus);
/*    */   }
/*    */ 
/*    */   public boolean onKeyDown(int keyCode, KeyEvent event)
/*    */   {
/* 79 */     return this.mQCARShared.onKeyDown(keyCode, event);
/*    */   }
/*    */ 
/*    */   public boolean onKeyUp(int keyCode, KeyEvent event)
/*    */   {
/* 84 */     return this.mQCARShared.onKeyUp(keyCode, event);
/*    */   }
/*    */ }
