package qut.pm.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ClockUtil {

	private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("YYYYMMdd-hhmmss");
	
	public static long currentTimeMillis() {
		return System.currentTimeMillis();
	}
	
	public static long nanoTime() {
		return System.nanoTime();
	}	
	
	public static Date currentTime() {
		return new Date();
	}

	public static String dateTime() {
		return DATE_FORMAT.format(currentTime());
	}
	
}
