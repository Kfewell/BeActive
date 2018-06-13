package org.swanseacharm.bactive;

import java.util.Calendar;

import org.swanseacharm.bactive.ui.Today;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class ServiceManager extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
    	if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
    		Intent today = new Intent(context, Today.class);
    		today.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		context.startActivity(today);
    	}
    }
    
    public static void startAll(Context context) {
		Scheduler.ensureAlarmsSet(context.getApplicationContext());
		context.startService(new Intent(context, ActivityMonitor.class));
		ensureAlarm(context);
    }
    
    public static void stopAll(Context context) {
		context.stopService(new Intent(context, ActivityMonitor.class));
    }

    /**
     * ensures alarm is set for activity monitor power scheme
     * @param context
     */
    public static void ensureAlarm(Context context)
	{
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(context,192837,intent,PendingIntent.FLAG_UPDATE_CURRENT);
		  
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, 10);
		AlarmManager mAlarmMgr = (AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);
		
		// cancel any existing alarm based on matching intent metadata
		mAlarmMgr.cancel(sender);
		mAlarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis()+(1000*35), 1000*35, sender);
	}
    
    /**
     * ensures alarm is set for waking the application when the study begins
     * @param context
    
    public static void ensureUserStudyStartAlarmSet(Context context)
    {
    	Intent intent = new Intent(context, Today.class);
		PendingIntent sender = PendingIntent.getActivity(context,2387,intent,PendingIntent.FLAG_UPDATE_CURRENT);
		
		AlarmManager mAlarmMgr = (AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);

		mAlarmMgr.cancel(sender);
		mAlarmMgr.set(AlarmManager.RTC, Globals.studyPeriodStart().getTimeInMillis(), sender);
    } */
}
