package org.swanseacharm.bactive;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.*;
import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;

/**
 * Used to catch uncaught exceptions globally and log them to the SD card
 * @author Simon Walton
 *
 */
public class GlobalExceptionHandler implements UncaughtExceptionHandler {
	private UncaughtExceptionHandler defaultUEH;
    private String localPath = "/sdcard";
    private Context context;
    private static PendingIntent pendingIntent = null;

    public GlobalExceptionHandler(Activity a) {
        this.localPath = localPath;
        this.context = a;
        pendingIntent = PendingIntent.getActivity(a.getBaseContext(), 0, new Intent(a.getIntent()), a.getIntent().getFlags());
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    public GlobalExceptionHandler() {
        this.localPath = localPath;
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {
    	String timeStr = (String) DateFormat.format(DateFormat.HOUR + ":" + DateFormat.MINUTE + ":" + DateFormat.SECONDS,new Date());
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        String filename = ".stacktrace.txt";
        writeToFile(stacktrace, filename);
        
        if(pendingIntent != null) {
	        // restart the app using the PendingIntent
	        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, pendingIntent);
        }
        
        defaultUEH.uncaughtException(t, e);
    }

    private void writeToFile(String stacktrace, String filename) {
        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(
                    localPath + "/" + filename));
            bos.write(stacktrace);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
