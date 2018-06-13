package org.swanseacharm.bactive.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.swanseacharm.bactive.ActivityRecord;
import org.swanseacharm.bactive.DateUtil;
import org.swanseacharm.bactive.FrugalityProxy;
import org.swanseacharm.bactive.Globals;
import org.swanseacharm.bactive.R;
import org.swanseacharm.bactive.TutorialToaster;
import org.swanseacharm.bactive.R.array;
import org.swanseacharm.bactive.R.drawable;
import org.swanseacharm.bactive.R.id;
import org.swanseacharm.bactive.R.layout;
import org.swanseacharm.bactive.database.DatabaseProxy;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

/**
 * 'Past week' activity: allows user to view past weeks' progress
 * @author Simon Walton
 */
public class PastWeek extends Week implements OnClickListener
{
    private ProgressBar mProgressBar;
    private TutorialToaster mTutorial = null;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {	
    	setContentView(R.layout.lastweek); 
		super.onCreate(savedInstanceState);
		
		initTabBar();
	
		// highlight the tab button
		((Button)findViewById(R.id.lastWeekTab)).setTextColor(Color.BLACK);
		((Button)findViewById(R.id.lastWeekTab)).setBackgroundDrawable(getResources().getDrawable(org.swanseacharm.bactive.R.drawable.tab_item_selected));
		
		mGraphView = (GraphView) findViewById(R.id.lastWeekGraphView);
		mGraphView.setWeekMode(GraphView.WEEK_MODE_TODAY);
		String[] labels = getResources().getStringArray(R.array.column_labels);
		mGraphView.setLabels(labels);
	
		((Button)findViewById(R.id.title_bar_show_week_avg)).setOnClickListener(this);
	
		mProgressBar = (ProgressBar) findViewById(R.id.lastWeekProgressBar);
    }

    @Override
    protected void onResume()
    {
		super.onResume();
		
		mProgressBar.setVisibility(View.VISIBLE); 
		new InitialiseGraphData().execute();
		
    	mTutorial = new TutorialToaster(this,this);  
			
		if(Globals.groupFeedback()) {
	    	mTutorial.setToasts(
	    		new String[]{
	    			"Welcome to the 'past week' screen!",
	    			"The graph shows activity over the past week, ending with today.",
	    			"Your activity per day (if any) displays as a green line.",
	    			"The group's activity is overlaid as purple lines.",
	    			"A solid purple line represents the average for everyone.",
	    			"If your green bar is above the dashed purple line, then you are in the top 20%!",
	    		},
	    		new int[][] {
	    			{0,200},
	       			{0,200},
	       			{0,200},
	       			{0,200},
	       			{0,200},
	       			{0,200}
	    		});
		}
		else {
	    	mTutorial.setToasts(
		    		new String[]{
		    			"Welcome to the 'past week' screen!",
		    			"The graph shows activity over the past week, ending with today.",
		    			"Your activity per day (if any) displays as a green line."		    			
		    		},
		    		new int[][] {
		    			{0,200},
		       			{0,200},
		       			{0,200}
		    		});
		}
    	mTutorial.execute();
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    	
    	if(mTutorial != null)
    		mTutorial.cancelAll();
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
     * handle touch event for showing day info
     */
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
    	synchronized(e) {
    		if(e.getAction() == MotionEvent.ACTION_UP) {
    			mGraphView.showDayInfo((int)((e.getRawX() / mGraphView.getWidth()) * 7));
    		}
    	}
		return false;
    }

    /**
     * AsyncTask to download group data from webservice without blocking UI thread
     * @author admin
     *
     */
    private class InitialiseGraphData extends AsyncTask<Void,Void,Boolean>
    {
		@Override
		protected Boolean doInBackground(Void... arg0)
		{
		    // get 'you' data from database
		    DatabaseProxy dbProxy = new DatabaseProxy(getApplicationContext());
	    	mARs = dbProxy.getActivityRecordsByDateRange(DateUtil.plusDays(DateUtil.today(), -6), DateUtil.today());
		    
		    try {
		    	if(Globals.groupFeedback()) {
		    		// try getting group data
		    		ArrayList<ActivityRecord> group = FrugalityProxy.getDateRangeGroup(getApplicationContext(), DateUtil.plusDays(DateUtil.today(), -6), DateUtil.today());
		    		    	
		    		// combine group into you
		    		mARs = dbProxy.combineGroupIntoYou(mARs, group);
		    	}

		       	// compute max
				mMax = ActivityRecord.getMax(mARs,GraphView.FIELDS_TO_RENDER);
		    }
		    catch(Exception e) { return false; }
		    
		    return true;	
		}
	
		@Override
		protected void onPostExecute(Boolean result)
		{ 
		    super.onPostExecute(result);
			mProgressBar.setVisibility(View.INVISIBLE);
		    
			if(result.booleanValue()) {
		    	refreshGraph();
		    }
		}


    }
}
