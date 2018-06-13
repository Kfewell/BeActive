package org.swanseacharm.bactive.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.swanseacharm.bactive.ActivityRecord;
import org.swanseacharm.bactive.DateUtil;
import org.swanseacharm.bactive.FrugalityProxy;
import org.swanseacharm.bactive.Globals;
import org.swanseacharm.bactive.R;
import org.swanseacharm.bactive.TameToaster;
import org.swanseacharm.bactive.TutorialToaster;
import org.swanseacharm.bactive.R.array;
import org.swanseacharm.bactive.R.drawable;
import org.swanseacharm.bactive.R.id;
import org.swanseacharm.bactive.R.layout;
import org.swanseacharm.bactive.R.menu;
import org.swanseacharm.bactive.database.DatabaseProxy;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * History activity: allows user to view past progress by moving between weeks
 * @author Simon Walton
 */
public class History extends Week implements OnClickListener
{
    //tab bar buttons
    private Button mToday, mYesterday, mLastWeek;
    private RadioGroup mRadioGroup;
    private ProgressBar mProgressBar;
    private Menu mMenu;
    private TutorialToaster mTutorial = null;

    private Calendar mDateStart;
    private Calendar mDateEnd;
    private Calendar mDateOffset;
    
    public static final int DAYS_IN_WEEK = 7;

    private final long ONE_DAY = 86400000;
    private ArrayList<ActivityRecord> mAllRecords;
    
    private final int STEPS = 0;
    private final int CALS = 2;
    private final int DIST = 3;
    private int mDisplayedData = STEPS;
  

    @Override
    public void onCreate(Bundle savedInstanceState)
    {		
    	setContentView(R.layout.history);
		super.onCreate(savedInstanceState);
		
		// highlight the tab button
		((Button)findViewById(R.id.historyTab)).setTextColor(Color.BLACK);
		((Button)findViewById(R.id.historyTab)).setBackgroundDrawable(getResources().getDrawable(org.swanseacharm.bactive.R.drawable.tab_item_selected));
	
		super.initTabBar();
		
		mGraphView = (GraphView) findViewById(R.id.historyGraphView);
		mGraphView.setWeekMode(GraphView.WEEK_MODE_MONDAY);

		String[] labels = getResources().getStringArray(R.array.column_labels);
		mGraphView.setLabels(labels);
		
		((Button)findViewById(R.id.title_bar_show_week_avg)).setOnClickListener(this);
		
		mProgressBar = (ProgressBar) findViewById(R.id.historyProgressBar);
		
		if(savedInstanceState != null)
			onRestoreInstanceState(savedInstanceState);
    }
    
    

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
		super.onResume();
	
		mTutorial = new TutorialToaster(this,this);
		
		mProgressBar.setVisibility(View.VISIBLE);
		new InitialiseGraphData().execute();
				
