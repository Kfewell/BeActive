package org.swanseacharm.bactive.ui;

import java.util.ArrayList;

import org.swanseacharm.bactive.ActivityRecord;
import org.swanseacharm.bactive.Globals;
import org.swanseacharm.bactive.R;
import org.swanseacharm.bactive.R.id;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

/**
 * Generic 'week' view that encapsulates a graph view displaying data for one week
 * @author Simon Walton
 */
public class Week extends Activity implements OnClickListener
{
	// values sent to graph view upon call to refreshGraph()
	protected float mMax = 0;
	protected ArrayList<ActivityRecord> mARs = new ArrayList<ActivityRecord>();
	
	protected SharedPreferences mPrefs;
    protected GraphView mGraphView = null;
    protected int mDisplayMode = 0;   
    
    //tab bar buttons
    private Button mToday, mYesterday;
    private Button mLastWeek;
    private Button mHistory;
    
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
		super.onCreate(savedInstanceState);
		
		if(!Globals.groupFeedback()) {
			findViewById(R.id.graphLegendGroup).setVisibility(View.INVISIBLE);
			findViewById(R.id.graphLegendGroupTop).setVisibility(View.INVISIBLE);
		}
    }
	
	protected void initTabBar()
	{
		// init tab bar buttons
		mToday = (Button) findViewById(org.swanseacharm.bactive.R.id.todayTab);
		mToday.setOnClickListener(this);
		mYesterday = (Button) findViewById(org.swanseacharm.bactive.R.id.yesterdayTab);
		mYesterday.setOnClickListener(this);
		mLastWeek = (Button) findViewById(org.swanseacharm.bactive.R.id.lastWeekTab);
		mLastWeek.setOnClickListener(this);
		mHistory = (Button) findViewById(org.swanseacharm.bactive.R.id.historyTab);
		mHistory.setOnClickListener(this);
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
	 * refreshes the graph based on current activitylist and max value
	 */
    protected void refreshGraph()
    {
    	if(mGraphView != null) {
    		mGraphView.setMax(mMax);
    		mGraphView.setValues(mARs);
    		mGraphView.refresh();
    	}
    }
    
    
}
