package org.swanseacharm.bactive;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A single record of the user entering and exiting the application
 * @author Simon Walton
 *
 */
public class UsageRecord 
{
	private long timeIn,timeOut;
	
	public static final String US_TIME_IN = "timeIn";
	public static final String US_TIME_OUT = "timeOut";
	
	public UsageRecord(long timeIn, long timeOut) {
		this.timeIn = timeIn;
		this.timeOut = timeOut;
	}
	
	public long getTimeIn() {
		return timeIn;
	}
	
	public long getTimeOut() {
		return timeOut;
	}
	
	public JSONObject toJSONObject() {
		JSONObject o = new JSONObject();
		try {
			o.put(US_TIME_IN, timeIn);
			o.put(US_TIME_OUT, timeOut);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return o;
	}
}
