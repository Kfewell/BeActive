package org.swanseacharm.bactive.ui;


import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.swanseacharm.bactive.ActivityRecord;
import org.swanseacharm.bactive.DateUtil;
import org.swanseacharm.bactive.Globals;
import org.swanseacharm.bactive.R;
import org.swanseacharm.bactive.R.color;
import org.swanseacharm.bactive.TameToaster;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * Graphing functionality in bActive 
 * @author Simon Walton
 */
public class GraphView extends SurfaceView implements SurfaceHolder.Callback
{
	private float mWidth;
	private float mHeight;
	private float mBarHeight;
	private float mMinRange;
	private float mMaxRange;
	private float mDataPointOuterRadius;
	private float mDataPointInnerRadius;
	private float mNumberOfColums;
	
	private Paint mPaintGroupPoly, mWeekAvgBox;
	private Paint mBackground, mBGStripe, mBarText, mTodayBarText, mFutureBarText, mLegendText, mWhiteOutline;
	private Paint mYouBarInside, mYouBarStroke, mGroupSpot, mYouLine, mGroupLine, mGroupTop20Line, mYouLabel, mGroupLabel;
	
	private String[] mLabels;
	
	protected ArrayList<ActivityRecord> mValues = null;

	private float[] mMe = {0,0,0,0,0,0,0};
	private float[] mGroupTop20 = {0,0,0,0,0,0,0};
	private float[] mGroupAvg = {0,0,0,0,0,0,0};
	
	public static int[] FIELDS_TO_RENDER = new int[] { ActivityRecord.FIELD_ME, ActivityRecord.FIELD_GROUPAVG, ActivityRecord.FIELD_GROUPTOP };
	
	public static final int STEPS = 0;
	public static final int CALS = 1;
	public static final int DISTANCE = 2;
	
	private int mTodayIdx = -1;
	
	Timer animTimer = null;
	boolean doneAnimating = false;
	boolean animRunning = false;
	GraphViewThread thread = null;
	private int mDataBounds[] = new int[] {0,6};
	
	private int mWeekMode = 0;
	private boolean mShowWeekAvg = false;
	private float mMax;
	public final static int WEEK_MODE_MONDAY = 0;
	public final static int WEEK_MODE_TODAY = 1;
	
	/**
	 * Thread that does all the drawing of the graph
	 * @author Simon Walton
	 *
	 */
	class GraphViewThread extends Thread {
	   
	    private SurfaceHolder mSurfaceHolder;
	    private boolean mRun = true;
	    GraphAnimTask graphAnimTask = null;
	    

	    public GraphViewThread(SurfaceHolder surfaceHolder) {
		    // get handles to some important objects
		    mSurfaceHolder = surfaceHolder;
	    }
	    
	    void setRunning(boolean run) {
	    	mRun = run;
	    }
	    
	    void reset() {
	    	if(graphAnimTask != null)
	    		graphAnimTask.reset();
	    }
	   
	    /*
	     * returns index of today's day of the week where 0: monday; 6: sunday
	     */
	    protected int todayIdx() {
	    	Calendar c = Calendar.getInstance();
	    	int idx = c.get(Calendar.DAY_OF_WEEK);
	
	    	// convert to 0 = monday; 6 = sunday
	       	return idx > 1 ? idx-2 : 6;
	    }
	    
	    /*
	     * given a column index (0-6), gives the actual index in the data array
	     */
	    protected int columnToDataIdx(int idx) {
	    	if(mWeekMode != WEEK_MODE_MONDAY) {
		    	int today = todayIdx();
		    	int a = today - (6 - idx);
		    	return a < 0 ? 7 + a : a % 7;
	    	}
	    	else {
	    		return idx;
	    	}
	    }
	    
