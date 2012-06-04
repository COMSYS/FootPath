package de.uvwxy.footpath2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.util.Log;
import de.uvwxy.footpath.Rev;
import de.uvwxy.footpath2.map.GraphNode;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.map.IndoorLocationList;
import de.uvwxy.footpath2.map.Map;
import de.uvwxy.footpath2.matching.BestFit;
import de.uvwxy.footpath2.matching.MatchingAlgorithm;
import de.uvwxy.footpath2.movement.MovementDetection;
import de.uvwxy.footpath2.movement.SensorEventDistributor;
import de.uvwxy.footpath2.movement.steps.StepDetectionImpl;
import de.uvwxy.footpath2.tools.FootPathException;
import de.uvwxy.footpath2.types.FP_LocationProvider;
import de.uvwxy.footpath2.types.FP_MatchingAlgorithm;
import de.uvwxy.footpath2.types.FP_MovementDetection;

/**
 * This class will be the main interface to use footpath.
 * 
 * @author paul
 * 
 */
public class FootPath {
	private final Context context;
	private final Map map;
	private final SensorEventDistributor sensorEventDistributor;
	private MovementDetection movementDetection;
	private MatchingAlgorithm matchingAlgorithm;
	private FP_MovementDetection settingsMovementDetection;
	private FP_MatchingAlgorithm settingsMatchingAlgorithm;
	private FP_LocationProvider settingsLocationProvider;

	// TODO: make this simpleton?
	public FootPath(Context context) {
		this.context = context;
		sensorEventDistributor = SensorEventDistributor.getInstance(context);
		map = new Map();
	}

	public String getRevision() {
		return Rev.rev.substring(0, 8);
	}

	private void exampleUsage() throws FootPathException {

		// initialization
		FootPath fp = new FootPath(null);

		// movement input and location output setup
		_b_setMovementDetection(FP_MovementDetection.MOVEMENT_DETECTION_STEPS);
		_c_setMatchingAlgorithm(FP_MatchingAlgorithm.MATCHING_BEST_FIT);
		_d_setInitialStepLength(0.5f);
		_e_setLocationProvider(FP_LocationProvider.LOCATION_PROVIDER_FOOTPATH);

		_f1_setDestination("room1");
		// set location (possibility: also later during navigation (Wifi, etc))
		_g1_setLocation("room2");

		// starting, stopping, resetting of navigation
		_h_start();
		_j_stop();

		return;
	}

	public void _a1_loadMapDataFromAsset(String uri) {
		// TODO:

	}

	public void _a2_loadMapDataFromXMLResource(int resID) {
		// TODO:
		try {
			map.addToGraphFromXMLResourceParser(context.getResources().getXml(resID));
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void _a3_loadMapDataFromURL(String uri) {
		// TODO:

	}

	public void _a4_loadMapDataFromXMLFile(String uri) {
		Log.i("FOOTPATH", "Loading map data from " + uri);
		try {
			map.addToGraphFromXMLFile(uri);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void _b_setMovementDetection(FP_MovementDetection movementType) {
		settingsMovementDetection = movementType;
		switch (movementType) {
		case MOVEMENT_DETECTION_STEPS:
			movementDetection = new StepDetectionImpl(context);
			sensorEventDistributor.addLinearAccelerometerListener((SensorEventListener) movementDetection);
			sensorEventDistributor.addOrientationListener((SensorEventListener) movementDetection);
			break;
		case MOVEMENT_DETECTION_SOUND_SEGWAY:
			break;
		case MOVEMENT_DETECTION_VIDEO_WHEELCHAIR:
			break;
		default:
			throw new IllegalArgumentException(movementType.toString());
		}
	}

	public void _c_setMatchingAlgorithm(FP_MatchingAlgorithm matchingType) {
		settingsMatchingAlgorithm = matchingType;
		switch (matchingType) {
		case MATCHING_BEST_FIT:
			matchingAlgorithm = new BestFit();
			Log.i("FOOTPATH", "Registering BestFit for movement detection");
			movementDetection.registerOnStepListener(matchingAlgorithm);
			break;
		case MATCHING_FIRST_FIT:
			break;
		case MATCHING_MULTI_FIT:
			break;
		default:
			throw new IllegalArgumentException(matchingType.toString());
		}
	}

	public void _d_setInitialStepLength(float f) throws FootPathException {
		if (matchingAlgorithm != null) {
			matchingAlgorithm.setInitialStepLength(f);
		} else {
			throw new FootPathException("No matching algorithm selected!");
		}
	}

	public void _e_setLocationProvider(FP_LocationProvider providerType) {
		// TODO:
		settingsLocationProvider = providerType;
	}

	public IndoorLocationList getPath(String location, String destination, boolean staircase, boolean elevator,
			boolean outside) {
		Log.i("FOOTPATH", "Trying to find path from " + location + " to " + destination);
		IndoorLocationList ret = new IndoorLocationList();
		Stack<GraphNode> buf = map.getShortestPath(location, destination, staircase, elevator, outside);
		if (buf != null) {
			for (GraphNode n : buf) {
				IndoorLocation x = new IndoorLocation(n.getName(), "FootPath");
				x.setLatitude(n.getLat());
				x.setLongitude(n.getLon());
				ret.add(x);
			}
		}
		return ret;
	}

	public void _fg_setPath(IndoorLocationList path) throws FootPathException {
		if (matchingAlgorithm != null) {
			if (path != null) {
				matchingAlgorithm.setPath(path);
			} else {
				throw new FootPathException("Path was null, bad boy!"); // ;)
			}
		} else {
			throw new FootPathException("No matching algorithm selected!");
		}

	}

	public void _f1_setDestination(String room) {

	}

	public void _g1_setLocation(String room) {

	}

	public void _f2_setDestination(Location l) {
		// TODO:

	}

	public void _g2_setLocation(Location l) {
		// TODO:

	}

	public String[] getRoomList() {
		return map.getRoomList();
	}

	public void _h_start() throws FootPathException {
		// TODO:
		matchingAlgorithm.init();
		sensorEventDistributor._a_startSensorUpdates();
		movementDetection._a_startMovementDetection();
		
	}

	public void _i1_pause() {
		sensorEventDistributor._b1_pauseSensorUpdates();
		movementDetection._b1_pauseMovementDetection();
	}

	public void _i2_unpause() {
		sensorEventDistributor._b2_unPauseSensorUpdates();
		movementDetection._b2_unPauseMovementDetection();
	}

	public int _j_stop() {
		sensorEventDistributor._c_stopSensorUpdates();
		movementDetection._c_stopMovementDetection();

		return 0;
	}
	
	@Deprecated
	public MatchingAlgorithm _debug_getMatchinAlgorithm(){
		return matchingAlgorithm;
	}
}
