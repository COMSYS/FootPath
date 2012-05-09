package de.uvwxy.footpath2.movement;

import java.util.LinkedList;

import de.uvwxy.footpath2.tools.Loggable;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

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

	public static SensorEventDistributor getInstance() {
		if (thisInstance == null) {
			thisInstance = new SensorEventDistributor();
		}
		return thisInstance;
	}

	private SensorEventDistributor() {
	}

	public void addLinearAccelerometerListener(SensorEventListener sel) {
		if (linearAccelerometerEventListenerList == null) {
			linearAccelerometerEventListenerList = new LinkedList<SensorEventListener>();
		}
		// TODO: if running start lin acc events if first listener
		linearAccelerometerEventListenerList.add(sel);
	}

	public void removeLinearAccelerometerListener(SensorEventListener sel) {
		if (linearAccelerometerEventListenerList == null || sel == null) {
			return;
		}
		// TODO: remove lin acc events if last listener
		linearAccelerometerEventListenerList.remove(sel);
	}

	public synchronized boolean isRunning() {
		return running;
	}

	public synchronized void _a_startSensorUpdates() {
		running = true;
	};

	public synchronized void _b1_pauseSensorUpdates() {
		running = false;
	};

	public synchronized void _b2_unPauseSensorUpdates() {
		running = true;
	};

	public synchronized void _c_stopSensorUpdates() {
		running = false;
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
