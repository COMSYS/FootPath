package de.uvwxy.footpath2.tools;

import de.uvwxy.footpath2.map.IndoorLocation;

public class GeoUtils {
	private static final double ARC_DISTANCE_PER_DEGREE = 60 * 1852;

	//TODO: crate a class for these functions!! call them from there
	public static int[] convertToPixelLocation(IndoorLocation gpsLocation, IndoorLocation center, double pixelsPerMeter) {
		int[] res = { 0, 0 };
		if (gpsLocation != null && center != null) {
			res[1] = (int) ((gpsLocation.getLatitude() - center.getLatitude()) * ARC_DISTANCE_PER_DEGREE
					* pixelsPerMeter * -1);
			res[0] = (int) ((gpsLocation.getLongitude() - center.getLongitude()) * ARC_DISTANCE_PER_DEGREE
					* Math.cos(Math.toRadians((center.getLatitude() + gpsLocation.getLatitude()) / 2)) * pixelsPerMeter);
		}

		return res;
	}

	/**
	 * Returns a GPS representation of the selected pixel
	 * @param x
	 * @param y
	 * @param center
	 * @param pixelsPerMeter
	 * @return a new object
	 */
	public static IndoorLocation convertPixelToGPSLocation(double x, double y, IndoorLocation center, double pixelsPerMeter) {
		IndoorLocation res = new IndoorLocation("Grid", null);

		x /= pixelsPerMeter;
		y /= pixelsPerMeter;

		res.setLatitude(0);
		res.setLongitude(0);
		if (center != null) {
			double latitude = center.getLatitude() + y / ARC_DISTANCE_PER_DEGREE;
			double longitude = center.getLongitude() + x
					/ (ARC_DISTANCE_PER_DEGREE * Math.cos(Math.toRadians((center.getLatitude() + latitude) / 2)));
			res.setLatitude(latitude);
			res.setLongitude(longitude);
		}
		return res;
	}
}
