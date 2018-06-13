package org.swanseacharm.bactive.ui;

import java.util.Date;

import org.swanseacharm.bactive.DateUtil;
import org.swanseacharm.bactive.R;
import org.swanseacharm.bactive.TutorialToaster;
import org.swanseacharm.bactive.R.string;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class Yesterday extends SingleDay
{
	private TutorialToaster mTutorial;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	// setup for base class
		setDay(DateUtil.yesterday());
    	super.onCreate(savedInstanceState);	
    
		// title bar strings
		mTitleNoData = org.swanseacharm.bactive.R.string.yesterday_no_data;
		mTitleNoFeedback = org.swanseacharm.bactive.R.string.yesterday_no_feedback;
		mTitleBelowAverage = org.swanseacharm.bactive.R.string.yesterday_below_average;
		mTitleBelowMidPoint = org.swanseacharm.bactive.R.string.yesterday_below_mid_point;
		mTitleBelowTop20 = org.swanseacharm.bactive.R.string.yesterday_below_top_20;
		mTitleAboveTop20 = org.swanseacharm.bactive.R.string.yesterday_above_top_20;
    }
    
    @Override
    public void onResume() 
    {
		mPrefsName = "Yesterday";
		setDay(DateUtil.yesterday());
    	super.onResume();
    	
    	mTutorial = new TutorialToaster(this,this);
    	mTutorial.setToasts(
    		new String[]{
    			"Welcome to the bActive 'yesterday' screen!",
    			"This screen is identical to the 'today' screen, except it shows yesterday's results."
    		},
    		new int[][] {
    			{0,200},
       			{0,200}
    		});
    	mTutorial.execute();
    	
    }
}	

