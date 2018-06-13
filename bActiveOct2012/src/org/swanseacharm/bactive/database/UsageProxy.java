package org.swanseacharm.bactive.database;

import static org.swanseacharm.bactive.database.DatabaseConstants.*;

import java.util.ArrayList;

import org.swanseacharm.bactive.UsageRecord;
import org.swanseacharm.bactive.WebServiceProxy;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class UsageProxy {
	
	private static final int PAUSED = 0;
	private static final int RESUMED = 1;
	private static int mState = PAUSED;
	private static long mPausedAt = 0;
	private static final int mPauseThreshold = 2000;
	private static long mCurrStart = 0;
	
	/**
	 * Returns a list of usage records not yet sent to the server API
	 * @param context
	 * @return
	 */
	public static ArrayList<UsageRecord> getUnsentUsage(Context context) 
    {
		bActiveData usageData = new bActiveData(context);
    	SQLiteDatabase db = usageData.getReadableDatabase();
    	String query = "SELECT * FROM " + US_TABLE_NAME + " WHERE " + US_SENT + " = 0";
    	Cursor c = db.rawQuery(query, null);
    	
    	ArrayList<UsageRecord> records = new ArrayList<UsageRecord>();
    	
    	try {
	    	while(c.moveToNext()) {
	    		records.add(new UsageRecord(c.getLong(0), c.getLong(1)));
	    	}
    	}
    	finally {
    		usageData.close();
    	}
    	
    	return records;
    }
	
	/**
	 * Call when any activity is resumed
	 * @param c
	 */
	public static void resume(Context c)
	{		
		mCurrStart = System.currentTimeMillis();
		
		if(mState == RESUMED)
			return; // shouldn't happen
		
		if(mCurrStart - mPausedAt > mPauseThreshold) {
			// there was a long delay - treat it as a new session
			bActiveData usageData = new bActiveData(c);
			SQLiteDatabase db = usageData.getWritableDatabase();
			
			ContentValues insertValues = new ContentValues();
			insertValues.put(US_TIME_IN, mCurrStart);
			insertValues.put(US_SENT, 0);
						
			try {
				db.insertOrThrow(US_TABLE_NAME, null, insertValues);
				Log.d("USAGE","New session");
			}
			finally {
				usageData.close();
			}    
		}
		else {
			// short delay; ignore and treat as a continuing session
		}
		
		mState = RESUMED;
	}
	
	/**
	 * Call when any activity is paused
	 * @param c
	 */
	public static void pause(Context c)
	{
		mPausedAt = System.currentTimeMillis();
		
		// assume that this is the the final pause of the app going into the background
		bActiveData usageData = new bActiveData(c);
		SQLiteDatabase db = usageData.getWritableDatabase();
		
		ContentValues insertValues = new ContentValues();
		insertValues.put(US_TIME_OUT, mPausedAt);
		
	    String where = US_TIME_IN + " = " + mCurrStart;
	    try {
			Log.d("USAGE","Updated session");
	    	db.update(US_TABLE_NAME, insertValues, where, null);	    	
	    }
	    finally {
	    	usageData.close();
	    }
	    
	    mState = PAUSED;
	}

	/**
	 * Removes all data from the usage table - call once successfully sent to server
	 * @param c Android context
	 */
	public static void removeSentUsageData(Context c) {
		bActiveData usageData = new bActiveData(c);
		SQLiteDatabase db = usageData.getWritableDatabase();
		
		try {
			Log.d("USAGE","Marking as sent");
	    	int rows = db.delete(US_TABLE_NAME, null, null);
	    	Log.d("USAGE","" + rows + " rows affected.");
	    }
	    finally {
	    	usageData.close();
	    }		
	}
	
	
}
