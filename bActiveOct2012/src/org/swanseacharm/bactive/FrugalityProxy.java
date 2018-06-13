package org.swanseacharm.bactive;

import java.util.GregorianCalendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.swanseacharm.bactive.database.DatabaseProxy;

import android.content.Context;


/**
 * Data Frugality Manager allows other classes to use data more frugally by allowing particular
 * tasks to have their last 'refresh' time to be logged. A threshold defines the amount of time
 * that must pass before the data operation 'should' be allowed to continue.
 * @author Simon Walton
 */
public class FrugalityProxy {
	public static final int PAST_WEEK = 0;
	public static final int ALL_WEEKS = 1;
	public static final int GROUP_AVERAGE_TODAY = 2;
	public static final int GROUP_AVERAGE_YESTERDAY = 3;
	private static final int ONE_DAY = 86400000;
	
    private static int ONE_HOUR = 3600000;
	private static int mThreshold = (int)(ONE_HOUR*0.1f);
	private static Map m = new HashMap();
	
	public static final int TYPE_YOU = 0;
	public static final int TYPE_GROUP = 1;
	
	private static boolean mOverride = false;
	
	/**
	 * Should the caller use the data connection for a particular task?
	 * @param task: one of the static ints within DataManager representing a data task
	 * @return True if the task should go ahead (is longer than the defined threshold) 
	 */
	public static boolean shouldUseData(int task) {
		// check for override first set by nextFromWeb()
		if(mOverride) {
			mOverride = false;
			return true;
		}
		
		// check the hash table
		if(m.containsKey(task)) {
			Date prev = (Date)m.get(task);
			Date now = new Date();
			m.put(task,now);
			return now.getTime() - prev.getTime() >= mThreshold;
		}
		else return true;
	}
	
	/**
	 * instruct the proxy to mark the task as completed
	 */
	public static void markAsCompleted(int task) {
		m.put(task, new Date());
	}
	
	/**
	 * instructs the proxy to use the web service for the next update instruction
	 */
	public static void makeDirty() {
		mOverride = true;
	}
	
	/**
	 * Set the threshold that defines whether a data task should go ahead.
	 * @param The number of milliseconds that must pass
	 */
	public static void setThreshold(int milliseconds) {
		mThreshold = milliseconds;
	}
	
	/**
	 * Returns the current threshold
	 */
	public static int getThreshold() {
		return mThreshold;
	}
	
	/**
	 * Gets group records within date range
	 */
	public static ArrayList<ActivityRecord> getDateRangeGroup(Context c, Calendar start, Calendar end) throws Exception
	{
		ArrayList<ActivityRecord> ars = getAllTimeGroup(c); // we know this is padded either side and inbetween
		if(ars.size() == 0)
			throw new Exception();
		
		ArrayList<ActivityRecord> out = new ArrayList<ActivityRecord>();
		
		for(ActivityRecord a: ars) {
			if(a.getDate() != null && DateUtil.timelessComparison(a.getDate(),start) >= 0 && DateUtil.timelessComparison(a.getDate(),end) <= 0)
				out.add(a);
		}
		
		return WebServiceProxy.padMissingDates(out, start, end);
	}
	
	/**
	 * Gets all group records; either from the web service or the cache (depending on shouldUseData())
	 */
	public static ArrayList<ActivityRecord> getAllTimeGroup(Context c) throws Exception
	{
		Calendar startDate = Globals.getStartDate();
		
		if(shouldUseData(ALL_WEEKS)) {
			// return fresh, padded (null-date for no data on those days) data from web service
			ArrayList<ActivityRecord> ars = (new WebServiceProxy()).getGroupDataByDateRange(c,startDate, DateUtil.today());
			(new DatabaseProxy(c)).setCachedGroupData(ars);
			if(ars.size() != 0)
				markAsCompleted(ALL_WEEKS);
			return ars;

		}
		else {
			// get from the internal db
			return (new DatabaseProxy(c)).getCachedGroupData(startDate, DateUtil.today());
		}
	}
	
	/**
	 * Gets maximum value of all fields for all activity records
	 */
	public static float getMaximumValueForAllRecords(Context c, Calendar start, Calendar end, int[] fields) throws Exception
	{
		ArrayList<ActivityRecord> ars = (new DatabaseProxy(c)).getActivityRecordsByDateRange(start, end);
		float max = ActivityRecord.getMax(ars,fields);
		ars = getDateRangeGroup(c, start, end);
		float max2 = ActivityRecord.getMax(ars,fields);
		
		return max > max2 ? max : max2;
	}
}
