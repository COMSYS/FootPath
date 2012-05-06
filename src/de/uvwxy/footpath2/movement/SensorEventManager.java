package de.uvwxy.footpath2.movement;

import java.util.LinkedList;

import android.hardware.SensorEventListener;

/**
 * The current idea behind yet another SensorEventManager is that we can here,
 * globally filter/modify/log the sensor data.
 * 
 * @author Paul Smith
 * 
 */
public class SensorEventManager {

	private boolean running = false;
	private SensorEventManager thisInstance = null;
	private LinkedList<SensorEventListener> sensorEventListenerList;

	public SensorEventManager getInstance() {
		if (thisInstance == null) {
			thisInstance = new SensorEventManager();
		}
		return thisInstance;
	}

	private SensorEventManager() {
	}

	public void registerOnStepListener(SensorEventListener sel) {
		if (sensorEventListenerList == null) {
			sensorEventListenerList = new LinkedList<SensorEventListener>();
		}
		sensorEventListenerList.add(sel);
	}

	public void removeOnStepListener(SensorEventListener sel) {
		if (sensorEventListenerList == null || sel == null) {
			return;
		}
		sensorEventListenerList.remove(sel);
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
	};
}
