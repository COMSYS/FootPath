package de.uvwxy.footpath2.gui;

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
import de.uvwxy.footpath2.map.Map;
import de.uvwxy.footpath2.map.GraphEdge;
import de.uvwxy.footpath2.map.GraphNode;
import de.uvwxy.footpath2.map.LatLonPos;
import de.uvwxy.footpath2.matching.NPConfig;
import de.uvwxy.footpath2.matching.Positioner;
import de.uvwxy.footpath2.matching.Positioner_OnlineBestFit;
import de.uvwxy.footpath2.matching.Positioner_OnlineFirstFit;
import de.uvwxy.footpath2.movement.StepTrigger;
import de.uvwxy.footpath2.movement.steps.StepDetection;
import de.uvwxy.footpath2.tools.AudioWriter;
import de.uvwxy.footpath2.tools.DataLogger;
import de.uvwxy.footpath2.tools.ToolBox;
/**
 * 
 * @author Paul Smith
 *
 */
public class NavigatorFootPath extends Navigator implements StepTrigger {
	
	

	// #########################################################################
	// ######################## Activity Life Cycle ############################
	// #########################################################################

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		footPath = true;
		// route calculation and so on..
		super.onCreate(savedInstanceState);
		
		double a = getSharedPreferences(Calibrator.CALIB_DATA,0).getFloat("a", 0.5f);
		double peak = getSharedPreferences(Calibrator.CALIB_DATA,0).getFloat("peak", 0.5f);
		int step_timeout_ms = getSharedPreferences(Calibrator.CALIB_DATA,0).getInt("timeout", 666);
		
		stepDetection = new StepDetection(this, this, a, peak, step_timeout_ms);

		
		
		setNavigating( true );
	}
	
	
	
	// #########################################################################
	// ########################## Step/Data Callbacks ##########################
	// #########################################################################
	
	@Override
	public void trigger(long now_ms, double compDir) {
		totalStepsWalked++;
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
	// ############################## Functions ################################
	// #########################################################################

	
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
}
