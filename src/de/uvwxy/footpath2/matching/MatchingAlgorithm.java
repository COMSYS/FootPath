package de.uvwxy.footpath2.matching;

import de.uvwxy.footpath2.movement.StepEventListener;

public abstract class MatchingAlgorithm  implements StepEventListener{

	@Override
	public abstract void onStepUpdate(double bearing, double steplength, long timestamp,
			double estimatedStepLengthError, double estimatedBearingError);

}