	    /**
	     * main drawing function called in loop
	     * @param t t-value {0,1} of animation's current state
	     */
	    protected void onDrawAnim(Canvas canvas, float t)
	    {
	    	//draw background rectangle
			canvas.drawRect(0, 0, mWidth, mHeight, mBackground);
		
			//draw bgStripes
			float oldX = 0;
			float setpSize = mWidth / mNumberOfColums;
			float newX = setpSize;
				
			//Array of center points of each column
			float[] centerXs = new float[(int) mNumberOfColums];
			
			mDataBounds[0] = 0;
			mDataBounds[1] = mTodayIdx == -1 ? mMe.length-1 : mTodayIdx;
		
			// draw background stripes
			for(int i = 0;i<mNumberOfColums;i++)
			{
			    if(i % 2 != 0)
			    	canvas.drawRect(oldX, 0, newX, mHeight, mBGStripe);
			    
			    //useful to keep track of these column centers for drawing data points etc..
			    centerXs[i] = oldX + ((newX - oldX) /2);	    
			    oldX = newX;
			    newX += setpSize;
			}
		
			// draw text labels along the x axis
			float textHeight = mHeight - (mBarHeight / 2);
			for(int i = 0;i<mNumberOfColums;i++) {
				int idx = columnToDataIdx(i);
				if(mTodayIdx == i)
				    canvas.drawText("Today", centerXs[i], textHeight, mTodayBarText);
				else if(i < mDataBounds[0] || i > mDataBounds[1])
					canvas.drawText(mLabels[idx], centerXs[i], textHeight, mFutureBarText);
				else if(i >= mDataBounds[0] && i <= mDataBounds[1])	
					canvas.drawText(mLabels[idx], centerXs[i], textHeight, mBarText);
			}
		
			float[] me = interpolatePoints(t, mMe);			
			float[] top = interpolatePoints(t, mGroupTop20);
			float[] avg = interpolatePoints(t, mGroupAvg);
			
			/*
			 * avg/top20
			 */
			
			// group lines
			for(int i = mDataBounds[0];i<mDataBounds[1];i++) {
				if(Globals.groupFeedback()) {
					canvas.drawLine(centerXs[i], avg[i], centerXs[i+1], avg[i+1], mGroupLine);
					canvas.drawLine(centerXs[i], top[i], centerXs[i+1], top[i+1], mGroupTop20Line);
				}
				canvas.drawLine(centerXs[i], me[i], centerXs[i+1], me[i+1], mYouLine);
				
			}
		

			// draw the data points as circles
			for(int i = mDataBounds[0];i<=mDataBounds[1];i++) {				
				canvas.drawCircle(centerXs[i], me[i], mDataPointOuterRadius, mWhiteOutline);
				canvas.drawCircle(centerXs[i], me[i], mDataPointInnerRadius, mYouLine);
			}
			
			/*
			 * user wants average shown?
			 */
			if(mShowWeekAvg) {
				float meAvg = getAvg(me);
							
				canvas.drawRect(0, 0, mWidth, mHeight, mWeekAvgBox);
				canvas.drawLine(centerXs[0], meAvg, centerXs[6], meAvg, mYouLine);
				
				float youStepsAvg = getAvg(mValues,ActivityRecord.FIELD_ME);
				canvas.drawText(new DecimalFormat("#").format(youStepsAvg), centerXs[0]-30, meAvg, mYouLabel);
				
				if(Globals.groupFeedback()) {
					float topAvg = getAvg(top);
					float avgAvg = getAvg(avg);
					float groupStepsAvg = getAvg(mValues,ActivityRecord.FIELD_GROUPAVG);
					float topStepsAvg = getAvg(mValues,ActivityRecord.FIELD_GROUPTOP);
					
					canvas.drawLine(centerXs[0], avgAvg, centerXs[6], avgAvg, mGroupLine);
					canvas.drawLine(centerXs[0], topAvg, centerXs[6], topAvg, mGroupTop20Line);
					
					canvas.drawText(new DecimalFormat("#").format(groupStepsAvg), centerXs[0]-30, avgAvg, mGroupLabel);
					canvas.drawText(new DecimalFormat("#").format(topStepsAvg), centerXs[0]-30, topAvg, mGroupLabel);
				}
			}
			
			/*
			// draw the data labels
			for(int i = mDataBounds[0];i<=mDataBounds[1];i++) {
				int idx = i;
	
			    float youY, groupY;
			    
			    // determine which label to draw above, which to draw below
			    if(mYouViewEnd[i] <= mGroupViewEnd[i]) {
					youY = mYouCurrVals[idx] + (2.5f * mDataPointOuterRadius);
					groupY = mGroupCurrVals[idx] - (1.5f * mDataPointOuterRadius);
			    }
			    else {
					youY = mYouCurrVals[idx] - (1.5f * mDataPointOuterRadius);
					groupY = mGroupCurrVals[idx] + (2.5f * mDataPointOuterRadius);
			    }
			    
			    if(mValidityYou[idx])
			    	canvas.drawText(new DecimalFormat("#.##").format(mYouLabels[idx]), centerXs[i], youY, mYouLabel);
			    if(mValidityGroup[idx])
			    	canvas.drawText(new DecimalFormat("#.##").format(mGroupLabels[idx]), centerXs[i], groupY, mGroupLabel);
			}*/
	    }
	    
