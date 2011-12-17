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
	private static boolean v = true;
	private static boolean vv = true;
	private static boolean vvv = false;
	
	/**
	 * We have all learnt that this should be a singleton object. But as we just
	 * want to have things working right _now_ it aint ;)
	 * @param v
	 * @param vv
	 * @param vvv
	 */
	public DebugOut(boolean v, boolean vv, boolean vvv) {
		super();
		DebugOut.v = v;
		DebugOut.vv = vv;
		DebugOut.vvv = vvv;
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
