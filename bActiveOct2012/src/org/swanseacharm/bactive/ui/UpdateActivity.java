package org.swanseacharm.bactive.ui;

import org.swanseacharm.bactive.Updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class UpdateActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState)
	{ 
		super.onCreate(savedInstanceState);
		setContentView(org.swanseacharm.bactive.R.layout.update_activity);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		AlertDialog.Builder dlg = new AlertDialog.Builder(this);
		dlg.setTitle("Update available");
		dlg.setMessage("An update is available for bActive. Would you like to install it now?");
		dlg.setPositiveButton("Install now", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { 
				Updater.install(UpdateActivity.this);
				finish();
			}
		});
		dlg.setNegativeButton("Remind me later", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				Updater.setPromptDismissedTime(UpdateActivity.this);
				finish();
			}
		});
		dlg.show();
	}
}
