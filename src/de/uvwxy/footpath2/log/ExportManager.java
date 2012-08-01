package de.uvwxy.footpath2.log;

import java.io.File;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

/**
 * This is the key control object of logging/exporting data. You can either manually trigger export events, or call a
 * continuous timed event. The timed event will reschedule a delay of a given MS to retrigger the export behavior after
 * it has finished exporting data.
 * 
 * Either set ExportBehavior and byteThreshold = 0, or byteThreshold is used.
 * 
 * Has the same methods as the interface Exporter but does not implement this to avoid linked circle.
 * 
 * @author Paul Smith
 * 
 */
public class ExportManager {
	private static ExportManager thisInstance = null;
	private static TreeMap<String, Exporter> exporters = new TreeMap<String, Exporter>();
	private IntervalExportBehavior behavior = IntervalExportBehavior.EXPORT_RECENTDATA;
	private static String directory = "footpath_exports/";
	private static String subdirectory = null;
	private int byteThreshold = 0; // number of bytes or 0 for direct save

	public enum IntervalExportBehavior {
		EXPORT_ALLDATA, EXPORT_AND_CLEAR_ALLDATA, EXPORT_RECENTDATA, EXPORT_AND_CLEAR_RECENTDATA, CLEAR_ALLDATA
	}

	public static ExportManager getInstance() {
		if (thisInstance == null) {
			thisInstance = new ExportManager();
		}
		return thisInstance;
	}

	private ExportManager() {

	}

	/**
	 * Set the log out put directory. Will save in the external storage directory.
	 * 
	 * @param directory
	 *            of the form "directory/"
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getDirectory() {
		return directory;
	}

	public String getSubdirectory() {
		if (subdirectory == null) {
			subdirectory = "" + System.currentTimeMillis();

			// check if first dir exists
			File dir = new File(Environment.getExternalStorageDirectory(), directory);
			if (!dir.exists()) {
				dir.mkdir();
			}

			// check if second dir exists
			dir = new File(Environment.getExternalStorageDirectory(), directory + subdirectory);
			if (!dir.exists()) {
				dir.mkdir();
			}
		}
		return subdirectory;
	}

	public String updateSubdirectory() {
		subdirectory = "" + System.currentTimeMillis();

		// check if first dir exists
		File dir = new File(Environment.getExternalStorageDirectory(), directory);
		if (!dir.exists()) {
			dir.mkdir();
		}

		// check if second dir exists
		dir = new File(Environment.getExternalStorageDirectory(), directory + subdirectory);
		if (!dir.exists()) {
			dir.mkdir();
		}
		
		return subdirectory;
	}

	public void add(String key, Exporter e) {
		exporters.put(key, e);
	}

	public void remove(String key) {
		exporters.remove(key);
	}

	public int size() {
		return exporters.size();
	}

	public void startIntervalExporting(long ms) {
		delayMillis = ms;
		if (mHandlerStop = true) // only start once!
			unPauseHandler();
	}

	public void stopIntervalExporting() {
		pauseHandler();
	}

	public void setBehavior(IntervalExportBehavior b) {
		this.behavior = b;
	}

	/**
	 * Set the byte threshold that has to be reached to export data.
	 * 
	 * @param b
	 */
	public void setByteThreshold(int b) {
		this.byteThreshold = b;
	}

	public int export_allData() {
		long ts = System.currentTimeMillis();
		int sum = 0;
		Log.i("FOOTPATH", "Starting all data export");
		Set<Entry<String, Exporter>> set = exporters.entrySet();
		for (Entry<String, Exporter> item : set) {
			sum += item.getValue().export_allData(item.getKey());
		}
		Log.i("FOOTPATH", "... exported " + sum + " entries (" + (System.currentTimeMillis() - ts) + "ms)");
		return sum;
	}

	public int export_recentData() {
		long ts = System.currentTimeMillis();
		int sum = 0;
		Log.i("FOOTPATH", "Starting recent data export");
		Set<Entry<String, Exporter>> set = exporters.entrySet();
		for (Entry<String, Exporter> item : set) {
			sum += item.getValue().export_recentData(item.getKey());
		}
		Log.i("FOOTPATH", "... exported " + sum + " entries (" + (System.currentTimeMillis() - ts) + "ms)");
		return sum;
	}

	public int export_clearRecentData() {
		long ts = System.currentTimeMillis();
		int sum = 0;
		Log.i("FOOTPATH", "Starting recent data clear");
		Set<Entry<String, Exporter>> set = exporters.entrySet();
		for (Entry<String, Exporter> item : set) {
			sum += item.getValue().export_clearRecentData();
		}
		Log.i("FOOTPATH", "... cleared " + sum + " entries (" + (System.currentTimeMillis() - ts) + "ms)");
		return sum;
	}

	public int export_clearAllData() {
		long ts = System.currentTimeMillis();
		int sum = 0;
		Log.i("FOOTPATH", "Starting all data clear");
		Set<Entry<String, Exporter>> set = exporters.entrySet();
		for (Entry<String, Exporter> item : set) {
			sum += item.getValue().export_clearAllData();
		}
		Log.i("FOOTPATH", "... cleared " + sum + " entries (" + (System.currentTimeMillis() - ts) + "ms)");
		return sum;
	}

	public int export_consumedBytes() {
		long ts = System.currentTimeMillis();
		int sum = 0;
		Log.i("FOOTPATH", "Starting gathering of consumed bytes");
		Set<Entry<String, Exporter>> set = exporters.entrySet();
		for (Entry<String, Exporter> item : set) {
			sum += item.getValue().export_consumedBytes();
		}
		Log.i("FOOTPATH", "... consuming " + sum + " bytes (" + (System.currentTimeMillis() - ts) + "ms)");
		return sum;
	}

	// ###############################################################
	// Below comes all the timer/handler stuff for intervall exporting

	private Handler mHandler = new Handler();
	private long delayMillis = 1000;

	private Runnable mUpdateTimeTask = new Runnable() {

		public void run() {
			int numBytes = export_consumedBytes();
			if (byteThreshold == 0) {

				switch (behavior) {
				case EXPORT_ALLDATA:
					export_allData();
					break;
				case EXPORT_AND_CLEAR_ALLDATA:
					export_allData();
					export_clearAllData();
					break;
				case EXPORT_RECENTDATA:
					export_recentData();
					break;
				case EXPORT_AND_CLEAR_RECENTDATA:
					export_recentData();
					export_clearRecentData();
					break;
				case CLEAR_ALLDATA:
					export_clearAllData();
				}
			} else {
				if (numBytes > byteThreshold) {
					export_allData();
					export_clearAllData();
				} else {
					Log.i("FOOTPATH", "Not enough data to export (" + numBytes + ")");
				}
			}
			if (!mHandlerStop)
				mHandler.postDelayed(this, delayMillis);
		}
	};

	private boolean mHandlerStop = false;

	private void pauseHandler() {
		mHandlerStop = true;
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	private void unPauseHandler() {
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandlerStop = false;
		mHandler.postDelayed(mUpdateTimeTask, delayMillis);
	}
}
