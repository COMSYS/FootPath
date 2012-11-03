package de.uvwxy.footpath2.movement.h263;

import android.util.Log;
import android.view.SurfaceHolder;
import de.uvwxy.footpath2.movement.MovementDetection;
import de.uvwxy.footpath2.movement.SensorEventDistributor;
import de.uvwxy.footpath2.movement.StepEventListener;
import de.uvwxy.footpath2.movement.h263.classifier.MVDClassifierMWC;
import de.uvwxy.footpath2.movement.h263.classifier.MVDClasssifier;

public class MVDMovementClassifier extends MovementDetection implements MVDTrigger {
	private FlowPathInterface flowpath;
	private SurfaceHolder flowPathSurfaceHolder = null;
	private MVDClasssifier mvdclassifier = new MVDClassifierMWC();
	private int numOfDetectedStep;
	private SensorEventDistributor sde = SensorEventDistributor.getInstance(null);

	public void setSurfaceHolder(SurfaceHolder flowPathSurfaceHolder) {
		this.flowPathSurfaceHolder = flowPathSurfaceHolder;
		flowpath = FlowPathInterface.getInterface();
		// register classifier to be updated with MVD fields
		flowpath.addMVDTrigger((MVDTrigger) this);
	}

	@Override
	public void processMVData(long now_ms, float[][][] mvds) {
		Log.i("FOOTPATH", "RECEIVED MVD ole");
		numOfDetectedStep = mvdclassifier.classify(now_ms, mvds);

		// TODO: call parsing function as thread and wait for it to be returned. this should speed up parsing on at
		// least dualcore devices
		// Skip parsing of mvd data if thread is not finished yet and thus parse stream as fast as possible

		// prevent NPE
		if (onStepListenerList != null && onStepListenerList.size() > 0) {
			for (StepEventListener l : onStepListenerList) {
				for (int i = 0; i < numOfDetectedStep; i++) {
					// TODO: Documentation 0.0 = not defined similar to accuracy in Location
					l.onStepUpdate(sde.getAzimuth(), 0.5, now_ms, 0.0, 0.0);
				}
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
