package de.uvwxy.footpath.gui;

import java.io.FileNotFoundException;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.uvwxy.flowpath.FileWriter;
import de.uvwxy.flowpath.FlowPathConfig;
import de.uvwxy.flowpath.FlowPathInterface;
import de.uvwxy.flowpath.PaintBoxMVs;
import de.uvwxy.footpath.R;

/**
 * This activity gives the user the possibility of recording sensor, WIFI and
 * audio/video data. The data will be dumped onto the sd card. There is the
 * possibility of entering a user name, and a manually counted amount of steps.
 * Time stamps are used for identifying different runs.
 * 
 * Output is in a directory, e.g. 1328199328345_320_240_60_h263
 * 
 * LOG_DIR LOG_ID DELIM - the delimiter used in csv files HANDLES: Start/Stop of
 * recording/parsing Display of PaintBoxMV. Creation of TCP/IP Server socket,
 * passes socket.InputStream to FlowPathParsingThread CAN HANDLE: Logging of
 * Sensors.
 * 
 * @author Paul
 * 
 */
public class FlowPathTestGUI extends Activity {
	// CONSTANTS:
	public static final String LOG_DIR = "flowpath_logs";
	public static final String LOG_ID = "FLOWPATH";
	private static final char DELIM = ',';

	// GUI
	Button btn01;
	TextView lbl01;
	TextView lbl02;
	EditText txt01;
	private TextView lblParserInfo = null;

	public static SurfaceView sv01;
	public static SurfaceHolder sh01;

	// Sensors
	private static SensorManager sm;
	List<Sensor> lSensor;

	// Logging I
	static long tsNow = 0;
	private long tsLastComp = 0;
	private long tsLastAcc = 0;
	private long tsLastGyro = 0;
	// log only at 30 FPS
	private long tsIntervall = (1000 / 30);
	FileWriter fwCompass;
	FileWriter fwAccelerometer;
	FileWriter fwBarometer;
	FileWriter fwGyrometer;
	FileWriter fwWifi;
	FileWriter fwGPS;
	// flag if we were able to start logging
	boolean isLogging = false;
	// disable all logging here
	boolean generalLogging = false;
	// be selective here
	boolean compLogging = true;
	boolean accLogging = true;
	boolean baroLogging = true;
	boolean gyroLogging = true;
	boolean wifiLogging = true;
	boolean gpsLogging = true;

	boolean compFiltering = true;

	private float[] compValues = { 0, 0, 0 };
	//
	// // Wifi
	WifiManager wm01;
	WifiReceiver wr01;
	List<ScanResult> lScanResult;
	StringBuilder sb;

	// GPS
	LocationManager locationManager = null;

	// if != -1 then write data
	// long gpsFirstStamp = -1;

	// Internal
	// private long tsFirst = 0;
	// private long tsWifiFirst = 0;

	private FlowPathInterface flowPathInterface = FlowPathInterface
			.getInterface();

	private PaintBoxMVs svMVs = null;

	private Handler mHandler = new Handler();
	private long delayMillis = 1000;

