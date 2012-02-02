package de.uvwxy.footpath.gui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
import de.uvwxy.footpath.R;
import de.uvwxy.footpath.h263.FileWriter;
import de.uvwxy.footpath.h263.SocketAudioVideoWriter;

/**
 * This activity gives the user the possibility of recording sensor, WIFI and
 * audio/video data. The data will be dumped onto the sd card. There is the
 * possibility of entering a user name, and a manually counted amount of steps.
 * Time stamps are used for identifying different runs.
 * 
 * Output is in a directory, e.g. 1328199328345_320_240_60_h263
 * 
 * @author Paul
 * 
 */
public class FlowPath extends Activity {
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
	FileWriter fwCompass;
	FileWriter fwAccelerometer;
	FileWriter fwBarometer;
	FileWriter fwGyrometer;
	FileWriter fwWifi;
	FileWriter fwGPS;
	boolean logging = false;

	// Audio
	SocketAudioVideoWriter avwCapture;

	// Wifi
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

	// Stream parsing
	// Format Video Resolution
	// SQCIF 128 × 96 @ 10 @ 30 (not 20)
	// QCIF 176 × 144 30,10
	// SCIF 256 x 192
	// SIF(525) 352 x 240
	// CIF/SIF(625) 352 × 288
	// 4SIF(525) 704 x 480
	// 4CIF/4SIF(625) 704 × 576
	// 16CIF 1408 × 1152
	// DCIF 528 × 384
	public static final int PIC_SIZE_WIDTH = 320;
	public static final int PIC_SIZE_HEIGHT = 240;
	public static final int PIC_FPS = 30;

	private FlowPathParsingThread parsingThread = null;

	private Handler mHandler = new Handler();
	private long delayMillis = 1000;

	private PaintBoxMVs svMVs = null;

	private Runnable mUpdateTimeTask = new Runnable() {

		public void run() {

			// Action
			lblParserInfo.setText(parsingThread.getParser().getStats());

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

	// server socket + functions:
	private ServerSocket sckSrvListen = null;
	private Socket sckSrvCon = null;

	private ServerThread st = new ServerThread();

	private class ServerThread extends Thread {
		public void run() {
			accept();
		}
	}

	private void accept() {
		try {
			sckSrvCon = sckSrvListen.accept();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startServer(int port) {
		try {
			sckSrvListen = new ServerSocket(port);
			st.start();
		} catch (IOException e3) {
			e3.printStackTrace();
		}
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
		lbl02 = (TextView) findViewById(R.id.lbl02);
		lblParserInfo = (TextView) findViewById(R.id.lblParserInfo);
		sv01 = (SurfaceView) findViewById(R.id.sv01);
		txt01 = (EditText) findViewById(R.id.txt01);

		// setup sv01 for use as preview (mediarecorder)
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

		// get sensors
		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		lSensor = sm.getSensorList(Sensor.TYPE_ALL);

		// get GPS
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		btn01.setOnClickListener(guiOnclickListener);
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

		// WiFi
		wm01 = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wr01 = new WifiReceiver(wm01);
		registerReceiver(wr01, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		// Sensors
		for (int i = 0; i < lSensor.size(); i++) {
			// Register only compass and accelerometer
			
			switch(lSensor.get(i).getType()){
			case Sensor.TYPE_ACCELEROMETER:
			case Sensor.TYPE_ORIENTATION:
			case Sensor.TYPE_GYROSCOPE:
			case Sensor.TYPE_PRESSURE:
				sm.registerListener(mySensorEventListener, lSensor.get(i),
						SensorManager.SENSOR_DELAY_GAME);
				Log.i(LOG_ID, "Registered " + lSensor.get(i).getName());
			}
		}

		// GPS
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, locationListener);
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
		unregisterReceiver(wr01);
		locationManager.removeUpdates(locationListener);
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
		locationManager.removeUpdates(locationListener);
		sm.unregisterListener(mySensorEventListener);
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

		startServer(1337);

		// create audio writer + start it
		avwCapture = new SocketAudioVideoWriter();
		try {
			avwCapture.registerCapture();
		} catch (IllegalStateException e) {
			// failed
			Log.i(LOG_ID, "Failed to register capture device (ISE).");
			return false;
		} catch (IOException e) {
			// failed
			Log.i(LOG_ID, "Failed to register capture device (IOE).");
			return false;
		}

		// Logging II
		fwCompass = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
				"compass.csv");
		fwAccelerometer = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
				"accelerometer.csv");
		fwBarometer = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
				"barometer.csv");
		fwGyrometer = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
				"gyroscope.csv");
		fwWifi = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
				"wifi.txt");
		fwGPS = new FileWriter(tsNow + "_" + PIC_SIZE_WIDTH + "_"
				+ PIC_SIZE_HEIGHT + "_" + PIC_FPS + "_" + txt01.getText(),
				"GPS.csv");

