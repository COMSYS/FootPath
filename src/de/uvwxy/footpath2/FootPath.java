package de.uvwxy.footpath2;

import de.uvwxy.footpath2.types.FP_LocationProvider;
import de.uvwxy.footpath2.types.FP_MovementDetection;
import android.location.Location;

/**
 * This class will be the main interface to use footpath.
 * 
 * @author paul
 * 
 */
public class FootPath {

	public FootPath() {
		// TODO:
	}

	private void exampleUsage() {

		// initialization
		FootPath fp = new FootPath();

		// movement input and location output setup
		setMovementDetection(FP_MovementDetection.MOVEMENT_DETECTION_STEPS);
		setLocationProvider(FP_LocationProvider.LOCATION_PROVIDER_FOOTPATH);

		setDestination(null);
		// set location (possibility: also later during navigation (Wifi, etc))
		setLocation(null);

		// starting, stopping, resetting of navigation
		start();
		stop();
		reset();

		return;
	}

	public void loadMapDataFromAsset(String uri) {
		// TODO:

	}

	public void loadMapDataFromResource(String uri) {
		// TODO:

	}

	public void loadMapDataFromURL(String uri) {
		// TODO:

	}

	public void loadMapDataFromXML(String uri) {
		// TODO:

	}

	public void setMovementDetection(FP_MovementDetection movementDetectionSteps) {
		// TODO:
	}

	public void setLocationProvider(FP_LocationProvider provider) {
		// TODO:

	}

	public void setDestination(Location l) {
		// TODO:

	}

	public void setLocation(Location l) {
		// TODO:

	}

	public int start() {
		// TODO:

		return 0;
	}

	public int stop() {
		// TODO:

		return 0;
	}

	public int reset() {
		// TODO:

		return 0;
	}
}