	private Runnable mUpdateTimeTask = new Runnable() {

		public void run() {

			// Action
			lblParserInfo.setText(flowPathInterface.getStats());

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

	/*
	 * needed for: test if video stream readable BufferedReader inBuffer;
	 */

	/**
	 * Called when the activity is first created. Sets up the GUI elements and
	 * retrieves all available sensors
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// GUI
		setContentView(R.layout.flowpath);

		btn01 = (Button) findViewById(R.id.btn01);
		lbl01 = (TextView) findViewById(R.id.lbl01);
		lblParserInfo = (TextView) findViewById(R.id.lblParserInfo);
		sv01 = (SurfaceView) findViewById(R.id.sv01);
		txt01 = (EditText) findViewById(R.id.txt01);

		// setup sv01 for use as preview
		// Note: this has to be done here, otherwise some sort of
		// "security exception"
		sh01 = sv01.getHolder();
		sh01.setSizeFromLayout();
		sh01.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		svMVs = new PaintBoxMVs(this);

		RelativeLayout layout = (RelativeLayout) findViewById(R.id.RelativeLayout01);
		SurfaceView svOld = (SurfaceView) findViewById(R.id.svMVPaint);
		LayoutParams lpHistory = svOld.getLayoutParams();

		// replace svMVPaint(xml) with PaintBoxMVs class
		layout.removeView(svOld);
		layout.addView(svMVs, lpHistory);

		// // get sensors
		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		lSensor = sm.getSensorList(Sensor.TYPE_ALL);
		//
		// // get GPS
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		btn01.setOnClickListener(guiOnclickListener);

		flowPathInterface.addMVDTrigger(svMVs);
	}

	/**
	 * Reset the GUI on resume. Capture has been stopped before.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// GUI
		btn01.setText("Start");
		txt01.setEnabled(true);
		// Request data from

		if (generalLogging) {
			// // WiFi
			if (wifiLogging) {
				wm01 = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				wr01 = new WifiReceiver(wm01);
				registerReceiver(wr01, new IntentFilter(
						WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			}
			// // Sensors
			for (int i = 0; i < lSensor.size(); i++) {
				// Register only compass and accelerometer

				switch (lSensor.get(i).getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					if (!accLogging)
						break;
					sm.registerListener(mySensorEventListener, lSensor.get(i),
							SensorManager.SENSOR_DELAY_NORMAL);
					Log.i(LOG_ID, "Registered " + lSensor.get(i).getName());
					break;
				case Sensor.TYPE_ORIENTATION:
					if (!compLogging)
						break;
					sm.registerListener(mySensorEventListener, lSensor.get(i),
							SensorManager.SENSOR_DELAY_NORMAL);
					Log.i(LOG_ID, "Registered " + lSensor.get(i).getName());
					break;
				case Sensor.TYPE_GYROSCOPE:
					if (!gyroLogging)
						break;
					sm.registerListener(mySensorEventListener, lSensor.get(i),
							SensorManager.SENSOR_DELAY_NORMAL);
					Log.i(LOG_ID, "Registered " + lSensor.get(i).getName());
					break;
				case Sensor.TYPE_PRESSURE:
					if (!baroLogging)
						break;
					sm.registerListener(mySensorEventListener, lSensor.get(i),
							SensorManager.SENSOR_DELAY_NORMAL);
					Log.i(LOG_ID, "Registered " + lSensor.get(i).getName());
					break;
				}
			}

			// // GPS
			if (gpsLogging)
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			// Logging (logging stopped by 'onStop' or 'onDestroy')
		}

		// logging cant run after (re)loading GUI
		isLogging = false;
	}

	/**
	 * GUI is no longer visible, stop everything.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// Logging
		stopLogging();
		if (wifiLogging)
			unregisterReceiver(wr01);
		if (gpsLogging)
			locationManager.removeUpdates(locationListener);
		if (accLogging || compLogging || gyroLogging || baroLogging)
			sm.unregisterListener(mySensorEventListener);
		Log.i(LOG_ID, "UnRegistered");
	}

	/**
	 * Bye Bye. Stop logging!
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopLogging();
		// locationManager.removeUpdates(locationListener);
		// sm.unregisterListener(mySensorEventListener);
		Log.i(LOG_ID, "Destroyed");
	}

	/**
	 * Handle events generated from GUI elements. Toggle between start and stop
	 * mode.
	 */
	private OnClickListener guiOnclickListener = new OnClickListener() {
		public void onClick(View v) {
			if (v.equals(btn01)) {
				if (isLogging) {
					// GUI
					btn01.setText("Start");
					txt01.setEnabled(true);
					// Logging
					stopLogging();
				} else {
					if (startLogging()) {
						// GUI
						btn01.setText("Stop");
						txt01.setEnabled(false);
					}
				}
			}
		}
	};

	/**
	 * Starts: (1.)Audio/Video capture, (2.)Sensors, (2.)Opening files to dump
	 * sensor data in.
	 * 
	 * If something fails at a certain point, everything started will be
	 * stopped.
	 * 
	 * Audio/Video capture is started first, because this takes the longest to
	 * load..
	 * 
	 * @return true if everything starts OK.
	 */
	private boolean startLogging() {
		tsNow = System.currentTimeMillis();
		tsLastComp = tsNow;
		tsLastAcc = tsNow;
		boolean fpOk = flowPathInterface.startFlowpath();

		if (fpOk) {
			if (generalLogging) {
				// // Logging II
				if (compLogging)
					fwCompass = new FileWriter(tsNow + "_"
							+ FlowPathConfig.PIC_SIZE_WIDTH + "_"
							+ FlowPathConfig.PIC_SIZE_HEIGHT + "_"
							+ FlowPathConfig.PIC_FPS + "_" + txt01.getText(),
							"compass.csv");

				if (accLogging)
					fwAccelerometer = new FileWriter(tsNow + "_"
							+ FlowPathConfig.PIC_SIZE_WIDTH + "_"
							+ FlowPathConfig.PIC_SIZE_HEIGHT + "_"
							+ FlowPathConfig.PIC_FPS + "_" + txt01.getText(),
							"accelerometer.csv");

				if (baroLogging)
					fwBarometer = new FileWriter(tsNow + "_"
							+ FlowPathConfig.PIC_SIZE_WIDTH + "_"
							+ FlowPathConfig.PIC_SIZE_HEIGHT + "_"
							+ FlowPathConfig.PIC_FPS + "_" + txt01.getText(),
							"barometer.csv");

				if (gyroLogging)
					fwGyrometer = new FileWriter(tsNow + "_"
							+ FlowPathConfig.PIC_SIZE_WIDTH + "_"
							+ FlowPathConfig.PIC_SIZE_HEIGHT + "_"
							+ FlowPathConfig.PIC_FPS + "_" + txt01.getText(),
							"gyroscope.csv");

				if (wifiLogging)
					fwWifi = new FileWriter(tsNow + "_"
							+ FlowPathConfig.PIC_SIZE_WIDTH + "_"
							+ FlowPathConfig.PIC_SIZE_HEIGHT + "_"
							+ FlowPathConfig.PIC_FPS + "_" + txt01.getText(),
							"wifi.txt");

				if (gpsLogging)
					fwGPS = new FileWriter(tsNow + "_"
							+ FlowPathConfig.PIC_SIZE_WIDTH + "_"
							+ FlowPathConfig.PIC_SIZE_HEIGHT + "_"
							+ FlowPathConfig.PIC_FPS + "_" + txt01.getText(),
							"GPS.csv");

				try {
					if (compLogging)
						fwCompass.createFileOnCard();
					if (accLogging)
						fwAccelerometer.createFileOnCard();
					if (baroLogging)
						fwBarometer.createFileOnCard();
					if (gyroLogging)
						fwGyrometer.createFileOnCard();
					if (wifiLogging)
						fwWifi.createFileOnCard();
					if (gpsLogging)
						fwGPS.createFileOnCard();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					// Don not cancel FlowPath for now.
					// avwCapture.unregisterCapture();
					return false;
				}

				String data = "";

				// // Logging III
				if (compLogging) {
					data = "time(millis)" + DELIM + "azimuth" + DELIM + "pitch"
							+ DELIM + "roll";
					fwCompass.appendLineToFile(data);
				}

				if (accLogging) {
					data = "time(millis)" + DELIM + "x" + DELIM + "y" + DELIM
							+ "z";
					fwAccelerometer.appendLineToFile(data);
				}

				if (baroLogging) {
					data = "time(millis)" + DELIM + "pressure";
					fwBarometer.appendLineToFile(data);
				}

				if (gyroLogging) {
					data = "time(millis)" + DELIM + "x" + DELIM + "y" + DELIM
							+ "z";
					fwGyrometer.appendLineToFile(data);
				}

				if (gpsLogging) {
					data = "time(millis)" + DELIM + "lat" + DELIM + "long"
							+ DELIM + "alti";
					fwGPS.appendLineToFile(data);
				}

				// Wifi
				if (wifiLogging)
					wm01.startScan();
				isLogging = true;
			}

			unPauseHandler();

			return true;
		} else {
			isLogging = false;
			return false;
		}
	}

	/**
	 * Stops logging, resets variables, closes log files.
	 */
	private void stopLogging() {
		Log.i("FLOWPATH", "Pausing handler");
		pauseHandler();

		Log.i("FLOWPATH", "Closing Files");
		if (isLogging && generalLogging) {
			isLogging = false;

			// Logging IV
			if (compLogging)
				fwCompass.closeFileOnCard();
			if (accLogging)
				fwAccelerometer.closeFileOnCard();
			if (baroLogging)
				fwBarometer.closeFileOnCard();
			if (gyroLogging)
				fwGyrometer.closeFileOnCard();
			if (wifiLogging)
				fwWifi.closeFileOnCard();
			if (gpsLogging)
				fwGPS.closeFileOnCard();

			// stop capture

		}
		Log.i("FLOWPATH", "Trying to stop FlowPath");
		flowPathInterface.stopFlowPath();
	}

	/**
	 * Handles sensor events. Writes the data to the appropriate files.
	 */
	private SensorEventListener mySensorEventListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (isLogging) {
				String data;
				long ts = System.currentTimeMillis();
				switch (event.sensor.getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					if (ts - tsLastAcc >= tsIntervall) {
						tsLastAcc = ts;
						// relative time stamp; x; y; z
						data = "" + ts + DELIM + event.values[0] + DELIM
								+ event.values[1] + DELIM + event.values[2];
						fwAccelerometer.appendLineToFile(data);
					}
					break;
				case Sensor.TYPE_ORIENTATION:
					if (ts - tsLastComp >= tsIntervall) {
						tsLastComp = ts;
						// relative time stamp; azimuth; pitch; roll
						if (compFiltering)
							compValues = compFilter(compValues, event.values,
									0.1f);
						else
							compValues = event.values;
						data = "" + ts + DELIM + compValues[0] + DELIM
								+ compValues[1] + DELIM + compValues[2];
						fwCompass.appendLineToFile(data);
					}
					break;
				case Sensor.TYPE_PRESSURE:
					data = "" + ts + DELIM + event.values[0];
					fwBarometer.appendLineToFile(data);
					break;
				case Sensor.TYPE_GYROSCOPE:
					if (ts - tsLastGyro >= tsIntervall) {
						tsLastGyro = ts;
						data = "" + ts + DELIM + event.values[0] + DELIM
								+ event.values[1] + DELIM + event.values[2];
						fwGyrometer.appendLineToFile(data);
					}
				default:
				}
			}
		}
	};

