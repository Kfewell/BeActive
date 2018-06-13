package org.swanseacharm.bactive.ui;

import org.swanseacharm.bactive.TrafficCounter;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class ViewDataUsage extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
  
		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, getUsageStringArray()));
  
		getListView().setTextFilterEnabled(true);
	}
	
	protected String[] getUsageStringArray() {
		return new String[] {
				"All: " + TrafficCounter.getFormattedAllCount(),
				"Mobile only: " + TrafficCounter.getFormattedMobileCount() };
	}
}
