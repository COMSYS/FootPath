package de.uvwxy.footpath2.matching.multifit;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import de.uvwxy.footpath2.drawing.DrawToCanvas;
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
public class MultiFit extends MatchingAlgorithm implements DrawToCanvas {
	private Map map;
	private PartialPenaltyTree ppt;

	private IndoorLocation start;
	private IndoorLocation target;
	private float trackedDistance = 0.0f;
	
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

		trackedDistance += steplength;
		trackedDistance -= initialStepLength;
		
		Log.i("FOOTPATH", "MultFit working...");
		long ms = System.currentTimeMillis();
		currentStep++;
		ppt.onStepUpdate((float) bearing, steplength, timestamp, estimatedStepLengthError, estimatedBearingError);
		returnedPositions.add(ppt.getCurrentBestLocation());
		currentLocation = ppt.getCurrentBestLocation();
		Log.i("FOOTPATH", "MultiFit done. (" + (System.currentTimeMillis() - ms) + "ms)");
		
		if (trackedDistance >= initialStepLength) {
			Log.i("FOOTPATH", "RETRACKING");
			// if we have detected a "longer" step than we have walked with the algorithm then repaeat with a further
			// step but do not add anything to the length -> 0.0f
			onStepUpdate(bearing, 0.0f, timestamp, estimatedStepLengthError, estimatedBearingError);
		}
	}

	@Override
	public void drawToCanvas(Canvas canvas, IndoorLocation center, int ox, int oy, float pixelsPerMeterOrMaxValue) {
		ppt.drawToCanvas(canvas, center, ox, oy, pixelsPerMeterOrMaxValue);

	}

}
