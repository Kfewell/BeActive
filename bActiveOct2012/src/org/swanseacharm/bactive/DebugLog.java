package org.swanseacharm.bactive;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * Functionality to write to a debug log on the SD card
 * @author Simon Walton
 */
public class DebugLog 
{
	private static String logStr = "";
	private static File logFile = null;
	private static BufferedOutputStream logWriter;
	private static int FLUSH_FREQ = 5;
	private static int count = 0;
	
	private static void start(boolean enabled)
	{		
		try { 
			logFile = new File(Environment.getExternalStorageDirectory() + "/debuglog.txt");
    		logWriter = new BufferedOutputStream(new FileOutputStream(logFile));
    	}
    	catch(FileNotFoundException e) {
    		Log.d("CHARM","Couldn't open log file");
    	}
    	finally {
    		Log.d("CHARM","Opened log file OK");
    	}
	}
	
	private static void stop() {
    	try {
    		logWriter.flush();
			logWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * appends string (with newline) to logfile
	 * @param str
	 */
	public static void appendLog(String str)
	{
		if(!Globals.debugMode())
			return;
		
		if(logFile == null)
    		start(true);
    	
    	String timeStr = (String) DateFormat.format(DateFormat.HOUR + ":" + DateFormat.MINUTE + ":" + DateFormat.SECONDS,new Date());
    	String out = timeStr + "," + str + "\n";
    	try {
    		logWriter.write(out.getBytes());
    		Log.d("CHARM","Sensor reading " + out);
    		
    		if(count++ % FLUSH_FREQ == 0)
    			logWriter.flush();
    	}
    	catch(Exception e) {
    		Log.d("CHARM", "Problem writing to log file.");
    	}
    }
}
