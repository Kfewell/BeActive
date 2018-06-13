package org.swanseacharm.bactive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.swanseacharm.bactive.database.DatabaseProxy;
import org.swanseacharm.bactive.database.UsageProxy;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * web service data gathering functionality
 */
public class WebServiceProxy
{
    private final static String BASE_URL = "http://cs.swansea.ac.uk/~charm/charmAPI_MainStudy/index.php/";
    private final String PHONE_ID = "phoneId";
    private final String DATE = "date";
    private final String DATE_TIME = "dateTime";
    
    private final String ACTIVITY = "activity";
    private final String GROUP_MIN = "min";
    private final String GROUP_MAX = "max";
    private final String GROUP_AVG = "avg";
    private final String GROUP_TOP = "top";
    private final String ME = "me";
    
    private final String FOR_DATE = "forDate"; 
    
    private final String US_RECORDS = "usage_records";

    private final String START_DATE = "startDate";
    private final String END_DATE = "endDate";
    private final String GROUP = "group";
    
    private final static long ONE_DAY = 86400000;

    /**
     * Sends an individual activity record to the server
     * @param c Android context
     * @param ar The Activity Record to send
     * @return true if the data was all sent successfully; false otherwise
     * @author Simon Walton
     */
    private boolean sendActivityRecord(Context c, ActivityRecord ar)
    {
    	boolean success = false;
    	
		String json = "";
		try
		{
		    JSONObject data = new JSONObject();
	
		    TelephonyManager manager = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
	
		    data.put(PHONE_ID, manager.getDeviceId());	
		    data.put(DATE_TIME, DateUtil.getSQLFormatted(ar.getDate()));
		    data.put(ACTIVITY,"" + (int)ar.getMe());
	
		    json = data.toString();
	
		    Log.v("mubaloo","json = " + json);
	
		}
		catch (JSONException e){e.printStackTrace();}
	
		// Create a new HttpClient and Post Header  
		HttpClient httpClient = new DefaultHttpClient();  
		HttpPost httpPost = new HttpPost(BASE_URL + "writeData/"); 
	
		try 
		{  
		    // Add data to post header  
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
		    nameValuePairs.add(new BasicNameValuePair("parameter", json)); 
		    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
		   // httpPost.setHeader("Accept-Encoding", "gzip");
	
		    // Execute HTTP Post Request  
		    TrafficCounter.begin(this);
		    HttpResponse response = httpClient.execute(httpPost);
		    TrafficCounter.end(this);
	
		    String responseString = responseAsString(response);
	
		    Log.v("mubaloo","result = " + responseString); 
	
		    JSONObject jsonResponse  = new JSONObject(responseString);
		    JSONObject result = jsonResponse.getJSONObject("result");
	
		    String status  = result.getString("status");
	
		    if(status.equals("OK")) {
				//(new DatabaseProxy(c)).markAsSent(ar);
				success = true;
		    } 
	
		} 
		catch (ClientProtocolException e) {e.printStackTrace();} 
		catch (IOException e) {e.printStackTrace();}
		catch (JSONException e) { e.printStackTrace();}
		
		return success;
    }
    
    /**
     * sends today's step count so far, plus any unsent records from previous days.
     * we do this with a simple loop because sending data from previous days should very rarely occur.
     * @param context
     * @return
     */
    public boolean sendTodayAndUnsentData(Context context)
    {
		DatabaseProxy dbProxy = new DatabaseProxy(context);
		ArrayList<ActivityRecord> records = dbProxy.getTodayAndUnsent();
				
		for(ActivityRecord ar : records) {
			if(ar.getDate() == null)
				continue;
			
			boolean isTodays = DateUtil.timelessComparison(ar.getDate(),DateUtil.today()) == 0;
			
			// if it's today's record, then update the time of the record
			if(isTodays)
				ar.setDate(DateUtil.today());
			
			// try sending
			if(sendActivityRecord(context, ar)) {
				// success. if the record is not today's (yesterday or older), then we need to mark it as sent
				if(!isTodays) {
					// the activity record is not today's; so mark as sent
					dbProxy.markAsSent(ar);
				}
			}
				
		}
		
		return true;
    }
    
