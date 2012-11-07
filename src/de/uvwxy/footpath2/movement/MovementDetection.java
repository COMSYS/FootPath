package de.uvwxy.footpath2.movement;

import java.util.LinkedList;

public abstract class MovementDetection {
	private boolean running = false;
	protected LinkedList<StepEventListener> onStepListenerList;
	public float initialStepLength;
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

	public abstract void _a_startMovementDetection();

	public abstract void _b1_pauseMovementDetection();

	public abstract void _b2_unPauseMovementDetection();

	public abstract void _c_stopMovementDetection();

	public void setInitialStepLength(float f) {
		// TODO Auto-generated method stub
		
	}

}
