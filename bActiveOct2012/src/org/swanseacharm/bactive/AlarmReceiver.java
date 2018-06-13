package org.swanseacharm.bactive;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Receives alarm signal that wakes the phone up 
 * @author Simon Walton
 */
public class AlarmReceiver extends BroadcastReceiver 
{
	private static WakeLock mWakeLock = null;
	
	@Override
    public void onReceive(final Context context, Intent intent)
    {
		// ActivityMonitor catches this
    	context.getApplicationContext().sendBroadcast(new Intent(Wakeful.BEGIN_MONITORING_BROADCAST));
    	
    	DebugLog.appendLog("***************** Alarm has fired"); 
    	Log.v("CHARM","Alarm fired"); 	
    	
    	// keep awake for one second to ensure that the above broadcast gets through
		PowerManager power = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
		mWakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alarmWakelock");
		mWakeLock.acquire(2000);
    }
	
	
}