	    @Override
	    public void run() {
	    	
	        while (mRun) {
	            Canvas c = null;
	            SurfaceHolder h = getHolder();
	            try {
	                c = h.lockCanvas(null);
	                synchronized (mSurfaceHolder) {
	                	onDraw(c);
	                }
	            } finally {
	                // do this in a finally so that if an exception is thrown
	                // during the above, we don't leave the Surface in an
	                // inconsistent state
	                if (c != null) {
	                    mSurfaceHolder.unlockCanvasAndPost(c);
	                }
	            }
	        }
	    }
	
	    protected void onDraw(Canvas canvas)
	    {
	    	if(graphAnimTask == null) {
	    		graphAnimTask = new GraphAnimTask(10);
	    	}
	    	
	    	if(animRunning) {
	    		onDrawAnim(canvas, graphAnimTask.getCurrT());
	    		try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	else onDrawAnim(canvas, 1.0f);
	    }

		private class GraphAnimTask extends TimerTask
		{
			private int executionTarget;
			private int executions;
			private float c;
			
				public GraphAnimTask(int executionTarget) {
					 this.executionTarget = executionTarget;
					 executions = 0;
					 animRunning = true;
					 c = 1.0f / (float)executionTarget;
				}
				
				private float tween(float t) {
					t-=1.0f;
					return (t*t*t + 1.0f); 
		
				}
				
				public float getCurrT() {
					float v = tween((float)(executions) / (float)executionTarget);
					if(executions < executionTarget)
						executions++;
					return v;
				}
				
				public void reset() {
					executions = 0;
					animRunning = true;
				}
				
				@Override
				public void run() {
				/*	 if(executions < executionTarget) {
						 GraphView.this.invalidate();/	Invalidate();
					 }
					 else {
						 this.cancel();
						 animRunning = false;
						 doneAnimating = true;
					 }
				}*/
			
			}
		}
	}

	 //constructors from super
    public GraphView(Context context, AttributeSet attrs)
    {
    	super(context, attrs);
    	
    	 // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        
        init();

        setFocusable(true); // make sure we get key events
    }
    
    public GraphView(Context context)
    {
    	super(context);
    	
    	 // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        init();

        setFocusable(true); // make sure we get key events
    }
    
    private void init() 
    {
    	if(Globals.groupFeedback())
    		FIELDS_TO_RENDER = new int[] { ActivityRecord.FIELD_ME, ActivityRecord.FIELD_GROUPAVG, ActivityRecord.FIELD_GROUPTOP };
    	else
    		FIELDS_TO_RENDER = new int[] { ActivityRecord.FIELD_ME };
	}

	/**
     * redraws the graph, recalculating coords from activity data
     */
    public void refresh() {
    	calculateViewCoords();
    	
    	try {
    		if(thread != null)
    			thread.reset();
    	}
    	catch(Exception e) {}
    }
    
    /**
     * Sets the start/end array indices for available data in this view
     * @author Simon Walton
     */
    public void setDataBounds(int[] bounds) {
    	mDataBounds = bounds.clone();
    }
    

    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread = new GraphViewThread(holder);
    	thread.setRunning(true);
    }
    
