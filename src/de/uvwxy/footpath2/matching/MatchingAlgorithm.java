package de.uvwxy.footpath2.matching;

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
	protected float initialStepLength = 0.5f;
	protected IndoorLocationList path;

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

	@Override
	public abstract void onStepUpdate(double bearing, double steplength, long timestamp,
			double estimatedStepLengthError, double estimatedBearingError);

}
