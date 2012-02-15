package de.uvwxy.footpath.gui;

import java.util.List;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
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
 * DEFINES: VIDEO SIZE x*y@f
 *          PORT
 *          LOG_DIR
 *          LOG_ID
 *          DELIM - the delimiter used in csv files
 * HANDLES: Start/Stop of recording/parsing
 *          Display of PaintBoxMV.
 *          Creation of TCP/IP Server socket, passes socket.InputStream to
 *          FlowPathParsingThread
 * CAN HANDLE: Logging of Sensors.
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
//	FileWriter fwCompass;
//	FileWriter fwAccelerometer;
//	FileWriter fwBarometer;
//	FileWriter fwGyrometer;
//	FileWriter fwWifi;
//	FileWriter fwGPS;
	boolean logging = false;

	
//
//	// Wifi
//	WifiManager wm01;
//	WifiReceiver wr01;
	List<ScanResult> lScanResult;
	StringBuilder sb;

	// GPS
	LocationManager locationManager = null;

	// if != -1 then write data
	// long gpsFirstStamp = -1;

	// Internal
	// private long tsFirst = 0;
	// private long tsWifiFirst = 0;
	
	private FlowPathInterface flowPathInterface = FlowPathInterface.getInterface();
	
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

//		// get sensors
//		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
//		lSensor = sm.getSensorList(Sensor.TYPE_ALL);
//
//		// get GPS
//		locationManager = (LocationManager) this
//				.getSystemService(Context.LOCATION_SERVICE);

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

//		// WiFi
//		wm01 = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//		wr01 = new WifiReceiver(wm01);
//		registerReceiver(wr01, new IntentFilter(
//				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

//		// Sensors
//		for (int i = 0; i < lSensor.size(); i++) {
//			// Register only compass and accelerometer
//			
//			switch(lSensor.get(i).getType()){
//			case Sensor.TYPE_ACCELEROMETER:
//			case Sensor.TYPE_ORIENTATION:
//			case Sensor.TYPE_GYROSCOPE:
//			case Sensor.TYPE_PRESSURE:
////				sm.registerListener(mySensorEventListener, lSensor.get(i),
//						SensorManager.SENSOR_DELAY_GAME);
//				Log.i(LOG_ID, "Registered " + lSensor.get(i).getName());
//			}
//		}