    /**
     * called every time view size changes; sets up measurements based on total view size
     */
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) 
    {
    	super.onSizeChanged(w, h, oldw, oldh);
    	Log.d("CHARM","Graph.onSizeChanged " + w + "," + h);
    	
		mWidth = w;
		mHeight = h;
		mBarHeight = mHeight / 10;
		
		//radius of data points is 1/8 width of column
		mDataPointOuterRadius = (mWidth / mNumberOfColums) / 16;
		mDataPointInnerRadius = (mWidth / mNumberOfColums) / 20;
	
		mMinRange = 3* mDataPointOuterRadius;
		mMaxRange = (mHeight - (2 * mBarHeight)) - (3 * mDataPointOuterRadius);
		
		initGraphView();
		refresh();		
    }
    
    /**
     * intialise once all Paint objects, rather than on each draw
     */
    protected void initGraphView() 
    {
		setFocusable(true);
		//initiate all variables here, so day strings, paints etc
	
		//initiate paints, colors and style
		mBackground = new Paint();
		mBackground.setColor(getResources().getColor(R.color.graph_view_bg_light));
		mBGStripe = new Paint();
		mBGStripe.setColor(getResources().getColor(R.color.graph_view_bg_dark));
	
		
		mLegendText = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLegendText.setColor(getResources().getColor(R.color.graph_view_text));
		mLegendText.setStyle(Style.FILL);
		mLegendText.setTextSize(mBarHeight * 0.6f);
		mLegendText.setTextAlign(Paint.Align.LEFT);
		
		//bar text paint
		mBarText = new Paint(Paint.ANTI_ALIAS_FLAG);
		mBarText.setColor(getResources().getColor(R.color.graph_view_text));
		mBarText.setStyle(Style.FILL);
		mBarText.setTextSize(mBarHeight * 0.75f);
		mBarText.setTextAlign(Paint.Align.CENTER);
		
		mTodayBarText = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FAKE_BOLD_TEXT_FLAG);
		mTodayBarText.setColor(getResources().getColor(R.color.graph_view_text));
		mTodayBarText.setStyle(Style.FILL); 
		mTodayBarText.setTextSize(mBarHeight * 0.75f);
		mTodayBarText.setTextAlign(Paint.Align.CENTER);
		
		mFutureBarText = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FAKE_BOLD_TEXT_FLAG);
		mFutureBarText.setColor(getResources().getColor(R.color.graph_view_future_text));
		mFutureBarText.setStyle(Style.FILL); 
		mFutureBarText.setTextSize(mBarHeight * 0.75f);
		mFutureBarText.setTextAlign(Paint.Align.CENTER);
		
		mPaintGroupPoly = new Paint(Paint.ANTI_ALIAS_FLAG );
		mPaintGroupPoly.setColor(getResources().getColor(R.color.graph_view_group_point));
		mPaintGroupPoly.setStyle(Style.FILL);
		mPaintGroupPoly.setAlpha(128);
	
		mWhiteOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
		mWhiteOutline.setColor(getResources().getColor(R.color.graph_view_point_outlines));
		mWhiteOutline.setTextAlign(Paint.Align.CENTER);
	
		mGroupSpot = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGroupSpot.setColor(getResources().getColor(R.color.graph_view_group_point));
		mGroupSpot.setTextAlign(Paint.Align.CENTER);
	
		mYouBarInside = new Paint(Paint.ANTI_ALIAS_FLAG);
		mYouBarInside.setColor(getResources().getColor(R.color.graph_view_you_point));
		mYouBarInside.setAlpha(128);
		mYouBarInside.setTextAlign(Paint.Align.CENTER);
		
		mYouBarStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		mYouBarStroke.setColor(getResources().getColor(R.color.graph_view_you_point));
		mYouBarStroke.setStrokeWidth(3);		
		
		mGroupLine = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGroupLine.setStrokeWidth(3);
		mGroupLine.setColor(getResources().getColor(R.color.graph_view_group_line));
		
		mGroupTop20Line = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGroupTop20Line.setStyle(Style.STROKE);
		mGroupTop20Line.setStrokeWidth(3);
		mGroupTop20Line.setAlpha(128);
		mGroupTop20Line.setPathEffect(new DashPathEffect(new float[] {10, 10}, 2));
		mGroupTop20Line.setColor(getResources().getColor(R.color.graph_view_group_line));
		
		
		mYouLine = new Paint(Paint.ANTI_ALIAS_FLAG);
		mYouLine.setStyle(Style.STROKE);
		mYouLine.setStrokeWidth(5);
		mYouLine.setColor(getResources().getColor(R.color.graph_view_you_line));
		
		//data label text paints
		mYouLabel = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FAKE_BOLD_TEXT_FLAG);
		mYouLabel.setColor(getResources().getColor(R.color.graph_view_you_point));
		mYouLabel.setStyle(Style.FILL);
		mYouLabel.setTextSize(3f * mDataPointOuterRadius);
		mYouLabel.setTextAlign(Paint.Align.CENTER);
	
		mGroupLabel = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FAKE_BOLD_TEXT_FLAG);
		mGroupLabel.setColor(getResources().getColor(R.color.graph_view_group_point));
		mGroupLabel.setStyle(Style.FILL);
		mGroupLabel.setTextSize(3f * mDataPointOuterRadius);
		mGroupLabel.setTextAlign(Paint.Align.CENTER);
		
		mWeekAvgBox = new Paint(Paint.ANTI_ALIAS_FLAG);
		mWeekAvgBox.setColor(Color.BLACK);
		mWeekAvgBox.setStyle(Style.FILL);
		mWeekAvgBox.setAlpha(180);
    }
    
    /**
     * Returns a set of interpolated points
     * @param t The t-value of the animation [0,1]
     * @param to The new values to reach from 0
     * @return The interpolated points
     */
    private float[] interpolatePoints(float t, float[] to)
    {
		float[] ps = new float[to.length];
		
		for(int i = 0;i <to.length;i++) {
			ps[i] = mMaxRange + ((to[i] - mMaxRange) * t);
		}		
		
		return ps;
    }
    
    /**
     * converts values of a field from ActivityRecord and returns a float array
     * @return
     */
    private float[] worldToView(ArrayList<ActivityRecord> in, int field, float max) 
    {
    	float[] out = new float[in.size()];    	
    	float range = mMaxRange - mMinRange;
		float ratio = range / max;
	
		for(int i = 0;i <in.size();i++) {
			out[i] = mMinRange + (mMaxRange - (in.get(i).getField(field) * ratio));
		}	
    	
    	return out;
    }
    
    /**
     * gets avg of a float array, ignoring future dates
     */
    private float getAvg(float[] arr) {    	
    	float avg = 0;
    	int count = 0;
    	for(int i=0;i<arr.length;i++) {
    		if(DateUtil.timelessComparison(mValues.get(i).getDate(), DateUtil.today()) <= 0) {
    			avg += arr[i];
    			count++;
    		}
    	}
    	return count > 0 ? avg / (float)count : 0;
    }
    
    /**
     * gets avg of specified field from a list of ActivityRecords, ignoring future dates
     */
    private float getAvg(ArrayList<ActivityRecord> arr, int field) {    	
    	float avg = 0;
    	int count = 0;
    	for(int i=0;i<arr.size();i++) {
    		if(DateUtil.timelessComparison(arr.get(i).getDate(), DateUtil.today()) <= 0) {
    			avg += arr.get(i).getField(field);
    			count++;
    		}
    	}
    	return count > 0 ? avg / (float)count : 0;
    }
      
    /**
     * set data from activity
     */
    public void setValues(ArrayList<ActivityRecord> values)
    {
    	mValues = values; 
    	
    	try {
    		if(!thread.isAlive())
    			thread.start();
    	}
    	catch(Exception e) {}
		
    	refresh();
    }
    
    /**
     * recalculates the viewspace coordinates used for drawing
     */
    protected void calculateViewCoords() 
    {
    	if(mValues == null || mValues.size() == 0)
    		return;
    	
    	mMe = worldToView(mValues,ActivityRecord.FIELD_ME, mMax);
    	mGroupTop20 = worldToView(mValues,ActivityRecord.FIELD_GROUPTOP, mMax);    	
    	mGroupAvg = worldToView(mValues,ActivityRecord.FIELD_GROUPAVG, mMax);
    	   	
    	// calculate the index of today (if any)
    	mTodayIdx = -1;
    	int i = 0;
    	for(ActivityRecord a : mValues) {
    		if(a.getDate() != null)
    			if(DateUtil.timelessComparison(a.getDate(),DateUtil.today()) == 0)
    				mTodayIdx = i;
    		i++;
    	}
    }
    
    /**
     * set the column labels from activity
     * @param labels
     */
    public void setLabels(String[] labels)
    {
		mLabels = labels;
		mNumberOfColums = mLabels.length;
    }

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
	}

	/**
	 * sets the 'week mode' that dictates the order in which the days are drawn
	 */
    public void setWeekMode(int weekMode) {
    	mWeekMode = weekMode;
    }
    
    /**
     * whether or not to show the week's average value
     */
    public void setShowWeekAvg(boolean show) {
    	mShowWeekAvg = show;
    }

    /**
     * shows info for given day index via a toast popup
     * @param i
     */
	public void showDayInfo(int i) 
	{
		if(i < 0 || i > 6)
			return;
    	if(mValues == null || mValues.size() == 0)
    		return;
		
		ActivityRecord ar = mValues.get(i);

		// is this activity record in the future?
		if(DateUtil.timelessComparison(ar.getDate(),DateUtil.today()) > 0)
			return;
		
		SimpleDateFormat sdf = new SimpleDateFormat("c dd/MM");
		Date d = ar.getDate().getTime();
		String toastStr = "On " + sdf.format(d) + ", You: " + (int)ar.getMe();
		toastStr += " steps";
		
		if(Globals.groupFeedback())
			toastStr += "; Group average: " + (int)ar.getAvg() + " steps"; 		
		
		if(DateUtil.timelessComparison(ar.getDate(),DateUtil.today()) == 0)
			toastStr += " so far";
		
		TameToaster.showToast(this.getContext(), toastStr, Toast.LENGTH_LONG);
	}

	/**
	 * informs the graph view of the maximum value of the total dataset represented by the graph
	 * so that the graph view can normalise the data within correct boundaries
	 */
	public void setMax(float max) {
		mMax = max;
	}
}
