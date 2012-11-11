package de.uvwxy.footpath2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.util.Log;
import android.view.SurfaceHolder;
import de.uvwxy.footpath2.drawing.OSM2DBuilding;
import de.uvwxy.footpath2.log.ExportManager;
import de.uvwxy.footpath2.log.ExportManager.IntervalExportBehavior;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.map.IndoorLocationList;
import de.uvwxy.footpath2.map.Map;
import de.uvwxy.footpath2.matching.BestFit;
import de.uvwxy.footpath2.matching.FirstFit;
import de.uvwxy.footpath2.matching.MatchingAlgorithm;
import de.uvwxy.footpath2.matching.multifit.MultiFit;
import de.uvwxy.footpath2.movement.MovementDetection;
import de.uvwxy.footpath2.movement.SensorEventDistributor;
import de.uvwxy.footpath2.movement.h263.MVDMovementClassifier;
import de.uvwxy.footpath2.movement.steps.StepDetectionImpl;
import de.uvwxy.footpath2.tools.FootPathException;
import de.uvwxy.footpath2.types.FP_LocationProvider;
import de.uvwxy.footpath2.types.FP_MatchingAlgorithm;
import de.uvwxy.footpath2.types.FP_MovementDetection;

/**
 * This class will be the main interface to use footpath.
 * 
 * @author Paul Smith
 * 
 */
public class FootPath {
	public static final boolean INVERTED_SRC_COMPASS = false;

	private static final long EXPORT_INTERVALL = 1000 * 60;

	private static FootPath thisInstance = null;

	private final Context context;
	private final Map map;
	private final SensorEventDistributor sensorEventDistributor;
	private MovementDetection movementDetection;
	private MatchingAlgorithm matchingAlgorithm;
	private FP_MovementDetection settingsMovementDetection;
	private FP_MatchingAlgorithm settingsMatchingAlgorithm;
	private FP_LocationProvider settingsLocationProvider;
	private ExportManager exportManager;

	public static FootPath getInstance(Context context) {
		if (thisInstance == null) {
			thisInstance = new FootPath(context);
		}

		return thisInstance;
	}

	private FootPath(Context context) {
		this.context = context;
		sensorEventDistributor = SensorEventDistributor.getInstance(context);
		sensorEventDistributor.registerExportData();
		exportManager = ExportManager.getInstance();
		exportManager.setBehavior(IntervalExportBehavior.EXPORT_RECENTDATA);
		exportManager.setByteThreshold(0);
		map = new Map();
	}

	public String getRevision() {
		return "B4DR3VN0";
	}

	private void exampleUsage() throws FootPathException {

		// initialization
		FootPath fp = new FootPath(null);

		_a4_loadMapDataFromXMLFile("file.osm");

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
		InputStream is = null;
		try {
			is = context.getAssets().open("ipin2012demo.osm");
		} catch (IOException e) {
			e.printStackTrace();
		}
		_a6_loadMapDataFromInputStream(is);
	}

	public void _a2_loadMapDataFromXMLResource(int resID) {
		AssetFileDescriptor afd = context.getResources().openRawResourceFd(resID);
		FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
		_a5_loadMapDataFromFileInputStream(fis);
	}

