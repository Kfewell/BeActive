package org.swanseacharm.bactive;

import java.util.GregorianCalendar;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Object to hold activity data for a given day
 * @author Simon Walton
 *
 */
public class ActivityRecord
{
    private Calendar mDate;
    private float mMe;
    private float mGroupMin;
    private float mGroupMax;
    private float mGroupAvg;
    private float mGroupTop;
    
    public final static int FIELD_ME = 0;
    public final static int FIELD_GROUPMIN = 1;
    public final static int FIELD_GROUPMAX = 2;
    public final static int FIELD_GROUPAVG = 3;
    public final static int FIELD_GROUPTOP = 4;
    public final static int FIELD_SIZE = 5;
    
    public ActivityRecord()
    {
    	super();
    	mMe = 0;
    	mDate = null;
    	mGroupMin = 0;
    	mGroupMax = 0;
    	mGroupAvg = 0;
    	mGroupTop = 0;
    }
   
    public float getField(int field) {
    	switch(field) {
	    	case FIELD_ME :
	    		return mMe;
	    	case FIELD_GROUPMIN:
	    		return mGroupMin;
	    	case FIELD_GROUPMAX:
	    		return mGroupMax;
	    	case FIELD_GROUPAVG:
	    		return mGroupAvg;
	    	case FIELD_GROUPTOP:
	    		return mGroupTop;
    	}
    	return -1;
    }
    
    public void setField(int field, float value) {
    	switch(field) {
	    	case FIELD_ME :
	    		mMe = value;
	    		break;
	    	case FIELD_GROUPMIN:
	    		mGroupMin = value;
	    		break;
	    	case FIELD_GROUPMAX:
	    		mGroupMax = value;
	    		break;
	    	case FIELD_GROUPAVG:
	    		mGroupAvg = value;
	    		break;
    	}
    }

    public Calendar getDate() { return mDate; }
    public void setDate(Calendar date) { mDate = (Calendar)date.clone(); }
    
    public float getMe() { return mMe; }
    public void setMe(float v) { mMe = v; }
    
    public float getMin() { return mGroupMin; }
    public void setMin(float v) { mGroupMin = v; }
    
    public float getMax() { return mGroupMax; }
    public void setMax(float v) { mGroupMax = v; }
    
    public float getAvg() { return mGroupAvg; }
    public void setAvg(float v) { mGroupAvg = v; }
    
    public void setTop(float v) { mGroupTop = v; }
    public float getTop() {	return mGroupTop; }
    
    public static float computeDistance(int steps) {
    	 return (float)steps * (float)ActivityMonitor.DISTANCE_STEPS_RATIO;
    }
    
    public static float computeCalories(int steps) {
   	 	return (float)steps * (float)ActivityMonitor.CALS_STEPS_RATIO;
    }
    
    public static String formatCalories(int steps) {
   	 	return new DecimalFormat("0").format(ActivityRecord.computeCalories(steps)) + " cal";
    }
    
    public static String formatDistance(int steps) {
   	 	return new DecimalFormat("0.0").format(ActivityRecord.computeDistance(steps))  + " mi";
    }
    
    /**
     * gets max value from given fields of list of ActivityRecords
     * @return
     */
    public static float getMax(ArrayList<ActivityRecord> in, int[] fields)
    {
		float max = -1;
		
		for(ActivityRecord a : in) {
			for(int f : fields)
				if(a.getField(f) > max) max = a.getField(f);
		}		
		
		if(max == 0) max++; //safety check, in case all are 0
		
		return max;
    }
}
