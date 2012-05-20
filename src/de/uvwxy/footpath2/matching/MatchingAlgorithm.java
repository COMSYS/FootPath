package de.uvwxy.footpath2.matching;

import android.location.Location;
import de.uvwxy.footpath2.movement.StepEventListener;

public abstract class MatchingAlgorithm implements StepEventListener {
	private Location currentLocation;

	public Location getCurrentLocation() {
		return currentLocation;
	}

	@Override
	public abstract void onStepUpdate(double bearing, double steplength, long timestamp,
			double estimatedStepLengthError, double estimatedBearingError);

}
