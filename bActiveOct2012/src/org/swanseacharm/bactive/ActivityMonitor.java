package org.swanseacharm.bactive;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import org.swanseacharm.bactive.database.DatabaseProxy;
import org.swanseacharm.bactive.ui.ActivityTunerDialog;
import org.swanseacharm.bactive.ui.Today;
import org.swanseacharm.bactive.ui.SingleDay.DataReceiver;

/**
 * Service for monitoring user activity through accelerometer
 * @author Simon Walton
 *
 */
public class ActivityMonitor extends Service
{	
    private SensorManager mSensorManager;
    private List<Sensor> mSensorList;
    private Sensor mAccSensor;
    public int mSensorDelay = SensorManager.SENSOR_DELAY_NORMAL;

    private int mUncommittedSteps = 0;    
    private static final float PEAK_MEAN_DEFAULT = 999999999;
    private float mMean = PEAK_MEAN_DEFAULT;
        
    // update frequency thresholds
    public static final int UPDATE_USER_AWAY = 100;
    public static final int UPDATE_USER_PRESENT = 10;
    private boolean mScreenOn = true;
    private boolean mForceUpdate = false;
    
    private boolean mStepInBuffer = false;
    private int mMagIdx = 0; //this is used as array list position
    private static final int MAG_ARRAY_SIZE = 100;
    private static final int KEEP_AWAKE_SAMPLE_SIZE_INITIAL = 30;
    private static final int KEEP_AWAKE_SAMPLE_SIZE_WALKING = 300;
    private float mMagVal = 0;
    private float[] mMagnitudeBuffer = new float[MAG_ARRAY_SIZE]; // array to hold magnitude values
    private float[] mMagnitudeBufferSmooth = new float[MAG_ARRAY_SIZE]; // array to hold smooth magnitude values

    private int[] mPeakBuffer = new int[MAG_ARRAY_SIZE];    
	private int mPeakCount;

    public static float MAG_LOWER_THRESHOLD_SQRT = 15.812f; 	// sqrt magnitude!
    public static float MAG_LOWER_THRESHOLD_NONSQRT = MAG_LOWER_THRESHOLD_SQRT*MAG_LOWER_THRESHOLD_SQRT; 	// non-sqrt magnitude!
    private static final float PEAK_SAFETY_THRESHOLD = 0.7f; // changed for desire S
    private static final int MOVING_AVG_WINDOW_SIZE = 5;
    private static final int MOVING_AVG = 0;
    private float mTotal = 0.0f;
    private int mCount = 0;
    
    private float alpha = 0.3f;		//used to calculate exponential moving average
    private Float oldValue;
    
    // distance of each step in miles - approx 3/4 metre
    public static final float DISTANCE_STEPS_RATIO = 0.00075f;
    public static final float CALS_STEPS_RATIO = 0.05f;
    public static float mGravitySquared = 0;
    
    // learning system
    private static final int LEARNING_MATRIX_SIZE = 100;
    private float mLearningKeys[][] = new float[LEARNING_MATRIX_SIZE][2];
    private int mLearningMatrix[][] = new int[LEARNING_MATRIX_SIZE][LEARNING_MATRIX_SIZE];
    
    // String for Broadcast intent (to send step count)
    public static final String STEP_COUNT_BROADCAST = "org.swanseacharm.bactive.step_count_broadcast";
    public static final String ACTIVITY_TUNER_BROADCAST = "org.swanseacharm.bactive.activity_tuner_broadcast";
    
    float mAx[] = new float[3]; // butterworth coefficients
    float mBy[] = new float[3]; // butterworth coefficients

    NotificationManager mNotificationManager = null;

    //sensor listeners
    SensorEventListener mSensorListeners[] = new SensorEventListener[2];   
    private int mCurrListener = 0;
    Notification notification;
    PowerManager.WakeLock wl;
	private BroadcastReceiver mDataReceiver;
	
	private static int mKeepAliveSamples = 0;
	private static int mKeepAliveSampleTarget = MAG_ARRAY_SIZE;
	private ActivityMonitorBinder mBinder = new ActivityMonitorBinder();

