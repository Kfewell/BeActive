package org.swanseacharm.bactive.database;

import android.provider.BaseColumns;

public class DatabaseConstants implements BaseColumns
{
    public static final String TABLE_NAME = "bActive";
    public static final String US_TABLE_NAME = "bActiveUsageStats";
    
    // columns in the usage stats table
    public static final String US_TIME_IN = "time_in";
    public static final String US_TIME_OUT = "time_out";
    public static final String US_SENT = "sent";
    
    // columns in the main table and cache tables
    public static final String DATE = "date";
    public static final String STEPS = "steps";
    public static final String GROUP_MIN = "min";
    public static final String GROUP_MAX = "max";
    public static final String GROUP_AVG = "avg";
    public static final String GROUP_TOP = "top";
    public static final String SENT = "sent"; 
    
    // past week cache table
    public static final String CACHE_TABLE_NAME = "cache";
    
    // all-time cache table
    public static final String ATC_TABLE_NAME = "allTimeCache";
}