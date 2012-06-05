package de.uvwxy.footpath2.gui.old;

import java.util.LinkedList;
import java.util.Stack;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ZoomControls;
import de.uvwxy.footpath.R;
import de.uvwxy.footpath2.gui.Loader;
import de.uvwxy.footpath2.map.GraphEdge;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.map.Map;
import de.uvwxy.footpath2.matching.NPConfig;
import de.uvwxy.footpath2.matching.Positioner;
import de.uvwxy.footpath2.matching.Positioner_OnlineBestFit;
import de.uvwxy.footpath2.matching.Positioner_OnlineFirstFit;
import de.uvwxy.footpath2.movement.steps.StepDetection;
import de.uvwxy.footpath2.tools.DataLogger;

/**
 * 
 * @author Paul Smith
 * 
 */
public abstract class Navigator extends Activity {
	public static final String LOG_DIR = "routelog/";

	// #########################################################################
	// ######################### Fields 'n Listener ############################
	// #########################################################################

	// GUI Elements
	PaintBoxMap pbMap; // Objects to handle the graphics
	Button btnRecalc;
	Button btnSwitchFit;
	Map g; // Reference to graph
	SurfaceView sv01;
	SurfaceHolder sh01;

	// Route information
	String nodeFrom; // Node we will start from, i.e. "5052"
	int nodeFromId = 0; // This is used if we choose nearest location from GPS fix
	String nodeTo; // Node we plan to end up with
	boolean staircase;
	boolean elevator;
	boolean outside;
	LinkedList<GraphEdge> navPathEdges; // Contains path with corrected compass bearings
										// used by PaintBoxMap to paint the path

	LinkedList<GraphEdge> tempEdges; // Stores the original edges on path
										// Needs to be global: is used for logging in onResume()
	double navPathLen = 0.0; // Total length of path
	double naiveStairsWidth = 0.25; // Naive amount in meters to use as stairs step length

	// Navigation
	double acceptanceWidth = 42.0; // Amount of derivation allowed for compassValue to path
	StepDetection stepDetection;
	Positioner posBestFit = null; // Object to do another progress estimation
	Positioner posFirstFit = null;
	NPConfig confBestFit = null;
	NPConfig confFirstFit = null;
	NPConfig conf = null;

	// Progress information
	boolean isNavigating = false; // Set to false when nodeTo is reached
	int totalStepsWalked = 0; // Total number of detected steps

	// Runtime information
	double compassValue = -1.0;

	LinkedList<Double> zVarHistory = new LinkedList<Double>(); // store the variance of each step
	int historySize = 64; // Back log of last 64 values to calculate variance
	double[] x_History = new double[historySize];
	double[] y_History = new double[historySize];
	double[] z_History = new double[historySize];
	int historyPtr = 0;

	// Logging
	DataLogger logger;
	boolean log = false;
	// NavigatorFootPath sets this to true so the right xmlgui is loaded
	boolean footPath = false;