    	mTutorial.setToasts(
    		new String[]{
    			"Welcome to the 'history' screen!",
    			"The graph is identical to that shown in the 'past week' screen, except you can change the date range.",
    			"Swipe your finger to the right to move back by one week.",
    			"Swipe your finger to the left to move forwards by one week."
    		},
    		new int[][] {
    			{0,200},
       			{0,200},
       			{0,200},
       			{0,200}
    		});
    	mTutorial.execute();
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    	
    	mTutorial.cancelAll();
    }
    
    protected int swipeDistanceThreshold = 40;
    protected float down[] = new float[2];
    
    /**
     * Swipe back/forward functionality
     * @author Simon Walton
     */
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
    	synchronized(e) {
    		try {
    			e.wait(16);
    			
    			if(e.getAction() == MotionEvent.ACTION_DOWN) {
    				down[0] = e.getRawX();
    				down[1] = e.getRawY();
    				return true;
    			}
    			else if(e.getAction() == MotionEvent.ACTION_UP) {
    				float delta[] = { e.getRawX() - down[0], e.getRawY() - down[1] };
    				
    				if(delta[0] > swipeDistanceThreshold) {
    					// left swipe
    					if(canGoBackward()) {
    						goBackward();
    						return true;
    					}
    					else
    						TameToaster.showToast(this,"Nothing happened the week before this.");
    				}
    				else if(delta[0] < -swipeDistanceThreshold) {
    					// right swipe
    					if(canGoForward()) {
    						goForward();
	    					return true;
    					}
    					else TameToaster.showToast(this,"Can't go past the current week.");
    				}
    				else {
    					// wasn't a swipe event - work out which day the user touched and show them something useful
    					mGraphView.showDayInfo((int)((e.getRawX() / mGraphView.getWidth()) * 7));
    				}
    			}
    		}
    		catch(Exception ex) {}
    	}
    	
    	return false;
    }



	private void goForward() {
		mDateOffset = DateUtil.plusDays(mDateOffset, 7);
		refreshData();
		refreshGraph();
		updateDateText();
	}



	private void goBackward() {
		mDateOffset = DateUtil.plusDays(mDateOffset, -7);
		refreshData();
		refreshGraph();
		updateDateText();
	}

   

    /**
     * handle button clicks
     */
    @Override
    public void onClick(View v)
    {
    	super.onClick(v);
    	
		switch (v.getId())
		{
		    case R.id.title_bar_show_week_avg:
		    	boolean checked = ((CheckBox)findViewById(R.id.title_bar_show_week_avg)).isChecked();
		    	mGraphView.setShowWeekAvg(checked);
		    	refreshGraph();
		    	break;
		}
    }


    /**
     * menu for going back/fwd in time for folk who don't enjoy flicking
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.history_menu, menu);
		return super.onCreateOptionsMenu(menu);
    }
    
    /**
     * @return Whether or not it is possible to go forward one week based on current offset
     * @author Simon Walton
     */
    private boolean canGoForward() {
    	return DateUtil.timelessComparison(DateUtil.plusDays(mDateOffset, 7),mDateEnd) <= 0;
    }
    
    /**
     * @return Whether or not it is possible to go back one week based on current offset
     * @author Simon Walton
     */
    private boolean canGoBackward() {
    	return DateUtil.timelessComparison(DateUtil.plusDays(mDateOffset, -7),mDateStart) >= 0;
    }

    /**
     * enable / disable options menu buttons depending on which week we are displaying
     * @author Simon Walton
     */

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {		
    	mMenu = menu;
    	mMenu.setGroupEnabled(R.id.history_menu_next_group, canGoForward());
    	mMenu.setGroupEnabled(R.id.history_menu_previous_group, canGoBackward());
		
		return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
		switch (item.getItemId()) {
		    case R.id.previousWeek:
				if(canGoBackward())
					goBackward();
				return true;
	
		    case R.id.next_week:
		    	if(canGoForward())
		    		goForward();
		    	return true;
	
		    default:	   
		    	return super.onOptionsItemSelected(item);
		}
    }
    
    /**
     * updates the title bar text informing the user of current date range
     */
    private void updateDateText()
    {
    	TextView dr = (TextView) findViewById(R.id.graphTitleDateRange);
		Calendar end = DateUtil.plusDays(mDateOffset, 6);
		dr.setText(DateUtil.formatShort(mDateOffset) + " to " + DateUtil.formatShort(DateUtil.plusDays(mDateOffset, 6)));
    }

    /**
     * Refresh the graph data & redraw the graph based on current offset
     * @author Simon Walton
     * @returns true if successful
     */
    private boolean refreshData()
    {
    	DatabaseProxy dbProxy = new DatabaseProxy(getApplicationContext());	  
    	Calendar target = DateUtil.plusDays(mDateOffset, 6);
    	mARs = dbProxy.getActivityRecordsByDateRange(mDateOffset, target);
    	
    	try {
    		if(Globals.groupFeedback()) {
    			ArrayList<ActivityRecord> group = FrugalityProxy.getDateRangeGroup(getApplicationContext(),mDateOffset, target);    	
    			mARs = dbProxy.combineGroupIntoYou(mARs, group);
    		}
	       	
	       	// compute max
			mMax = FrugalityProxy.getMaximumValueForAllRecords(getApplicationContext(), mDateStart, mDateEnd, GraphView.FIELDS_TO_RENDER);
    	}
    	catch(Exception e) {
    		Log.d("CHARM,","Exception in refreshData() " + e.getMessage());
    		return false;
    	}
    	
    	return true;
    }

    /**
     * AsyncTask to download group data from webservice without blocking UI thread
     * @author
     */
    private class InitialiseGraphData extends AsyncTask<Void,Void,Boolean>
    {
		@Override
		protected Boolean doInBackground(Void... arg0)
		{
			
			/*
			 *  note: most of the date code below provided by mubaloo. ugly as hell, but it works
			 *  so i'm not touching it. S Walton 
			 */
		    Calendar cal = Calendar.getInstance();
		    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		    if(dayOfWeek == 1) dayOfWeek = 8; //as Calendar has Sunday as day 1 not Monday
		    int daysSinceMonday = dayOfWeek - Calendar.MONDAY;
	
		    Date currentDate = new Date();
		    long time = System.currentTimeMillis();
		    Date monday = new Date(time - (daysSinceMonday * ONE_DAY));

		    Log.v("mubaloo","current date = " + currentDate.toString());
		    Log.v("mubaloo","moday  = " + monday);
	
		    Log.v("mubaloo","days since monday = " + daysSinceMonday);
	
		    Date sunday = new Date(time + ((6 - daysSinceMonday) * ONE_DAY));
		    Date today = new Date();
	
		    Log.v("mubaloo","sunday = " + sunday );
	
		    DatabaseProxy dbProxy = new DatabaseProxy(getApplicationContext());
		    Date earliestInDb = dbProxy.getEarliestDate();
	
		    Log.v("mubaloo","first " +earliestInDb);
	
		    long earliestTime = earliestInDb.getTime();
	
		    Calendar earliestDay = Calendar.getInstance(); 
		    earliestDay.setTimeInMillis(earliestTime);
	
		    int dayOfFirstWeek = earliestDay.get(Calendar.DAY_OF_WEEK);
		    if(dayOfFirstWeek == 1) dayOfFirstWeek = 8; //as Calendar has Sunday as day 1 not Monday
		    int daysSinceFirstMonday = dayOfFirstWeek - Calendar.MONDAY;
	
		    Date firstModay = new Date(earliestTime - (daysSinceFirstMonday * ONE_DAY));
	
		    Log.v("mubaloo","first monday = " + firstModay);
	
		    mDateStart = DateUtil.calendarFromDate(firstModay);
		    mDateEnd = DateUtil.calendarFromDate(sunday);
		    mDateOffset = DateUtil.plusDays(mDateEnd, -6);
		    
		    return refreshData();
		}
	
		@Override
		protected void onPostExecute(Boolean result)
		{ 
		    mProgressBar.setVisibility(View.INVISIBLE);
		    super.onPostExecute(result);
		    
		    if(result == null || !result.booleanValue())
		    	return;
		    
			if(mMenu != null) {
				mMenu.setGroupEnabled(R.id.history_menu_next_group, canGoBackward());
				mMenu.setGroupEnabled(R.id.history_menu_next_group, canGoForward());
			}
			
			// remind user that they can swipe to go back a week
			if(mTutorial.hasSeen() && canGoBackward()) {
				TutorialToaster.oneShotToast(History.this, "youCanSwipe", Toast.makeText(History.this, "Note: swipe your finger to the right to see data from last week!", Toast.LENGTH_LONG));
			}
			
		    refreshGraph();
		    updateDateText();
		}
    }

}


