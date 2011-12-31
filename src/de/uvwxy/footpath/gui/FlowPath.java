package de.uvwxy.footpath.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.uvwxy.footpath.R;
import de.uvwxy.footpath.h263.AudioVideoWriter;
import de.uvwxy.footpath.h263.FileWriter;

/**
 * This activity gives the user the possiblity of recording sensor,wifi and
 * audio/video data. The data will be dumped onto the sdcard. There is the
 * possiblity of entering a username, and a manually counted amount of steps.
 * Timestamps are used for identifying different runs.
 * 
 * A possible output:
 * 
 * /sdcard/footsteps_logs/1287315334162_Paul.mp4
 * /sdcard/footsteps_logs/1287315334162_Paul_accelerometer.csv
 * /sdcard/footsteps_logs/1287315334162_Paul_compass.csv
 * /sdcard/footsteps_logs/1287315334162_Paul_steps.txt
 * /sdcard/footsteps_logs/1287315334162_Paul_wifi.txt
 * @author Paul
 * 
 */
public class FlowPath extends Activity {
	// CONSTANTS:
	public static final String LOG_DIR = "footsteps_logs/";
	public static final String LOG_ID = "FootSteps";

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

	// Logging
	static long tsNow = 0;
	FileWriter fwCompass;
	FileWriter fwAccelerometer;
	FileWriter fwWifi;
	FileWriter fwGPS;
	boolean logging = false;

	// Audio
	AudioVideoWriter avwCapture;
//
//	// Wifi
//	WifiManager wm01;
//	WifiReceiver wr01;
//	List<ScanResult> lScanResult;
//	StringBuilder sb;
//
//	// GPS
//	LocationManager locationManager = null;
//	// if != -1 then write data
//	long gpsFirstStamp = -1;

	// Internal
	private long tsFirst = 0;
	private long tsWifiFirst = 0;
	private int stepsCount = 0;
	private String stepsBuf = "";
	private double lastDirection = 0.0;

	// Stream parsing
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
		btn01.setOnClickListener(guiOnclickListener);
		lbl01 = (TextView) findViewById(R.id.lbl01);
		lbl02 = (TextView) findViewById(R.id.lbl02);
		txt01 = (EditText) findViewById(R.id.txt01);
		
		lblParserInfo = (TextView) findViewById(R.id.lblParserInfo);

		sv01 = (SurfaceView) findViewById(R.id.sv01);
		sh01 = sv01.getHolder();
		sh01.setSizeFromLayout();
		sh01.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		
				// get SurfaceView defined in xml
								// get its layout params
		
		
		
		svMVs = new PaintBoxMVs(this);
		
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.RelativeLayout01);
		SurfaceView svOld = (SurfaceView) findViewById(R.id.svMVPaint);	
		LayoutParams lpHistory = svOld.getLayoutParams();
		
		layout.removeView(svOld);
		layout.addView(svMVs, lpHistory);

//		// Sensors
//		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
//		lSensor = sm.getSensorList(Sensor.TYPE_ALL);
//
//		// GPS
//		locationManager = (LocationManager) this
//				.getSystemService(Context.LOCATION_SERVICE);

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
//
//		// Sensors
//		for (int i = 0; i < lSensor.size(); i++) {
//			// Register only compass and accelerometer
//			if (lSensor.get(i).getType() == Sensor.TYPE_ACCELEROMETER
//					|| lSensor.get(i).getType() == Sensor.TYPE_ORIENTATION) {
//				sm.registerListener(mySensorEventListener, lSensor.get(i),
//						SensorManager.SENSOR_DELAY_GAME);
//				Log.i(LOG_ID, "Registered " + lSensor.get(i).getName());
//			}
//		}
//		
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
		Log.i(LOG_ID, "##### Starting");
		tsNow = System.currentTimeMillis();

		Log.i(LOG_ID, "##### creacting avwCapture");
		// Audio (create audio writer + start)
		avwCapture = new AudioVideoWriter("" + tsNow + "_" + txt01.getText()
				+ ".mp4");
		try {
			Log.i(LOG_ID, "##### registering avwCapture");
			avwCapture.registerCapture();
		} catch (IllegalStateException e) {
			// failed
			Log.i(LOG_ID, "Failed to register capture device.");
			return false;
		} catch (IOException e) {
			// failed
			Log.i(LOG_ID, "Failed to register capture device.");
			return false;
		}
//
//		// Logging (open files for writing)
//
//		fwCompass = new FileWriter("" + tsNow + "_" + txt01.getText()
//				+ "_comp.csv");
//		fwAccelerometer = new FileWriter("" + tsNow + "_" + txt01.getText()
//				+ "_accelerometer.csv");
//		fwWifi = new FileWriter("" + tsNow + "_" + txt01.getText()
//				+ "_wifi.txt");
//		fwGPS = new FileWriter("" + tsNow + "_" + txt01.getText() + "_GPS.csv");
//
//		try {
//			fwCompass.createFileOnCard();
//			fwAccelerometer.createFileOnCard();
//			fwWifi.createFileOnCard();
//			fwGPS.createFileOnCard();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			avwCapture.unregisterCapture();
//			tsFirst = 0;
//			tsWifiFirst = 0;
//			return false;
//		}
//
		// Start Capture
		avwCapture.startCapture();
