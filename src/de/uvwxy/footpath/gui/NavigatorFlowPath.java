package de.uvwxy.footpath.gui;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Stack;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ZoomControls;
import de.uvwxy.flowpath.FlowPathInterface;
import de.uvwxy.flowpath.MVDTrigger;
import de.uvwxy.flowpath.PaintBoxMVs;
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
public class NavigatorFlowPath extends Navigator implements StepTrigger, MVDTrigger{

	private FlowPathInterface flowPathInterface = FlowPathInterface
			.getInterface();

	public static SurfaceView sv01;
	public static SurfaceHolder sh01;
	
	// #########################################################################
	// ############################## Functions ################################
	// #########################################################################

	// #########################################################################
	// ######################## Activity Life Cycle ############################
	// #########################################################################

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// route calculation and so on..
		super.onCreate(savedInstanceState);

		double a = getSharedPreferences(Calibrator.CALIB_DATA,0).getFloat("a", 0.5f);
		double peak = getSharedPreferences(Calibrator.CALIB_DATA,0).getFloat("peak", 0.5f);
		int step_timeout_ms = getSharedPreferences(Calibrator.CALIB_DATA,0).getInt("timeout", 666);
		
		// Use StepDetection to obtain compass data. Ignore Step triggers
		// TODO: FiX DiS: Split Step And Compass Trigger
		// BUT: Is still needed for logging, see below.
		// Has to be initialized, because it is loaded in onResume in Parent
		// class
		stepDetection = new StepDetection(this, this, a, peak, step_timeout_ms);
		
		sv01 = (SurfaceView) findViewById(R.id.sv01);
		
		// setup sv01 for use as preview
		// Note: this has to be done here, otherwise some sort of
		// "security exception"
		sh01 = sv01.getHolder();
		sh01.setSizeFromLayout();
		sh01.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		flowPathInterface.addMVDTrigger(this);
		setNavigating(true);
	}
	
	/**
	 * Reset the GUI on resume. Capture has been stopped before.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		boolean fpOk = flowPathInterface.startFlowpath();
	}
	
	/**
	 * GUI is no longer visible, stop everything.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		flowPathInterface.stopFlowPath();
	}
	

	/**
	 * Bye Bye. Stop logging!
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		flowPathInterface.stopFlowPath();
	}
	
	// #########################################################################
	// ########################## Step/Data Callbacks ##########################
	// #########################################################################
	
	
	@Override
	public void trigger(long now_ms, double compDir) {
		if (!isNavigating) {
			// Destination was reached
			return;
		}
		
		if(log){
			logger.logStep(now_ms, compDir);
		}
		
		// Do not use steps for navigation!
		// posBestFit.addStep(compDir);
		// posFirstFit.addStep(compDir);
		
		// Log.i("FOOTPATH", "posBestFit: " + posBestFit.getProgress());
		// Log.i("FOOTPATH", "posFirstFit: " + posFirstFit.getProgress());
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


	@Override
	public void processMVData(long now_ms, float[][][] mvds) {
		// TODO Auto-generated method stub
		totalStepsWalked++;
	}
	
	
}
