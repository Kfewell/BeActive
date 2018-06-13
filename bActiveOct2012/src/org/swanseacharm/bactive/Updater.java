package org.swanseacharm.bactive;

import java.io.File;
import java.io.FileOutputStream;
import java.net.ResponseCache;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.swanseacharm.bactive.ui.UpdateActivity;
import org.swanseacharm.receivers.UpdateReceiver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

public class Updater {
	public static final String SHARED_PREFS_NAME = "bActiveUpdate";
	private static final String mUpdateDir = "/sdcard/";
	private static final String mUpdateFilename = "bActiveUpdate.apk";
	private static final long mDismissPeriod = 1000;//3600000*6; // six hours

	/**
	 * checks the web API for latest version and downloads/initiates installation if necessary
	 * @param context
	 */
	public static void check(Context context) {				
		String APKURL = WebServiceProxy.getUpdateAPKURL(context,getVersionCode(context));
		
		// update available? (based on server vs client versions)
		if(APKURL != "") {			
			// first check if this update has already downloaded and exists on the SD card	 
			if(isDownloaded(context))
				// yes! just prompt if they want to install now
				prompt(context);
			else
				// no; download it to the SD card
				downloadAPK(context, APKURL);
		}
		else {
			// there's no update available
			if(isDownloaded(context)) {
				// but there is an .apk on the SD card! delete it
				file().delete();
			}
		}
	}
	
	/**
	 * Downloads update APK to SD card
	 * @param context
	 * @param APKURL URL of update APK (HTTP)
	 */
	public static void downloadAPK(final Context context, final String APKURL) {
		Log.v("CHARM","Downloading update " + APKURL);
		new Thread() {
			@Override 
			public void run () 
			{
				HttpClient httpClient = new DefaultHttpClient();  
				HttpGet httpGet = new HttpGet(APKURL); 
				try {
					 HttpResponse response = httpClient.execute(httpGet);
					 try {
						 writeToSDCard(response);
					 }
					 catch(Exception e) {}
					 
					 // send a message to this class to check() again
					 context.sendBroadcast(new Intent(context,UpdateReceiver.class));			 
				} catch (Exception e) { }
			}
		}.start();
	}
	
	/**
	 * gives the integer version code contained in AndroidManifest.xml
	 * @param context
	 * @return
	 */
	public static int getVersionCode(Context context) {
	   PackageManager pm = context.getPackageManager();
	   try {
	      PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
	      return pi.versionCode;
	   } catch (NameNotFoundException ex) {}
	   return 0;
	}
	
	/**
	 * writes update HTTPResponse to the sdcard 
	 * @param response
	 */
	private static void writeToSDCard(HttpResponse response) throws Exception {
		try {
			file().delete();
		}
		catch(Exception e) {}
		
		// only write if HTTP OK
		if(response.getStatusLine().getStatusCode() == 200) {
			FileOutputStream fos =  new FileOutputStream(new File(mUpdateDir,mUpdateFilename));
			response.getEntity().writeTo(fos);
			fos.flush();
		}
	}
		
	/**
	 * marks the shared preferences to denote that an update .APK has been downloaded
	 * @param context
	 
	private static void markAsDownloaded(Context context, boolean downloaded) {
		SharedPreferences.Editor prefs = context.getSharedPreferences(SHARED_PREFS_NAME, 0).edit();
		prefs.putBoolean("APKDownloaded", downloaded);
		prefs.commit();
	}*/
	
	/**
	 * checks whether the update mechanism has downloaded an update .APK
	 * @param context
	 * @return
	 */
	public static boolean isDownloaded(Context context) {
		return file().exists();
	}
	
	/**
	 * returns File object for downloaded update APK
	 */
	private static File file() {
		return new File(mUpdateDir,mUpdateFilename);
	}

	/**
	 * launches any downloaded update .APK
	 * @param context
	 */
	public static void install(Context context) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(mUpdateDir + mUpdateFilename)), "application/vnd.android.package-archive");
		context.startActivity(intent);
	}

	public static void prompt(final Context c) {	
		if(Updater.isDownloaded(c) && canShowPrompt(c)) {
			Intent i = new Intent(c,UpdateActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			c.startActivity(i); 
		}
		
	}
	
	private static boolean canShowPrompt(Context context)
	{
		Date dismissed = new Date();
		dismissed.setTime(context.getSharedPreferences(SHARED_PREFS_NAME, 0).getLong("timeDismissed", 0));
		Date now = new Date();
		
		return now.getTime() - dismissed.getTime() > mDismissPeriod;
	}
	
	public static void setPromptDismissedTime(Context context)
	{
		SharedPreferences.Editor prefs = context.getSharedPreferences(SHARED_PREFS_NAME, 0).edit();
	    prefs.putLong("timeDismissed", new Date().getTime());
		prefs.commit();
	}

	/*@Override
	public void onReceive(Context context, Intent intent) {
		check(context);		
	}*/
}
