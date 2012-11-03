package de.uvwxy.footpath2.movement.h263.classifier;

import android.util.Log;
import de.uvwxy.footpath2.movement.h263.FlowPathConfig;

public class MVDClassifierMWC implements MVDClasssifier {

	@Override
	public int classify(long now_ms, float[][][] mvds) {
		return processMVData(now_ms, mvds);
	}

	private static final int STEPMAX = 500;

	private long tsLastStep = 0;
	private int speed[];
	private long tsLastMove = 0;

	private int[] lastSpeeds = { 0, 0, 0 };
	private int numSpeeds = 0;

	public int processMVData(long now_ms, float[][][] mvds) {
		int ret = 0;

		long tsNow = System.currentTimeMillis();
		// totalStepsWalked++;

		mvdHeatMap(mvds, ++hmPtr);

		// speed = (int) ToolBox.lowpassFilter(speed, getSpeed(heatMaps),
		// 0.01f);
		speed = getSpeed(heatMaps);

		lastSpeeds[0] += speed[0];
		lastSpeeds[1] += speed[1];
		lastSpeeds[2] += speed[2];
		numSpeeds++;

		Log.i("FLOWPATH", "Speed: " + lastSpeeds[0] / numSpeeds + " " + lastSpeeds[1] / numSpeeds + " " + lastSpeeds[2]
				/ numSpeeds + " " + (tsNow - tsLastMove));

		if (lastSpeeds[0] / numSpeeds < 3 && lastSpeeds[1] / numSpeeds < 3 && lastSpeeds[2] / numSpeeds < 3) {
			// not moving

		} else if (lastSpeeds[0] / numSpeeds >= 3 && lastSpeeds[2] / numSpeeds <= 0 && (tsNow - tsLastMove > STEPMAX)) {
			// moving "fast"

			// TODO: fixt this: trigger steps:
			// posBestFit.addStep(compassValue);
			// posFirstFit.addStep(compassValue);
			// posBestFit.addStep(compassValue);
			// posFirstFit.addStep(compassValue);
			ret = 2;

			// Log.i("FLOWPATH", "posBestFit: " + posBestFit.getProgress() + " "
			// + (tsNow - tsLastMove));
			// Log.i("FLOWPATH", "posFirstFit: " + posFirstFit.getProgress());

			tsLastMove = tsNow;
			numSpeeds = 0;
			lastSpeeds[0] = 0;
			lastSpeeds[1] = 0;
			lastSpeeds[2] = 0;
		} else if ((tsNow - tsLastMove > STEPMAX)) {
			// moving slow

			// TODO: fix this: trigger steps:
			// posBestFit.addStep(compassValue);
			// posFirstFit.addStep(compassValue);
			ret = 1;
			// Log.i("FLOWPATH", "posBestFit: " + posBestFit.getProgress() + " "
			// + (tsNow - tsLastMove));
			// Log.i("FLOWPATH", "posFirstFit: " + posFirstFit.getProgress());

			tsLastMove = tsNow;
			numSpeeds = 0;
			lastSpeeds[0] = 0;
			lastSpeeds[1] = 0;
			lastSpeeds[2] = 0;
		}

		if (numSpeeds >= 60) {
			numSpeeds = 0;
			lastSpeeds[0] = 0;
			lastSpeeds[1] = 0;
			lastSpeeds[2] = 0;
		}
		tsLastStep = tsNow;

		return ret;
	}

	private int numOfHeatMaps = 5;
	private int[][][] heatMaps = new int[numOfHeatMaps][][];
	private int hmPtr = 0;

	private int numMVs = ((FlowPathConfig.PIC_SIZE_HEIGHT / 16) * (FlowPathConfig.PIC_SIZE_WIDTH / 16) * numOfHeatMaps);

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
		int[] ret = { s0, s1, s2 };
		return ret;
	}

}
