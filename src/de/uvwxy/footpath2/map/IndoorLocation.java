package de.uvwxy.footpath2.map;

import android.location.Location;

public class IndoorLocation extends Location {
	private String name;
	private float level;
	private boolean isDoor = false;
	private boolean isInDoors = true;
	// planet radius in meters
	private static final int r = 6378137;
	// meters per degree
	private static final double scale = (Math.PI * r) / 180.0;

	public IndoorLocation(String name, String provider) {
		super(provider);
		this.name = name;
	}

	public float getLevel() {
		return level;
	}

	public void setLevel(float level) {
		this.level = level;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isDoor() {
		return isDoor;
	}

	public void setDoor(boolean isDoor) {
		this.isDoor = isDoor;
	}

	public boolean isInDoors() {
		return isInDoors;
	}

	public void setInDoors(boolean isInDoors) {
		this.isInDoors = isInDoors;
	}

	/**
	 * Return the x value of this coordinate in meters concerning the mercator projection.
	 * 
	 * @return x value in meters
	 */
	public double getMercatorX() {
		// source: http://mathworld.wolfram.com/MercatorProjection.html
		double x = getLongitude();
		// translate into meters
		x *= scale;
		return x;
	}

	/**
	 * Return the y value of this coordinate in meters concerning the mercator projection.
	 * 
	 * @return y value in meters
	 */
	public double getMercatorY() {
		// source: http://mathworld.wolfram.com/MercatorProjection.html
		double y = 0.5 * Math.log((1 + Math.sin(Math.toRadians(getLatitude())))
				/ (1 - Math.sin(Math.toRadians(getLatitude()))));
		// rad to degrees
		y = Math.toDegrees(y);
		// translate into meters
		y *= scale;
		return y;
	}

	public void moveIntoDirection(IndoorLocation nextNode, double factor) {
		// First step: Do Mercator Projection with latitude.
		setLatitude(getLatitude() + (nextNode.getLatitude() - getLatitude()) * factor);
		setLongitude(getLongitude() + (nextNode.getLongitude() - getLongitude()) * factor);
	}

}
