package org.swanseacharm.receivers;

import org.swanseacharm.bactive.GlobalExceptionHandler;
import org.swanseacharm.bactive.WebServiceProxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StepSyncReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, Intent intent) {
		new Thread(new Runnable() {
		    public void run() {
		    	Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
				(new WebServiceProxy()).sendTodayAndUnsentData(context);
		    }
		  }).start();
	}
}