	/**
	 * A class that receives the scans results of a WIFI scan. After each scan a
	 * new scan is started.
	 * 
	 * @author Paul Smith
	 * 
	 */
	class WifiReceiver extends BroadcastReceiver {
		WifiManager wmLocal;

		public WifiReceiver(WifiManager wm01) {
			wmLocal = wm01;
		}

		@Override
		public void onReceive(Context c, Intent intent) {
			// currently not sure if this is triggered beforehand
			if (isLogging) {
				long ts = System.currentTimeMillis();
				fwWifi.appendLineToFile("<timestamp>" + ts + "</timestamp>: ");
				fwWifi.appendLineToFile("<data>");
				lScanResult = wm01.getScanResults();
				for (int i = 0; i < lScanResult.size(); i++) {
					fwWifi.appendLineToFile("    " + (i + 1) + "."
							+ (lScanResult.get(i)).toString());
				}
				fwWifi.appendLineToFile("</data>");
			}

			wmLocal.startScan();
		}

	}

	/*
	 * Handles the writing of GPS data.
	 */
	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (isLogging) {
				// gpsFirstStamp has been set after the first Acc/Comp reading
				long ts = System.currentTimeMillis();
				fwGPS.appendLineToFile("" + ts + ";" + location.getLatitude()
						+ ";" + location.getLongitude() + ";"
						+ location.getAltitude());
			}
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}

	};

	private float[] compFilter(float[] oldv, float[] newv, float factor) {
		if (oldv == null || newv == null || factor == 0 || oldv.length != 3
				|| newv.length != 3)
			return null;
		float[] ret = compDiff(oldv, newv);

		ret[0] = factor * ret[0] + oldv[0];
		ret[1] = factor * ret[1] + oldv[1];
		ret[2] = factor * ret[2] + oldv[2];

		for (float f : ret) {
			if (f > 360)
				f = (f - 360);
			if (f < 0)
				f = (f + 360);
		}

		return ret;
	}

	private float[] compDiff(float[] oldv, float[] newv) {
		if (oldv == null || newv == null || oldv.length != 3
				|| newv.length != 3)
			return null;

		float[] ret = { 0, 0, 0 };

		ret[0] = newv[0] - oldv[0];
		ret[1] = newv[1] - oldv[1];
		ret[2] = newv[2] - oldv[2];

		for (int f = 0; f < 3; f++) {
			if (ret[f] > 180)
				ret[f] = (ret[f] - 360);
			else if (ret[f] < -180)
				ret[f] = (ret[f] + 360);
		}

		return ret;
	}

}