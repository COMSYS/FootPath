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
import de.uvwxy.footpath2.map.GraphNode;
import de.uvwxy.footpath2.map.LocationHistory;
import de.uvwxy.footpath2.map.Map;
import de.uvwxy.footpath2.matching.BestFit;
import de.uvwxy.footpath2.matching.MatchingAlgorithm;
import de.uvwxy.footpath2.movement.MovementDetection;
import de.uvwxy.footpath2.movement.SensorEventDistributor;
import de.uvwxy.footpath2.movement.steps.StepDetection;
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
	private Context context;
	private Map map;
	private SensorEventDistributor sensorEventDistributor;
	private MovementDetection movementDetection;
	private MatchingAlgorithm matchingAlgorithm;
	
	
	public FootPath(Context context) {
		this.context = context;
		sensorEventDistributor = SensorEventDistributor.getInstance(context);
		map = new Map();
	}

	private void exampleUsage() {

		// initialization
		FootPath fp = new FootPath(null);

		// movement input and location output setup
		setMovementDetection(FP_MovementDetection.MOVEMENT_DETECTION_STEPS);
		setMatchingAlgorithm(FP_MatchingAlgorithm.MATCHING_BEST_FIT);
		setLocationProvider(FP_LocationProvider.LOCATION_PROVIDER_FOOTPATH);

		setDestination("room1");
		// set location (possibility: also later during navigation (Wifi, etc))
		setLocation("room2");

		// starting, stopping, resetting of navigation
		_a_start();
		_c_stop();

		return;
	}

	public void setMovementDetection(FP_MovementDetection movementType) {
		switch (movementType) {
		case MOVEMENT_DETECTION_STEPS:
			movementDetection = new StepDetection(context);
			sensorEventDistributor.addLinearAccelerometerListener((SensorEventListener) movementDetection);
			break;
		case MOVEMENT_DETECTION_SOUND_SEGWAY:
			break;
		case MOVEMENT_DETECTION_VIDEO_WHEELCHAIR:
			break;
		}
	}

	public void setMatchingAlgorithm(FP_MatchingAlgorithm matchingType) {
		switch (matchingType) {
		case MATCHING_BEST_FIT:
			matchingAlgorithm = new BestFit();
			movementDetection.registerOnStepListener(matchingAlgorithm);
			break;
		case MATCHING_FIRST_FIT:
			break;
		case MATCHING_MULTI_FIT:
			break;
		}

	}

	public void setLocationProvider(FP_LocationProvider providerType) {
		// TODO:

	}

	public void loadMapDataFromAsset(String uri) {
		// TODO:

	}

	public void loadMapDataFromXMLResource(int resID) {
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

	public void loadMapDataFromURL(String uri) {
		// TODO:

	}

	public void loadMapDataFromXMLFile(String uri) {
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
	
	public LocationHistory getPath(String location, String destination, boolean staircase, boolean elevator,
			boolean outside){
		LocationHistory ret = new LocationHistory();
		Stack<GraphNode> buf = map.getShortestPath(location, destination, staircase, elevator, outside);
		for(GraphNode n : buf){
			Location x = new Location(n.getName());
			x.setLatitude(n.getLat());
			x.setLongitude(n.getLon());
			ret.add(x);
		}
		return ret;
	}

	public void setDestination(String room) {
		
	}

	public void setLocation(String room) {

	}

	public void setDestination(Location l) {
		// TODO:

	}

	public void setLocation(Location l) {
		// TODO:

	}

	public String[] getRoomList() {
		return map.getRoomList();
	}

	public void _a_start() {
		// TODO:
		sensorEventDistributor._a_startSensorUpdates();
		movementDetection._a_startMovementDetection();
	}

	public void _b1_pause() {
		sensorEventDistributor._b1_pauseSensorUpdates();
		movementDetection._b1_pauseMovementDetection();
	}

	public void _b2_unpause() {
		sensorEventDistributor._b2_unPauseSensorUpdates();
		movementDetection._b2_unPauseMovementDetection();
	}

	public int _c_stop() {
		sensorEventDistributor._c_stopSensorUpdates();
		movementDetection._c_stopMovementDetection();

		return 0;
	}
}
