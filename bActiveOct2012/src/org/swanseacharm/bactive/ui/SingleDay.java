
package org.swanseacharm.bactive.ui;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.swanseacharm.bactive.ActivityMonitor;
import org.swanseacharm.bactive.ActivityRecord;
import org.swanseacharm.bactive.DateUtil;
import org.swanseacharm.bactive.FrugalityProxy;
import org.swanseacharm.bactive.Globals;
import org.swanseacharm.bactive.R;
import org.swanseacharm.bactive.TameToaster;
import org.swanseacharm.bactive.TutorialToaster;
import org.swanseacharm.bactive.WebServiceProxy;
import org.swanseacharm.bactive.R.anim;
import org.swanseacharm.bactive.R.drawable;
import org.swanseacharm.bactive.R.id;
import org.swanseacharm.bactive.R.layout;
import org.swanseacharm.bactive.R.menu;
import org.swanseacharm.bactive.R.string;
import org.swanseacharm.bactive.database.DatabaseProxy;
import org.swanseacharm.bactive.database.UsageProxy;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity that represents a single day (parent of Today and Yesterday screens)
 * @author Simon Walton
 */
public class SingleDay extends Activity implements OnClickListener 
{
    private int mSteps = 0;
    private int mGroupStepsValue = 0;
    private int mGroupTopSteps = 0;

    //constants for position relative to group
    private final int BELOW_AVERAGE = 0;
    private final int BELOW_MIDPOINT = 1;
    private final int BELOW_TOP = 2;
    private final int ABOVE_TOP = 3;
    private final int NO_DATA = 4;

    private Animation mTextBarAnimation;
    private Animation mTextBarAltAnimation;

    //Receiver - listens for broadcasts from services
    private DataReceiver mDataReceiver;

    //TitleBar text views
    private TextView mTitleText;
    private TextView mAltTitleText;
    private LinearLayout mAltTitleLayout;
    private ImageView mSmiley1;
    private ImageView mSmiley2;
    private ImageView mSmiley3;
    private boolean mTitleAnimationRunning = false;

    //table text views
    protected TextView mYouSteps;
    protected TextView mYouCalories;
    protected TextView mYouDistance;
    protected TextView mGroupSteps;
    protected TextView mGroupCalories;
    protected TextView mGroupDistance;

    protected WakeLock wl;

    //tab bar buttons
    private Button mToday, mYesterday;
    private Button mLastWeek;
    private Button mHistory;

    private SharedPreferences mPrefs;
    protected String mPrefsName;
    
    private Handler mHandler;
    
    // which day does this view represent?
    protected Calendar mDay;
    protected int mGroupProxyDataId;
    
    // animation resources to use
	protected int mYouBelowAverageAnim;
	protected int mYouBelowMidPointAnim;
	protected int mYouBelowTopAnim;
	protected int mYouAboveTopAnim;
	protected int mGroupAnim;
	protected int mBGAnimYou, mBGAnimGroup;
	
	// string resources
	protected int mTitleNoFeedback;
	protected int mTitleNoData;
	protected int mTitleBelowAverage;
	protected int mTitleBelowMidPoint;
	protected int mTitleBelowTop20;
	protected int mTitleAboveTop20;
	
	// delay before being allowed to fire more toasts after displaying a toast
	protected Timer mToastTimer = null;
	protected boolean mCanToast = true; 
	

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
		super.onCreate(savedInstanceState);
		
		if(Globals.groupFeedback())
			setContentView(org.swanseacharm.bactive.R.layout.today_group);
		else
			setContentView(org.swanseacharm.bactive.R.layout.today_individual);
		
		//setContentView(demo.bActive.group3.R.layout.today);
		