    /**
     * Sends statistics on usage to the server - see UsageProxy.java
     * @author Simon Walton
     */
    public boolean sendUsageStats(Context context)
    {  	
		boolean success = false;
	
		String json = "";
		try
		{
			// populate JSON array of usage stats
			JSONArray array = new JSONArray();
			ArrayList<UsageRecord> records = UsageProxy.getUnsentUsage(context);
			
			// bail if there's no usage data to send
			if(records.size() == 0) 
				return true;
			
			// shove the records into an array of JSON objects
			for(UsageRecord record : records) {
				array.put(record.toJSONObject());
			}
			
			// compile into a JSON string
			TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		    String androidID = manager.getDeviceId();
	
		    JSONObject mainJSONObject = new JSONObject();
		    mainJSONObject.put(PHONE_ID, androidID);
		    mainJSONObject.put(US_RECORDS, array);
		    
		    json = mainJSONObject.toString();
	
		    Log.v("USAGE","json = " + json);
		}
		catch (JSONException e){e.printStackTrace();}
	
		// Create a new HttpClient and Post Header  
		HttpClient httpClient = new DefaultHttpClient();  
		HttpPost httpPost = new HttpPost(BASE_URL + "writeUsageData/"); 
	
		try 
		{  
		    // Add data to post header  
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
		    nameValuePairs.add(new BasicNameValuePair("parameter", json)); 
		    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs)); 
		   // httpPost.setHeader("Accept-Encoding", "gzip");
		   // httpPost.setHeader("Content-Encoding", "GZIP");
	
		    // Execute HTTP Post Request  
		    TrafficCounter.begin(this);
		    HttpResponse response = httpClient.execute(httpPost);
		    TrafficCounter.end(this);
	
		    String responseString = responseAsString(response);
	
		    Log.v("USAGE","result = " + responseString);
	
		    JSONObject jsonResponse  = new JSONObject(responseString);
		    JSONObject result = jsonResponse.getJSONObject("result");
	
		    String status  = result.getString("status");
	