//		// GPS
//		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
//				0, locationListener);
		// Logging (logging stopped by 'onStop' or 'onDestroy')
		logging = false;
	}

	/**
	 * GUI is no longer visible, stop everything.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// Logging
		stopLogging();
//		unregisterReceiver(wr01);
//		locationManager.removeUpdates(locationListener);
//		sm.unregisterListener(mySensorEventListener);
		Log.i(LOG_ID, "UnRegistered");
	}

	/**
	 * Bye Bye. Stop logging!
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopLogging();
//		locationManager.removeUpdates(locationListener);
//		sm.unregisterListener(mySensorEventListener);
		Log.i(LOG_ID, "Destroyed");
	}

	/**
	 * Handle events generated from GUI elements. Toggle between start and stop
	 * mode.
	 */
	private OnClickListener guiOnclickListener = new OnClickListener() {
		public void onClick(View v) {
			if (v.equals(btn01)) {
				if (logging) {
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

		boolean fpOk = flowPathInterface.startFlowpath();
		
		if (fpOk){
		
//		// Logging II
//		fwCompass = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
//				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
//				"compass.csv");
//		fwAccelerometer = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
//				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
//				"accelerometer.csv");
//		fwBarometer = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
//				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
//				"barometer.csv");
//		fwGyrometer = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
//				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
//				"gyroscope.csv");
//		fwWifi = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
//				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
//				"wifi.txt");
//		fwGPS = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
//				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
//				"GPS.csv");

		;

//		try {
//			fwCompass.createFileOnCard();
//			fwAccelerometer.createFileOnCard();
//			fwBarometer.createFileOnCard();
//			fwGyrometer.createFileOnCard();
//			fwWifi.createFileOnCard();
//			fwGPS.createFileOnCard();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			avwCapture.unregisterCapture();
//			return false;
//		}



//		// Logging III
//		String data = "time(millis)"+DELIM+"azimuth"+DELIM+"pitch"+DELIM+"roll";
//		fwCompass.appendLineToFile(data);
//
//		data = "time(millis)"+DELIM+"x"+DELIM+"y"+DELIM+"z";
//		fwAccelerometer.appendLineToFile(data);
//
//		data = "time(millis)"+DELIM+"pressure";
//		fwBarometer.appendLineToFile(data);
//		
//		data = "time(millis)"+DELIM+"x"+DELIM+"y"+DELIM+"z";
//		fwGyrometer.appendLineToFile(data);
//		
//		data = "time(millis)"+DELIM+"lat"+DELIM+"long"+DELIM+"alti";
//		fwGPS.appendLineToFile(data);

		// Wifi
//		wm01.startScan();

		logging = true;

		
		unPauseHandler();

		return true;
		} else {
			logging = false;
			return false;
		}
	}

	/**
	 * Stops logging, resets variables, closes log files.
	 */
	private void stopLogging() {
		flowPathInterface.stopFlowPath();
		pauseHandler();

		if (logging) {
			logging = false;
//
//			// Logging IV
//			fwCompass.closeFileOnCard();
//			fwAccelerometer.closeFileOnCard();
//			fwBarometer.closeFileOnCard();
//			fwGyrometer.closeFileOnCard();
//			fwWifi.closeFileOnCard();
//			fwGPS.closeFileOnCard();

			// stop capture
			
		}
	}

//	/**
//	 * Handles sensor events. Writes the data to the appropriate files.
//	 */
//	private SensorEventListener mySensorEventListener = new SensorEventListener() {
//
//		@Override
//		public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		}
//
//		@Override
//		public void onSensorChanged(SensorEvent event) {
//			if (logging) {
//				String data;
//				long ts = System.currentTimeMillis();
//				switch (event.sensor.getType()) {
//				case Sensor.TYPE_ACCELEROMETER:
//					// relative time stamp; x; y; z
//					data = "" + ts + DELIM + event.values[0] + DELIM
//							+ event.values[1] + DELIM + event.values[2];
//					fwAccelerometer.appendLineToFile(data);
//					break;
//				case Sensor.TYPE_ORIENTATION:
//					// relative time stamp; azimuth; pitch; roll
//					data = "" + ts + DELIM + event.values[0] + DELIM
//							+ event.values[1] + DELIM + event.values[2];
//					fwCompass.appendLineToFile(data);
//					break;
//				case Sensor.TYPE_PRESSURE:
//					data = "" + ts + DELIM + event.values[0];
//					fwBarometer.appendLineToFile(data);
//					break;
//				case Sensor.TYPE_GYROSCOPE:
//					data = "" + ts + DELIM + event.values[0] + DELIM
//							+ event.values[1] + DELIM + event.values[2];
//					fwGyrometer.appendLineToFile(data);
//				default:
//				}
//			}
//		}
//	};
//
//	/**
//	 * A class that receives the scans results of a WIFI scan. After each scan a
//	 * new scan is started.
//	 * 
//	 * @author Paul Smith
//	 * 
//	 */
//	class WifiReceiver extends BroadcastReceiver {
//		WifiManager wmLocal;
//
//		public WifiReceiver(WifiManager wm01) {
//			wmLocal = wm01;
//		}
//
//		@Override
//		public void onReceive(Context c, Intent intent) {
//			// currently not sure if this is triggered beforehand
//			if (logging) {
//				long ts = System.currentTimeMillis();
//				fwWifi.appendLineToFile("<timestamp>" + ts + "</timestamp>: ");
//				lScanResult = wm01.getScanResults();
//				for (int i = 0; i < lScanResult.size(); i++) {
//					fwWifi.appendLineToFile((new Integer(i + 1).toString()
//							+ "." + lScanResult.get(i)).toString());
//				}
//			}
//
//			wmLocal.startScan();
//		}
//
//	}
//
//	/*
//	 * Handles the writing of GPS data.
//	 */
//	LocationListener locationListener = new LocationListener() {
//		public void onLocationChanged(Location location) {
//			if (logging) {
//				// gpsFirstStamp has been set after the first Acc/Comp reading
//				long ts = System.currentTimeMillis();
//				fwGPS.appendLineToFile("" + ts + ";" + location.getLatitude()
//						+ ";" + location.getLongitude() + ";"
//						+ location.getAltitude());
//			}
//		}
//
//		public void onStatusChanged(String provider, int status, Bundle extras) {
//		}
//
//		public void onProviderEnabled(String provider) {
//		}
//
//		public void onProviderDisabled(String provider) {
//		}
//
//	};

}