		;

		try {
			fwCompass.createFileOnCard();
			fwAccelerometer.createFileOnCard();
			fwBarometer.createFileOnCard();
			fwGyrometer.createFileOnCard();
			fwWifi.createFileOnCard();
			fwGPS.createFileOnCard();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			avwCapture.unregisterCapture();
			return false;
		}

		avwCapture.startCapture();

		// Logging III
		String data = "time(millis)"+DELIM+"azimuth"+DELIM+"pitch"+DELIM+"roll";
		fwCompass.appendLineToFile(data);

		data = "time(millis)"+DELIM+"x"+DELIM+"y"+DELIM+"z";
		fwAccelerometer.appendLineToFile(data);

		data = "time(millis)"+DELIM+"pressure";
		fwBarometer.appendLineToFile(data);
		
		data = "time(millis)"+DELIM+"x"+DELIM+"y"+DELIM+"z";
		fwGyrometer.appendLineToFile(data);
		
		data = "time(millis)"+DELIM+"lat"+DELIM+"long"+DELIM+"alti";
		fwGPS.appendLineToFile(data);

		// Wifi
		wm01.startScan();

		logging = true;

		while (sckSrvCon == null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		InputStream sckIn = null;
		try {
			sckIn = sckSrvCon.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		parsingThread = new FlowPathParsingThread(svMVs, sckIn);
		parsingThread.setRunning(true);
		parsingThread.start();
		unPauseHandler();

		return true;
	}

	/**
	 * Stops logging, resets variables, closes log files.
	 */
	private void stopLogging() {
		parsingThread.setRunning(false);
		pauseHandler();

		if (logging) {
			logging = false;

			// Logging IV
			fwCompass.closeFileOnCard();
			fwAccelerometer.closeFileOnCard();
			fwBarometer.closeFileOnCard();
			fwGyrometer.closeFileOnCard();
			fwWifi.closeFileOnCard();
			fwGPS.closeFileOnCard();

			// stop capture
			avwCapture.stopCapture();
			avwCapture.unregisterCapture();
		}
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
			if (logging) {
				String data;
				long ts = System.currentTimeMillis();
				switch (event.sensor.getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					// relative time stamp; x; y; z
					data = "" + ts + DELIM + event.values[0] + DELIM
							+ event.values[1] + DELIM + event.values[2];
					fwAccelerometer.appendLineToFile(data);
					break;
				case Sensor.TYPE_ORIENTATION:
					// relative time stamp; azimuth; pitch; roll
					data = "" + ts + DELIM + event.values[0] + DELIM
							+ event.values[1] + DELIM + event.values[2];
					fwCompass.appendLineToFile(data);
					break;
				case Sensor.TYPE_PRESSURE:
					data = "" + ts + DELIM + event.values[0];
					fwBarometer.appendLineToFile(data);
					break;
				case Sensor.TYPE_GYROSCOPE:
					data = "" + ts + DELIM + event.values[0] + DELIM
							+ event.values[1] + DELIM + event.values[2];
					fwGyrometer.appendLineToFile(data);
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
			if (logging) {
				long ts = System.currentTimeMillis();
				fwWifi.appendLineToFile("<timestamp>" + ts + "</timestamp>: ");
				lScanResult = wm01.getScanResults();
				for (int i = 0; i < lScanResult.size(); i++) {
					fwWifi.appendLineToFile((new Integer(i + 1).toString()
							+ "." + lScanResult.get(i)).toString());
				}
			}

			wmLocal.startScan();
		}

	}

	/*
	 * Handles the writing of GPS data.
	 */
	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (logging) {
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

}