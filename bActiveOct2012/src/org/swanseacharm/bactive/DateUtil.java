package org.swanseacharm.bactive;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Misc static date utilities
 * @author Simon Walton
 */
public class DateUtil 
{
	public static Calendar today() {
		Calendar c = new GregorianCalendar();
		c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		return c;
	}
	
	public static Calendar yesterday() {
		Calendar y = today();
		y.add(Calendar.DATE, -1);
		return y;
	}
	
	public static Calendar epoch() {
		Calendar c = new GregorianCalendar();
		c.setTimeInMillis(0);
		return c;
	}
	
	/**
	 * returns given date (cloned) plus 'days' days added (can be negative)
	 */
	public static Calendar plusDays(Calendar d, int days) {
		Calendar c = (Calendar)d.clone();
		c.add(Calendar.DATE, days);
		return c;
	}
	
	/**
	 * gives a Calendar object converted from given Date object (strips time)
	 */
	public static Calendar calendarFromDate(Date d) {
		Calendar c = new GregorianCalendar();
		c.setTimeInMillis(0);
		c.set(d.getYear() + 1900, d.getMonth(), d.getDate(), 0,0,0);
		return c;
	}
	
	/**
	 * returns formatted date in short form (no year)
	 */
	public static String formatShort(Calendar c) {
		java.text.DecimalFormat fmt = new java.text.DecimalFormat("00");
		return fmt.format(new Integer(c.get(Calendar.DAY_OF_MONTH))) + "/" + fmt.format(new Integer(c.get(Calendar.MONTH) + 1));
	}
	
	/**
	 * Gets formatted string from given date suitable for use with SQL queries
	 */
	public static String getSQLFormatted(Calendar c)
	{
    	String str = "";
    	try {
    		java.text.DecimalFormat f = new java.text.DecimalFormat("00");
    		str = c.get(Calendar.YEAR) + "-" + f.format(new Integer(c.get(Calendar.MONTH) + 1)) + "-" + f.format(new Integer(c.get(Calendar.DATE)));
    	}
    	catch(Exception e) {}
    	return str;
	}
	
	/**
	 * Attempts to parse given string for a valid date; returns epoch if failure
	 */
	public static Calendar parseSQLFormatted(String s)
	{
		Calendar c = epoch();
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date d = sdf.parse(s);
			c.set(d.getYear() + 1900, d.getMonth(), d.getDate(), 0, 0, 0);
    	}
    	catch(Exception e) {}
    	
    	return c;
	}
	
	/**
	 * compares two dates without considering time
	 * @return <0 if c1 < c2; 0 if c1 == c2; >0 if c1 > c2
	 */
	public static int timelessComparison(Calendar c1, Calendar c2)
	{
		if (c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR)) 
	        return c1.get(Calendar.YEAR) - c2.get(Calendar.YEAR);
	    if (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH)) 
	        return c1.get(Calendar.MONTH) - c2.get(Calendar.MONTH);
	    return c1.get(Calendar.DAY_OF_MONTH) - c2.get(Calendar.DAY_OF_MONTH);
	}
	
	/**
	 * is a date between two other dates?
	 * @param dateToTest The date to test
	 * @param from The beginning of the test period
	 * @param to The end of the test period
	 * @return True if dateToTest is within the from/to period
	 */
	public static boolean withinPeriod(Calendar dateToTest, Calendar from, Calendar to) {
		int r1 = DateUtil.timelessComparison(dateToTest, from);
		int r2 = DateUtil.timelessComparison(dateToTest, to);
		
		return r1 == 0 || r2 == 0 || (r1 > 0 && r2 < 0);
	}
	
	/**
	 * is today's date within the given period?
	 * @param from The beginning of the test period
	 * @param to The end of the test period
	 * @return True if dateToTest is within the from/to period
	 */
	public static boolean todayWithinPeriod(Calendar c1, Calendar c2) {
		return DateUtil.withinPeriod(DateUtil.today(), c1, c2);
	}
}
