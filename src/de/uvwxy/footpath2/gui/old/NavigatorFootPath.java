package de.uvwxy.footpath2.gui.old;

import android.os.Bundle;
import android.util.Log;
import de.uvwxy.footpath2.map.IndoorLocation;
/**
 * 
 * @author Paul Smith
 *
 */
public class NavigatorFootPath extends Navigator {
	
	

	// #########################################################################
	// ######################## Activity Life Cycle ############################
	// #########################################################################

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
//		footPath = true;
		// route calculation and so on..
		super.onCreate(savedInstanceState);
		
		double a = getSharedPreferences(Calibrator.CALIB_DATA,0).getFloat("a", 0.5f);
		double peak = getSharedPreferences(Calibrator.CALIB_DATA,0).getFloat("peak", 0.5f);
		int step_timeout_ms = getSharedPreferences(Calibrator.CALIB_DATA,0).getInt("timeout", 666);
		
//		stepDetection = new StepDetection(this, this, a, peak, step_timeout_ms);

		
		
//		setNavigating( true );
	}
	
	
	
	// #########################################################################
	// ########################## Step/Data Callbacks ##########################
	// #########################################################################
	
//	@Override
	public void trigger(long now_ms, double compDir) {
//		totalStepsWalked++;
//		if (!isNavigating) {
//			// Destination was reached
//			return;
//		}
//		
//		if(log){
//			logger.logStep(now_ms, compDir);
//		}
//		
//		posBestFit.addStep(compDir);
//		posFirstFit.addStep(compDir);
//		
//		Log.i("FOOTPATH", "posBestFit: " + posBestFit.getProgress());
//		Log.i("FOOTPATH", "posFirstFit: " + posFirstFit.getProgress());
//		if(log){
//			// Write location to file after detected step
//			IndoorLocation bestPos = getPosition(confBestFit);
//			IndoorLocation firstPos = getPosition(confFirstFit);
//			logger.logPosition(now_ms, bestPos.getLatitude(), bestPos.getLongitude(), posBestFit.getProgress()/this.navPathLen
//					, firstPos.getLatitude(), firstPos.getLongitude(), posFirstFit.getProgress()/this.navPathLen);
//		}
	}
	
//	@Override
//	public void dataHookAcc(long now_ms, double x, double y, double z){
//		// add values to history (for variance)
//		addTriple(x, y, z);
//		if(log){
//			logger.logRawAcc(now_ms, x, y, z);
//		}
//	}
//	
//	@Override
//	public void dataHookComp(long now_ms, double x, double y, double z){
//		if(log){
//			logger.logRawCompass(now_ms, x, y, z);
//		}
//		compassValue = ToolBox.lowpassFilter(compassValue,  x, 0.5);
//	}
//	
//	@Override
//	public void timedDataHook(long now_ms, double[] acc, double[] comp){
//		double varZ = getVarianceOfZ();
//		zVarHistory.add(new Double(acc[2]));
//		
//		if(log){
//			logger.logTimedVariance(now_ms, varZ);
//		}
//		if(log){
//			// Write Compass and Accelerometer data
//			logger.logTimedAcc(now_ms, acc[2]);
//			logger.logTimedCompass(now_ms, comp[0]);
//		}
//	}
//	

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
//		x_History[(historyPtr + 1) % historySize] = x;
//		y_History[(historyPtr + 1) % historySize] = y;
//		z_History[(historyPtr + 1) % historySize] = z;
//		historyPtr++;
	}
}
