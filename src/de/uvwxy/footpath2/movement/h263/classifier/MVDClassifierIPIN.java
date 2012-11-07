package de.uvwxy.footpath2.movement.h263.classifier;

import java.util.Arrays;

import android.util.Log;
import de.uvwxy.footpath2.movement.h263.FlowPathConfig;

public class MVDClassifierIPIN implements MVDClasssifier {

	private float c0 = 1.5f;
	private float c1 = 4.0f;
	private boolean useX = true;
	private float[] vectors = null;
	private PercentileBuffer p10;
	private PercentileBuffer p90;
	private long BACKLOGMILLIS = 2000;

	public MVDClassifierIPIN() {
		p10 = new PercentileBuffer(FlowPathConfig.PIC_FPS * 2);
		p90 = new PercentileBuffer(FlowPathConfig.PIC_FPS * 2);
	}

	public boolean isUseX() {
		return useX;
	}

	public void setUseX(boolean useX) {
		this.useX = useX;
	}

	public float getC0() {
		return c0;
	}

	public float getC1() {
		return c1;
	}

	public void setC0(float c0) {
		this.c0 = c0;
	}

	public void setC1(float c1) {
		this.c1 = c1;
	}

	@Override
	public float classify(long now_ms, float[][][] mvds) {
		gatherVectorsAndSort(mvds);
		p10.add(now_ms, getPercentile(10));
		p90.add(now_ms, getPercentile(90));
		float avgP10 = p10.getAverage(now_ms, BACKLOGMILLIS);
		float avgP90 = p90.getAverage(now_ms, BACKLOGMILLIS);

//		Log.i("FLOWPATH", "avgP10 = " + avgP10 + ", avgP90 = " + avgP90);

		if (Math.abs(avgP90 - avgP10) <= c0)
			return 0.0f;
		if (c0 < Math.abs(avgP90 - avgP10) && Math.abs(avgP90 - avgP10) <= c1)
			return 0.75f;
		else
			return 2.0f;

	}

	private float getPercentile(int p) {
		return vectors[(int) (((float) p / vectors.length) * vectors.length)];
	}

	private void gatherVectorsAndSort(float[][][] mvds) {
		if (vectors == null) {
			vectors = new float[mvds.length * mvds[0].length];
		}

		int x_len = mvds.length;
		int y_len = mvds[0].length;
		int i = 0;
		for (int x = 0; x < x_len; x++) {
			for (int y = 0; y < y_len; y++) {
				if (useX)
					vectors[i] = mvds[x][y][0];
				else
					vectors[i] = mvds[x][y][1];
				i++;
			}
		}

		Arrays.sort(vectors);
	}
}
