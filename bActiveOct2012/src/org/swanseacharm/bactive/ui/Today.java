package org.swanseacharm.bactive.ui;

import android.os.IBinder;
import java.util.Calendar;
import java.util.Date;

import org.swanseacharm.bactive.ActivityMonitor;
import org.swanseacharm.bactive.AlarmReceiver;
import org.swanseacharm.bactive.DateUtil;
import org.swanseacharm.bactive.GlobalExceptionHandler;
import org.swanseacharm.bactive.Globals;
import org.swanseacharm.bactive.Scheduler;
import org.swanseacharm.bactive.R;
import org.swanseacharm.bactive.ServiceManager;
import org.swanseacharm.bactive.TameToaster;
import org.swanseacharm.bactive.TutorialToaster;
import org.swanseacharm.bactive.ActivityMonitor.ActivityMonitorBinder;
import org.swanseacharm.bactive.R.string;
import org.swanseacharm.bactive.Updater;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Debug;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * the Today screen: entry point activity for the application
 */
public class Today extends SingleDay
{
	private TutorialToaster mTutorial;

    @Override
    public void onCreate(Bundle savedInstanceState)
    { 
		setDay(DateUtil.today());
    	super.onCreate(savedInstanceState);	
    	
    	// has the study ended?
    	if(Globals.hasStudyEnded()) {
    		// yes; end everything
    		ServiceManager.stopAll(this);
    		finish();
    		return;
    	}
    	
    	// start all services - we should always monitor activity and return data even before the trial begins (control period)
    	ServiceManager.startAll(this);
    		
    	// is the user allowed to use the app's UI based on the allowable time period?
		if(!Globals.withinStudyPeriod()) {
			// no - display a message
			String disabledStr = "Sorry - bActive is disabled";
			if(DateUtil.timelessComparison(DateUtil.today(),Globals.studyPeriodStart()) < 0)					
				disabledStr += " until " + DateUtil.formatShort(Globals.studyPeriodStart());
			disabledStr += ".";
			
			TameToaster.showToast(this, disabledStr);

			finish();
			return;
		}
		
		/*
		 * control group: show placeholder activity and finish this one
		 */
		if(Globals.isControlGroup()) {
			startActivity(new Intent(this,ControlPlaceholder.class));
			finish();
		}
		
		ServiceConnection conn = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i("INFO", "Service bound ");
				((ActivityMonitor.ActivityMonitorBinder)service).startGlobals();
			}
			@Override
			public void onServiceDisconnected(ComponentName arg0) {
				Log.i("INFO", "Service Unbound ");
			}
		};
		
		bindService(new Intent(Today.this, ActivityMonitor.class), conn, Context.BIND_AUTO_CREATE);
		
		Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler(this));
	
		// title bar strings
		mTitleNoData = org.swanseacharm.bactive.R.string.today_no_data;
		mTitleNoFeedback = org.swanseacharm.bactive.R.string.today_no_feedback;
		mTitleBelowAverage = org.swanseacharm.bactive.R.string.today_below_average;
		mTitleBelowMidPoint = org.swanseacharm.bactive.R.string.today_below_mid_point;
		mTitleBelowTop20 = org.swanseacharm.bactive.R.string.today_below_top_20;
		mTitleAboveTop20 = org.swanseacharm.bactive.R.string.today_above_top_20;
		
        // initiates the wake lock which prevents the unit from completely sleeping.
    	int flags;
        flags = PowerManager.PARTIAL_WAKE_LOCK;
        flags |= PowerManager.ON_AFTER_RELEASE;
        flags |= PowerManager.ACQUIRE_CAUSES_WAKEUP;
    }
    
    @Override
    public void onResume() 
    {
		mPrefsName = "Today";
		setDay(DateUtil.today());
    	super.onResume();
    	    	 
    	mTutorial = new TutorialToaster(this,this);
    	
    	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	
    	if(!mTutorial.hasSeen() && pm.isScreenOn()) {
	    	if(Globals.groupFeedback()) {
		    	mTutorial.setToasts( 
		    		new String[]{
		    			"Welcome to the bActive 'today' screen!",
		    			"This figure represents your progress.",
		    			"These figures represent the group as a whole.",
		    			"The position of your figure relative to the group shows you how well you're doing.",
		    			"The numbers below show everyone's step count, calories, and distance.",
		    			"The numbers below the green figure are yours, and the other numbers are the average of everyone else.",
		    			"The numbers will update periodically throughout the day.",
		    			"We hope you enjoy your time with bActive!"
		    		},
		    		new int[][] {
		    			{0,200},
		    			{0,250},
		    			{0,50},
		    			{0,50},
		    			{0,390},
		    			{0,390},
		    			{0,390},
		    			{0,250}
		    		});
	    	}
	    	else {
	    		mTutorial.setToasts(
	    	    		new String[]{
	    	    			"Welcome to the bActive 'today' screen!",    	    			
	    	    			"The numbers below show your step count, calories, and distance.",
	    	    			"The numbers will update periodically throughout the day.",
	    	    			"We hope you enjoy your time with bActive!"
	    	    		},
	    	    		new int[][] {
	    	    			{0,200},    	    			
	    	    			{0,390},
	    	    			{0,390},
	    	    			{0,200}
	    	    		});
	    	}
	    	
	    	mTutorial.execute();
    	}
    	
    	//((Button)findViewById(2)).;
        	
    }
    
	@Override
	public void onPause() {
		super.onPause();
		
		//Debug.stopMethodTracing();
		
		mTutorial.cancelAll();
	}
	
}	

