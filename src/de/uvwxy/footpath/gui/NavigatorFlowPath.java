package de.uvwxy.footpath.gui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import de.uvwxy.footpath.R;
import de.uvwxy.footpath2.map.LatLonPos;
import de.uvwxy.footpath2.movement.StepDetection;
import de.uvwxy.footpath2.movement.StepTrigger;
import de.uvwxy.footpath2.movement.h263.FlowPathConfig;
import de.uvwxy.footpath2.movement.h263.FlowPathInterface;
import de.uvwxy.footpath2.movement.h263.MVDTrigger;
import de.uvwxy.footpath2.tools.ToolBox;

/**
 * 
 * @author Paul Smith
 * 
 */
public class NavigatorFlowPath extends Navigator implements StepTrigger,
		MVDTrigger {
	// TODO: this value is crap, like MC Donalds
	private static final int STEPMAX = 500;

	private FlowPathInterface flowPathInterface = FlowPathInterface
			.getInterface();

	private Button btnStartNav = null;
	boolean fpOk = false;

	// #########################################################################
	// ############################## Functions ################################
	// #########################################################################

	private OnClickListener onClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (view.equals(btnStartNav)) {
				if (!fpOk) {
					fpOk = flowPathInterface.startFlowpath(sh01);
					btnStartNav.setText("Stop");
				} else {
					flowPathInterface.stopFlowPath();
					btnStartNav.setText("Start");
				}
			}
		}
	};

	// #########################################################################
	// ######################## Activity Life Cycle ############################
	// #########################################################################

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// route calculation and so on..
		super.onCreate(savedInstanceState);

		double a = getSharedPreferences(Calibrator.CALIB_DATA, 0).getFloat("a",
				0.5f);
		double peak = getSharedPreferences(Calibrator.CALIB_DATA, 0).getFloat(
				"peak", 0.5f);
		int step_timeout_ms = getSharedPreferences(Calibrator.CALIB_DATA, 0)
				.getInt("timeout", 666);

		btnStartNav = (Button) findViewById(R.id.btnStartNav);
		btnStartNav.setOnClickListener(onClickListener);
		// Use StepDetection to obtain compass data. Ignore Step triggers
		// TODO: FiX DiS: Split Step And Compass Trigger
		// BUT: Is still needed for logging, see below.
		// Has to be initialized, because it is loaded in onResume in Parent
		// class
		stepDetection = new StepDetection(this, this, a, peak, step_timeout_ms);

		flowPathInterface.addMVDTrigger(this);
		setNavigating(true);
	}

	/**
	 * Reset the GUI on resume. Capture has been stopped before.
	 */
	@Override
	protected void onResume() {
		super.onResume();
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

		if (log) {
			logger.logStep(now_ms, compDir);
		}

		// Do not use steps for navigation!
		// posBestFit.addStep(compDir);
		// posFirstFit.addStep(compDir);

		// Log.i("FOOTPATH", "posBestFit: " + posBestFit.getProgress());
		// Log.i("FOOTPATH", "posFirstFit: " + posFirstFit.getProgress());
		if (log) {
			// Write location to file after detected step
			LatLonPos bestPos = getPosition(confBestFit);
			LatLonPos firstPos = getPosition(confFirstFit);
			logger.logPosition(now_ms, bestPos.getLat(), bestPos.getLon(),
					posBestFit.getProgress() / this.navPathLen,
					firstPos.getLat(), firstPos.getLon(),
					posFirstFit.getProgress() / this.navPathLen);
		}
	}

	@Override
	public void dataHookAcc(long now_ms, double x, double y, double z) {
		if (log) {
			logger.logRawAcc(now_ms, x, y, z);
		}
	}

	private float[] tf = { 0f, 0f, 0f };
	private float[] compDirs = { 0f, 0f, 0f };

	@Override
	public void dataHookComp(long now_ms, double x, double y, double z) {
		if (log) {
			logger.logRawCompass(now_ms, x, y, z);
		}
		// compassValue = ToolBox.lowpassFilter(compassValue, x, 0.5);
		tf[0] = (float) x;
		tf[1] = (float) y;
		tf[2] = (float) z;

		compDirs = compFilter(compDirs, tf, 0.1f);
		double fixedCompDir = compDirs[0];

		if (fixedCompDir > 360)
			fixedCompDir -= 360;
		compassValue = fixedCompDir;
	}

	@Override
	public void timedDataHook(long now_ms, double[] acc, double[] comp) {
		double varZ = getVarianceOfZ();
		zVarHistory.add(new Double(acc[2]));

		if (log) {
			logger.logTimedVariance(now_ms, varZ);
		}
		if (log) {
			// Write Compass and Accelerometer data
			logger.logTimedAcc(now_ms, acc[2]);
			logger.logTimedCompass(now_ms, comp[0]);
		}
	}

	private long tsLastStep = 0;
	private int speed[];
	private long tsLastMove = 0;
	
	private int[] lastSpeeds = {0,0,0};
	private int numSpeeds = 0;

	@Override
	public void processMVData(long now_ms, float[][][] mvds) {
		long tsNow = System.currentTimeMillis();
		totalStepsWalked++;

		mvdHeatMap(mvds, ++hmPtr);

		// speed = (int) ToolBox.lowpassFilter(speed, getSpeed(heatMaps),
		// 0.01f);
		speed = getSpeed(heatMaps);
		
		lastSpeeds[0] += speed[0];
		lastSpeeds[1] += speed[1];
		lastSpeeds[2] += speed[2];
		numSpeeds++;
		
		Log.i("FLOWPATH", "Speed: " + lastSpeeds[0]/numSpeeds + " " + lastSpeeds[1]/numSpeeds + " " + lastSpeeds[2]/numSpeeds  + " " + (tsNow - tsLastMove));

		if (lastSpeeds[0]/numSpeeds < 3 && lastSpeeds[1]/numSpeeds < 3 && lastSpeeds[2]/numSpeeds < 3 ) {
			// not moving

		} else if (lastSpeeds[0]/numSpeeds >= 3 && lastSpeeds[2]/numSpeeds <= 0 && (tsNow - tsLastMove > STEPMAX)) {
			// moving "fast"
			
			posBestFit.addStep(compassValue);
			posFirstFit.addStep(compassValue);
			posBestFit.addStep(compassValue);
			posFirstFit.addStep(compassValue);

			Log.i("FLOWPATH", "posBestFit: " + posBestFit.getProgress() + " "
					+ (tsNow - tsLastMove));
			Log.i("FLOWPATH", "posFirstFit: " + posFirstFit.getProgress());

			tsLastMove = tsNow;
			numSpeeds = 0;
			lastSpeeds[0] = 0;
			lastSpeeds[1] = 0;
			lastSpeeds[2] = 0;
		} else if ((tsNow - tsLastMove > STEPMAX)) {
			// moving slow
			posBestFit.addStep(compassValue);
			posFirstFit.addStep(compassValue);

			Log.i("FLOWPATH", "posBestFit: " + posBestFit.getProgress() + " "
					+ (tsNow - tsLastMove));
			Log.i("FLOWPATH", "posFirstFit: " + posFirstFit.getProgress());
			tsLastMove = tsNow;
			numSpeeds = 0;
			lastSpeeds[0] = 0;
			lastSpeeds[1] = 0;
			lastSpeeds[2] = 0;
		}

		if (numSpeeds >= 60){
			numSpeeds = 0;
			lastSpeeds[0] = 0;
			lastSpeeds[1] = 0;
			lastSpeeds[2] = 0;
		}
		tsLastStep = tsNow;
	}

	private int numOfHeatMaps = 5;
	private int[][][] heatMaps = new int[numOfHeatMaps][][];
	private int hmPtr = 0;

	private int numMVs = ((FlowPathConfig.PIC_SIZE_HEIGHT / 16)
			* (FlowPathConfig.PIC_SIZE_WIDTH / 16) * numOfHeatMaps);

	private int[][] accumulatedMap = null;

	private void mvdHeatMap(float[][][] mvs, int ptr) {
		int x_len = mvs.length;
		int y_len = mvs[0].length;

		if (heatMaps[ptr % numOfHeatMaps] == null)
			heatMaps[ptr % numOfHeatMaps] = new int[32][32];

		for (int x = 0; x < 32; x++) {
			for (int y = 0; y < 32; y++) {
				heatMaps[ptr % numOfHeatMaps][x][y] = 0;
			}
		}

		for (int x = 0; x < x_len; x++) {
			for (int y = 0; y < y_len; y++) {
				int mvx = (int) mvs[x][y][0];
				int mvy = (int) mvs[x][y][1];
				mvx += 16;
				mvy += 16;
				heatMaps[ptr % numOfHeatMaps][mvy][mvx]++;
			}
		}
	}

	private int[] getSpeed(int[][][] maps) {

		if (maps[hmPtr % numOfHeatMaps] == null)
			return null;

		int x_len = maps[hmPtr % numOfHeatMaps].length;
		int y_len = maps[hmPtr % numOfHeatMaps][0].length;

		if (accumulatedMap == null)
			accumulatedMap = new int[x_len][y_len];

		for (int x = 0; x < x_len; x++) {
			for (int y = 0; y < y_len; y++) {
				// reset map, as it is only created once
				accumulatedMap[x][y] = 0;
				for (int i = 0; i < numOfHeatMaps; i++) {
					if (maps[i] != null)
						accumulatedMap[x][y] += maps[i][x][y];
				}
			}
		}

		int rowSum = 0;

		int s0 = 0;
		int s1 = 0;
		int s2 = 0;

		for (int y = 0; y < y_len; y++) {
			for (int x = 0; x < x_len; x++) {

				int v = accumulatedMap[x][y];
				rowSum += v;

				if (s0 == 0 && rowSum > (numMVs / 4)) {
					s0 = y;
				}

				if (s1 == 0 && rowSum > (numMVs / 2)) {
					s1 = y;
				}

				if (s2 == 0 && rowSum > (numMVs / 4) * 3) {
					s2 = y;
				}

			}

		}

		// speed "normalization" positive forward..
		s0 = (s0 - 16) * -1;
		s1 = (s1 - 16) * -1;
		s2 = (s2 - 16) * -1;

		// c.drawText(action, xoffset, yoffset + 64, p);
		int[] ret = {s0,s1,s2};
		return ret;
	}

	private float[] compFilter(float[] oldv, float[] newv, float factor) {
		if (oldv == null || newv == null || factor == 0 || oldv.length != 3
				|| newv.length != 3)
			return null;
		float[] ret = compDiff(oldv, newv);

		ret[0] = factor * ret[0] + oldv[0];
		ret[1] = factor * ret[1] + oldv[1];
		ret[2] = factor * ret[2] + oldv[2];

		for (float f : ret) {
			if (f > 360)
				f = (f - 360);
			if (f < 0)
				f = (f + 360);
		}

		return ret;
	}

	private float[] compDiff(float[] oldv, float[] newv) {
		if (oldv == null || newv == null || oldv.length != 3
				|| newv.length != 3)
			return null;

		float[] ret = { 0, 0, 0 };

		ret[0] = newv[0] - oldv[0];
		ret[1] = newv[1] - oldv[1];
		ret[2] = newv[2] - oldv[2];

		for (int f = 0; f < 3; f++) {
			if (ret[f] > 180)
				ret[f] = (ret[f] - 360);
			else if (ret[f] < -180)
				ret[f] = (ret[f] + 360);
		}

		return ret;
	}
}