	// Listeners
	OnClickListener onClick = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			if (arg0.equals(btnRecalc)) {
				if (true) {
					((Positioner_OnlineFirstFit) posFirstFit).recalcPos();
				}

			} else if (arg0.equals(btnSwitchFit)) {
				if (btnSwitchFit.getText().equals("Switch to First Fit Algorithm")) {
					btnSwitchFit.setText("Switch to Best Fit Algorithm");
					conf = confFirstFit;
				} else {
					btnSwitchFit.setText("Switch to First Fit Algorithm");
					conf = confBestFit;
				}
			}
		}

	};

	// #########################################################################
	// ######################### Getters 'n Setters ############################
	// #########################################################################

	public LinkedList<GraphEdge> getNavPathEdges() {
		return navPathEdges;
	}

	public double getAcceptanceWidth() {
		return this.acceptanceWidth;
	}

	// last compass value
	public double getCompassValue() {
		return compassValue;
	}

	public GraphEdge getCurrentEdge(NPConfig conf) {
		if (conf.getNpPointer() >= navPathEdges.size()) {
			return navPathEdges.get(navPathEdges.size() - 1);
		}
		GraphEdge ret = navPathEdges.get(conf.getNpPointer());
		return ret;
	}

	public float getCurrentFloorLevel() {
		return getLastSeenNode(conf).getLevel();
	}

	public double getEstimatedStepLength() {
		double pathLength = getNavPathWalked();
		double totalSteps = getTotalStepsWalked();
		for (int i = 0; i < conf.getNpPointer(); i++) {
			GraphEdge edge = navPathEdges.get(i);
			if (edge.isStairs()) {
				if (edge.getSteps() == -1) {
					pathLength -= edge.getLen();
					totalSteps -= edge.getLen() / naiveStairsWidth;
				} else if (edge.getSteps() > 0) {
					pathLength -= edge.getLen();
					totalSteps -= edge.getSteps();
				}
			}
		}

		return pathLength / totalSteps;

	}

	public double getNaiveStairsWidth() {
		return naiveStairsWidth;
	}

	public IndoorLocation getLastSeenNode(NPConfig conf) {
		GraphEdge currentEdge = getCurrentEdge(conf);
		GraphEdge previousEdge = getPreviousEdge(conf);
		if (previousEdge == null) { // no previous edge, thus this is the first edge
			return getRouteBegin();
		}
		// last seen node is node which is in current and previous edge
		IndoorLocation ret = currentEdge.getNode0();
		if (!previousEdge.contains(ret)) {
			ret = currentEdge.getNode1();
		}
		return ret;
	}

	public double getNavPathLen() {
		return navPathLen;
	}

	public double getNavPathLenLeft() {
		return navPathLen - getNavPathWalked();
	}

	public double getNavPathDir() {
		if (conf.getNpPointer() >= navPathEdges.size()) {
			return navPathEdges.get(navPathEdges.size() - 1).getCompDir();
		}
		return navPathEdges.get(conf.getNpPointer()).getCompDir();
	}

	// returns remaining meters on edge
	public double getNavPathEdgeLenLeft() {
		// catch route end, return -1.0
		if (conf.getNpPointer() >= navPathEdges.size())
			return -1.0;
		return navPathEdges.get(conf.getNpPointer()).getLen() - conf.getNpCurLen();
	}

	// return show far we have walked on the path
	public double getNavPathWalked() {
		double len = 0.0;
		// sum all traversed edges
		for (int i = 0; i < conf.getNpPointer(); i++) {
			len += navPathEdges.get(i).getLen();
		}
		// and how far we have walked on current edge
		len += conf.getNpCurLen();
		return len;
	}

	public IndoorLocation getPosition() {
		return getPosition(conf);
	}

	// estimated(?) position of user
	public IndoorLocation getPosition(NPConfig conf) {
		IndoorLocation ret = new IndoorLocation("", "");
		IndoorLocation lastSeenNode = getLastSeenNode(conf);
		GraphEdge currentEdge = getCurrentEdge(conf);
		IndoorLocation nextNode = currentEdge.getNode0().equals(lastSeenNode) ? currentEdge.getNode1() : currentEdge
				.getNode0();

		// catch route end, return destination
		if (conf.getNpPointer() >= navPathEdges.size()) {
			IndoorLocation lastNode = this.getRouteEnd();
			ret.setLatitude(lastNode.getLatitude());
			ret.setLongitude(lastNode.getLongitude());
			ret.setLevel(lastNode.getLevel());
			return ret;
		}

		ret.setLevel(lastSeenNode.getLevel());
		ret.setLatitude(lastSeenNode.getLatitude());
		ret.setLongitude(lastSeenNode.getLongitude());

		// move pos into direction; amount of traveled m on edge
		ret.moveIntoDirection(nextNode, conf.getNpCurLen() / navPathEdges.get(conf.getNpPointer()).getLen());
		return ret;
	}

	public GraphEdge getPreviousEdge(NPConfig conf) {
		if (conf.getNpPointer() == 0) {
			// no previous edge
			return null;
		}
		return navPathEdges.get(conf.getNpPointer() - 1);
	}

	public IndoorLocation getRouteBegin() {
		if (nodeFromId == 0) {
			return g.getNodeFromName(nodeFrom);
		} else {
			return g.getNode(nodeFromId);
		}
	}

	public IndoorLocation getRouteEnd() {
		return g.getNodeFromName(nodeTo);
	}

	// length of each step
	public double getStepLengthInMeters() {
		return conf.getNpStepSize();
	}

	// total steps walked
	public int getTotalStepsWalked() {
		return totalStepsWalked;
	}

	// total steps not roughly in correct direction
	public int getUnmatchedSteps() {
		return conf.getNpUnmatchedSteps();
	}

	// are we navigating?
	public boolean isNavigating() {
		return isNavigating;
	}

	/**
	 * Returns the variance from the back log for x values
	 * 
	 * @return the variance for x
	 */
	public double getVarianceOfX() {
		return varianceOfSet(x_History);
	}

	/**
	 * Returns the variance from the back log for y values
	 * 
	 * @return the variance for y
	 */
	public double getVarianceOfY() {
		return varianceOfSet(y_History);
	}

	/**
	 * Returns the variance from the back log for z values
	 * 
	 * @return the variance for z
	 */
	public double getVarianceOfZ() {
		return varianceOfSet(z_History);
	}

	public void setNavigating(boolean b) {
		this.isNavigating = b;
	}

	// #########################################################################
	// ######################## Activity Life Cycle ############################
	// #########################################################################

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		if (footPath) {
			setContentView(R.layout.displayroutefootpath);
		} else {
			setContentView(R.layout.displayrouteflowpath);

			sv01 = (SurfaceView) findViewById(R.id.sv01);

			// setup sv01 for use as preview
			// Note: this has to be done here, otherwise some sort of
			// "security exception"
			sh01 = sv01.getHolder();
			sh01.setSizeFromLayout();
			sh01.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		}

		Stack<IndoorLocation> navPathStack;

		// Get location and destination

		nodeFrom = this.getIntent().getStringExtra("from");
		nodeFromId = this.getIntent().getIntExtra("fromId", 0);
		nodeTo = this.getIntent().getStringExtra("to");

		// Get reference to graph object
		this.g = Loader.getGraph();
		staircase = this.getIntent().getBooleanExtra("stairs", true);
		elevator = this.getIntent().getBooleanExtra("elevator", false);
		outside = this.getIntent().getBooleanExtra("outside", true);
		log = this.getIntent().getBooleanExtra("log", false);

		// calculate route
		if (nodeFromId == 0) {
			navPathStack = this.g.getShortestPath(nodeFrom, nodeTo, staircase, elevator, outside);
			logger = new DataLogger(this, System.currentTimeMillis(), nodeFrom, nodeTo);
		} else {
			navPathStack = this.g.getShortestPath(nodeFromId, nodeTo, staircase, elevator, outside);
			logger = new DataLogger(this, System.currentTimeMillis(), "" + nodeFromId, nodeTo);
		}

		if (navPathStack != null) { // no route found!
			// The navPathStack consists of the correct order of nodes on the path
			// From these nodes the corresponding edge is used to get the original
			// data connected to it. What has to be recalculated is the initial bearing
			// because this is depending on the order of nodes being passed.

			// List to store the new edges in
			tempEdges = new LinkedList<GraphEdge>();
			// Get first node. This is always the 'left' node, when considering
			// a path going from left to right.
			IndoorLocation node0 = navPathStack.pop();

			while (!navPathStack.isEmpty()) {
				// Get 'right' node
				IndoorLocation node1 = navPathStack.pop();
				// Get Edge connecting 'left' and 'right' nodes
				GraphEdge origEdge = g.getEdge(node0, node1);

				// Get data which remains unchanged
				double len = origEdge.getLen();
				short wheelchair = origEdge.getWheelchair();
				float level = origEdge.getLevel();
				boolean indoor = origEdge.isIndoor();

				// Direction has to be recalculated
				double dir = node0.bearingTo(node1);

				// Create new edge
				GraphEdge tempEdge = new GraphEdge(node0, node1, len, dir, wheelchair, level, indoor);
				// Update additional values
				tempEdge.setElevator(origEdge.isElevator());
				tempEdge.setStairs(origEdge.isStairs());
				tempEdge.setSteps(origEdge.getSteps());
				tempEdges.add(tempEdge);

				// Update path length
				navPathLen += origEdge.getLen();

				// 'right' node is new 'left' node
				node0 = node1;
			}

			// Set current path
			navPathEdges = tempEdges;

			// Get handles to button and zoom controls and save their configuration
			ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoomCtrl);
			btnRecalc = (Button) findViewById(R.id.btnRecalc);
			btnSwitchFit = (Button) findViewById(R.id.btnSwitchFit);

			// Load fancy graphics
			pbMap = new PaintBoxMap(this, this);
			// REPLACING :: has to be done in order of appearance on display (top to bottom)
			replaceSurfaceView(pbMap, (SurfaceView) findViewById(R.id.svPath)); // svPath with pbNavigator

			btnRecalc.setOnClickListener(onClick);
			btnSwitchFit.setOnClickListener(onClick);
			zoomControls.setOnZoomInClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pbMap.zoomIn();
				}
			});

			zoomControls.setOnZoomOutClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pbMap.zoomOut();
				}
			});

			confBestFit = new NPConfig();
			confBestFit.setNpCurLen(0.0);
			confBestFit.setNpLastMatchedStep(-1);
			confBestFit.setNpMatchedSteps(0);
			confBestFit.setNpPointer(0);
			// /100.0f -> cm to m
			confBestFit.setNpStepSize(this.getIntent().getFloatExtra("stepLength", 191.0f / 0.415f / 100.0f) / 100.0f);
			confBestFit.setNpUnmatchedSteps(0);

			confFirstFit = new NPConfig(confBestFit);

			// Create correct pointer to chosen positioner
			conf = confBestFit;

			posBestFit = new Positioner_OnlineBestFit(this, this.navPathEdges, confBestFit);
			posFirstFit = new Positioner_OnlineFirstFit(this, this.navPathEdges, confFirstFit);
		} else { // navPathStack was null
			this.setResult(RESULT_CANCELED);
			this.finish();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// stepDetection.unload();
		if (log) {
			// Log to info file
			// logger.logInfo("a: " + stepDetection.getA());
			logger.logInfo("peak: " + stepDetection.getPeak());
			// logger.logInfo("step timeout (ms): " + stepDetection.getStep_timeout_ms());
			logger.logInfo("Recognised steps: " + this.totalStepsWalked);
			logger.logInfo("Estimated stepsize: " + this.getEstimatedStepLength());
			logger.logInfo("Output of columns:");
			logger.stopLogging();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (log) {
			logger.startLogging();
			// Only log route if files opened correctly
			if (logger.started()) {

				if (log) {
					for (GraphEdge e : navPathEdges) {
						logger.logSimpleRoute(e.getNode0().getLatitude(), e.getNode0().getLongitude());
					}
					GraphEdge e = navPathEdges.get(navPathEdges.size() - 1);
					logger.logSimpleRoute(e.getNode1().getLatitude(), e.getNode1().getLongitude());
				}
				if (log) {
					for (GraphEdge e : tempEdges) {
						logger.logRoute(e.getNode0().getLatitude(), e.getNode0().getLongitude());
					}
					GraphEdge e = tempEdges.get(tempEdges.size() - 1);
					logger.logRoute(e.getNode1().getLatitude(), e.getNode1().getLongitude());
				}

			}

		}

		// stepDetection.load();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	// #########################################################################
	// ############################## Functions ################################
	// #########################################################################

	void replaceSurfaceView(SurfaceView svNew, SurfaceView svOld) {
		LayoutParams layParam = svOld.getLayoutParams();
		LinearLayout ll = (LinearLayout) findViewById(R.id.ll01);
		ll.removeView(svOld);
		ll.addView(svNew, layParam);
	}

	/**
	 * Calculates the mean of a given set
	 * 
	 * @param set
	 *            the set
	 * @return the mean value
	 */
	double meanOfSet(double[] set) {
		double res = 0.0;
		for (double i : set) {
			res += i;
		}
		return res / set.length;

	}

	/**
	 * Calculates the variance of a given set
	 * 
	 * @param set
	 *            the set
	 * @return the variance value
	 */
	double varianceOfSet(double[] set) {
		double res = 0.0;
		double mean = meanOfSet(set);
		for (double i : set) {
			res += (i - mean) * (i - mean);
		}
		return res / set.length;
	}

	// -1 := left
	// 0 := straight on
	// 1 := right
	public int getNextTurn() {
		if (conf.getNpPointer() == navPathEdges.size() - 1) {
			// Walking on the last edge, go straight on
			return 0;
		}

		if (Positioner.isInRange(navPathEdges.get(conf.getNpPointer()).getCompDir(),
				navPathEdges.get(conf.getNpPointer() + 1).getCompDir(), 10)) {
			// +- 10 degrees is straight on
			return 0;
		}
		if (Positioner.isInRange(navPathEdges.get(conf.getNpPointer()).getCompDir() - 90,
				navPathEdges.get(conf.getNpPointer() + 1).getCompDir(), 90)) {
			// This edge -90 degrees is in range of next edge
			// -> next turn is left turn
			return -1;
		}
		// Else its a right turn
		return 1;
	}

}