		//set up title text animations
		mTitleText = (TextView)findViewById(R.id.title_bar_title);
		mAltTitleText= (TextView)findViewById(R.id.title_bar_alt_title);
		mAltTitleLayout = (LinearLayout)findViewById(R.id.title_bar_alt_layout);
		mSmiley1 = (ImageView) findViewById(R.id.smiley_icon1);
		mSmiley2 = (ImageView) findViewById(R.id.smiley_icon2);
		mSmiley3 = (ImageView) findViewById(R.id.smiley_icon3);
		mTextBarAnimation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.title_bar_text_slide);
		mTextBarAltAnimation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.title_bar_text_slide);
		
		// init table text views
		mYouSteps = (TextView)findViewById(org.swanseacharm.bactive.R.id.todayYouStepCount);
		mYouCalories = (TextView)findViewById(org.swanseacharm.bactive.R.id.todayYouCaloriesCount);       
		mYouDistance = (TextView)findViewById(org.swanseacharm.bactive.R.id.todayYouDistanceCount);
		mGroupSteps = (TextView)findViewById(org.swanseacharm.bactive.R.id.todayGroupStepCount);
		mGroupCalories = (TextView)findViewById(org.swanseacharm.bactive.R.id.todayGroupCaloriesCount);  
		mGroupDistance = (TextView)findViewById(org.swanseacharm.bactive.R.id.todayGroupDistanceCount);  
		
		// animation defines
		mYouBelowAverageAnim = org.swanseacharm.bactive.R.id.todayYouBelowAverageAnim;
		mYouBelowMidPointAnim = org.swanseacharm.bactive.R.id.todayYouBelowMidPointAnim;
		mYouBelowTopAnim = org.swanseacharm.bactive.R.id.todayYouBelowTopAnim;
		mYouAboveTopAnim = org.swanseacharm.bactive.R.id.todayYouAboveTopAnim;
		mGroupAnim = org.swanseacharm.bactive.R.id.todayGroupAnim;
		mBGAnimYou = org.swanseacharm.bactive.R.id.todayYouAnimLayout;
		mBGAnimGroup = org.swanseacharm.bactive.R.id.todayGroupAnimLayout;
	
		// set up animation listeners
		mTextBarAnimation.setAnimationListener(new AnimationListener() 
		{
		    @Override
		    public void onAnimationStart(Animation arg0)  
		    {
		    	mTitleText.setVisibility(View.VISIBLE);
		    }
	
		    @Override
		    public void onAnimationRepeat(Animation arg0) {}
	
		    @Override
		    public void onAnimationEnd(Animation a)
		    {
				mTitleText.setVisibility(View.INVISIBLE);
				if(mTitleAnimationRunning) 
					mAltTitleLayout.startAnimation(mTextBarAltAnimation);
		    }
		});
	
		mTextBarAltAnimation.setAnimationListener(new AnimationListener() 
		{
		    @Override
		    public void onAnimationStart(Animation arg0) 
		    {
		    	mAltTitleLayout.setVisibility(View.VISIBLE);
		    }
	
		    @Override
		    public void onAnimationRepeat(Animation arg0) {}
			    @Override
			    public void onAnimationEnd(Animation arg0) 
			    {
			    	mAltTitleLayout.setVisibility(View.INVISIBLE);
			    	if(mTitleAnimationRunning)
			    		mTitleText.startAnimation(mTextBarAnimation);
			    }
		});
		
		/*
		 *  init tab bar buttons
		 */
		mToday = (Button) findViewById(org.swanseacharm.bactive.R.id.todayTab);
		mToday.setOnClickListener(this);
		mYesterday = (Button) findViewById(org.swanseacharm.bactive.R.id.yesterdayTab);
		mYesterday.setOnClickListener(this);
		mLastWeek = (Button) findViewById(org.swanseacharm.bactive.R.id.lastWeekTab);
		mLastWeek.setOnClickListener(this);
		mHistory = (Button) findViewById(org.swanseacharm.bactive.R.id.historyTab);
		mHistory.setOnClickListener(this);
		
		/*
		 *  helpful tooltips
		 */
		TameToaster.attachToView(this,findViewById(org.swanseacharm.bactive.R.id.you_header_image),
				"The numbers below the green figure represent your activity.", Toast.LENGTH_LONG);
				
		if(Globals.groupFeedback()) {
			TameToaster.attachToView(this,findViewById(org.swanseacharm.bactive.R.id.todayYouAnimLayout),
					"This is you. Your position will change depending on your progress relative to the group (above).", Toast.LENGTH_LONG);
			TameToaster.attachToView(this,findViewById(org.swanseacharm.bactive.R.id.group_header_image),
				"The numbers below the three grey figures represent the average of everyone's activity.", Toast.LENGTH_LONG);
			TameToaster.attachToView(this,findViewById(org.swanseacharm.bactive.R.id.todayGroupAnimLayout),
				"This is the group. Their position does not change.", Toast.LENGTH_LONG);
		}
			
		/*
		 * selected tab button style
		 */
		Button selectedTab = DateUtil.timelessComparison(mDay, DateUtil.today()) == 0 ? mToday : mYesterday;
		selectedTab.setTextColor(Color.BLACK);
		selectedTab.setBackgroundDrawable(getResources().getDrawable(org.swanseacharm.bactive.R.drawable.tab_item_selected));
		
		
		
		mHandler = new Handler();
    }
    
    /**
     * shows/disables the spinning progress bar for group data refresh
     */
    private void showProgress(boolean show) {
    	if(Globals.groupFeedback())
    		findViewById(org.swanseacharm.bactive.R.id.dayProgressBar).setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }
    
    /**
     * sets which calendar day this view represents
     * @param day
     */
    protected void setDay(Calendar day)
    {
    	this.mDay = day;
    	mGroupProxyDataId = DateUtil.timelessComparison(mDay,DateUtil.today()) == 0 ? FrugalityProxy.GROUP_AVERAGE_TODAY : FrugalityProxy.GROUP_AVERAGE_YESTERDAY;
    }
    
    /**
     * loads persistent data from shared preferences file to save downloading again
     */
    private void loadPersistentData()
    {
    	if(!Globals.groupFeedback())
    		return;
    	
    	try {
	        mPrefs = getSharedPreferences(mPrefsName, 0);
	        Date d = new Date();
	        d.setTime(mPrefs.getLong("date", new Date().getTime()));
	        
	        mGroupStepsValue = mPrefs.getInt("mGroupStepsValue", 0);
	        mGroupSteps.setText(String.valueOf(mGroupStepsValue));
		    mGroupCalories.setText(String.valueOf(mPrefs.getInt("mGroupCalories", 0) + " cal"));
		    mGroupDistance.setText(String.valueOf(mPrefs.getFloat("mGroupDistance", 0) + " mi"));
		    mGroupTopSteps = mPrefs.getInt("mGroupTopSteps", 0);
    	}
    	catch(NumberFormatException e) { e.printStackTrace(); }
    }
    
    /**
     * saves persistent data to shared preferences
     */
    private void savePersistentData()
    {
    	if(!Globals.groupFeedback())
    		return;
    	
    	try {
	    	SharedPreferences.Editor ed = mPrefs.edit();
	    	ed.putInt("mGroupStepsValue", mGroupStepsValue);
	    	ed.putInt("mGroupSteps", Integer.parseInt(mGroupSteps.getText().toString(),10));
	    	ed.putInt("mGroupCalories", Integer.parseInt(mGroupCalories.getText().toString().replace(" cal", ""),10));
	    	ed.putFloat("mGroupDistance", Float.parseFloat(mGroupDistance.getText().toString().replace(" mi", "")));
	    	ed.putInt("mGroupTopSteps", mGroupTopSteps);
	    	ed.putLong("date", new Date().getTime());
	    	ed.commit();
    	}
    	catch(NumberFormatException e) { e.printStackTrace(); }
    }

    /**
     *  load most recent 'me' step, calorie, distance data from internal sql lite database 
     */
    public void loadData() 
    {    
		DatabaseProxy dbProxy = new DatabaseProxy(getApplicationContext());
		ActivityRecord ar = dbProxy.getActivityRecordByDate(mDay);
	
		mSteps = (int)ar.getMe();
		mYouSteps.setText(""+ mSteps);
		mYouCalories.setText(ActivityRecord.formatCalories(mSteps));
		mYouDistance.setText(ActivityRecord.formatDistance(mSteps));
    }

    @Override
    public void onResume()
    {
		super.onResume();
		
		loadPersistentData(); 
		
		// keep track of usage stats
		UsageProxy.resume(this);
		
		// register data receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(ActivityMonitor.STEP_COUNT_BROADCAST);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		mDataReceiver = new DataReceiver();    	
		registerReceiver(mDataReceiver,filter);
	
		// need to load 'me' values from the database 
		loadData();
	
		// can't believe that this is necessary for the animation to work. poor, Google.
		mHandler.postDelayed(new AnimRunnable(),1000); 
		
		if(FrugalityProxy.shouldUseData(mGroupProxyDataId)) {
			showProgress(true);
			new DownloadGroupDataTask().execute();			
		}
    };
    
    private class AnimRunnable implements Runnable
    {
		@Override
		public void run() {
			animate();
			setTitleText();
		}
    } 

    @Override
    protected void onPause()
    {
		super.onPause();
		
		savePersistentData();
	
		mTitleAnimationRunning = false;
		mTitleText.setAnimation(null);
		mAltTitleLayout.setAnimation(null);
		mTextBarAnimation.reset();
		mTextBarAltAnimation.reset();
	
		unregisterReceiver(mDataReceiver);
		
		System.gc();
		
		// keep track of usage stats
		UsageProxy.pause(this);
    }
    
    /**
     * plays or stops the background animation(s)
     * @param play true: play; false: stop
     */
    private void playBackgroundAnim(boolean play) 
    {
    	LinearLayout mBGLayoutYou = (LinearLayout) findViewById(mBGAnimYou);	
    	AnimationDrawable BGAnimYou = (AnimationDrawable) mBGLayoutYou.getBackground();
		AnimationDrawable BGAnimGroup = null;
		LinearLayout mBGLayoutGroup = null;
		
		if(Globals.groupFeedback()) {
			mBGLayoutGroup = (LinearLayout) findViewById(mBGAnimGroup);
			BGAnimGroup = (AnimationDrawable) mBGLayoutGroup.getBackground();
			BGAnimGroup.selectDrawable(12);
			
			if(play)
				BGAnimGroup.start();
			else
				BGAnimGroup.stop();
		}
		
		if(play)
			BGAnimYou.start();
		else
			BGAnimYou.stop();
    }

    /**
     * play Animation based on animationPlacement() calculation
     */
    private void animate()
    {
		ImageView imageView = (ImageView)findViewById(mYouBelowMidPointAnim);
		AnimationDrawable youBelowMidPointAnim = (AnimationDrawable) imageView.getDrawable();
		AnimationDrawable youBelowAverageAnim = null, youBelowTopAnim = null, youAboveTopAnim = null;
		
		AnimationDrawable groupCenterAnim = null;
		
		if(Globals.groupFeedback()) {
			imageView = (ImageView)findViewById(mYouBelowAverageAnim);		
			youBelowAverageAnim = (AnimationDrawable) imageView.getDrawable();
			imageView = (ImageView)findViewById(mYouBelowTopAnim);
			youBelowTopAnim = (AnimationDrawable) imageView.getDrawable();		
			imageView = (ImageView)findViewById(mYouAboveTopAnim);
			youAboveTopAnim = (AnimationDrawable) imageView.getDrawable();	

			imageView = (ImageView)findViewById(mGroupAnim);
			groupCenterAnim = (AnimationDrawable) imageView.getDrawable();
			groupCenterAnim.start();   
			
			findViewById(mYouBelowAverageAnim).setVisibility(View.INVISIBLE);
			findViewById(mYouBelowTopAnim).setVisibility(View.INVISIBLE);
			findViewById(mYouAboveTopAnim).setVisibility(View.INVISIBLE);
			
			youBelowAverageAnim.stop();
			youBelowTopAnim.stop(); 
			youAboveTopAnim.stop();
		}
	
		int playAnimation = animationPlacement();			
		playBackgroundAnim(true);
		
		findViewById(mYouBelowMidPointAnim).setVisibility(View.INVISIBLE);
		youBelowMidPointAnim.stop();
		
		if(!Globals.groupFeedback()) {
			findViewById(mYouBelowMidPointAnim).setVisibility(View.VISIBLE);
			youBelowMidPointAnim.start();
		}
		
		// depending on step count set which animation to play
		switch (playAnimation) 
		{
		    case BELOW_AVERAGE:
				findViewById(mYouBelowAverageAnim).setVisibility(View.VISIBLE);

				//Play left animation
				youBelowAverageAnim.start();
			break;
	
		    case BELOW_MIDPOINT:
				findViewById(mYouBelowMidPointAnim).setVisibility(View.VISIBLE);

				//Play center animation
				youBelowMidPointAnim.start();    		
			break;
	
			//Individual count is > group	
		    case BELOW_TOP:
				findViewById(mYouBelowTopAnim).setVisibility(View.VISIBLE);
		
				//Play right animation
				youBelowTopAnim.start();  
			break; 
		    case ABOVE_TOP:
				findViewById(mYouAboveTopAnim).setVisibility(View.VISIBLE);
		
				//Play right animation
				youAboveTopAnim.start();
			break;
		    case NO_DATA:
		    	findViewById(mYouBelowMidPointAnim).setVisibility(View.VISIBLE);
		    	youBelowMidPointAnim.stop(); 
		    	if(groupCenterAnim != null)
		    		groupCenterAnim.stop();   	
		    	playBackgroundAnim(false);
				break;
		}
		
		// only animate if the today view is selected
		if(DateUtil.timelessComparison(DateUtil.today(),mDay) != 0) {
			if(Globals.groupFeedback() ) {
				youBelowMidPointAnim.stop();
				youBelowAverageAnim.stop();
				youBelowMidPointAnim.stop();
				youBelowTopAnim.stop(); 
				youAboveTopAnim.stop(); 
				groupCenterAnim.stop();
			}
			youBelowMidPointAnim.stop();
			
			playBackgroundAnim(false);
		}
		
		System.gc();
    }   

    /**
     * Work out which animation to play based on step count relative to group 
     * @return
     */
    public int animationPlacement() 
    {
    	if(!Globals.groupFeedback())
    		return BELOW_MIDPOINT;
    	
		int placement;
		int midPoint = mGroupStepsValue + ((mGroupTopSteps - mGroupStepsValue) / 2);
		
		if((mGroupStepsValue == 0 && mSteps == 0)
				|| (mSteps < 10 && DateUtil.timelessComparison(mDay, DateUtil.today()) == 0))
			placement = NO_DATA;
		else if(mSteps < mGroupStepsValue)
			placement = BELOW_AVERAGE;
		else if(mSteps <= midPoint)
			placement = BELOW_MIDPOINT;
		else if(mSteps <= mGroupTopSteps)
			placement = BELOW_TOP;
		else
			placement = ABOVE_TOP;
		
		return placement;
    }

    /**
     * sets and animates the title bar text depending on user's progress
     */
    private void setTitleText()
    {
		int placement = animationPlacement();
		
		mSmiley1.setImageDrawable(null);
		mSmiley2.setImageDrawable(null);
		mSmiley3.setImageDrawable(null);
		mAltTitleText.setText("");
		
		mAltTitleLayout.setVisibility(View.INVISIBLE);
		
		if(!Globals.groupFeedback()) {
			mTitleText.setText(getResources().getString(mTitleNoFeedback));
    	}
		else if(placement == BELOW_AVERAGE) 
		{
		    mTitleText.setText(getResources().getString(mTitleBelowAverage));
		    
		    // don't want to start animation, just show static text
		    mTitleText.setVisibility(View.VISIBLE);
		}
		else if(placement == BELOW_MIDPOINT) 
		{
		    mTitleText.setText(getResources().getString(mTitleBelowMidPoint));
		    mTitleAnimationRunning = true; 
		    mTitleText.startAnimation(mTextBarAnimation);
	
		    mSmiley1.setImageDrawable(getResources().getDrawable(R.drawable.smiley_icon));
		    
		}
		else if(placement == BELOW_TOP) 
		{
		    mTitleText.setText(getResources().getString(mTitleBelowTop20));
		    mAltTitleText.setText(getResources().getString(R.string.title_alt_text));
		    mTitleAnimationRunning = true;
		    mTitleText.startAnimation(mTextBarAnimation);
	
		    mSmiley1.setImageDrawable(getResources().getDrawable(R.drawable.smiley_icon));
		    mSmiley2.setImageDrawable(getResources().getDrawable(R.drawable.smiley_icon));
		}
		else if(placement == ABOVE_TOP)
		{
		    mTitleText.setText(getResources().getString(mTitleAboveTop20));
		    mAltTitleText.setText(getResources().getString(R.string.title_alt_text));
		    mTitleAnimationRunning = true;
		    mTitleText.startAnimation(mTextBarAnimation);
	
		    mSmiley1.setImageDrawable(getResources().getDrawable(R.drawable.smiley_icon));
		    mSmiley2.setImageDrawable(getResources().getDrawable(R.drawable.smiley_icon));
		    mSmiley3.setImageDrawable(getResources().getDrawable(R.drawable.smiley_icon));
		    mAltTitleLayout.setVisibility(View.INVISIBLE);
		}
		else if(placement == NO_DATA)
		{
			 mTitleText.setText(getResources().getString(mTitleNoData));
			 mAltTitleText.setText("");
			 mTitleAnimationRunning = true;
			 mTitleText.startAnimation(mTextBarAnimation);
			 mSmiley1.setImageDrawable(getResources().getDrawable(R.drawable.smiley_icon));
			 mAltTitleLayout.setVisibility(View.INVISIBLE);
		}
    }


    //Menu System
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.today_menu, menu);		
		return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	MenuItem phoneIdItem = (MenuItem)menu.findItem(org.swanseacharm.bactive.R.id.YourPhoneId);
		phoneIdItem.setTitle("Phone ID: " + Globals.getShortIMEI(this));
		phoneIdItem.setEnabled(false);
		
		MenuItem tunerItem = (MenuItem)menu.findItem(org.swanseacharm.bactive.R.id.TuneActivityMonitor);
		tunerItem.setVisible(Globals.activityTunerActive());
		return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {		
		// Handle item selection
		switch (item.getItemId()) 
		{
		    case R.id.Help:
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(Globals.helpURL()));
				startActivity(i);
				return true;
		    case R.id.ViewDataUsage:
		    	Intent iv = new Intent(this,ViewDataUsage.class);
		    	startActivity(iv); 
		    	return true;
		    case R.id.ShowTutorialsAgain:
		    	TutorialToaster.showAllAgain();
		    	onResume();
		    	return true;
		    case R.id.TuneActivityMonitor:
		    	(new ActivityTunerDialog(this)).show();
		    	return true;
		/*    case R.id.SyncNow:
		    	(new WebServiceProxy()).sendTodayAndUnsentData(this);
		    	FrugalityProxy.makeDirty();
		    	new DownloadGroupDataTask().execute();
		    	return true;
		*/
		    default:	   
		    	return super.onOptionsItemSelected(item);
		}
    }

    /**
     * receives update requests from activity monitor
     * @author admin
     *
     */
    public class DataReceiver extends BroadcastReceiver
    {
		@Override 
		public void onReceive(Context context, Intent intent)
		{
			if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {				
				FrugalityProxy.makeDirty();
			}
			else if(intent.getAction().equals(ActivityMonitor.STEP_COUNT_BROADCAST)) {
				loadData();
				animate();
			}
		}
    }

    /**
     * click listener for tab buttons
     */
    @Override
    public void onClick(View v)
    {
    	int id = v.getId();
    	
    	if(id == org.swanseacharm.bactive.R.id.todayTab)
			startActivity(new Intent(getApplicationContext(),Today.class));
    	else if(id == org.swanseacharm.bactive.R.id.yesterdayTab)
    		startActivity(new Intent(getApplicationContext(),Yesterday.class));
    	else if(id == org.swanseacharm.bactive.R.id.lastWeekTab)
			startActivity(new Intent(getApplicationContext(),PastWeek.class));
    	else if(id == org.swanseacharm.bactive.R.id.historyTab)
			startActivity(new Intent(getApplicationContext(),History.class));
    }

    /**
     *  AsyncTask to download group data from webservice without blocking UI thread
     */
    private class DownloadGroupDataTask extends AsyncTask<Void,Void,ActivityRecord>
    {
		@Override
		protected ActivityRecord doInBackground(Void... arg0)
		{
			if(!Globals.groupFeedback())
				return null;
			
		    WebServiceProxy wsProxy = new WebServiceProxy();
		    ActivityRecord ar = null;
		    
		    try { 
		    	ar = wsProxy.getGroupDataByDate(getApplicationContext(),mDay);
		    }
		    catch(Exception e) {}
	
		    return ar;
		}
	
		@Override
		protected void onPostExecute(ActivityRecord ar)
		{			
			super.onPostExecute(ar);
			
			showProgress(false);
			
			if(!Globals.groupFeedback())
				return;
		   
			// unsuccessful in getting group data - do nothing
			if(ar == null) {
			//	Toast t = Toast.makeText(getApplicationContext(), "No data signal - the displayed figures may be out of date!", Toast.LENGTH_SHORT);
			//	t.setGravity(Gravity.TOP, 0, 350);
			//	t.show();
				return;
			}
		   
			// set group text views
			mGroupStepsValue = (int)ar.getAvg();
			mGroupSteps.setText("" + mGroupStepsValue);
			mGroupDistance.setText(ActivityRecord.formatDistance(mGroupStepsValue));
			mGroupCalories.setText(ActivityRecord.formatCalories(mGroupStepsValue));
		
			// get top 20% average
			mGroupTopSteps = (int)ar.getTop();
		
			// start animations
			mHandler.postDelayed(new AnimRunnable(),500);
		   
			// mark the task as complete in the proxy so that we don't grab again for a while
			FrugalityProxy.markAsCompleted(mGroupProxyDataId);
		}
    }
}	
