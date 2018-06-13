package org.swanseacharm.receivers;

import org.swanseacharm.bactive.GlobalExceptionHandler;
import org.swanseacharm.bactive.Updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UpdateReceiver extends BroadcastReceiver {
	@Override 
	public void onReceive(final Context context, Intent intent) {
		new Thread(new Runnable() {
		    public void run() {
		    	Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
				Updater.check(context);	
		    }
		  }).start();		
	}
}