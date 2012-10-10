package de.uvwxy.footpath2.matching;

public class Score {

	public double score(double x, double y, boolean diagonal) {
		double ret = 2.0; // = penalty

		// Sanitize:
		double t = Math.abs(x - y);
		t = (t > 180.0) ? 360.0 - t : t;

		// And score:
		if (t < 45.0) {
			ret = 0.0;
		} else if (t < 90.0) {
			ret = 1.0;
		} else if (t < 120.0) {
			ret = 2.0;
		} else {
			ret = 10.0;
		}

		ret = !diagonal ? ret + 1.5 : ret;
		return ret;
	}


}