//
//		File dir = new File(Environment.getExternalStorageDirectory(),
//				FlowPath.LOG_DIR);
//		FileInputStream in = null;
//		try {
//			in = new FileInputStream(new File(dir, "" + tsNow + "_"
//					+ txt01.getText() + ".3gp"));
//		} catch (FileNotFoundException ex) {
//			Log.i(LOG_ID, "File not found: " + ex.getLocalizedMessage());
//		}
//
//		// write headers to files
//		String data = "time(ms);azimuth;pitch;roll";
//		fwCompass.appendLineToFile(data);
//		data = "time(ms);x;y;z";
//		fwAccelerometer.appendLineToFile(data);
//		data = "time(ms);lat;long;alti";
//		fwGPS.appendLineToFile(data);
//
//		// Wifi
//		wm01.startScan();
//
//		// set relative timestamps
//		tsWifiFirst = System.currentTimeMillis();
//		gpsFirstStamp = tsWifiFirst;
//		// enable logging of events
//		Log.i(LOG_ID, "starting logging");
		logging = true;

		/*
		 * // now try accessing video file written to sd // it DOES work (v2.1 /
		 * Milestone) File dir = new
		 * File(Environment.getExternalStorageDirectory(), Main.LOG_DIR);
		 * 
		 * FileInputStream in = null; try { in = new FileInputStream(new
		 * File(dir, "" + tsNow + "_" + txt01.getText() + ".3gp")); } catch
		 * (FileNotFoundException ex) { Log.i(LOG_ID, "File not found: " +
		 * ex.getLocalizedMessage()); } InputStreamReader inReader = new
		 * InputStreamReader(in); inBuffer = new BufferedReader(inReader);
		 * 
		 * try { int inBuf = inBuffer.read();
		 * lbl02.setText("Reading from video file: " + ((inBuf == -1)? " EOS." :
		 * inBuf)); } catch (IOException e) { // TODO Auto-generated catch block
		 * lbl02.setText("Could not read from buffer/video file"); }
		 */
		
		
		parsingThread = new FlowPathParsingThread(avwCapture.getFilePath(), svMVs);
		parsingThread.setRunning(true);
		parsingThread.start();
		unPauseHandler();
		
		return true;
	}

	/**
	 * Stops: (1.) Audio/Video capture, (2.) Sensors, (3.) Closes files(from
	 * sensor data).
	 * 
	 * Then the user is asked to enter the number of steps during 'start' and
	 * 'stop'.
	 */
	private void stopLogging() {
		parsingThread.setRunning(false);
		pauseHandler();
//		
//		if (logging) {
//			// set to false, so listeners stop writing on files
//			logging = false;
//
//			// Sensors
//			tsFirst = 0;
//			tsWifiFirst = 0;
//			gpsFirstStamp = -1;
//
//			// Logging (close files)
//			fwCompass.closeFileOnCard();
//			fwAccelerometer.closeFileOnCard();
//			fwWifi.closeFileOnCard();
//			fwGPS.closeFileOnCard();
//
//			// Audio/Video (stop capture)
			avwCapture.stopCapture();
			avwCapture.unregisterCapture();
			File f = new File(avwCapture.getFilePath());
			f.delete();
//
//			// Request steps count
//			AlertDialog.Builder alert = new AlertDialog.Builder(this);
//
//			alert.setTitle("Steps count.");
//			alert.setMessage("Please enter your number of steps:");
//
//			// Set an EditText view to get user input
//			final EditText input = new EditText(this);
//			input.setText("" + stepsCount + "?");
//			alert.setView(input);
//
//			alert.setPositiveButton("Ok",
//					new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog,
//								int whichButton) {
//							FileWriter steps = new FileWriter(tsNow + "_"
//									+ txt01.getText() + "_steps.txt");
//							try {
//								steps.createFileOnCard();
//								steps.appendLineToFile("Number of steps (counted by hum.): "
//										+ input.getText());
//								steps.appendLineToFile("Number of steps (counted by algo): "
//										+ stepsCount);
//								steps.appendLineToFile(stepsBuf);
//
//								steps.closeFileOnCard();
//							} catch (FileNotFoundException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
//					});
//
//			alert.show();
//
//			stepsCount = 0;
//			for (int i = 0; i < values_history.length; i++) {
//				values_history[i] = 0;
//			}
//			lbl02.setText("Saved (x)");
//		}
	}

