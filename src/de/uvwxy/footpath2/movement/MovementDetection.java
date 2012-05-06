package de.uvwxy.footpath2.movement;

import java.util.LinkedList;

public abstract class MovementDetection {
	private boolean running = false;
	private LinkedList<StepEventListener> onStepListenerList;

	public void registerOnStepListener(StepEventListener osl) {
		if (onStepListenerList == null) {
			onStepListenerList = new LinkedList<StepEventListener>();
		}
		onStepListenerList.add(osl);
	}

	public void removeOnStepListener(StepEventListener osl) {
		if (onStepListenerList == null || osl == null) {
			return;
		}
		onStepListenerList.remove(osl);
	}

	public synchronized boolean isRunning() {
		return running;
	}

	public synchronized void _a_startMovementDetection() {
		running = true;

	};

	public synchronized void _b1_pauseMovementDetection() {
		running = false;

	};

	public synchronized void _b2_unPauseMovementDetection() {
		running = true;

	};

	public synchronized void _c_stopMovementDetection() {
		running = false;

	};

}
