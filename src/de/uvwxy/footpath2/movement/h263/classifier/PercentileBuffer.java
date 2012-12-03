package de.uvwxy.footpath2.movement.h263.classifier;

public class PercentileBuffer {

	private float[] floats;
	private long[] longs;
	private int bufferPtr = 0;
	private int fps;

	public PercentileBuffer(int fps) {
		floats = new float[fps];
		longs = new long[fps];
		bufferPtr = 0;
		this.fps = fps;
	}

	public void add(long ts, float f) {
		bufferPtr++;
		longs[bufferPtr % fps] = ts;
		floats[bufferPtr % fps] = f;
	}

	public float getAverage(long ts_now, long maxdiff) {
		float sum = 0.0f;
		float n = 0;
		for (int i = 0; i < fps; i++) {
			if (ts_now - longs[i] <= maxdiff) {
				n++;
				sum += floats[i];
			}
		}
		
		return sum/n;
	}

}
