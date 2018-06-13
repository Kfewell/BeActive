package org.swanseacharm.receivers;

import org.swanseacharm.bactive.GlobalExceptionHandler;
import org.swanseacharm.bactive.WebServiceProxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UsageReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, Intent intent) {
		new Thread(new Runnable() {
		    public void run() {
				Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
				(new WebServiceProxy()).sendUsageStats(context);	
		    }
		  }).start();
	}
}