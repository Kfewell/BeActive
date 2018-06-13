package org.swanseacharm.bactive;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.swanseacharm.bactive.ui.SingleDay;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * 'Tame' Toasts: toasts that set a timer to ensure that other toasts are not 
 * allowed to be shown for a given delay. Prevents screen-stabbing from queuing
 * lots of toasts. 
 * @author Simon Walton
 */
public class TameToaster 
{
	private static Map<Context,Boolean> mTamedMap = new HashMap<Context,Boolean>();
	protected static final int TOAST_NOFIRE_DELAY = 4000;

	/**
	 * Shows a given string as a tame toast with given length (Toast.LENGTH_SHORT or LONG)
	 * @param c
	 * @param msg
	 */
	public static void showToast(final Context c, final String msg, int length) {
		if(!mTamedMap.containsKey(c) || (mTamedMap.containsKey(c) && mTamedMap.get(c))) {					
			// show the toast
			Toast t = Toast.makeText(c, msg, length);
			t.setGravity(Gravity.CENTER,0,0);
			t.show();
			
			mTamedMap.put(c,false);
			// fire a timer to reset mCanToast to true
			Timer tim = new Timer();
			tim.schedule(
					new TimerTask() {
						@Override
						public void run() {
							mTamedMap.put(c, true);
						}
					}, TOAST_NOFIRE_DELAY);
		}
	} 
	
	public static void showToast(final Context c, final String msg) {
		showToast(c,msg, Toast.LENGTH_SHORT);
	}
	
	/**
     * attaches a toast message to the onclick handler of a given View object.
     * will overwrite any existing onclick handler. also fires a timer that disables any more
     * toasts attached with this function from firing for TOAST_NOFIRE_DELAY milliseconds
     */
    public static void attachToView(final Context c, View v, final String msg) 
    {
    	attachToView(c,v,msg, Toast.LENGTH_SHORT);
    }
    
    /**
     * attaches a toast message to the onclick handler of a given View object.
     * will overwrite any existing onclick handler. also fires a timer that disables any more
     * toasts attached with this function from firing for TOAST_NOFIRE_DELAY milliseconds
     */
    public static void attachToView(final Context c, View v, final String msg, final int length) 
    {
    	if(v == null)
    		return;
    	
    	v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {		
				showToast(c,msg,length);
			}
		});
    }
}
