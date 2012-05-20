package de.uvwxy.footpath2.tools;

import java.io.FileNotFoundException;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import de.uvwxy.footpath.Rev;

/**
 * Interface to log compass, accelerometer, GPS, WiFi and route Use startLogging() to register file writers and GPS/WiFi
 * interfaces Use log[..](params) from class using the DataLogger interface GPS/WiFi Logging will be handled in this
 * object Use stopLogging() to close everything mentioned above
 * 
 * @author Paul Smith
 * 
 */
public class DataLogger {
	// Objects to work with
	private long routeID;
	private final String from;
	private final String to;

	private boolean started = false;
	private boolean accOpen = false; // To check if files have been opened successfully
	private boolean compOpen = false;

	private FWriter fwCompass; // Log data to /sdcard/routelog/
	private FWriter fwAccelerometer;
	private FWriter fwVariance;
	private FWriter fwPosition;
	private FWriter fwSteps;
	private FWriter fwGPS;
	private FWriter fwWifi;
	private FWriter fwRawAccel;
	private FWriter fwRawCompass;
	private FWriter fwRoute;
	private FWriter fwSimpleRoute;
	private FWriter fwInfo;
	private LocationManager locationManager = null;

	private WifiManager wm01;
	private WifiReceiver wr01;
	private List<ScanResult> lScanResult;

	private final Context context;

	/*
	 * Handles writing of GPS data.
	 */
	LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			try {
				fwGPS.openFileOnCard();
				fwGPS.appendLineToFile("" + System.currentTimeMillis() + "," + location.getLatitude() + ","
						+ location.getLongitude());
				fwGPS.closeFileOnCard();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

	};

	/**
	 * A class that receives the scan results of a WiFi scan. After each scan a new scan is started.
	 * 
	 * @author Paul
	 * 
	 */
	class WifiReceiver extends BroadcastReceiver {
		// Wifi
		WifiManager wmLocal;

		public WifiReceiver(WifiManager wm01) {
			wmLocal = wm01;
		}

		@Override
		public void onReceive(Context c, Intent intent) {
			lScanResult = wm01.getScanResults();
			boolean retry = true;
			int tries = 0;
			while (retry && tries < 3) {
				try {
					fwWifi.openFileOnCard();
					fwWifi.appendLineToFile("Time passed (ms): " + (System.currentTimeMillis()));
					retry = false;
				} catch (FileNotFoundException e) {
					tries++;
				}
			}
			// retry was set to false, can log data
			if (!retry) {
				for (int i = 0; i < lScanResult.size(); i++) {
					fwWifi.appendLineToFile((Integer.valueOf(i + 1).toString() + "." + lScanResult.get(i)).toString());
				}
				fwWifi.closeFileOnCard();
			}
			// After each scan, start a new scan.
			wmLocal.startScan();
		}
	}

