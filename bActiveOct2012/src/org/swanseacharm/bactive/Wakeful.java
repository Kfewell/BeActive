package org.swanseacharm.bactive;


import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;

/**
 * functionality to keep the phone awake and allow it to sleep
 * @author Simon Walton
 */
public class Wakeful
{
	private static WakeLock mWakeLock = null;
	private static PowerManager mPowerMgr = null;
	public static final String WAKE_LOCK_TAG = "Wakeful.wakelock";
    public static final String BEGIN_MONITORING_BROADCAST = "org.swanseacharm.bactive.alarm_begin_monitoring";
	        
    /**
     * class must be initialised manually as it's used statically
     * @param context
     */
	public static void init(Context context) {
		int flags;
        flags = PowerManager.PARTIAL_WAKE_LOCK;
		mPowerMgr = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
		mWakeLock = mPowerMgr.newWakeLock(flags, WAKE_LOCK_TAG);
		
		DebugLog.appendLog("Initialised the wake lock.");
	}
	
	/**
	 * returns true if the wakelock is currently being held
	 * @return
	 */
	public static boolean isHoldingWakeLock() {
		if(mWakeLock != null)
			return mWakeLock.isHeld();
		else return false;
	}
	
	/**
	 * attempts to obtain wakelock to keep phone alive
	 * @param context
	 */
    public static void obtainWakeLock(Context context) {
    	DebugLog.appendLog("***** Obtaining wake lock");  
    	
    	if(mWakeLock == null) {
    		DebugLog.appendLog("null WakeLock");
    		return;
    	}
    	
        DebugLog.appendLog("Before isHeld = " + (mWakeLock.isHeld() ? "yes!" : "no"));
        
        if(!mWakeLock.isHeld())
        	mWakeLock.acquire();
        
        DebugLog.appendLog("After isHeld = " + (mWakeLock.isHeld() ? "yes!" : "no"));
        
        Globals.getInstance().setObj(WAKE_LOCK_TAG, mWakeLock);
    }
    
    private static void releaseWakeLock() {
		DebugLog.appendLog("Trying to release the wake lock.");

    	if(mWakeLock != null) {
    		if(mWakeLock.isHeld()) {
    			DebugLog.appendLog("**** Releasing the wake lock!");
    			mWakeLock.release();
    		}
    		else DebugLog.appendLog("Did not release - was not held.");
    	}
    	else DebugLog.appendLog("Did not release. Was null.");
    }
    
    /**
     * informs the system that the phone may now sleep
     */
    public static void youMaySleep() {
    	DebugLog.appendLog("Been told that I can sleep");
    	releaseWakeLock();
    }
} 