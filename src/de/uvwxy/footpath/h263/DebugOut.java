package de.uvwxy.footpath.h263;

import android.util.Log;

/**
 * 
 * @author Paul Smith
 * 
 * THE ANDROID VERSION for posting messages with Log.i(...)
 *
 */
public class DebugOut {
	private static boolean v = false;
	private static boolean vv = false;
	private static boolean vvv = false;
	
	public static void setV(boolean flag){
		v = flag;
	}
	public static void setVV(boolean flag){
		vv = flag;
	}
	public static void setVVV(boolean flag){
		vvv = flag;
	}
	

	public static void debug_v(String s) {
		if (v)
			Log.i("FLOWPATH",s);
	}

	public static void debug_vv(String s) {
		if (vv)
			Log.i("FLOWPATH",s);
	}

	public static void debug_vvv(String s) {
		if (vvv)
			Log.i("FLOWPATH",s);
	}
}
