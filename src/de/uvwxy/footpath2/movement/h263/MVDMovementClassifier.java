package de.uvwxy.footpath2.movement.h263;

import android.util.Log;
import android.view.SurfaceHolder;
import de.uvwxy.footpath2.movement.MovementDetection;
import de.uvwxy.footpath2.movement.SensorEventDistributor;
import de.uvwxy.footpath2.movement.StepEventListener;
import de.uvwxy.footpath2.movement.h263.classifier.MVDClassifierIPIN;
import de.uvwxy.footpath2.movement.h263.classifier.MVDClasssifier;

public class MVDMovementClassifier extends MovementDetection implements MVDTrigger {
	private FlowPathInterface flowpath;
	private SurfaceHolder flowPathSurfaceHolder = null;
	private MVDClasssifier mvdclassifier = new MVDClassifierIPIN();
	private float stepLength;
	private SensorEventDistributor sde = SensorEventDistributor.getInstance(null);

	public void setSurfaceHolder(SurfaceHolder flowPathSurfaceHolder) {
		this.flowPathSurfaceHolder = flowPathSurfaceHolder;
		flowpath = FlowPathInterface.getInterface();
		// register classifier to be updated with MVD fields
		flowpath.addMVDTrigger((MVDTrigger) this);
	}

	private float curEvalLength;

	@Deprecated
	public float getCurEvalLength() {
		return curEvalLength;
	}

	private float fpsF;

	public float getFPS() {
		return fpsF;
	}

	private long s = System.currentTimeMillis();

	@Override
	public void processMVData(long now_ms, float[][][] mvds) {

		curEvalLength = mvdclassifier.classify(now_ms, mvds);
		stepLength = curEvalLength / FlowPathConfig.PIC_FPS;
		fpsF = 1000f / (System.currentTimeMillis() - s);
//		Log.i("FLOWPATH", "@ " + ((System.currentTimeMillis() - s)));
		s = System.currentTimeMillis();
		// Log.i("FLOWPATH", "Classifier returned " + stepLength + "m");
		if (stepLength == 0.0f) {
			// no speed detected
			return;
		}

		// TODO: call parsing function as thread and wait for it to be returned. this should speed up parsing on at
		// least dualcore devices
		// Skip parsing of mvd data if thread is not finished yet and thus parse stream as fast as possible

		// prevent NPE
		if (onStepListenerList != null && onStepListenerList.size() > 0) {
			for (StepEventListener l : onStepListenerList) {
				// TODO: Documentation 0.0 = not defined similar to accuracy in Location
				l.onStepUpdate(sde.getAzimuth(), stepLength, now_ms, 0.0, 0.0);
			}
		}

	}

	@Override
	public void _a_startMovementDetection() {
		// simple starting/stopping of flowpath

		flowpath.startFlowpath(flowPathSurfaceHolder);
	}

	@Override
	public void _b1_pauseMovementDetection() {
		flowpath.stopFlowPath();
	}

	@Override
	public void _b2_unPauseMovementDetection() {
		flowpath.startFlowpath(flowPathSurfaceHolder);
	}

	@Override
	public void _c_stopMovementDetection() {
		flowpath.stopFlowPath();
	}

}
