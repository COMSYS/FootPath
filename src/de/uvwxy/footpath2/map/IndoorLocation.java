package de.uvwxy.footpath2.map;

import java.util.LinkedList;
import java.util.List;

import android.location.Location;

public class IndoorLocation extends Location {
	// planet radius in meters
	private static final int r = 6378137;
	// meters per degree
	private static final double scale = (Math.PI * r) / 180.0;

	private String name;
	private String indoor;
	private String door;

	private float level;
	private int id;
	private String mergeid;
	private int numSteps = 0;
	private List<GraphEdge> loc_edges = new LinkedList<GraphEdge>();

	public IndoorLocation(Location l) {
		super(l);
	}

	public IndoorLocation(String provider) {
		super(provider);
	}

	public IndoorLocation(String name, String provider) {
		super(provider);
		this.name = name;
	}

	public void setIndoor(String indoor) {
		this.indoor = indoor;
	}

	public String getIndoor() {
		return indoor;
	}

	public int getId() {

		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSteps() {
		return numSteps;
	}

	public void setSteps(int numSteps) {
		this.numSteps = numSteps;
	}

	public float getLevel() {
		return level;
	}

	public String getMergeId() {
		return mergeid;
	}

	public void setMergeId(String id) {
		this.mergeid = id;
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
		if (door == null)
			return false;
		if (door.equals("yes"))
			return true;
		else
			return false;
	}

	public void setDoor(String door) {
		this.door = door;
	}

	public boolean isIndoors() {
		if (indoor == null)
			return false;
		if (indoor.equals("yes"))
			return true;
		else
			return false;
	}

	public List<GraphEdge> getEdges() {
		return loc_edges;
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

	@Override
	public String toString() {
		String ret = "\nNode(" + this.id + "): ";
		ret += name != null ? name : "N/A";
		ret += "Indoor: " + indoor;
		ret += "\n    Level: " + this.level;
		ret += "\n    Lat: " + this.getLatitude();
		ret += "\n    Lon: " + this.getLongitude();
		if (getMergeId() != null) {
			ret += "\n    Merges with: " + getMergeId();
		}
		return ret;
	}

	/**
	 * This function is used to export a created graph to XML
	 * 
	 * @return an XML representation of this Location
	 */
	public String toXML() {
		String ret = "\n  <node id='" + this.id + "' action='modify' visible='true' ";
		ret += "lat='" + this.getLatitude() + "' lon='" + this.getLongitude() + "'>";
		ret += tag("indoor", indoor);
		ret += tag("level", "" + level);
		ret += tag("door", door);

		if (name != null && !name.equals("")) {
			ret += tag("name", name);
		}
		ret += "\n  </node>";
		return ret;
	}

	private String tag(String k, String v) {
		return "\n    <tag k='" + k + "' v='" + v + "' />";
	}
}
