package de.uvwxy.footpath2.movement;

public interface StepEventListener {
	void onStepUpdate(float bearing, float steplength, long timestamp,
			float estimatedStepLengthError, float estimatedBearingError);
}
