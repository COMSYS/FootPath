package de.uvwxy.footpath2.movement;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import de.uvwxy.footpath2.tools.Loggable;

/**
 * The current idea behind yet another SensorEventManager is that we can here,
 * globally filter/modify/log the sensor data.
 * 
 * @author Paul Smith
 * 
 */
public class SensorEventDistributor implements Loggable, SensorEventListener {
	private boolean running = false;
	private static SensorEventDistributor thisInstance = null;
	private LinkedList<SensorEventListener> linearAccelerometerEventListenerList;
	private SensorHistory linearAccelerometerHistory = new SensorHistory();
	private static SensorManager sm;
	private List<Sensor> lSensor;
	private static Context context;

	/**
	 * Initialize the SensorEventDistributor. Only the first provided context is
	 * considered.
	 * 
	 * @param context for accessing of SensorService
	 * @return a singleton SensorEventDistributor
	 */
	public static SensorEventDistributor getInstance(Context context) {
		if (thisInstance == null) {
			thisInstance = new SensorEventDistributor();
			SensorEventDistributor.context = context;
		}
		return thisInstance;
	}

	private SensorEventDistributor() {
	}

	public void addLinearAccelerometerListener(SensorEventListener sel) {
		if (linearAccelerometerEventListenerList == null) {
			linearAccelerometerEventListenerList = new LinkedList<SensorEventListener>();
		}
		if (running){
			// TODO: if running start lin acc events if first listener			
		}
		linearAccelerometerEventListenerList.add(sel);
	}

	public void removeLinearAccelerometerListener(SensorEventListener sel) {
		if (linearAccelerometerEventListenerList == null || sel == null) {
			return;
		}
		if (linearAccelerometerEventListenerList.size()==0){
			// TODO: remove lin acc events if last listener
			
		}

		linearAccelerometerEventListenerList.remove(sel);
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
				sm.registerListener(this, lSensor.get(i),
						SensorManager.SENSOR_DELAY_GAME);
				break;
			}
		}
	}

	public synchronized void _a_startSensorUpdates() {
		running = true;

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
	public void exportData(String path) {
		// TODO Auto-generated method stub

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
			linearAccelerometerHistory.add(new SensorTriple(event.values, now,
					event.sensor.getType()));
			for (SensorEventListener sel : linearAccelerometerEventListenerList) {
				if (sel != null) {
					sel.onSensorChanged(event);
				}
			}
			break;
		}
	};
}
