package de.uvwxy.footpath2.matching;

import java.util.List;

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

	@Override
	public abstract void onStepUpdate(double bearing, double steplength, long timestamp,
			double estimatedStepLengthError, double estimatedBearingError);

	protected IndoorLocation getPositionFromProgress() {
		IndoorLocation ret = new IndoorLocation("" + currentStep, "FootPath");
		if (edges == null){
			return null;
		}
		
		double tempProgress = progress;
		Log.i("FOOTPATH", "BestFit: tempProgress=" + tempProgress);
		for (int i = 0; i < edges.size() - 1; i++) {
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
				Log.i("FOOTPATH", "BestFit: !! mv factor = " + tempProgress + " / " +  x.distanceTo(path.get(i + 1))
						+ " = " + tempProgress /  x.distanceTo(path.get(i + 1)));

				ret.moveIntoDirection(path.get(i + 1),tempProgress / x.distanceTo(path.get(i + 1)) );
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
