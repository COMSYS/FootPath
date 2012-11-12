package de.uvwxy.footpath2.matching;

import android.util.Log;

public class ScoreMultiFit extends Score {
	// How many steps should we be able to jump forward instead of going backwards when using MultiFit?
	public final static int VIRTUAL_STEPS_LOOKAHEAD = 20;

	public final static double BASE = 1.03;
	public final static double DEGREE = 90;
	// How high is the penalty when skipping a step or discarding a step?
	public final static double PENALTY_NOT_DIAGONAL = Math.pow(BASE, DEGREE);

	private static final double DELTA = 18.0;

	/*
	 * With a penalty of 10.0 for errors of >= 120.0 degrees we will not jump ahead if the penalty is to low for this
	 * case. MultiFit will match this onto an edge going backwards
	 */

	public double score(double x, double y, int diagonal) {
		double ret = 1.0; // = penalty

		// Sanitize:
		double t = Math.abs(x - y);
		t = (t > 180.0) ? 360.0 - t : t;

		// http://www.wolframalpha.com/input/?i=1.02%5Ex+from+1+to+180
		if (t <= DELTA) {
			ret += 0;
		} else {
			ret += Math.pow(BASE, t);
		}
		switch (diagonal) {
		case -1:
			ret = ret + PENALTY_NOT_DIAGONAL;
			break;
		case 1:
			ret = ret + PENALTY_NOT_DIAGONAL;
			break;
		default:

		}
		
		Log.i("SCORE", "x: " + x + " y: " + y + " --> " + ret + ", t = " + t);
		return ret;
	}
}