		    if(status.equals("OK"))
		    {
		    	// all sent to server - now remove from internal db
				UsageProxy.removeSentUsageData(context);
				success = true;
		    }
	
		} 
		catch (ClientProtocolException e) {e.printStackTrace();} 
		catch (IOException e) {e.printStackTrace();}
		catch (JSONException e) { e.printStackTrace();}
		
		return success;
    }
    
    /**
     * Asks the server whether an update is available based on our current version code. 
     * @param versionCode This application's current version code to send to the server
     * @return The URL of the updated .APK file if there is an update; empty string otherwise.
     */
    public static String getUpdateAPKURL(Context context, int versionCode) 
    {
		String json = "";
		try
		{
		    JSONObject data = new JSONObject();
	
		    TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		    data.put("phoneId", manager.getDeviceId());	
		    data.put("versionCode", versionCode);
	
		    json = data.toString();
	
		}
		catch (JSONException e){e.printStackTrace();}
	
		// Create a new HttpClient and Post Header  
		HttpClient httpClient = new DefaultHttpClient();  
		HttpPost httpPost = new HttpPost(BASE_URL + "update/"); 
	
		String APKURL = "";
		
		try {		
		    // Add data to post header  
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
		    nameValuePairs.add(new BasicNameValuePair("parameter", json)); 
		    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
		
		    // Execute HTTP Post Request  
		    TrafficCounter.begin(context); 
		    HttpResponse response = httpClient.execute(httpPost);
		    TrafficCounter.end(context);
	
		    String responseString = responseAsString(response);
	
		    JSONObject jsonResponse  = new JSONObject(responseString);
		    JSONObject result = jsonResponse.getJSONObject("result");
	
		    int serverVersionCode = Integer.parseInt(result.getString("versionCode"), 10);
		    if(serverVersionCode > versionCode)
		    	APKURL  = result.getString("APKURL");
		}
		catch(Exception e) {}
		
		return APKURL;		
	}

   /**
    * gets averaged data for the whole group for a given date
    */
    public ActivityRecord getGroupDataByDate(Context c, Calendar day) throws Exception
    {
    	return getGroupDataByDateRange(c,day,day).get(0);
    }

    /**
     * gets averaged data for the whole group for a given date range
     */
    public ArrayList<ActivityRecord> getGroupDataByDateRange(Context context, Calendar startDate, Calendar endDate) throws Exception
    {
		ArrayList<ActivityRecord> groupArs = new ArrayList<ActivityRecord>(); 
	
		String json = "";
		try
		{
		    JSONObject data = new JSONObject();
	
		    String startDateString = DateUtil.getSQLFormatted(startDate);
		    String endDateString = DateUtil.getSQLFormatted(endDate);
		    TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		    // if we're requesting only today, then update today's stepcount also with the request
		    if(DateUtil.timelessComparison(startDate, DateUtil.today()) == 0 && DateUtil.timelessComparison(endDate, DateUtil.today()) == 0) {
		    	DatabaseProxy dbProxy = new DatabaseProxy(context);
		    	ActivityRecord ar = dbProxy.getActivityRecordByDate(startDate);
		    	data.put(ME, (int)ar.getMe());
		    }
		   // else data.put(ME, "-1");
		    
		    data.put(START_DATE, startDateString);
		    data.put(END_DATE, endDateString);
		    data.put(PHONE_ID, manager.getDeviceId());	
	
		    json = data.toString();
	
		    Log.v("mubaloo","json = " + json);
		}
		catch (JSONException e){e.printStackTrace();}
	
		// Create a new HttpClient and Post Header  
		HttpClient httpClient = new DefaultHttpClient();  
		HttpPost httpPost = new HttpPost(BASE_URL + "readData/"); 
	
	    // Add data to post header  
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	    nameValuePairs.add(new BasicNameValuePair("parameter", json)); 
	    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
	  //  httpPost.setHeader("Accept-Encoding", "gzip");


	    // Execute HTTP Post Request  
	    TrafficCounter.begin(this); 
	    HttpResponse response = httpClient.execute(httpPost);
	    TrafficCounter.end(this);

	    String responseString = responseAsString(response);
	    Log.v("mubaloo","result = " + responseString);

	    JSONObject jsonResponse  = new JSONObject(responseString);
	    JSONObject stepData = jsonResponse.getJSONObject("stepData");

	    JSONArray results = stepData.getJSONArray("results"); 

	    Log.v("mubaloo","results length = " + results.length());


	    for(int i = 0; i < results.length(); i++)
	    {
			ActivityRecord ar = new ActivityRecord();
			JSONObject result = results.getJSONObject(i);
	
			Log.v("mubaloo", result.toString());
	
			ar.setDate(DateUtil.parseSQLFormatted(result.getString(DATE)));
			ar.setMin((float)result.getDouble(GROUP_MIN));
			ar.setMax((float)result.getDouble(GROUP_MAX));
			ar.setAvg((float)result.getDouble(GROUP_AVG));
			ar.setTop((float)result.getDouble(GROUP_TOP));
	
			groupArs.add(ar);
	    }

		//pad any missing dates with empty ARs in case data is missing
		return padMissingDates(groupArs,startDate,endDate);
    }

    /**
     * Given a list of activity records, returns a new list padded with blank records for specified date range
     */
    public static ArrayList<ActivityRecord> padMissingDates(ArrayList<ActivityRecord> sourceARs, Calendar startDate, Calendar endDate)
    {
		ArrayList<ActivityRecord> paddedARs = new ArrayList<ActivityRecord>();
		Calendar d = (Calendar)startDate.clone();
		int i = 0;
		
		while(DateUtil.timelessComparison(d,endDate) <= 0)
		{
		    if(i < sourceARs.size() && DateUtil.timelessComparison(sourceARs.get(i).getDate(),d) == 0)
		    {
				// have an entry for this date so just add it to padded ARs
				paddedARs.add(sourceARs.get(i));
				i++;
		    }
		    else
		    {
				paddedARs.add(new ActivityRecord());
		    }
		    
		    d.add(Calendar.DATE, 1);
		}
		
		return paddedARs;
    }

    //convert http response to a string
    private static String responseAsString(HttpResponse response) throws IllegalStateException, IOException 
    {

		InputStream is =  response.getEntity().getContent();
		if (is != null) 
		{
		    StringBuilder sb = new StringBuilder();
		    String line;
		    try 
		    {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				while ((line = reader.readLine()) != null) 
				{
				    sb.append(line);
				}
		    } 
		    finally 
		    {
		    	is.close();
		    }
	
		    return sb.toString();
		}
		return "";
    }
}
