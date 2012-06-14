package de.uvwxy.footpath2.log;

import java.util.LinkedList;

import android.os.Handler;

/**
 * Has the same methods as the interface Exporter but does not implement this to avoid linked circle.
 * 
 * @author Paul Smith
 * 
 */
public class ExportManager {
	private static ExportManager thisInstance = null;
	private static LinkedList<Exporter> exporters = new LinkedList<Exporter>();
	private ExportBehavior behavior = ExportBehavior.RECENTDATA;
	
	public enum ExportBehavior {
		ALLDATA,
		ALLDATA_ANDCLEARALL,
		RECENTDATA,
		RECENTDATA_ANDCLEARALL,
		CLEARALLONLY
	}
	
	public static ExportManager getInstance() {
		if (thisInstance == null) {
			thisInstance = new ExportManager();
		}
		return thisInstance;
	}

	private ExportManager() {

	}

	public void setBehavior(ExportBehavior b) {
		this.behavior = b;
	}
	
	public void add(Exporter e) {
		exporters.add(e);
	}

	public void remove(Exporter e) {
		exporters.remove(e);
	}

	public int size() {
		return exporters.size();
	}

	public void startIntervalExporting(long ms) {
		delayMillis = ms;
		unPauseHandler();
	}
	
	public void stopIntervalExporting() {
		pauseHandler();
	}

	public int export_allData(String path) {
		int sum = 0;
		for (int i = 0; i < exporters.size(); i++) {
			sum+= exporters.get(i).export_allData(path);
		}
		return sum;
	}

	public int export_recentData(String path) {
		int sum = 0;
		for (int i = 0; i < exporters.size(); i++) {
			sum+= exporters.get(i).export_recentData(path);
		}
		return sum;
	}

	public int export_clearData() {
		int sum = 0;
		for (int i = 0; i < exporters.size(); i++) {
			sum+= exporters.get(i).export_clearData();
		}
		return sum;
	}

	public int export_consumedBytes() {
		int sum = 0;
		for (int i = 0; i < exporters.size(); i++) {
			sum += exporters.get(i).export_consumedBytes();
		}
		return sum;
	}
	
	// ###############################################################
	// Below comes all the timer/handler stuff for intervall exporting
	
	private Handler mHandler = new Handler();
	private long delayMillis = 1000;

	private Runnable mUpdateTimeTask = new Runnable() {

		public void run() {

			switch(behavior){
			case ALLDATA:
				break;
			case ALLDATA_ANDCLEARALL:
				break;
			case CLEARALLONLY:
				break;
			case RECENTDATA:
				break;
			case RECENTDATA_ANDCLEARALL:
				break;
			}
			mHandler.postDelayed(this, delayMillis);
		}
	};

	private void pauseHandler() {
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	private void unPauseHandler() {
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, delayMillis);
	}
}