	public void _a3_loadMapDataFromURL(String uri) {
		// TODO: download uri and then load map.
		// TODO: cache map?

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

	public void _a5_loadMapDataFromFileInputStream(FileInputStream fis) {
		try {
			map.addToGraphFromFileInputStream(fis);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void _a6_loadMapDataFromInputStream(InputStream fis) {
		try {
			map.addToGraphFromInputStream(fis);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
			sensorEventDistributor.addGravityListener((SensorEventListener) movementDetection);
			break;
		case MOVEMENT_DETECTION_SOUND_SEGWAY:
			break;
		case MOVEMENT_DETECTION_VIDEO_WHEELCHAIR:
			// TODO: setup FLOW PATH HERE
			// create classifier object to receive MVD data and then pass "detected" steps to onStepListener, which is
			// added done in _c_
			movementDetection = new MVDMovementClassifier();
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
			matchingAlgorithm = new FirstFit();
			Log.i("FOOTPATH", "Registering FirstFit for movement detection");
			movementDetection.registerOnStepListener(matchingAlgorithm);
			break;
		case MATCHING_MULTI_FIT:
			MultiFit mf = new MultiFit();
			mf.setMap(map);
			matchingAlgorithm = mf;
			Log.i("FOOTPATH", "Registering MultiFit for movement detection");
			movementDetection.registerOnStepListener(mf);
			break;
		default:
			throw new IllegalArgumentException(matchingType.toString());
		}
	}

	/**
	 * Set the step length to use during navigation
	 * 
	 * @param f
	 *            step length in meters (float)
	 * @throws FootPathException
	 */
	public void _d_setInitialStepLength(float f) throws FootPathException {
		if (matchingAlgorithm != null) {
			matchingAlgorithm.setInitialStepLength(f);
			movementDetection.setInitialStepLength(f);
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
		Stack<IndoorLocation> buf = map.getShortestPath(location, destination, staircase, elevator, outside);
		if (buf != null) {
			for (IndoorLocation n : buf) {
				ret.addFirst(n);
			}
		}
		return ret;
	}

	public void _fg_setPath(IndoorLocationList path) throws FootPathException {
		if (matchingAlgorithm != null) {
			if (path != null || path.size() == 0) {
				matchingAlgorithm.setPath(path);
			} else {
				throw new FootPathException("Path was null/empty!");
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
		// exportManager.startIntervalExporting(EXPORT_INTERVALL);
		sensorEventDistributor._a_startSensorUpdates();
		movementDetection._a_startMovementDetection();

	}

	public void _i1_pause() {
		if (exportManager != null) {
			// exportManager.stopIntervalExporting();
			// exportManager.export_recentData();
		}
		if (sensorEventDistributor != null)
			sensorEventDistributor._b1_pauseSensorUpdates();
		if (movementDetection != null)
			movementDetection._b1_pauseMovementDetection();
	}

	public void _i2_unpause() {
		// exportManager.startIntervalExporting(EXPORT_INTERVALL);
		sensorEventDistributor._b2_unPauseSensorUpdates();
		movementDetection._b2_unPauseMovementDetection();
	}

	public void _j_stop() {
		if (exportManager != null) {
			// exportManager.stopIntervalExporting();
			// exportManager.export_recentData();
		}
		if (sensorEventDistributor != null)
			sensorEventDistributor._c_stopSensorUpdates();
		if (movementDetection != null)
			movementDetection._c_stopMovementDetection();
	}

	public void passThroughSurfaceViewForFlowPath(SurfaceHolder svh) throws FootPathException {
		if (settingsMovementDetection != FP_MovementDetection.MOVEMENT_DETECTION_VIDEO_WHEELCHAIR) {
			throw new FootPathException("Setting SurfaceHolder allthough not using MOVEMENT_DETECTION_VIDEO_WHEELCHAIR");
		}
		if (!(movementDetection instanceof MVDMovementClassifier)) {
			throw new FootPathException("Setting SurfaceHolder allthough not using MVDMovementClassifier");
		}

		((MVDMovementClassifier) movementDetection).setSurfaceHolder(svh);
		Log.i("FOOTPATH", "Setting Surface Holder");
	}

	@Deprecated
	public MovementDetection _debug_getMovementDetection() {
		return movementDetection;
	}
	
	@Deprecated
	public MatchingAlgorithm _debug_getMatchinAlgorithm() {
		return matchingAlgorithm;
	}

	@Deprecated
	public OSM2DBuilding _debug_getOSM2DBuilding() {
		return map.getOsm2Dbuilding();
	}

	public FP_MovementDetection getSettingsMovementDetection() {
		return settingsMovementDetection;
	}

	public FP_MatchingAlgorithm getSettingsMatchingAlgorithm() {
		return settingsMatchingAlgorithm;
	}

	public FP_LocationProvider getSettingsLocationProvider() {
		return settingsLocationProvider;
	}
}
