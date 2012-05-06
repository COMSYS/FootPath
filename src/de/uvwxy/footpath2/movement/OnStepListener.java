package de.uvwxy.footpath2.movement;

public interface OnStepListener {
	void onStepUpdate(double bearing, double steplength, long timestamp,
			double estimatedStepLengthError, double estimatedBearingError);
}
