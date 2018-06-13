package org.swanseacharm.bactive;

import org.swanseacharm.receivers.StepSyncReceiver;
import org.swanseacharm.receivers.UpdateReceiver;
import org.swanseacharm.receivers.UsageReceiver;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/**
 * Creates a couple of threads to do routine data posting to server
 */
public class Scheduler
{
    static private int HALF_HOUR = 1800000;
    static private int ONE_HOUR = 3600000;

    private static void setAlarm(Context context, Class<?> cls, long delay, long period)
    {
    	AlarmManager mAlarmMgr = (AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);
    	Intent in = new Intent(context, cls);
    	PendingIntent pe = PendingIntent.getBroadcast(context,991811,in,PendingIntent.FLAG_NO_CREATE);
    	
    	if(pe == null) {
    		// alarm has NOT been set previously. 
    		pe = PendingIntent.getBroadcast(context,991811,in,PendingIntent.FLAG_UPDATE_CURRENT);
    		// doesn't hurt...
   			mAlarmMgr.cancel(pe);
   			// set
   			mAlarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+delay, period, pe);
    	}
    }
    
    public static void ensureAlarmsSet(Context context) 
    {		 
    	setAlarm(context,StepSyncReceiver.class,1000*60,ONE_HOUR*2);
    	
    	if(!Globals.isControlGroup())
    		setAlarm(context,UsageReceiver.class,1000*60,ONE_HOUR*4);
    	
    	setAlarm(context,UpdateReceiver.class,1000*60,Globals.debugMode() ? (1000*60*30) : ONE_HOUR*6);
    }
     
    /**
     * sends activity data to server
    
    public boolean executeActivityHttpPost()
    {
		return (new WebServicePr oxy()).sendTodayAndUnsentData(this);
    } */
    
    /**
     * sends usage data to server
     * @return
    
    public boolean executeUsageHttpPost()
    {
    	return (new WebServiceProxy()).sendUsageStats(this);
    }
    
    public void executeUpdateHttpPost()
    {
    	Updater.check(this);
    }
    
    @Override
    public void onDestroy()
    {
    	mAbortThread = true;
    }

    @Override 
    public IBinder onBind(Intent intent)
    {
    	return null;
    } */

    /*
    class UpdateHttpThread extends Thread 
    {
    	@Override 
		public void run () 
		{
			Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
			
		    while(!mAbortThread) 
		    {
				try
				{
					executeUpdateHttpPost();
				   	Thread.sleep(ONE_HOUR*12);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				try{
					Thread.sleep(ONE_HOUR*6);
				}
				catch(InterruptedException e) {}
		    }
		}
    }
    
    class HttpThread extends Thread 
	{
		@Override 
		public void run () 
		{
			Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
			
		    while(!mAbortThread) 
		    {
				try
				{
				    if(executeActivityHttpPost())
				    	Thread.sleep(ONE_HOUR);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				try{
					Thread.sleep(HALF_HOUR);
				}
				catch(InterruptedException e) {}
		    }
		}
    }
    
    class UsageHttpThread extends Thread 
	{
		@Override 
		public void run () 
		{
			Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
			
		    while(!mAbortThread) 
		    {
				try
				{
				    executeUsageHttpPost();
				    Thread.sleep(ONE_HOUR*4); // sync usage stats every four hours
				}
				catch (Exception e) {
					e.printStackTrace();
				}
		    }
		}
    }
    
    public class WarningReceiver extends BroadcastReceiver 
    {
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			// if the battery is low, then sync the data to avoid potentially losing unsent activity
		    if(intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
		    	executeActivityHttpPost();
		    }
		}
    };*/
}

