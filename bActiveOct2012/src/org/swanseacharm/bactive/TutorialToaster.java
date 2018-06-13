package org.swanseacharm.bactive;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.swanseacharm.bactive.ui.SingleDay;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * Tutorial Toaster
 * @author Simon Walton
 * Upon first use of each app screen, will show given toast messages in specified positions
 */
public class TutorialToaster
{
	private Activity mActivity = null;
	private Context mContext = null;
	private static SharedPreferences mPrefs = null;
	private static final String mPrefsName = "TutorialToaster";
	private String[] mMessages;
	private int[][] mOffsets;
	private Handler mHandler = null;
	private boolean mCancelAll = false;
	private ToastThread mThread = null;
	private ArrayList<Toast> mToastQueue = null;
	
	public TutorialToaster(Context c, Activity a) {
		mContext = c;
		mActivity = a;
		
		if(mPrefs == null)
			mPrefs = a.getSharedPreferences(mPrefsName, 0);
		
		mHandler = new Handler();
		
	}
	
	/**
	 * sets all known tutorials in app to original state ready to be shown again 
	 */
	public static void showAllAgain()
	{
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean((new org.swanseacharm.bactive.ui.Today()).getClass().getName(), false);
		editor.putBoolean((new org.swanseacharm.bactive.ui.Yesterday()).getClass().getName(), false);
		editor.putBoolean((new org.swanseacharm.bactive.ui.PastWeek()).getClass().getName(), false);
		editor.putBoolean((new org.swanseacharm.bactive.ui.History()).getClass().getName(), false);
		editor.commit();
	}
	
	/**
	 * sets the toasts to be shown for this tutorial
	 * @param An array of Strings; one for each toast
	 * @param An array of {x,y} offsets from the default toast position
	 */
	public void setToasts(String[] messages, int[][] offsets) {
		mCancelAll = false;
		
		mMessages = messages.clone();
		mOffsets = offsets.clone();
		mToastQueue = new ArrayList<Toast>();
	}
	
	/**
	 * cancels all pending toasts
	 */
	public void cancelAll() {
		mCancelAll = true;
		if(mThread != null)
			mThread.stopToasts();
	}
	
	/**
	 * shows toasts if user has never seen them before
	 */
	public void execute() {
		// have the user seen this tutorial? if so, bail		
		if(hasSeen())
			return;
		
		if(mThread != null)
			mThread.stopToasts();
		
		for(int i=0;i<mMessages.length;i++) {
			Toast t = Toast.makeText(mActivity.getApplicationContext(), mMessages[i], Toast.LENGTH_LONG);
			t.setGravity(Gravity.TOP, mOffsets[i][0], mOffsets[i][1]);
			mToastQueue.add(t);
		}
		
		mThread = new ToastThread();
		mThread.start();				
	}
	
	/**
	 * Dedicated thread for showing toasts
	 * @author Simon Walton
	 *
	 */
	private class ToastThread extends Thread 
	{
		private boolean mRunning = true;
		
		public void stopToasts() {
			mRunning = false;
		}
		
		@Override 
		public void run () 
		{
			mRunning = true;
			ArrayList<Toast> toasts = new ArrayList<Toast>();
			for(Toast t : mToastQueue) 
				toasts.add(t);
			
			try {
				for(int i=0;i<toasts.size();i++) {				
					// fire multiple times to compensate for the fact that there's no custom
					// duration for toasts
					for(int j=0;j<3;j++) {			
						// check if we should still run
						if(!mRunning)
							break;
						
						toasts.get(i).show();
						Thread.sleep(2500);
					}
					
					// user has now seen one toast; tutorial should not be shown again
					if(i == 0)
						setAsSeen();
				}
			}
			catch(Exception e) {
				//Toast.makeText(mActivity, "Problem showing toasts.", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	/**
	 * checks whether user has been shown toasts for the Activity
	 */
	public boolean hasSeen() {
		return mPrefs.getBoolean(mActivity.getClass().getName(), false);
	}
	
	/**
	 * sets the tutorial as 'seen' for the activity
	 */
	private void setAsSeen() {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean(mActivity.getClass().getName(), true);
		editor.commit();
	}
	
	/**
	 * sets the tutorial as 'unseen' for the activity
	 */
	private void setAsUnseen() {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean(mActivity.getClass().getName(), false);
		editor.commit();
	}
	
	/**
	 * Show a one-shot toast: shows the toast only once no matter how many times the function is called
	 * by setting a flag in the sender activity's SharedPreferences when the toast is shown
	 */
	public static void oneShotToast(Activity sender, String toastName, Toast t) {
		SharedPreferences sp = sender.getSharedPreferences("oneShotToasts", 0);
		if(!sp.getBoolean(toastName, false))
			t.show();
		SharedPreferences.Editor editor = sp.edit();
		editor.putBoolean(toastName, true);
		editor.commit();
	}
	
    
	
}
