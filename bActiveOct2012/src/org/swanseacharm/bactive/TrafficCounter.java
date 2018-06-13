package org.swanseacharm.bactive;
import java.text.DecimalFormat;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import android.net.TrafficStats;

/**
 * A traffic logger for measuring bActive's data usage over mobile networks.
 * Measures global traffic consumption for short periods and keeps a total 
 * @author Simon Walton
 */
public class TrafficCounter 
{	
	private static Map m = new HashMap();
	private static long mMobRxBytes = 0, mMobTxBytes = 0;
	private static long mAllRxBytes = 0, mAllTxBytes = 0;
	
	/**
	 * Call before measuring some data activity
	 * @param caller: the object doing the calling (use 'this')
	 */
	public static void begin(Object caller) {
		synchronized(m) {
			long[] txrx = new long[] { TrafficStats.getMobileTxBytes(),
				 TrafficStats.getMobileRxBytes(),
				 TrafficStats.getTotalTxBytes(),
				 TrafficStats.getTotalRxBytes()
				 };
			m.put(caller, txrx);
		}		
	}
	
	/**
	 * Call once the data operation has completed to stop measuring
	 * @param caller: the object that previously called (use 'this')
	 */
	public static void end(Object caller) {
		synchronized(m) {
			try {
				long[] rxtx = (long[])m.get(caller);				
				mMobTxBytes += TrafficStats.getMobileTxBytes() - rxtx[0];
				mMobRxBytes += TrafficStats.getMobileRxBytes() - rxtx[1];
				mAllTxBytes += TrafficStats.getTotalTxBytes() - rxtx[2];
				mAllRxBytes += TrafficStats.getTotalRxBytes() - rxtx[3];
			}
			catch(Exception ex) { ex.printStackTrace(); }
		}		
	}

	/**
	 * Gets the total bytes transmitted (sent and received) by the mobile transmitter
	 * @return Byte count
	 */
	public static long getMobileByteCount() {
		return mMobRxBytes + mMobTxBytes;
	}
	
	/**
	 * Gets the total bytes transmitted (sent and received) by all transmitters (inc wifi etc)
	 * @return Byte count
	 */
	public static long getAllByteCount() {
		return mAllRxBytes + mAllTxBytes;
	}
	
	private static String formatDataString(long bytes) {
		if(bytes < 1024)
			return bytes + " bytes";
		else return  (new DecimalFormat("#.##").format(bytes / 1024.0f)) + " KB";
	}
	
	public static String getFormattedMobileCount() {		
		return formatDataString(getMobileByteCount()) + 
			" (" + formatDataString(mMobTxBytes) + " sent; " + formatDataString(mMobRxBytes) + " received)";
	}
	
	public static String getFormattedAllCount() {		
		return formatDataString(getAllByteCount()) + 
			" (" + formatDataString(mAllTxBytes) + " sent; " + formatDataString(mAllRxBytes) + " received)";
	}
	
}
