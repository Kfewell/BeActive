package org.swanseacharm.bactive;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * App global variables
 * @author Simon Walton
 */
public class Globals
{
	public static final int GROUP_1 = 1;		// control group
	public static final int GROUP_2 = 2;		// individual feedback only
	public static final int GROUP_3 = 3;		// individual & group feedback
	private static final String mHelpURL = "http://cs.swansea.ac.uk/bactive/"; 	
	
    private static Globals sInstance;
	private Map<String,Object> mObjMap; 
	
	/**
	 * static final options for application - set before compiling .apk
	 */
	private static final int mGroup = GROUP_1; 
	private static final boolean mDebugMode = false;
	//private static final boolean mAnimationsEnabled = false;
	
	// period during which the user is permitted to use the application
	private static final Calendar mTrialStart = new GregorianCalendar(2011, 9, 31);
	private static final Calendar mTrialEnd = new GregorianCalendar(2013, 8, 1);
	// period during which the app monitors and sends activity data
	private static final Calendar mActivityMonitoringStart = new GregorianCalendar(2011, 9, 1);
	private static final Calendar mActivityMonitoringEnd = mTrialEnd;

	// activity tuner
	private static boolean mActivityTuner = false;
	
	public static Globals getInstance() {
		return sInstance;
    }
	
	/**
	 * returns the last seven digits of phone IMEI for user's reference
	 */
	public static String getShortIMEI(Context c) {
	    TelephonyManager manager = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
	    return manager.getDeviceId().substring(8);
	}
	
	/**
	 * is this a valid period for the app UI to function?
	 */
	public static boolean withinStudyPeriod() {
		if(mDebugMode)
			return true;
		return DateUtil.todayWithinPeriod(mTrialStart, mTrialEnd);
	}
	
	/**
	 * is this a valid period for the app to monitor activity?
	 */
	public static boolean hasStudyEnded() {
		if(mDebugMode)
			return false;	
		return DateUtil.timelessComparison(DateUtil.today(),mTrialEnd) > 0;
	}
	
	public static Calendar studyPeriodStart() {
		return mTrialStart;
	}
	
	public static Calendar studyPeriodEnd() {
		return mTrialEnd;
	}
	
	public static Calendar activityMonitoringStart() {
		return mActivityMonitoringStart;
	}
	
	public static Calendar activityMonitoringEnd() {
		return mActivityMonitoringEnd;
	}
	
	public static boolean activityTunerActive() {
		return mActivityTuner;
	}
	 
	/**
	 * is this bActive giving group feedback?
	 * @return
	 */
	public static boolean groupFeedback() {
		return mGroup == GROUP_3;
	} 
	
	/**
	 * play animations?
	
	public static boolean animationsEnabled() {
		return mAnimationsEnabled;
	} */
	
	/**
	 * is this the control group?
	 */
	public static boolean isControlGroup() {
		return mGroup == GROUP_2;
	}
	
	/**
	 * which group is this app?
	 */
	public static int group() {
		return mGroup;
	}
	
	/**
	 * returns help URL based on the app's group setting
	 */
	public static String helpURL() {
		return mHelpURL + "?g=" + mGroup;
	}
	
	/**
	 * fixed start date for all date comparisons
	 * @return
	 */
	public static Calendar getStartDate() {
		Calendar c = DateUtil.epoch();
		c.set(2011,5,1,0,0,0);
		return c;
	}
	
	public void setObj(String tag, Object obj) {
		mObjMap.put(tag, obj);
	}
	
	public Object getObj(String tag) {
		if(mObjMap.containsKey(tag))
			return mObjMap.get(tag);
		else return null;
	}

	public static void init() {
		sInstance = new Globals();
		sInstance.mObjMap = new HashMap<String,Object>();
	}

	public static boolean debugMode() {
		// TODO Auto-generated method stub
		return mDebugMode;
	}

}
