package qut.pm.util;

import java.util.Date;

public class ClockUtil {

	public static long currentTimeMillis() {
		return System.currentTimeMillis();
	}
	
	public static long nanoTime() {
		return System.nanoTime();
	}	
	
	public static Date currentTime() {
		return new Date();
	}
	
}
