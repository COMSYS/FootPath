package de.uvwxy.footpath2.movement;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import de.uvwxy.footpath2.FootPath;
import de.uvwxy.footpath2.log.ExportManager;

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
public class SensorEventDistributor implements SensorEventListener {
	private boolean running = false;
	private static SensorEventDistributor thisInstance = null;
	private List<SensorEventListener> linearAccelerometerEventListenerList;
	private List<SensorEventListener> orientationEventListenerList;
	private List<SensorEventListener> gravityEventListenerList;
	private List<SensorEventListener> pressureEventListenerList;

	private final SensorHistory linearAccelerometerHistory = new SensorHistory();
	private final SensorHistory orientationHistory = new SensorHistory();
	private final SensorHistory gravityHistory = new SensorHistory();
	private final SensorHistory pressureHistory = new SensorHistory();

	private static SensorManager sm;
	private List<Sensor> lSensor;
	private static Context context;
	private ExportManager em = ExportManager.getInstance();

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

	/**
	 * Obtain a direction of the user. Needed for example by the compass needle drawn onto the screen.
	 * 
	 * @return the azimuth of the heading
	 */
	public float getAzimuth() {
		SensorTriple t = orientationHistory.getLast();

		if (t != null)
			return t.getValues()[0];
		else
			return -1f;
	}

	public void addLinearAccelerometerListener(SensorEventListener sel) {
		if (linearAccelerometerEventListenerList == null) {
			linearAccelerometerEventListenerList = new LinkedList<SensorEventListener>();
		}
		Log.i("FOOTPATH", "Adding Acc Ev Lis");
		linearAccelerometerEventListenerList.add(sel);
	}

	public void removeLinearAccelerometerListener(SensorEventListener sel) {
		if (linearAccelerometerEventListenerList == null || sel == null) {
			return;
		}
		Log.i("FOOTPATH", "Removing Acc Ev Lis");
		linearAccelerometerEventListenerList.remove(sel);
	}

	public void addOrientationListener(SensorEventListener sel) {
		if (orientationEventListenerList == null) {
			orientationEventListenerList = new LinkedList<SensorEventListener>();
		}
		Log.i("FOOTPATH", "Adding orientation listener");
		orientationEventListenerList.add(sel);
	}

	public void removeOrientationListener(SensorEventListener sel) {
		if (orientationEventListenerList == null || sel == null) {
			return;
		}
		Log.i("FOOTPATH", "Removing orientation listener");
		orientationEventListenerList.remove(sel);
	}

	public void addGravityListener(SensorEventListener sel) {
		if (gravityEventListenerList == null) {
			gravityEventListenerList = new LinkedList<SensorEventListener>();
		}
		Log.i("FOOTPATH", "Adding Grav Ev Lis");
		gravityEventListenerList.add(sel);
	}

	public void removeGravityListener(SensorEventListener sel) {
		if (gravityEventListenerList == null || sel == null) {
			return;
		}
		Log.i("FOOTPATH", "Removing Gravity Ev Lis");
		gravityEventListenerList.remove(sel);
	}

	public void addpressureListener(SensorEventListener sel) {
		if (pressureEventListenerList == null) {
			pressureEventListenerList = new LinkedList<SensorEventListener>();
		}
		Log.i("FOOTPATH", "Adding Baromter Ev Lis");
		pressureEventListenerList.add(sel);
	}

	public void removepressureListener(SensorEventListener sel) {
		if (pressureEventListenerList == null || sel == null) {
			return;
		}
		Log.i("FOOTPATH", "Removing pressure Ev Lis");
		pressureEventListenerList.remove(sel);
	}

	public synchronized boolean isRunning() {
		return running;
	}

	/**
	 * This method is to be called to register wanted loggers for sensors.
	 */
	public void registerExportData() {
		// TODO: extract this information from config later
		boolean regLinAcc = true;
		boolean regOrientation = true;
		boolean regGravity = true;
		boolean regpressure = true;

		if (regLinAcc) {
			em.add("linearAccelerometerHistory", linearAccelerometerHistory);
		}

		if (regOrientation) {
			em.add("orientationHistory", orientationHistory);
		}

		if (regGravity) {
			em.add("gravityHistory", gravityHistory);
		}

		if (regpressure) {
			em.add("baromterHistory", pressureHistory);
		}
	}

	private void initSensorsForExistingListeners() {
		for (int i = 0; i < lSensor.size(); i++) {
			// Specifiy required sensor(s)
			switch (lSensor.get(i).getType()) {
			case Sensor.TYPE_LINEAR_ACCELERATION:
				Log.i("FOOTPATH", "Registering Linear Acceleration Sensor");
				sm.registerListener(this, lSensor.get(i), SensorManager.SENSOR_DELAY_FASTEST);
				break;
			case Sensor.TYPE_ORIENTATION:
				Log.i("FOOTPATH", "Registering Orientation Sensor");
				sm.registerListener(this, lSensor.get(i), SensorManager.SENSOR_DELAY_FASTEST);
				break;
			case Sensor.TYPE_GRAVITY:
				Log.i("FOOTPATH", "Registering Gravity Sensor");
				sm.registerListener(this, lSensor.get(i), SensorManager.SENSOR_DELAY_FASTEST);
				break;
			case Sensor.TYPE_PRESSURE:
				Log.i("FOOTPATH", "Registering Pressure Sensor");
				sm.registerListener(this, lSensor.get(i), SensorManager.SENSOR_DELAY_FASTEST);
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
		if (sm != null)
			sm.unregisterListener(this);
	};

	public synchronized void _b2_unPauseSensorUpdates() {
		running = true;
		initSensorsForExistingListeners();
	};

	public synchronized void _c_stopSensorUpdates() {
		running = false;
		if (sm != null)
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
			SensorTriple tempTriple = new  SensorTriple(event.values, now, event.sensor.getType());
			if (FootPath.INVERTED_SRC_COMPASS){
				tempTriple.getValues()[0] = tempTriple.getValues()[0]*-1f;
			}
			orientationHistory.add(tempTriple);
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
		case Sensor.TYPE_PRESSURE:
			pressureHistory.add(new SensorTriple(event.values, now, event.sensor.getType()));
			if (pressureEventListenerList != null) {
				for (SensorEventListener sel : pressureEventListenerList) {
					if (sel != null) {
						sel.onSensorChanged(event);
					}
				}
			}
			break;

		}
	}

}
