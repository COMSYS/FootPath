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
	public void onStepUpdateOnThread(float bearing, float steplength, long timestamp, float estimatedStepLengthError,
			float estimatedBearingError) {

		Log.i("FOOTPATH", "MultFit working...");
		long ms = System.currentTimeMillis();
		currentStep++;
		ppt.onStepUpdate(bearing, steplength, timestamp, estimatedStepLengthError, estimatedBearingError);
		returnedPositions.add(ppt.getCurrentBestLocation());
		currentLocation = ppt.getCurrentBestLocation();
		Log.i("FOOTPATH", "MultiFit done. (" + (System.currentTimeMillis() - ms) + "ms)");
	}

	@Override
	public void drawToCanvas(Canvas canvas, IndoorLocation center, int ox, int oy, float pixelsPerMeterOrMaxValue) {
		ppt.drawToCanvas(canvas, center, ox, oy, pixelsPerMeterOrMaxValue);

	}
}
