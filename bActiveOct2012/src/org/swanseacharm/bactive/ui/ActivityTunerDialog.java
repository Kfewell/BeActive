package org.swanseacharm.bactive.ui;

import org.swanseacharm.bactive.ActivityMonitor;
import org.swanseacharm.bactive.R;
import org.swanseacharm.bactive.Wakeful;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

public class ActivityTunerDialog extends Dialog implements SeekBar.OnSeekBarChangeListener, OnClickListener {
	
	private static final int mSeekBarMin = 100;
	Context mContext;
	
	public ActivityTunerDialog(Context c) {
		super(c);
		mContext = c;
	}
	
	public static float defaultThreshold() {
		return mSeekBarMin;
	} 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tuner_dialog);
		
		SeekBar sb = (SeekBar)findViewById(R.id.tunerSeekBar);
		sb.setOnSeekBarChangeListener(this);
		
		Button b = (Button)findViewById(R.id.tunerButton);
		b.setOnClickListener(this);
		
		sb.setProgress((int)ActivityMonitor.MAG_LOWER_THRESHOLD_NONSQRT - mSeekBarMin);
		
		updateTextView();
		informActivityMonitor();
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
	{
		if(fromUser){
			updateTextView();
			informActivityMonitor();
		}
	}
	
	private void informActivityMonitor() {
		Intent i = new Intent(ActivityMonitor.ACTIVITY_TUNER_BROADCAST);
		i.putExtra("threshold", (double)seekBarActualValue());
    	mContext.getApplicationContext().sendBroadcast(i);
	}
	
	private void updateTextView() {
		TextView tv = (TextView)findViewById(R.id.tunerTextView);
		tv.setText(seekBarActualValue() + "");
	}
	
	private int seekBarActualValue() {
		SeekBar sb = (SeekBar)findViewById(R.id.tunerSeekBar);
		return sb.getProgress() + mSeekBarMin;
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.tunerButton) {
			this.dismiss();
		}
	}
}
