package de.uvwxy.footpath2.map;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import de.uvwxy.footpath2.drawing.DrawToCanvas;
import de.uvwxy.footpath2.tools.GeoUtils;

public class IndoorLocation extends Location implements DrawToCanvas {
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
	private ArrayList<GraphEdge> loc_edges = new ArrayList<GraphEdge>();

	public IndoorLocation(Location l) {
		super(l);
		pLocation.setColor(Color.WHITE);
	}

	public IndoorLocation(IndoorLocation l) {
		super(l);
		this.door = l.door;
		this.indoor = l.indoor;
		this.name = l.name;

		this.level = l.level;
		this.id = l.id;
		this.mergeid = l.mergeid;
		this.numSteps = l.numSteps;
		this.loc_edges = l.loc_edges;

		pLocation.setColor(Color.WHITE);
	}

	public IndoorLocation(String provider) {
		super(provider);

		pLocation.setColor(Color.WHITE);
	}

	public IndoorLocation(String name, String provider) {
		super(provider);
		this.name = name;
		pLocation.setColor(Color.WHITE);
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

	public ArrayList<GraphEdge> getEdges() {
		return loc_edges;
	}

	public int getDegree() {
		return loc_edges.size();
	}
	
	@Override
	public float bearingTo(Location dest) {
		float r =  super.bearingTo(dest);
		r = r < 0 ? r + 360 : r;
		return r;
	}

	public List<IndoorLocation> getAdjacentIndoorLocations() {
		ArrayList<IndoorLocation> buf = new ArrayList<IndoorLocation>();

		if (loc_edges != null && loc_edges.size() > 0) {
			for (GraphEdge e : loc_edges) {
				if (e != null) {
					IndoorLocation a = e.getNode0();
					IndoorLocation b = e.getNode1();

					// add the "other" node
					buf.add(equals(a) ? b : a);
				}
			}
		}

		return buf;
	}

	public List<IndoorLocation> getAdjacentIndoorLocationsWithoutElevators() {
		ArrayList<IndoorLocation> buf = new ArrayList<IndoorLocation>();

		if (loc_edges != null && loc_edges.size() > 0) {
			for (GraphEdge e : loc_edges) {
				if (e != null && !e.isElevator()) {
					IndoorLocation a = e.getNode0();
					IndoorLocation b = e.getNode1();

					// add the "other" node
					buf.add(equals(a) ? b : a);
				}
			}
		}

		return buf;
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

	private int[] apix = { 0, 0 };
	private Paint pLocation = new Paint();

	@Override
	public void drawToCanvas(Canvas canvas, IndoorLocation center, int ox, int oy, float pixelsPerMeterOrMaxValue) {
		apix = GeoUtils.convertToPixelLocation(this, center, pixelsPerMeterOrMaxValue);
		canvas.drawCircle(ox + apix[0], oy + apix[1], pixelsPerMeterOrMaxValue * 0.5f, pLocation);
	}
	
	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
}