//	/**
//	 * Handles sensor events. Writes the data to the appropriate files.
//	 */
//	private SensorEventListener mySensorEventListener = new SensorEventListener() {
//
//		@Override
//		public void onAccuracyChanged(Sensor sensor, int accuracy) {
//			// Auto-generated method stub
//		}
//
//		@Override
//		public void onSensorChanged(SensorEvent event) {
//			if (logging) {
//				String data;
//				if (tsFirst == 0) {
//					// set relative timestamp
//					tsFirst = event.timestamp;
//				} // if (tsFirst == 0)
//				switch (event.sensor.getType()) {
//				case Sensor.TYPE_ACCELEROMETER:
//					// data format:
//					// relative timestamp; x; y; z
//					data = ("" + ((event.timestamp - tsFirst) / 1000) + "; "
//							+ event.values[0] + "; " + event.values[1] + "; " + event.values[2])
//							.replace('.', ',');
//					// Log.i(LOG_ID, data);
//					fwAccelerometer.appendLineToFile(data);
//					if (feedData(event.values[2], 1.5)) {
//						stepsCount++;
//						lbl02.setText("Steps: " + stepsCount + " @ " + 1.5);
//						stepsBuf += ""
//								+ (System.currentTimeMillis() - tsWifiFirst)
//								+ ";" + lastDirection + "\n";
//					}
//					break;
//				case Sensor.TYPE_ORIENTATION:
//					// data format:
//					// relative timestamp; azimuth; pitch; roll
//					data = ("" + ((event.timestamp - tsFirst) / 1000) + "; "
//							+ event.values[0] + "; " + event.values[1] + "; " + event.values[2])
//							.replace(".", ",");
//					// Log.i(LOG_ID, data);
//					fwCompass.appendLineToFile(data);
//					lastDirection = event.values[0];
//					break;
//				default:
//				}// switch (event.sensor.getType())
//			}// if(logging)
//		}
//	};

//	/**
//	 * A class that receives the scans results of a wifi scan. After each scan a
//	 * new scan is started.
//	 * 
//	 * @author Paul
//	 * 
//	 */
//	class WifiReceiver extends BroadcastReceiver {
//		// Wifi
//		WifiManager wmLocal;
//
//		public WifiReceiver(WifiManager wm01) {
//			wmLocal = wm01;
//		}
//
//		@Override
//		public void onReceive(Context c, Intent intent) {
//			// only log when enabled.
//			// currently not shure if this is triggered beforehand
//			if (logging) {
//				Log.i(LOG_ID, "Wifi receiving");
//				fwWifi.appendLineToFile("Time passed (ms): "
//						+ (System.currentTimeMillis() - tsWifiFirst));
//				Log.i(LOG_ID,
//						"Time passed (ms): "
//								+ (System.currentTimeMillis() - tsWifiFirst));
//				lScanResult = wm01.getScanResults();
//				for (int i = 0; i < lScanResult.size(); i++) {
//					fwWifi.appendLineToFile((new Integer(i + 1).toString()
//							+ "." + lScanResult.get(i)).toString());
//				}
//			}// if(logging)
//
//			/*
//			 * // try reading from video file (it DOES work! v 2.1/Milestone) //
//			 * test is put here, because this is called periodically try {
//			 * String inBuf = inBuffer.readLine();
//			 * lbl02.setText("Reading from video file: " + ((inBuf == null)?
//			 * " null." : inBuf)); } catch (IOException e) { // TODO
//			 * Auto-generated catch block
//			 * lbl02.setText("Could not read from buffer/video file"); }
//			 */
//
//			// After each scan, start a new scan.
//			wmLocal.startScan();
//		}
//
//	}

//	/*
//	 * Handles the writing of GPS data.
//	 */
//	LocationListener locationListener = new LocationListener() {
//		public void onLocationChanged(Location location) {
//			if (logging) {
//				// gpsFirstStamp has been set after the first Acc/Comp reading
//				if (gpsFirstStamp != -1) {
//					fwGPS.appendLineToFile(""
//							+ (location.getTime() - gpsFirstStamp) + ";"
//							+ location.getLatitude() + ";"
//							+ location.getLongitude() + ";"
//							+ location.getAltitude());
//				}
//			}// if (logging)
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

	// #########################################################################
	// ############## Step recognition... a first approach ;) ##################
	// #########################################################################
	private static final int vhSize = 64;
	double[] values_history = new double[vhSize];
	int vhPointer = 0;

	private boolean feedData(double value, double peakSize) {
		int normVal = 0;
		values_history[vhPointer % vhSize] = value;
		vhPointer++;

		double local_min = Double.MAX_VALUE;
		double local_max = Double.MIN_VALUE;
		for (int i = 0; i < vhSize; i++) {
			if (values_history[i] < local_min) {
				local_min = values_history[i];
			}
			if (values_history[i] > local_max) {
				local_max = values_history[i];
			}
		}

		if (local_max - local_min < peakSize) {
			normVal = 0;

		}

		if (values_history[vhPointer % vhSize] - local_min >= peakSize) {
			normVal = 1;
		}

		return stepAutomaton(normVal);
	}

	int[][] transitions = { { 0, 0, 0, 4, 0 }, { 1, 2, 3, 3, 0 } };
	boolean[] finStates = { false, false, false, false, true };
	int state = 0;

	private boolean stepAutomaton(int value) {
		state = transitions[value][state];
		return finStates[state];
	}

}