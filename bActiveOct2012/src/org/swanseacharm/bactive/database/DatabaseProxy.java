package org.swanseacharm.bactive.database;


import static org.swanseacharm.bactive.database.DatabaseConstants.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.swanseacharm.bactive.ActivityRecord;
import org.swanseacharm.bactive.DateUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseProxy
{
    private bActiveData bActive;
    private final String NOT_SENT = "no";
    private final String IS_SENT = "yes";

    private final long ONE_DAY = 86400000;

    public DatabaseProxy(Context context)
    {
		super();
		bActive = new bActiveData(context);
    }

    //update data for a specified date
    //or if date does not exist in database, insert it
    public void updateActivityRecord(ActivityRecord ar)
    {
		//get handle to database
		SQLiteDatabase db = bActive.getWritableDatabase();
	
		if(databaseHasRow(ar.getDate()))
		{
		    //row exists so update it
		    ContentValues insertValues = new ContentValues();
		    insertValues.put(STEPS, ar.getMe());
		    insertValues.put(SENT, NOT_SENT );
	
		    String where = DATE + " = '" + DateUtil.getSQLFormatted(ar.getDate()) + "'";
		    try {
		    	db.update(TABLE_NAME, insertValues, where, null);
		    }
		    finally {
		    	bActive.close();
		    }    
		}
		else
		{
		    //row doesn't exist so insert it
		    ContentValues values = new ContentValues();
		    values.put(DATE, DateUtil.getSQLFormatted(ar.getDate()));
		    values.put(STEPS, ar.getMe());
		    values.put(SENT, NOT_SENT );
		    try {
		    	db.insertOrThrow(TABLE_NAME, null, values);
		    }
		    finally {
		    	bActive.close();
		    }
		}
    }

    //updates step data for a given date
    //or if date does not exist, inserts it
    public void updateSteps(ActivityRecord ar)
    {
		//get handle to database
		SQLiteDatabase db = bActive.getWritableDatabase();
	
		if(databaseHasRow(ar.getDate()))
		{
		    //row exists so update it
		    ContentValues insertValues = new ContentValues();
		    insertValues.put(STEPS, ar.getMe());
		    insertValues.put(SENT, NOT_SENT );
	
		    String where = DATE + " = '" + DateUtil.getSQLFormatted(ar.getDate()) + "'";
		    try {
		    	db.update(TABLE_NAME, insertValues, where, null);
		    }
		    finally {
		    	bActive.close();
		    } 
		}
		else
		{
		    //row doesn't exist so insert it
		    ContentValues values = new ContentValues();
		    values.put(DATE, DateUtil.getSQLFormatted(ar.getDate()));
		    values.put(STEPS, ar.getMe());
		   // values.put(CALS, 0);
		   // values.put(DISTANCE, 0);
		    values.put(SENT, NOT_SENT );
		    try {
		    	db.insertOrThrow(TABLE_NAME, null, values);
		    }
		    finally {
		    	bActive.close();
		    }
		}
    }

  

    /**
     * mark the sent column as yes for the given activity record
     * @param ar The activity record to mark as 'sent'
     * @author Simon Walton
     */
    public void markAsSent(ActivityRecord ar)
    {
		ContentValues values = new ContentValues();
		values.put(SENT, IS_SENT );
		String where = DATE + " = '" + DateUtil.getSQLFormatted(ar.getDate()) + "'";
	
		// get handle to database
		SQLiteDatabase db = bActive.getWritableDatabase();
		
		try {
			db.update(TABLE_NAME, values, where, null);
		}
		finally {
		    bActive.close();
		} 
    }
    
    /**
     * gets all unsent activity records, plus today's record
     * @author Simon Walton
     */
    public ArrayList<ActivityRecord> getTodayAndUnsent()
    {
		//get handle to database
		SQLiteDatabase db = bActive.getReadableDatabase();
	
		//query db for row with specified date
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + DATE + " = '" + DateUtil.getSQLFormatted(DateUtil.today()) + "' OR " + SENT + " = '" + NOT_SENT + "'";
		Cursor c = db.rawQuery(query, null);
			
		ArrayList<ActivityRecord> ars = new ArrayList<ActivityRecord>();
		while(c.moveToNext())
		{
			ActivityRecord ar = new ActivityRecord();
		    ar.setDate(DateUtil.parseSQLFormatted(c.getString(c.getColumnIndex(DATE))));
		    ar.setMe(c.getInt(c.getColumnIndex(STEPS)));
	//	    ar.setCals(c.getFloat(c.getColumnIndex(CALS)));
	//	    ar.setDistance(c.getFloat(c.getColumnIndex(DISTANCE)));
		    ars.add(ar);
		}
		
		bActive.close();
		c.close();
		
		return ars;
    }
    
    /**
     * gets all unsent activity records
     * @author Simon Walton
     */
    public ArrayList<ActivityRecord> getUnsent()
    {
		//get handle to database
		SQLiteDatabase db = bActive.getReadableDatabase();
	
		//query db for row with specified date
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + SENT + " = '" + NOT_SENT + "'";
		Cursor c = db.rawQuery(query, null);
			
		ArrayList<ActivityRecord> ars = new ArrayList<ActivityRecord>();
		while(c.moveToNext())
		{
			ActivityRecord ar = new ActivityRecord();
		    ar.setDate(DateUtil.parseSQLFormatted(c.getString(c.getColumnIndex(DATE))));
		    ar.setMe(c.getInt(c.getColumnIndex(STEPS)));
//		    ar.setCals(c.getFloat(c.getColumnIndex(CALS)));
	//	    ar.setDistance(c.getFloat(c.getColumnIndex(DISTANCE)));
		    ars.add(ar);
		}
		
		bActive.close();
		c.close();
		
		return ars;
    }

    //get activity record for a specified date
    public ActivityRecord getActivityRecordByDate(Calendar date)
    {
		//get handle to database
		SQLiteDatabase db = bActive.getReadableDatabase();
	
		//query db for row with specified date
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + DATE + " = '" + DateUtil.getSQLFormatted(date) + "'";
		Cursor c = db.rawQuery(query, null);
			
		ActivityRecord ar = new ActivityRecord();
		ar.setDate(date);
		
		while(c.moveToNext()) {
		    ar.setMe(c.getInt(c.getColumnIndex(STEPS)));
		}
		
		bActive.close();
		c.close();
		
		return ar;
    }


    public ArrayList<ActivityRecord> getActivityRecordsByDateRange(Calendar startDate, Calendar endDate)
    {
		ArrayList<ActivityRecord> ars = new ArrayList<ActivityRecord>();
		Calendar curr = (Calendar)startDate.clone();
		
		while(DateUtil.timelessComparison(curr, endDate) <= 0)
		{	
		    //get handle to database
		    SQLiteDatabase db = bActive.getReadableDatabase();
	
		    //query db for row with specified date
		    String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + DATE + " = '" + DateUtil.getSQLFormatted(curr) + "'";
		    Cursor c = db.rawQuery(query, null);
	
		    ActivityRecord ar = new ActivityRecord();
		    ar.setDate(curr);
		    
		    while(c.moveToNext())
		    {
				ar.setMe(c.getInt(c.getColumnIndex(STEPS)));		
		    }
		    bActive.close();
		    c.close();
	
		    ars.add(ar);
	
		    curr.add(Calendar.DATE, 1);
		}
		return ars;
    }
    
    /**
     * Based on list provided in 'you', combine 'group' values into the list and return
     * @param you Must be same size as group
     * @param group Must be same size as you
     * @return Combined ArrayList
     */
    public ArrayList<ActivityRecord> combineGroupIntoYou(ArrayList<ActivityRecord> you, ArrayList<ActivityRecord> group) 
    {
    	if(group == null)
    		return you;
    	
    	ArrayList<ActivityRecord> out = new ArrayList<ActivityRecord>();
    	for(int i=0;i<you.size();i++) {
    		ActivityRecord combined = new ActivityRecord();
    		
    		// if 'you' has some data for today, then add group data
    		if(you.get(i).getDate() != null) {
    			combined.setDate(you.get(i).getDate());
    			combined.setMe(you.get(i).getMe());
    			combined.setMin(group.get(i).getMin());
    			combined.setMax(group.get(i).getMax());
    			combined.setAvg(group.get(i).getAvg());
    			combined.setTop(group.get(i).getTop());
    		}
    		
    		out.add(combined);
    	}
    	
    	return out;
    }


    private boolean databaseHasRow(Calendar calendar)
    {

		//get handle to database
		SQLiteDatabase db = bActive.getReadableDatabase();
	
		//check if row already exists
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + DATE + " = '" + DateUtil.getSQLFormatted(calendar) + "'";
		Cursor c = db.rawQuery(query, null);
		if(c.getCount() > 0)
		{
		    //row exists
		    c.close();
		    return true;
		}
		else
		{
		    //row does not exit
		    c.close();
		    return false;	    
		}
    }

    public Date getEarliestDate()
    {
		//get handle to database
		SQLiteDatabase db = bActive.getReadableDatabase();
	
		//check if row already exists
		String query = "SELECT " + DATE + " FROM " + TABLE_NAME + " WHERE " + DATE + " != '' " + " ORDER BY " + DATE + " LIMIT 1";
		Cursor c = db.rawQuery(query, null);
	
		Date d = new Date(); //init to today in case database has no data
	
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
		while(c.moveToNext())
		{
		    try
		    {
		    	d = sdf.parse(c.getString(c.getColumnIndex(DATE)));
		    }
		    catch (ParseException e)
		    {
	
		    	e.printStackTrace();
		    }
		}
		bActive.close();
		c.close();
		
		return d;
    }

  

    /**
     * Saves cached group data to avoid asking the web service each time
     * @param ars 
     */
    public void setCachedGroupData(ArrayList<ActivityRecord> ars) 
	{
		SQLiteDatabase db = bActive.getWritableDatabase();
		
		try {
			// remove the existing cache
			db.delete(CACHE_TABLE_NAME,null,null);
		
			// for each item, add to cache
			for(ActivityRecord ar : ars) {
			    ContentValues values = new ContentValues();
			    values.put(DATE, DateUtil.getSQLFormatted(ar.getDate()));
			    values.put(STEPS, ar.getMe());
			    values.put(GROUP_MIN, ar.getMin());
			    values.put(GROUP_MAX, ar.getMax());
			    values.put(GROUP_AVG, ar.getAvg());	
			    values.put(GROUP_TOP, ar.getTop());
			    db.insertOrThrow(CACHE_TABLE_NAME, null, values);
			}
		}
	    finally
	    {
	    	bActive.close();
	    }		
	}

	public ArrayList<ActivityRecord> getCachedGroupData(Calendar startDate, Calendar endDate)
	{
		ArrayList<ActivityRecord> ars = new ArrayList<ActivityRecord>();
	
	    //query db for row with specified date
		SQLiteDatabase db = bActive.getReadableDatabase();

		try {
		    String query = "SELECT * FROM " + CACHE_TABLE_NAME;
		    Cursor c = db.rawQuery(query, null);
		    
		    while(c.moveToNext())
		    {
		    	ActivityRecord ar = new ActivityRecord();
				ar.setDate(DateUtil.parseSQLFormatted(c.getString(c.getColumnIndex(DATE))));
				ar.setMe(c.getInt(c.getColumnIndex(STEPS)));
				ar.setMin(c.getFloat(c.getColumnIndex(GROUP_MIN)));
				ar.setMax(c.getFloat(c.getColumnIndex(GROUP_MAX)));
				ar.setAvg(c.getFloat(c.getColumnIndex(GROUP_AVG)));
				ar.setTop(c.getFloat(c.getColumnIndex(GROUP_TOP)));
				ars.add(ar);
		    }
		    
		    c.close();
		}
		finally {
			bActive.close();
		}
		
		return ars;
	}
}
