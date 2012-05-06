package de.uvwxy.footpath2.movement;

public interface StepEventListener {
	void onStepUpdate(double bearing, double steplength, long timestamp,
			double estimatedStepLengthError, double estimatedBearingError);
}
