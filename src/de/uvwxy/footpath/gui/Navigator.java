package de.uvwxy.footpath.gui;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Stack;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
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
import de.uvwxy.footpath.ToolBox;
import de.uvwxy.footpath.core.NPConfig;
import de.uvwxy.footpath.core.Positioner;
import de.uvwxy.footpath.core.Positioner_OnlineBestFit;
import de.uvwxy.footpath.core.Positioner_OnlineFirstFit;
import de.uvwxy.footpath.core.StepDetection;
import de.uvwxy.footpath.core.StepTrigger;
import de.uvwxy.footpath.graph.Graph;
import de.uvwxy.footpath.graph.GraphEdge;
import de.uvwxy.footpath.graph.GraphNode;
import de.uvwxy.footpath.graph.LatLonPos;
import de.uvwxy.footpath.log.AudioWriter;
import de.uvwxy.footpath.log.DataLogger;
/**
 * 
 * @author Paul Smith
 *
 */
public class Navigator extends Activity implements StepTrigger {
	public static final String LOG_DIR = "routelog/";
	
	// #########################################################################
	// ######################### Fields 'n Listener ############################
	// #########################################################################
	
	// GUI Elements
	private PaintBoxMap pbMap;							// Objects to handle the graphics
	private Button btnRecalc;
	private Button btnSwitchFit;
	private Graph g;									// Reference to graph
	
	
	// Route information
	private String nodeFrom;							// Node we will start from, i.e. "5052"
	private int nodeFromId = 0;							// This is used if we choose nearest location from GPS fix
	private String nodeTo;								// Node we plan to end up with
	private boolean staircase;
	private boolean elevator;
	private boolean outside;
	private LinkedList<GraphEdge> navPathEdges;			// Contains path with corrected compass bearings
														// used by PaintBoxMap to paint the path
	
	private LinkedList<GraphEdge> tempEdges;			// Stores the original edges on path
														// Needs to be global: is used for logging in onResume()
	private double navPathLen = 0.0;					// Total length of path
	private double naiveStairsWidth = 0.25;				// Naive amount in meters to use as stairs step length
	
	
	// Navigation
	private double acceptanceWidth = 42.0;				// Amount of derivation allowed for compassValue to path
	private StepDetection stepDetection;
	private Positioner posBestFit = null; 				// Object to do another progress estimation
	private Positioner posFirstFit = null;
	private NPConfig confBestFit = null;
	private NPConfig confFirstFit = null;
	private NPConfig conf = null;
	
	// Progress information
	private boolean isNavigating = false;				// Set to false when nodeTo is reached
	private int totalStepsWalked = 0;					// Total number of detected steps
	
	
	// Runtime information
	private double compassValue = -1.0;
	
