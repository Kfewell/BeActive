package org.swanseacharm.bactive.ui;

import org.swanseacharm.bactive.R;
import org.swanseacharm.bactive.R.layout;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activity shown to control group upon attempting to start app (group 1)
 * @author Simon Walton
 */
public class ControlPlaceholder extends Activity 
{
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(org.swanseacharm.bactive.R.layout.control_placeholder);
	}
}
