package org.swanseacharm.bactive.database;


import static android.provider.BaseColumns._ID;
import static org.swanseacharm.bactive.database.DatabaseConstants.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class bActiveData extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "bActive.db";
    private static final int DATABASE_VERSION = 3;

    public bActiveData(Context context)
    {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
    	// step table
		db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
			+ DATE + " TEXT NOT NULL," 
			+ STEPS + " INTEGER,"
			+ SENT + " INTEGER);");
		
		// usage stats table
		db.execSQL("CREATE TABLE " + US_TABLE_NAME + " (" + US_TIME_IN + " INT, " 
				+ US_TIME_OUT + " INT, " + US_SENT + " INTEGER);");
		
		// past-week cache table
		db.execSQL("CREATE TABLE " + CACHE_TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
				+ DATE + " TEXT NOT NULL," 
				+ STEPS + " INTEGER,"
				+ GROUP_MIN + " REAL," 
				+ GROUP_MAX + " REAL," 
				+ GROUP_TOP + " REAL," 
				+ GROUP_AVG + " REAL);");
		
		// dummy data for screenshots
		String s = "INSERT INTO " + TABLE_NAME + "(" + DATE + "," + STEPS + "," + SENT + ") VALUES(";
		db.execSQL(s + "'2011-09-11',1736,0);");
		db.execSQL(s + "'2011-09-10',1931,0);");
		db.execSQL(s + "'2011-09-09',82,0);");
		db.execSQL(s + "'2011-09-08',1721,0);");
		db.execSQL(s + "'2011-09-07',4602,0);");
		db.execSQL(s + "'2011-09-06',3321,0);");
		db.execSQL(s + "'2011-09-05',122,0);");
	}
    

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion,int newVersion) 
    { 
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    	db.execSQL("DROP TABLE IF EXISTS " + US_TABLE_NAME);
    	db.execSQL("DROP TABLE IF EXISTS " + CACHE_TABLE_NAME);
    	db.execSQL("DROP TABLE IF EXISTS " + ATC_TABLE_NAME);
    	onCreate(db);
    }
}
