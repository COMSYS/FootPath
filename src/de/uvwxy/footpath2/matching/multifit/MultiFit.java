package de.uvwxy.footpath2.matching.multifit;

import android.util.Log;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.map.IndoorLocationList;
import de.uvwxy.footpath2.map.Map;
import de.uvwxy.footpath2.matching.MatchingAlgorithm;
import de.uvwxy.footpath2.tools.FootPathException;

/**
 * Path to give: Start and End point!
 * 
 * @author Paul Smith
 * 
 */
public class MultiFit extends MatchingAlgorithm {
	private Map map;
	private PartialPenaltyTree ppt;

	private IndoorLocation start;
	private IndoorLocation target;

	public MultiFit() {
		ppt = new PartialPenaltyTree();
	}

	public void setMap(Map map) {
		this.map = map;
	}

	@Override
	public void setPath(IndoorLocationList path) {
		super.setPath(path);
		start = path.getFirst();
		target = path.getLast();
		ppt._a_setStartLocation(start);
	}

	@Override
	public void setInitialStepLength(float f) {
		super.setInitialStepLength(f);
		ppt._b_setVirtualStepLength(f);
	}

	@Override
	public void init() throws FootPathException {
		if (path == null) {
			throw new FootPathException("Best Fit: Path was null during init");
		}
	}

	@Override
	public void onStepUpdate(double bearing, double steplength, long timestamp, double estimatedStepLengthError,
			double estimatedBearingError) {
		Log.i("FOOTPATH", "MultFit working...");
		currentStep++;
		ppt.onStepUpdate((float) bearing, steplength, timestamp, estimatedStepLengthError, estimatedBearingError);
		returnedPositions.add(ppt.getCurrentBestLocation());
	}

}
