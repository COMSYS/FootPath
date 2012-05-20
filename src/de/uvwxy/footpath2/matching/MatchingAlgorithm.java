package de.uvwxy.footpath2.matching;

import android.location.Location;
import de.uvwxy.footpath2.movement.StepEventListener;

//this would rahter be an itnerface, wouldnt it?! TODO.
public abstract class MatchingAlgorithm implements StepEventListener {
	private Location currentLocation;

	public Location getCurrentLocation() {
		return currentLocation;
	}

	@Override
	public abstract void onStepUpdate(double bearing, double steplength, long timestamp,
			double estimatedStepLengthError, double estimatedBearingError);

}
