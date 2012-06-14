package de.uvwxy.footpath2.movement;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import de.uvwxy.footpath2.log.Exporter;

/**
 * The current idea behind yet another SensorEventManager is that we can here, globally filter/modify/log the sensor
 * data.
 * 
 * TODO: at the moment it seems there is a lot of code to be doubled for each new sensor. The advantage will be to
 * implement filters here. Also we only request/supply only the registered sensors.
 * 
 * @author Paul Smith
 * 
 */
public class SensorEventDistributor implements Exporter, SensorEventListener {
	private boolean running = false;
	private static SensorEventDistributor thisInstance = null;
	private List<SensorEventListener> linearAccelerometerEventListenerList;
	private List<SensorEventListener> orientationEventListenerList;
	private List<SensorEventListener> gravityEventListenerList;
	private final SensorHistory linearAccelerometerHistory = new SensorHistory();
	private final SensorHistory orientationHistory = new SensorHistory();
	private final SensorHistory gravityHistory = new SensorHistory();
	private static SensorManager sm;
	private List<Sensor> lSensor;
	private static Context context;

	/**
	 * Initialize the SensorEventDistributor. Only the first provided context is considered.
	 * 
	 * @param context
	 *            for accessing of SensorService
	 * @return a singleton SensorEventDistributor
	 */
	public static SensorEventDistributor getInstance(Context context) {
		if (thisInstance == null) {
			Log.i("FOOTPATH", "Creating SensorEventDistributorInstance");
			thisInstance = new SensorEventDistributor();
			SensorEventDistributor.context = context;
		}
		Log.i("FOOTPATH", "Returned SensorEventDistributor");
		return thisInstance;
	}

	/**
	 * singleton.
	 */
	private SensorEventDistributor() {
	}

	public void addLinearAccelerometerListener(SensorEventListener sel) {
		if (linearAccelerometerEventListenerList == null) {
			linearAccelerometerEventListenerList = new LinkedList<SensorEventListener>();
		}
		if (running) {
			// TODO: if running start lin acc events if first listener
		}
		Log.i("FOOTPATH", "Adding Acc Ev Lis");
		linearAccelerometerEventListenerList.add(sel);
	}

	public void removeLinearAccelerometerListener(SensorEventListener sel) {
		if (linearAccelerometerEventListenerList == null || sel == null) {
			return;
		}
		if (linearAccelerometerEventListenerList.size() == 0) {
			// TODO: remove lin acc events if last listener

		}
		Log.i("FOOTPATH", "Removing Acc Ev Lis");
		linearAccelerometerEventListenerList.remove(sel);
	}

	public void addOrientationListener(SensorEventListener sel) {
		if (orientationEventListenerList == null) {
			orientationEventListenerList = new LinkedList<SensorEventListener>();
		}
		if (running) {
			// TODO: if running start orientation events if first listener
		}
		Log.i("FOOTPATH", "Adding orientation listener");
		orientationEventListenerList.add(sel);
	}

	public void removeOrientationListener(SensorEventListener sel) {
		if (orientationEventListenerList == null || sel == null) {
			return;
		}
		if (orientationEventListenerList.size() == 0) {
			// TODO: remove orientation events if last listener

		}
		Log.i("FOOTPATH", "Removing orientation listener");
		orientationEventListenerList.remove(sel);
	}

	public void addGravityListener(SensorEventListener sel) {
		if (gravityEventListenerList == null) {
			gravityEventListenerList = new LinkedList<SensorEventListener>();
		}
		if (running) {
			// TODO: if running start lin acc events if first listener
		}
		Log.i("FOOTPATH", "Adding Grav Ev Lis");
		gravityEventListenerList.add(sel);
	}

	public void removeGravityListener(SensorEventListener sel) {
		if (gravityEventListenerList == null || sel == null) {
			return;
		}
		if (gravityEventListenerList.size() == 0) {
			// TODO: remove grav events if last listener

		}
		Log.i("FOOTPATH", "Removing Gravity Ev Lis");
		gravityEventListenerList.remove(sel);
	}

	public synchronized boolean isRunning() {
		return running;
	}

	private void initSensorsForExistingListeners() {
		for (int i = 0; i < lSensor.size(); i++) {
			// Specifiy required sensor(s)
			switch (lSensor.get(i).getType()) {
			case Sensor.TYPE_LINEAR_ACCELERATION:
				Log.i("FOOTPATH", "Registering Linear Acceleration Sensor");
				sm.registerListener(this, lSensor.get(i), SensorManager.SENSOR_DELAY_GAME);
				break;
			case Sensor.TYPE_ORIENTATION:
				Log.i("FOOTPATH", "Registering Orientation Sensor");
				sm.registerListener(this, lSensor.get(i), SensorManager.SENSOR_DELAY_GAME);
				break;
			case Sensor.TYPE_GRAVITY:
				Log.i("FOOTPATH", "Registering Gravity Sensor");
				sm.registerListener(this, lSensor.get(i), SensorManager.SENSOR_DELAY_GAME);
				break;

			}
		}
	}

	public synchronized void _a_startSensorUpdates() {
		running = true;
		Log.i("FOOTPATH", "Starting sensor updates");
		sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		lSensor = sm.getSensorList(Sensor.TYPE_ALL);
		initSensorsForExistingListeners();
	};

	public synchronized void _b1_pauseSensorUpdates() {
		running = false;
		sm.unregisterListener(this);
	};

	public synchronized void _b2_unPauseSensorUpdates() {
		running = true;
		initSensorsForExistingListeners();
	};

	public synchronized void _c_stopSensorUpdates() {
		running = false;
		sm.unregisterListener(this);
	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		long now = System.currentTimeMillis();
		// WARNING: DONT FORGET THIS!!
		event.timestamp = now;

		switch (event.sensor.getType()) {
		case Sensor.TYPE_LINEAR_ACCELERATION:
			linearAccelerometerHistory.add(new SensorTriple(event.values, now, event.sensor.getType()));
			if (linearAccelerometerEventListenerList != null) {
				for (SensorEventListener sel : linearAccelerometerEventListenerList) {
					if (sel != null) {
						sel.onSensorChanged(event);
					}
				}
			}
			break;
		case Sensor.TYPE_ORIENTATION:
			orientationHistory.add(new SensorTriple(event.values, now, event.sensor.getType()));
			if (orientationEventListenerList != null) {
				for (SensorEventListener sel : orientationEventListenerList) {
					if (sel != null) {
						sel.onSensorChanged(event);
					}
				}
			}
			break;
		case Sensor.TYPE_GRAVITY:
			gravityHistory.add(new SensorTriple(event.values, now, event.sensor.getType()));
			if (gravityEventListenerList != null) {
				for (SensorEventListener sel : gravityEventListenerList) {
					if (sel != null) {
						sel.onSensorChanged(event);
					}
				}
			}
			break;
		}
	}

	@Override
	public int export_allData(String path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int export_recentData(String path) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int export_clearData() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int export_consumedBytes() {
		// TODO Auto-generated method stub
		return 0;
	};
}