	private LinkedList<Double> zVarHistory = new LinkedList<Double>();  // store the variance of each step
	private int historySize = 64;						// Back log of last 64 values to calculate variance
	private double[] x_History = new double[historySize];
	private double[] y_History = new double[historySize];
	private double[] z_History = new double[historySize];
	private int historyPtr = 0;
		
	
	// Logging
	private DataLogger logger;
	private boolean log = false;
	private boolean logAudio = false;
	private AudioWriter avwCapture;

	
	// Listeners
	OnClickListener onClick = new OnClickListener(){

		@Override
		public void onClick(View arg0) {
			if(arg0.equals(btnRecalc)){
				if(true){
					((Positioner_OnlineFirstFit) posFirstFit).recalcPos();
				}
				
			} else if (arg0.equals(btnSwitchFit)){
				if ( btnSwitchFit.getText().equals("Switch to First Fit Algorithm")){
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

	public LinkedList<GraphEdge> getNavPathEdges(){
		return navPathEdges;
	}
	public double getAcceptanceWidth() {
		return this.acceptanceWidth;
	}
	
	// last compass value
	public double getCompassValue() {
		return compassValue;
	}

	public GraphEdge getCurrentEdge(NPConfig conf){
		if(conf.npPointer >= navPathEdges.size()){
			return navPathEdges.get(navPathEdges.size()-1);
		}
		GraphEdge ret = navPathEdges.get(conf.npPointer);
		return ret;
	}

	public float getCurrentFloorLevel() {
		return getLastSeenNode(conf).getLevel();
	}
	
	public double getEstimatedStepLength(){
		double pathLength = getNavPathWalked();
		double totalSteps = getTotalStepsWalked();
		for(int i = 0; i < conf.npPointer; i++){
			GraphEdge edge = navPathEdges.get(i);
			if(edge.isStairs()){
				if(edge.getSteps()==-1){
					pathLength -= edge.getLen();
					totalSteps -= edge.getLen()/naiveStairsWidth; 
				} else if(edge.getSteps() > 0){
					pathLength -= edge.getLen();
					totalSteps -= edge.getSteps();
				}
			}
		}
		
		return pathLength/totalSteps;
		
	}
	
	public double getNaiveStairsWidth(){
		return naiveStairsWidth;
	}
	
	public GraphNode getLastSeenNode(NPConfig conf){
		GraphEdge currentEdge = getCurrentEdge(conf);
		GraphEdge previousEdge = getPreviousEdge(conf);
		if(previousEdge == null){ // no previous edge, thus this is the first edge
			return getRouteBegin();
		}
		// last seen node is node which is in current and previous edge
		GraphNode ret = currentEdge.getNode0();
		if(!previousEdge.contains(ret)){
			ret = currentEdge.getNode1();
		}
		return ret;
	}

	public double getNavPathLen() {
		return navPathLen;
	}
	
	public double getNavPathLenLeft(){
		return navPathLen - getNavPathWalked();
	}

	public double getNavPathDir() {
		if(conf.npPointer >= navPathEdges.size()){
			return navPathEdges.get(navPathEdges.size()-1).getCompDir();
		}
		return navPathEdges.get(conf.npPointer).getCompDir();
	}

	// returns remaining meters on edge
	public double getNavPathEdgeLenLeft() {
		// catch route end, return -1.0
		if (conf.npPointer >= navPathEdges.size())
			return -1.0;
		return navPathEdges.get(conf.npPointer).getLen() - conf.npCurLen;
	}

	
	// return show far we have walked on the path
	public double getNavPathWalked(){
		double len = 0.0;
		// sum all traversed edges
		for(int i = 0; i < conf.npPointer; i++){
			len += navPathEdges.get(i).getLen();
		}
		// and how far we have walked on current edge
		len += conf.npCurLen;
		return len;
	}
	
	public LatLonPos getPosition() {
		return getPosition(conf);
	}

	// estimated(?) position of user
	public LatLonPos getPosition(NPConfig conf) {
		LatLonPos ret = new LatLonPos();
		GraphNode lastSeenNode = getLastSeenNode(conf);
		GraphEdge currentEdge = getCurrentEdge(conf);
		GraphNode nextNode = currentEdge.getNode0().equals(lastSeenNode)? currentEdge.getNode1() : currentEdge.getNode0();
		
		// catch route end, return destination
		if (conf.npPointer >= navPathEdges.size()) {
			GraphNode lastNode = this.getRouteEnd();
			ret.setLat(lastNode.getLat());
			ret.setLon(lastNode.getLon());
			ret.setLevel(lastNode.getLevel());
			return ret;
		}
		
		ret.setLevel(lastSeenNode.getLevel());
		ret.setLat(lastSeenNode.getLat());
		ret.setLon(lastSeenNode.getLon());
		
		// move pos into direction; amount of traveled m on edge
		ret.moveIntoDirection(nextNode.getPos(), conf.npCurLen/navPathEdges.get(conf.npPointer).getLen());
		return ret;
	}

	public GraphEdge getPreviousEdge(NPConfig conf){
		if(conf.npPointer == 0){
			// no previous edge
			return null;
		}
		return navPathEdges.get(conf.npPointer - 1);
	}

	public GraphNode getRouteBegin() {
		if(nodeFromId == 0){
			return g.getNodeFromName(nodeFrom);
		} else {
			return g.getNode(nodeFromId);
		}
	}

	public GraphNode getRouteEnd() {
		return g.getNodeFromName(nodeTo);
	}

	// length of each step
	public double getStepLengthInMeters() {
		return conf.npStepSize;
	}

	// total steps walked
	public int getTotalStepsWalked() {
		return totalStepsWalked;
	}

	// total steps not roughly in correct direction
	public int getUnmatchedSteps() {
		return conf.npUnmatchedSteps;
	}
	
	// are we navigating?
	public boolean isNavigating() {
		return isNavigating;
	}
	
	/**
	 * Returns the variance from the back log for x values
	 * @return the variance for x
	 */
	public double getVarianceOfX(){
		return varianceOfSet(x_History);
	}
	/**
	 * Returns the variance from the back log for y values
	 * @return the variance for y
	 */
	public double getVarianceOfY(){
		return varianceOfSet(y_History);
	}
	/**
	 * Returns the variance from the back log for z values
	 * @return the variance for z
	 */
	public double getVarianceOfZ(){
		return varianceOfSet(z_History);
	}
	
	public void setNavigating(boolean b){
		this.isNavigating = b;
	}

	// #########################################################################
	// ########################## Step/Data Callbacks ##########################
	// #########################################################################
	
	@Override
	public void trigger(long now_ms, double compDir) {
		this.totalStepsWalked++;
		if (!isNavigating) {
			// Destination was reached
			return;
		}
		
		if(log){
			logger.logStep(now_ms, compDir);
		}
		
		posBestFit.addStep(compDir);
		posFirstFit.addStep(compDir);
		
		Log.i("FOOTPATH", "posBestFit: " + posBestFit.getProgress());
		Log.i("FOOTPATH", "posFirstFit: " + posFirstFit.getProgress());
		if(log){
			// Write location to file after detected step
			LatLonPos bestPos = getPosition(confBestFit);
			LatLonPos firstPos = getPosition(confFirstFit);
			logger.logPosition(now_ms, bestPos.getLat(), bestPos.getLon(), posBestFit.getProgress()/this.navPathLen
					, firstPos.getLat(), firstPos.getLon(), posFirstFit.getProgress()/this.navPathLen);
		}
	}
	
	@Override
	public void dataHookAcc(long now_ms, double x, double y, double z){
		// add values to history (for variance)
		addTriple(x, y, z);
		if(log){
			logger.logRawAcc(now_ms, x, y, z);
		}
	}
	
	@Override
	public void dataHookComp(long now_ms, double x, double y, double z){
		if(log){
			logger.logRawCompass(now_ms, x, y, z);
		}
		compassValue = ToolBox.lowpassFilter(compassValue,  x, 0.5);
	}
	
	@Override
	public void timedDataHook(long now_ms, double[] acc, double[] comp){
		double varZ = getVarianceOfZ();
		zVarHistory.add(new Double(acc[2]));
		
		if(log){
			logger.logTimedVariance(now_ms, varZ);
		}
		if(log){
			// Write Compass and Accelerometer data
			logger.logTimedAcc(now_ms, acc[2]);
			logger.logTimedCompass(now_ms, comp[0]);
		}
	}


	// #########################################################################
	// ######################## Activity Life Cycle ############################
	// #########################################################################


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
                WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		setContentView(R.layout.displayroute);
		
		Stack<GraphNode> navPathStack;
		
		// Get location and destination
		
		nodeFrom 	= 	this.getIntent().getStringExtra("from");
		nodeFromId 	= 	this.getIntent().getIntExtra("fromId",0);
		nodeTo 		= 	this.getIntent().getStringExtra("to");

		// Get reference to graph object
		this.g = Loader.getGraph();
		staircase 	= 		this.getIntent().getBooleanExtra("stairs", true);
		elevator 	= 		this.getIntent().getBooleanExtra("elevator", false);
		outside 	= 		this.getIntent().getBooleanExtra("outside", true);
		log 		= 		this.getIntent().getBooleanExtra("log", false);
		logAudio	= 		this.getIntent().getBooleanExtra("audio", false);
		
		// calculate route
		if(nodeFromId==0){
			navPathStack = this.g.getShortestPath(nodeFrom, nodeTo, staircase, elevator, outside);
			logger = new DataLogger(this, System.currentTimeMillis(), nodeFrom, nodeTo);
		} else {
			navPathStack = this.g.getShortestPath(nodeFromId, nodeTo, staircase, elevator, outside);
			logger = new DataLogger(this, System.currentTimeMillis(), "" + nodeFromId, nodeTo);
		}
		
		if(navPathStack != null){											// no route found!
			// The navPathStack consists of the correct order of nodes on the path
			// From these nodes the corresponding edge is used to get the original
			// data connected to it. What has to be recalculated is the initial bearing
			// because this is depending on the order of nodes being passed.
			
			// List to store the new edges in
			tempEdges = new LinkedList<GraphEdge>();
			// Get first node. This is always the 'left' node, when considering
			// a path going from left to right.
			GraphNode node0 = navPathStack.pop();
			
			while(!navPathStack.isEmpty()){
				// Get 'right' node
				GraphNode node1 = navPathStack.pop();
				// Get Edge connecting 'left' and 'right' nodes
				GraphEdge origEdge = g.getEdge(node0,node1);
				
				// Get data which remains unchanged
				double len = origEdge.getLen();					
				short wheelchair = origEdge.getWheelchair();
				float level = origEdge.getLevel();
				boolean indoor = origEdge.isIndoor();
				
				// Direction has to be recalculated
				double dir = g.getInitialBearing(node0.getLat(), node0.getLon(), node1.getLat(), node1.getLon());
				
				// Create new edge
				GraphEdge tempEdge = new GraphEdge(node0, node1,len,dir,wheelchair,level,indoor);
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
			
			Log.i("FOOTPATH", "Number of edges before merge: " + tempEdges.size());
			
			
			// Set current path
			navPathEdges = tempEdges;
			Log.i("FOOTPATH", "EDGES: " + navPathEdges);
			Log.i("FOOTPATH", "Number of edges after merge: " + navPathEdges.size());	

			// Get handles to button and zoom controls and save their configuration
			ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoomCtrl);
			btnRecalc = (Button) findViewById(R.id.btnRecalc);
			btnSwitchFit = (Button) findViewById(R.id.btnSwitchFit);
			
			// Load fancy graphics
			pbMap = new PaintBoxMap(this, this);
			// REPLACING :: has to be done in order of appearance on display (top to bottom)
			replaceSurfaceView(pbMap, (SurfaceView) findViewById(R.id.svPath));			// svPath with pbNavigator

			btnRecalc.setOnClickListener(onClick);
			btnSwitchFit.setOnClickListener(onClick);
			zoomControls.setOnZoomInClickListener(new OnClickListener() {
				public void onClick(View v) {
					pbMap.zoomIn();
				}
			});
			
			zoomControls.setOnZoomOutClickListener(new OnClickListener() {
				public void onClick(View v) {
					pbMap.zoomOut();
				}
			});
			
			confBestFit = new NPConfig();
			confBestFit.npCurLen = 0.0;
			confBestFit.npLastMatchedStep = -1;
			confBestFit.npMatchedSteps = 0;
			confBestFit.npPointer = 0;
			// /100.0f -> cm to m
			confBestFit.npStepSize = this.getIntent().getFloatExtra("stepLength", 191.0f/0.415f/100.0f)/100.0f;
			confBestFit.npUnmatchedSteps = 0;
			
			confFirstFit = new NPConfig(confBestFit);
				
			// Create correct pointer to chosen positioner
			conf = confBestFit;
			
			double a = getSharedPreferences(Calibrator.CALIB_DATA,0).getFloat("a", 0.5f);
			double peak = getSharedPreferences(Calibrator.CALIB_DATA,0).getFloat("peak", 0.5f);
			int step_timeout_ms = getSharedPreferences(Calibrator.CALIB_DATA,0).getInt("timeout", 666);
			
			stepDetection = new StepDetection(this, this, a, peak, step_timeout_ms);
			
			posBestFit = new Positioner_OnlineBestFit(this, this.navPathEdges, confBestFit);
			posFirstFit = new Positioner_OnlineFirstFit(this, this.navPathEdges, confFirstFit);
		
			Log.i("FOOTPATH", "Starting Navigation!");
			
			setNavigating( true );
		} else { // navPathStack was null
			this.setResult(RESULT_CANCELED);
			this.finish();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		stepDetection.unload();
		if(log){
			// Log to info file
			logger.logInfo("a: " + stepDetection.getA());
			logger.logInfo("peak: " + stepDetection.getPeak());
			logger.logInfo("step timeout (ms): " + stepDetection.getStep_timeout_ms());
			logger.logInfo("Recognised steps: " + this.totalStepsWalked);
			logger.logInfo("Estimated stepsize: " + this.getEstimatedStepLength());
			logger.logInfo("Output of columns:");
			logger.stopLogging();
			if(logAudio){
				avwCapture.stopCapture();
				avwCapture.unregisterCapture();
			}
		}
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		if(log){
			logger.startLogging();
			// Only log route if files opened correctly
			if(logger.started()){
				
				if(log){
					for(GraphEdge e: navPathEdges){
						logger.logSimpleRoute(e.getNode0().getLat(), e.getNode0().getLon());
					}
					GraphEdge e = navPathEdges.get(navPathEdges.size()-1);
					logger.logSimpleRoute(e.getNode1().getLat(), e.getNode1().getLon());
				}
				if(log){
					for(GraphEdge e: tempEdges){
						logger.logRoute(e.getNode0().getLat(), e.getNode0().getLon());
					}
					GraphEdge e = tempEdges.get(tempEdges.size()-1);
					logger.logRoute(e.getNode1().getLat(), e.getNode1().getLon());
				}
				
				
				
				
				// Create files for AudioWrite here, with correct file name as other log files
				if(nodeFromId==0){
					if(logAudio){
						avwCapture = new AudioWriter("" + logger.getRouteId() + "_" + nodeFrom + "_" + nodeTo +"/", "video.3gp");
					}
				} else {
					if(logAudio){
						avwCapture = new AudioWriter("" + logger.getRouteId() + "_" + nodeFromId + "_" + nodeTo +"/", "video.3gp");
					}
				}
				
				if(logAudio){
					try {
						avwCapture.registerCapture();
						avwCapture.startCapture();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		
		}
		
		stepDetection.load();
		
	}

	

	// #########################################################################
	// ############################## Functions ################################
	// #########################################################################

	private void replaceSurfaceView(SurfaceView svNew, SurfaceView svOld) {
		LayoutParams layParam = svOld.getLayoutParams();
		LinearLayout ll = (LinearLayout) findViewById(R.id.ll01);
		ll.removeView(svOld);
		ll.addView(svNew, layParam);
	}

	
	/**
	 * Add values to backlog (for variance)
	 * @param x	Sensor x value
	 * @param y	Sensor y value
	 * @param z Sensor z value
	 */
	private void addTriple(double x, double y, double z) {
		x_History[(historyPtr + 1) % historySize] = x;
		y_History[(historyPtr + 1) % historySize] = y;
		z_History[(historyPtr + 1) % historySize] = z;
		historyPtr++;
	}
	/**
	 * Calculates the mean of a given set
	 * @param set the set
	 * @return	the mean value
	 */
	private double meanOfSet(double[] set) {
		double res = 0.0;
		for (double i : set) {
			res += i;
		}
		return res / set.length;

	}
	/**
	 * Calculates the variance of a given set
	 * @param set the set
	 * @return	the variance value
	 */
	private double varianceOfSet(double[] set) {
		double res = 0.0;
		double mean = meanOfSet(set);
		for (double i : set) {
			res += (i - mean) * (i - mean);
		}
		return res / set.length;
	}
	
	// -1 := left
	//  0 := straight on
	//  1 := right
	public int getNextTurn(){
		if(conf.npPointer == navPathEdges.size()-1){
			// Walking on the last edge, go straight on
			return 0;
		}
		
		if(Positioner.isInRange(navPathEdges.get(conf.npPointer).getCompDir(),navPathEdges.get(conf.npPointer+1).getCompDir(),10)){
			// +- 10 degrees is straight on
			return 0;
		}
		if(Positioner.isInRange(navPathEdges.get(conf.npPointer).getCompDir()-90,navPathEdges.get(conf.npPointer+1).getCompDir(),90)){
			// This edge -90 degrees is in range of next edge
			// -> next turn is left turn
			return -1;
		}
		// Else its a right turn
		return 1;
	}
	
}
