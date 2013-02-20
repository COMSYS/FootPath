package de.uvwxy.footpath2.matching;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.util.Log;
import de.uvwxy.footpath2.map.GraphEdge;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.map.IndoorLocationList;
import de.uvwxy.footpath2.movement.StepEventListener;
import de.uvwxy.footpath2.tools.FootPathException;

/**
 * What is new in FootPath2? The path is supplied by a IndoorLocationHistory, not as a list of GraphEdges This way it is
 * possible to add custom paths created outside of the library, i.e. not created by our map class. A path is described
 * by waypoints, i.e. navigating form one indoor location to another.
 * 
 * 
 * 
 * @author paul
 * 
 */
public abstract class MatchingAlgorithm implements StepEventListener {
	protected IndoorLocation currentLocation;
	protected double progress = 0.0;
	protected int currentStep = 0;
	protected List<GraphEdge> edges = null;

	protected float initialStepLength = 0.5f;
	protected IndoorLocationList path;
	protected IndoorLocationList returnedPositions = new IndoorLocationList();

	public void setInitialStepLength(float f) {
		initialStepLength = f;
	}

	public IndoorLocation getLocation() {
		return currentLocation;
	}

	public void setPath(IndoorLocationList path) {
		this.path = path;
		currentLocation = path.getFirst();
	}

	public abstract void init() throws FootPathException;

	private float trackedDistance = 0.0f;

	private Object threadRunningLock = new Object();
	private boolean threadRunning = false;

	private class StepWorker implements Runnable {
		private boolean queue_is_empty = true;

		@Override
		public void run() {
			setThreadRunningSemaphore(true);
			checkXIsEmptySemaphore();

			int i = 0;
			while (!queue_is_empty) {
				Step s = stepQueue.pop();
				onStepUpdateOnThread(s.bearing, s.steplength, s.timestamp, 0.0f, 0.0f);
				Log.i("FOOTPATH", "Thread consumed step " + ++i);
				checkXIsEmptySemaphore();
			}

			setThreadRunningSemaphore(false);
		}

		private void setThreadRunningSemaphore(boolean b) {
			synchronized (threadRunningLock) {
				threadRunning = b;
			}

		}

		private void checkXIsEmptySemaphore() {
			synchronized (stepQueue) {
				queue_is_empty = stepQueue.isEmpty();
			}
		}

	}

	private LinkedList<Step> stepQueue = new LinkedList<Step>();

	private class Step {
		float bearing, steplength; // TODO: estimatedStepLengthError, estimatedBearingError;
		long timestamp;

		public Step(float bearing, float steplength, long timestamp) {
			this.bearing = bearing;
			this.steplength = steplength;
			this.timestamp = timestamp;
		}
	}

	public void onStepUpdate(float bearing, float steplength, long timestamp, float estimatedStepLengthError,
			float estimatedBearingError) {

		
		while (bearing < 0f){
			Log.i("FOOTPATH", "Fixing negative input: " + bearing);
			bearing +=360f;
		}
		
		trackedDistance += steplength;
		if (trackedDistance < initialStepLength) {
			// virtual step length not reached, return; nothing to add to queue
			// Log.i("FOOTPATH", "Steps not long enough yet trackedDistance = " + trackedDistance + " ; steplength = " +
			// steplength);
			return;
		}

		// add virtual steps to queue:
		while (trackedDistance >= initialStepLength) {
			// we use the bearing of latest detected step here.
			// as we currently do not use initial/virtual step lengths which differ largely from the detected step
			// length we do not change too much of the value, but still:
			// TODO: !
			stepQueue.addLast(new Step(bearing, initialStepLength, timestamp));
			// Log.i("FOOTPATH", "Added step to queue " + bearing + ", " + initialStepLength + "m");
			trackedDistance -= initialStepLength;
		}

		// trigger thread to start consuming virtual steps, if there is none running
		synchronized (threadRunningLock) {
			if (!threadRunning) {
				Thread t = new Thread(new StepWorker());
				t.start();
			}
		}

	}

	public abstract void onStepUpdateOnThread(float bearing, float steplength, long timestamp,
			float estimatedStepLengthError, float estimatedBearingError);

	protected IndoorLocation getPositionFromProgress() {
		IndoorLocation ret = new IndoorLocation("" + currentStep, "FootPath");
		if (edges == null) {
			return null;
		}

		double tempProgress = progress;
		Log.i("FOOTPATH", "BestFit: tempProgress=" + tempProgress);
		for (int i = 0; i < edges.size() - 1; i++) {
			Log.i("FOOTPATH", "BestFit: -> tempProgress was " + tempProgress);
			
			if (tempProgress - edges.get(i).getLen() > 0) {
				tempProgress -= edges.get(i).getLen();
				Log.i("FOOTPATH", "BestFit: -> tempProgress=" + tempProgress);
			} else {
				Log.i("FOOTPATH", "BestFit: !! tempProgress=" + tempProgress);
				// read: edge e_i = (loc_i, loc_(i+1))
				IndoorLocation x = path.get(i);
				ret.setLatitude(x.getLatitude());
				ret.setLongitude(x.getLongitude());
				Log.i("FOOTPATH", "BestFit: !! dist = " + x.distanceTo(path.get(i + 1)));
				Log.i("FOOTPATH", "BestFit: !! mv factor = " + tempProgress + " / " + x.distanceTo(path.get(i + 1))
						+ " = " + tempProgress / x.distanceTo(path.get(i + 1)));

				ret.moveIntoDirection(path.get(i + 1), tempProgress / x.distanceTo(path.get(i + 1)));
				ret.setLevel(x.getLevel());
				// ret=path.get(i);
				break;
			}
			
		}
		Log.i("FOOTPATH", "BestFit: " + ret.getLatitude() + "/ " + ret.getLongitude());
		return ret;
	}

	@Deprecated
	public IndoorLocationList _debug_getLocHist() {
		return returnedPositions;
	}
}