	/**
	 * Allow binding to initialise globals that survive the lifespan of the app 
	 * since Dalvik will destroy our static variables
	 * @author Simon Walton
	 *
	 */
	public class ActivityMonitorBinder extends Binder implements IActivityMonitor {
		public void startGlobals() {
			initGlobals();
		}
		/*public void changeNonSQRTThreshold(float val) {
			MAG_LOWER_THRESHOLD_SQRT = (float)Math.sqrt(val);
		}*/
	}
	
    @Override
    public IBinder onBind(Intent arg0)
    {
    	return mBinder;
    };

    /**
     * Create the sensor and register intent listener and database control 
     */
    @Override
    public void onCreate()
    {
		//set sensor manager
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		mSensorList = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if(mSensorList.size() > 0) 
		{
		    mAccSensor = mSensorList.get(0);
		}
	
		BroadcastReceiver mReceiver = new ScreenReceiver();
		registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_DATE_CHANGED));
		
		for(int i=0;i<2;i++) {
			 mSensorListeners[i] = new SensorEventListener() {
				public void onSensorChanged(SensorEvent event) {
					sensorChanged(event);
				}

				public void onAccuracyChanged(Sensor sensor, int accuracy) {
					//debugLog.appendLog("************** accuracy changed to " + accuracy);
				}
		    };	    
		}
		
		//setupWakelock();
		makeForegroundWithNotificationEntry();
		calculateButterworth(5,2.0f);
		fillLearningMatrix();
  		
		// register data receiver for signal from AlarmReceiver & tuner
		IntentFilter filter = new IntentFilter();
		filter.addAction(Wakeful.BEGIN_MONITORING_BROADCAST);
		filter.addAction(ActivityMonitor.ACTIVITY_TUNER_BROADCAST);
		mDataReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent i) {
				if(i.filterEquals(new Intent(Wakeful.BEGIN_MONITORING_BROADCAST)))
					startMeasuringForKeepAlive();
				else if (i.filterEquals(new Intent(ActivityMonitor.ACTIVITY_TUNER_BROADCAST))) {
					/**
					 * messy shit. but a last-minute hack.
					 */
					if(Globals.activityTunerActive()) {
						MAG_LOWER_THRESHOLD_NONSQRT = (float)i.getDoubleExtra("threshold", ActivityTunerDialog.defaultThreshold());
						MAG_LOWER_THRESHOLD_SQRT = (float)Math.sqrt(MAG_LOWER_THRESHOLD_NONSQRT);
						debug_zeroSteps();
						broadcastDataUpdate();
						Log.d("CHARM","Got threshold " + MAG_LOWER_THRESHOLD_SQRT);
					}
				}
			}
		};
		registerReceiver(mDataReceiver,filter);
		
		//mAlarm = new AlarmReceiver(this);
		//Wakeful.init(this);
		
		mGravitySquared = mSensorManager.GRAVITY_EARTH*mSensorManager.GRAVITY_EARTH; 
    }
    
    /**
     * use this service to maintain app globals too since it runs for lifetime
     */
    protected void initGlobals() 
    {
    	Globals.init();
    	Wakeful.init(this.getApplicationContext());
    }
    
    /**
     * initialises learning matrix
     */
    protected void fillLearningMatrix()
    {
    	if(Globals.debugMode()){
    		return;
    	}
    	
    	// first, the lower threshold
    	float t = 100.0f;	//threshold
    	float tInc = 200.0f / (float)LEARNING_MATRIX_SIZE;	//how much to increment t by
    	for(int i=0;i<LEARNING_MATRIX_SIZE;i++) {
    		mLearningKeys[i][0] = t;
    		t += tInc;
    	}
    	
    	// now the mean peak safety threshold
    	t = 0.4f;	//safety threshold
    	tInc = 2.0f / (float)LEARNING_MATRIX_SIZE;	//how much to increment t by
    	for(int i=0;i<LEARNING_MATRIX_SIZE;i++) {
    		mLearningKeys[i][1] = t;
    		t += tInc;
    	}
    	
    	// reset the matrix
    	for(int i=0;i<LEARNING_MATRIX_SIZE;i++) {
    		for(int j=0;j<LEARNING_MATRIX_SIZE;j++) {
    			mLearningMatrix[i][j] = 0;
    		}
    	}
    }
    
    /**
     * outputs the result of learning phase to a text file
     */
    protected void outputLearningMatrix() 
    {
       	if(!Globals.debugMode()){            
       		return;
       	}
    		
       	
    	BufferedOutputStream logWriter = null;
    	File logFile = new File(Environment.getExternalStorageDirectory() + "/matrix-" + (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")).format(new Date()) + ".txt");
    	try { 
    		logWriter = new BufferedOutputStream(new FileOutputStream(logFile));
    	}
    	catch(FileNotFoundException e) {
    		Log.d("CHARM","Couldn't open log file");
    	}
    	finally {
    		
    		try {
    			String out = "";
    			
    			// first row
	    		for(int i=0;i<LEARNING_MATRIX_SIZE;i++) {
	    			out = mLearningKeys[i][0] + ",";
	    			logWriter.write(out.getBytes());
	    		}
	    		
	    		out = "\n";
	    		logWriter.write(out.getBytes());
	    		
	    		// subsequent rows
	    		for(int i=0;i<LEARNING_MATRIX_SIZE;i++) {
	    			out = mLearningKeys[i][1] + ",";
	    				    			
	        		for(int j=0;j<LEARNING_MATRIX_SIZE;j++) {
	        			out += mLearningMatrix[j][i] + ",";						
	        		}
	        		
	        		out += "\n";
	        		logWriter.write(out.getBytes());
	        	}
    		
    
    		logWriter.close();
    	
    		} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	fillLearningMatrix();
    }

    /**
     * re-registers the sensor listener for the current sensor delay
     * ensures that at least one listener is attached by swapping two listeners around
     */
    protected void reregisterListener()
    {
    	// register the new listener
    	int newListener = (mCurrListener+1) % 2;
    	Log.d("CHARM","Setting newListener to (" + (mCurrListener+1) + " mod 2): " + newListener);
    	
		if(mSensorManager.registerListener(mSensorListeners[newListener], mAccSensor, mSensorDelay))
			Log.d("CHARM","...success registering listener " + newListener);
		else
			Log.d("CHARM","...registerListener returned false for listener " + newListener);
    	
		unregisterCurrListener();
	
		mCurrListener = newListener;
    }
    
    protected void unregisterCurrListener()
    {
    	Log.d("CHARM","Unregistering " + mCurrListener + "...");
 		mSensorManager.unregisterListener(mSensorListeners[mCurrListener]);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
		// register the sensor listener
		mSensorManager.registerListener(mSensorListeners[mCurrListener], mAccSensor, mSensorDelay);
	
		// start sticky, want service to be restarted if killed
		return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
		// unregister sensor listener
		mSensorManager.unregisterListener(mSensorListeners[mCurrListener]);
	
		super.onDestroy();
		
		Log.d("CHARM","onDestroy()");
    }
    
    /**
     * start measuring user's steps to decide whether or not to stay alive
     * essentially a no-op if the phone is already awake (i.e. user is walking)
     */
    private void startMeasuringForKeepAlive() {
    	// are we already holding the wake lock?
    	if(Wakeful.isHoldingWakeLock()) {
    		DebugLog.appendLog("Was holding the wakelock; abandoning...");
    		return;
    	}
    	else DebugLog.appendLog("Wasn't holding wakelock");
    		
    	// no? ok, keep the phone awake by obtaining it
    	Wakeful.obtainWakeLock(this.getApplicationContext());
    	
    	mKeepAliveSampleTarget = KEEP_AWAKE_SAMPLE_SIZE_INITIAL;
    	mKeepAliveSamples = 0;
    	DebugLog.appendLog("Starting measuring for keep alive...");
    	
    	if(mSensorDelay != SensorManager.SENSOR_DELAY_NORMAL) {
    		mSensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
    		reregisterListener();
    	}
    	
    	// begin filling the buffer again from scratch
    	mMagIdx = 0;
    	mStepInBuffer = false;
    }
    
    /**
     * gives necessary value from MOVING_AVG_WINDOW_SIZE positions ago in the magnitude array to compute moving avg
    */
    //TO BE CONTINUED
    protected float calcMovingAvgValue(){
	return 0.0f;
    }
    	
    protected double getAverage() {
        return mTotal / mCount;
    }
    
    protected void add(float x) {
    	/**Queue<Float> list = new LinkedList<Float>();
    	mTotal += x;
    	list.add(x);
    	if(list.size() > 50){
    		mTotal -= 
    	}
    	else{
    		mCount++;
    	}
    	
    	*/
    }
    
    
    /**
     * currently unimplemented feature. This will be a moving average
     * to improve the step count algorithm.
     * @param event
     */
    private void ExponentialMovingAverage(float alpha) {
        this.alpha = alpha;
    }

    private float average(float value) {
        if (oldValue == null) {
            oldValue = value;
            return value;
        }
        float newValue = oldValue + alpha * (value - oldValue);
        oldValue = newValue;
        return newValue;
    }

    
    /**
     * sensor change callback
     * fills buffer with sensor magnitude readings and calls step measurement function when full, providing
     * a reading 'big' enough is within the buffer
     * @param event
     */
    protected void sensorChanged(SensorEvent event)
    {
    	// only process accel readings
    	if(event.sensor == mAccSensor) {
    		// compute non-sqrt magnitude and store (minus gravity)
    		mMagVal = (event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2]) - mGravitySquared;
    		if(mMagVal < 0)
    			mMagVal = -mMagVal;	//if mMagVal is negative, make positive
    		
    		// add to buffer
    		mMagnitudeBuffer[mMagIdx++] = mMagVal;//0.2f*(mMagVal-mMagnitudeBufferRaw[mMagIdxRaw]);
 //   		mMagnitudeBufferRaw[mMagIdxRaw++] = mMagVal;

    //		Log.d("CHARM","Raw: " + mMagVal + "\t" + mMagnitudeBuffer[mMagIdx-1]);
    		
   // 		if(mMagIdxRaw == MOVING_AVG_WINDOW_SIZE)
    //			mMagIdxRaw = 0;
    		
    		// if the value is big enough, then this buffer should be analysed when full
    		if(mMagVal > MAG_LOWER_THRESHOLD_NONSQRT*0.8) {
    			// is this the first step detected in the current buffer?
    			if(!mStepInBuffer) {
    				// make sure the sensor delay is minimal (fast readings from now on)
    		    	if(mSensorDelay != SensorManager.SENSOR_DELAY_FASTEST) {
    		    		mSensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
    		    		reregisterListener();
    		    	}
    		    	
    		    	mStepInBuffer = true;
    		    	
    		    	DebugLog.appendLog("***** Keeping alive for longer!");
    		    	// increase the keep-alive sample target
    		    	mKeepAliveSampleTarget = KEEP_AWAKE_SAMPLE_SIZE_WALKING;
    		    	// reset the sample count to zero
    		    	mKeepAliveSamples = 0;
    			}
    		}
    		else {
    			// this value wasn't big enough so add to mKeepAliveSamples
    			if(++mKeepAliveSamples == mKeepAliveSampleTarget) {
    				// calm the sensor down ready for sleep 
    				if(mSensorDelay != SensorManager.SENSOR_DELAY_NORMAL) {
    		    		mSensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
    		    		reregisterListener();
    		    	}
    				
    				// the phone may now sleep
    				Wakeful.youMaySleep();
    			}
    		}
    		
    		// is the buffer full?
    		if(mMagIdx == MAG_ARRAY_SIZE) {
    			// if we picked up on likely steps, then call measureSteps
    			// if the step in buffer has been set to true, or the screen
    			// is on, we call measureSteps()
    			if(mStepInBuffer || mForceUpdate) {
    	    		DebugLog.appendLog("Step in buffer or forced update");
    				measureSteps();
    				mStepInBuffer = false;
    				mForceUpdate = false;
    				
    				// reset the 'keep alive' sample count to zero
    		    	mKeepAliveSamples = 0;
    			}
    			else {
    				// else make sure sensor delay is normal (slower readings - more battery life)
    				if(mSensorDelay != SensorManager.SENSOR_DELAY_NORMAL) {
    	        		mSensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
    	        		reregisterListener();
    	        	}
    				
    	    		DebugLog.appendLog("No steps in buffer.");
    			}
    			mMagIdx = 0;
    		}
    	}
    }
    
	    
    /**
     * precompute Butterworth filter coefficients
     * @param samplerate
     * @param cutoff
     */
    private void calculateButterworth(int samplerate, float cutoff)
    {
    	float PI      = 3.1415926535897932385f;
    	float sqrt2 = 1.4142135623730950488f;

    	float QcRaw  = (2 * PI * cutoff) / samplerate; // Find cutoff frequency in [0..PI]
    	float QcWarp = (float) Math.tan(QcRaw); // Warp cutoff frequency

    	float gain = 1 / (1+sqrt2/QcWarp + 2/(QcWarp*QcWarp));
        mBy[2] = (1 - sqrt2/QcWarp + 2/(QcWarp*QcWarp)) * gain;
        mBy[1] = (2 - 2 * 2/(QcWarp*QcWarp)) * gain;
        mBy[0] = 1;
        mAx[0] = 1 * gain;
        mAx[1] = 2 * gain;
        mAx[2] = 1 * gain;
    }
    

    
    /**
	 * steps measurement function; called when the magnitude buffer fills up
	 */
    public void measureSteps()
    {      
    	int i = 0;
    	
    	// peak mean
    	mMean = 0.0f;
    	mPeakCount = 0;
    	
    	//mMagnitudeBufferSmooth[i]= mMagnitudeBuffer[i-1] + (0.1f*(mMagnitudeBuffer[i]-mMagnitudeBuffer[i-1]))
    	//low pass filter
    	//mMagnitudeBufferSmooth[1] = mMagnitudeBuffer[0] + (0.1f*(mMagnitudeBuffer[1]-mMagnitudeBuffer[0]));    	
    	mMagnitudeBufferSmooth[0] = average(mMagnitudeBuffer[0]);
    	mMagnitudeBufferSmooth[1] = average(mMagnitudeBuffer[1]);
    	// mean peak calculation
    	for(i=2;i<MAG_ARRAY_SIZE;i++) {
    		// low-pass filter
    		Log.d("CHARM","Before filtering " + mMagnitudeBuffer[i]);
    		mMagnitudeBufferSmooth[i] = average(mMagnitudeBuffer[i]);
    		Log.d("CHARM","After filtering " + mMagnitudeBufferSmooth[i]);
    		
    		if(mMagnitudeBufferSmooth[i-1] > mMagnitudeBufferSmooth[i-2] && mMagnitudeBufferSmooth[i-1] > mMagnitudeBufferSmooth[i]) {
    			// keep track of the mean sum
    			mMean += mMagnitudeBufferSmooth[i-1];
    			// add peak to peak buffer for analysis later
    			mPeakBuffer[mPeakCount++] = i-1;
    		}
    	}
    	
    	mMean /= (float)mPeakCount;
    	
    	DebugLog.appendLog("Peaks in buffer: " + mPeakCount);
    	
    	if(Globals.debugMode()) {
    		// go through the values in the learning matrix and try the sample against each
    		int pIdx = 0;
    		for(int m=0;m<mPeakCount;m++) {
    			pIdx = mPeakBuffer[m];
	    		for(i=0;i<LEARNING_MATRIX_SIZE;i++) {
	    			for(int j=0;j<LEARNING_MATRIX_SIZE;j++) {
	    				//if the peak found is greater than the corresponding threshold
	    				//and greater than the mean multiplied by safety constant
	    				//then increment mLearningMatrix
	    				if(mMagnitudeBufferSmooth[pIdx] > mLearningKeys[i][0] && mMagnitudeBufferSmooth[pIdx] > mMean * mLearningKeys[j][1]) {
	    					mLearningMatrix[i][j]++;
	    	    		}  
	    			}
	    		}
    		}
    	}
    	
		// final peak analysis using peak indices in peak buffer
		int pIdx = 0;
    	for(i=0;i<mPeakCount;i++) {
    		pIdx = mPeakBuffer[i];
    		//if magnitude buffer at peak exceeds mean multiplied by a safety threshold and 
    		//the mag. buffer is greater than a preset lower threshold, then increment
    		//uncommitted steps.
    		if(mMagnitudeBufferSmooth[pIdx] > mMean * PEAK_SAFETY_THRESHOLD && mMagnitudeBufferSmooth[pIdx] > MAG_LOWER_THRESHOLD_NONSQRT) {
    			mUncommittedSteps++;
    		}    		
    	} 
    	
    	DebugLog.appendLog("mUncommittedSteps: " + mUncommittedSteps);
    	
    	// do we need to write to the database?
    	if(mUncommittedSteps != 0) {
    		DebugLog.appendLog("Committing " + mUncommittedSteps + " steps to database.");
			addActivityCountToDatabase();
			
			// do we need to inform the UI?
			if(mScreenOn || mForceUpdate)
				broadcastDataUpdate();
    	}	
    }
    
    /**
     * Adds the current activity count to the activity record for today
     * @author Simon Walton
     */
    public void addActivityCountToDatabase() 
    {
		DatabaseProxy dbProxy = new DatabaseProxy(this);
		ActivityRecord ar = dbProxy.getActivityRecordByDate(DateUtil.today());		
		ar.setMe(ar.getMe() + mUncommittedSteps);
		ar.setDate(DateUtil.today());
		dbProxy.updateActivityRecord(ar);
		
		mUncommittedSteps = 0;
    }
    
    /**
     * resets count to zero (only used for debugging)
     */
    private void debug_zeroSteps() 
    {
    	DatabaseProxy dbProxy = new DatabaseProxy(this);
		ActivityRecord ar = dbProxy.getActivityRecordByDate(DateUtil.today());		
		ar.setMe(0);
		ar.setDate(DateUtil.today());
		dbProxy.updateActivityRecord(ar);
		
		mUncommittedSteps = 0;
    }
    
    /**
     * foreground activity stuff for activity monitor
     */
    
    private static final Class<?>[] mStartForegroundSignature = new Class[] {
        int.class, Notification.class};
    private static final Class<?>[] mStopForegroundSignature = new Class[] {
        boolean.class};
    private Method mStartForeground;
    private Method mStopForeground;
	private boolean mDateChanged = false;
    
    /*
     * makes the step counter service a foreground service (to avoid being killed by Android)
     * and creates a permanent notification in the status bar
     */
    void makeForegroundWithNotificationEntry() {
    	mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.bactivelogo_statusbar, 
				Globals.isControlGroup() || !Globals.withinStudyPeriod() ? "bActive" :"bActive is counting your steps!", System.currentTimeMillis());
		
	    try {
	        mStartForeground = getClass().getMethod("startForeground",
	        mStartForegroundSignature);
	        mStopForeground = getClass().getMethod("stopForeground",
	            mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
		    // Running on an older platform.
		    mStartForeground = mStopForeground = null;
		    return;
		}
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "bActive is running";
		CharSequence contentText = Globals.isControlGroup() || !Globals.withinStudyPeriod() ? "" : "Click to check how you're doing!";
		Intent notificationIntent = new Intent(this, Today.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.flags |= Notification.FLAG_NO_CLEAR;
		
		if (mStartForeground != null) {
			Object[] args = new Object[2];
			args[0] = Integer.valueOf(1);
			args[1] = notification;
	        invokeMethod(mStartForeground, args);
	    }
		else {
			mNotificationManager.notify(1, notification);
		}
    }
    
    void invokeMethod(Method method, Object[] args) {
        try {
            mStartForeground.invoke(this, args);
        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.w("CHARM", "Unable to invoke method", e);
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.w("ChARM", "Unable to invoke method", e);
        }
    }


    /**
     * notifies activities of data update
     */
    public void broadcastDataUpdate()
    {
		Intent intent = new Intent(STEP_COUNT_BROADCAST);
		sendBroadcast(intent);
    }

    /**
     * receives any broadcast signals such as the screen being turned off, etc
     */
    public class ScreenReceiver extends BroadcastReceiver 
    {
		@Override
		public void onReceive(Context context, Intent intent) 
		{
		    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))// || intent.getAction().equals(Intent.ACTION_SCREEN_ON)) 
		    {
		    	reregisterListener();
				
				mScreenOn = false;
				
				if(Globals.debugMode())
					outputLearningMatrix();
		    }
		    else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				mScreenOn = true;
				mForceUpdate = true;
		    }
		   
		    DebugLog.appendLog("************ ACTION ************** " + intent.getAction()); 
		}
    };
}