	public DataLogger(Context context, long routeID, String from, String to) {
		this.routeID = routeID;
		this.from = from;
		this.to = to;
		this.context = context;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	public void logTimedCompass(long timestamp, double value) {
		writeTofwObject(fwCompass, "" + timestamp + ", " + value);
	}

	public void logTimedVariance(long timestamp, double value) {
		writeTofwObject(fwVariance, "" + timestamp + ", " + value);
	}

	public void logRawCompass(long timestamp, double x, double y, double z) {
		if (!compOpen) {
			boolean retry = true;
			int tries = 0;
			while (retry && tries < 3) {
				try {
					fwRawCompass.openFileOnCard();
					retry = false;
					compOpen = true;
				} catch (FileNotFoundException e) {
					tries++;
				}
			}
		}
		if (compOpen) {
			fwRawCompass.appendLineToFile("" + timestamp + ", " + x + ", " + y + ", " + z);
		}
	}

	public void logTimedAcc(long timestamp, double value) {
		writeTofwObject(fwAccelerometer, "" + timestamp + ", " + value);
	}

	public void logRawAcc(long timestamp, double x, double y, double z) {
		if (!accOpen) {
			boolean retry = true;
			int tries = 0;
			while (retry && tries < 3) {
				try {
					fwRawAccel.openFileOnCard();
					retry = false;
					accOpen = true;
				} catch (FileNotFoundException e) {
					tries++;
				}
			}
		}
		if (accOpen) {
			fwRawAccel.appendLineToFile("" + timestamp + ", " + x + ", " + y + ", " + z);
		}
	}

	public void logPosition(long timestamp, double latBest, double lonBest, double progressBest, double latFirst,
			double lonFirst, double progressFirst) {
		// NOTE: progress is in [0,1]
		writeTofwObject(fwPosition, "" + timestamp + ", " + latBest + ", " + lonBest + ", " + progressBest + latFirst
				+ ", " + lonFirst + ", " + progressFirst);
	}

	public void logRoute(double lat0, double lon0) {
		writeTofwObject(fwRoute, "" + lat0 + ", " + lon0);
	}

	public void logSimpleRoute(double lat0, double lon0) {
		writeTofwObject(fwSimpleRoute, "" + lat0 + ", " + lon0);
	}

	public void logStep(long timestamp, double direction) {
		writeTofwObject(fwSteps, "" + timestamp + ", " + direction);
	}

	public void logInfo(String s) {
		writeTofwObject(fwInfo, s);
	}

	public boolean started() {
		return started;
	}

	public long getRouteId() {
		return routeID;
	}

	public void startLogging() {
		Log.i("FOOTPATH", "Starting Logging");
		routeID = System.currentTimeMillis();
		createFileObjects();

		try {
			fwRawAccel.openFileOnCard();
			accOpen = true;
			fwRawCompass.openFileOnCard();
			compOpen = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		wm01 = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		wr01 = new WifiReceiver(wm01);
		context.registerReceiver(wr01, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.i("FOOTPATH", "Registered WifiReceiver");
		wm01.startScan();
		Log.i("FOOTPATH", "Started WiFi Scan");
		started = true;
	}

	public void stopLogging() {
		started = false;
		locationManager.removeUpdates(locationListener);
		context.unregisterReceiver(wr01);
		if (accOpen) {
			fwRawAccel.closeFileOnCard();
		}
		if (compOpen) {
			fwRawCompass.closeFileOnCard();
		}
	}

	private void createFileObjects() {
		fwAccelerometer = new FWriter("" + routeID + "_" + from + "_" + to, "acc.csv");
		fwCompass = new FWriter("" + routeID + "_" + from + "_" + to, "comp.csv");
		fwVariance = new FWriter("" + routeID + "_" + from + "_" + to, "zVar.csv");
		fwPosition = new FWriter("" + routeID + "_" + from + "_" + to, "pos.csv");
		fwSteps = new FWriter("" + routeID + "_" + from + "_" + to, "steps.csv");
		fwGPS = new FWriter("" + routeID + "_" + from + "_" + to, "gps.csv");
		fwWifi = new FWriter("" + routeID + "_" + from + "_" + to, "wifi.csv");
		fwRawAccel = new FWriter("" + routeID + "_" + from + "_" + to, "rawacc.csv");
		fwRawCompass = new FWriter("" + routeID + "_" + from + "_" + to, "rawcomp.csv");
		fwRoute = new FWriter("" + routeID + "_" + from + "_" + to, "route.csv");
		fwSimpleRoute = new FWriter("" + routeID + "_" + from + "_" + to, "simpleroute.csv");
		fwInfo = new FWriter("" + routeID + "_" + from + "_" + to, "info(rev. " + Rev.rev + ").txt");
	}

	private void writeTofwObject(FWriter fw, String data) {
		boolean retry = true;
		int tries = 0;
		while (retry && tries < 3) {
			try {
				fw.openFileOnCard();
				fw.appendLineToFile(data);
				fw.closeFileOnCard();
				retry = false;
			} catch (FileNotFoundException e) {
				// e.printStackTrace();
				tries++;
			}
		}
	}
}
