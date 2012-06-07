package de.uvwxy.footpath2.map;

import java.util.LinkedList;
import java.util.List;

/**
 * A class to maintain a node with given parameters found in OSM/XML data.
 * 
 * @author Paul Smith
 * 
 */
public class GraphNode {
	private double lon;
	private double lat;
	private String name;
	private boolean isInDoors;
	private boolean isDoor;
	private float level;
	private int id;
	private String merge_id;
	private int numSteps = 0;
	private final List<GraphEdge> loc_edges;

	/**
	 * Constructor to create an empty node with everything set to 0/null/false
	 */
	public GraphNode() {
		this.lon = 0.0;
		this.lat = 0.0;
		this.name = null;
		this.isInDoors = false;
		this.isDoor = false;
		this.level = 0;
		this.merge_id = null;
		loc_edges = new LinkedList<GraphEdge>();
	}

	/**
	 * Constructor to create a node with given parameters.
	 * 
	 * @param lon
	 *            the longitude
	 * @param lat
	 *            the latitude
	 * @param name
	 *            the name
	 * @param indoors
	 *            true if is indoor node
	 * @param isDoor
	 *            true if is door
	 * @param level
	 *            the level
	 */
	public GraphNode(double lon, double lat, String name, boolean indoors, boolean isDoor, float level) {
		this.lon = lon;
		this.lat = lat;
		this.name = name;
		this.isInDoors = indoors;
		this.isDoor = isDoor;
		this.level = level;
		this.merge_id = null;
		loc_edges = new LinkedList<GraphEdge>();
	}

	public double getLon() {
		return lon;
	}

	public double getLat() {
		return lat;
	}

	public String getName() {
		return name;
	}

	public boolean isIndoors() {
		return isInDoors;
	}

	public boolean isDoor() {
		return isDoor;
	}

	public float getLevel() {
		return level;
	}

	public int getId() {
		return id;
	}

	public String getMergeId() {
		return this.merge_id;
	}

	public int getSteps() {
		return numSteps;
	}

	public List<GraphEdge> getLocEdges() {
		return loc_edges;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setIndoors(boolean indoors) {
		this.isInDoors = indoors;
	}

	public void setDoor(boolean door) {
		this.isDoor = door;
	}

	public void setLevel(float level) {
		this.level = level;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setMergeId(String mergeId) {
		this.merge_id = mergeId;
	}

	public void setSteps(int numSteps) {
		this.numSteps = numSteps;
	}

	public boolean equals(GraphNode node) {
		if (node == null)
			return false;
		return this.getId() == node.getId();
	}

	@Override
	public String toString() {
		String ret = "\nNode(" + this.id + "): ";
		ret += name != null ? name : "N/A";
		ret += isInDoors ? " (indoors)" : " (outdoors)";
		ret += "\n    Level: " + this.level;
		ret += "\n    Lat: " + this.lat;
		ret += "\n    Lon: " + this.lon;
		if (getMergeId() != null) {
			ret += "\n    Merges with: " + getMergeId();
		}
		return ret;
	}

	public String toXML() {
		String ret = "\n  <node id='" + this.id + "' action='modify' visible='true' ";
		ret += "lat='" + this.lat + "' lon='" + this.lon + "'>";
		ret += tag("indoor", this.isInDoors ? "yes" : "no");
		ret += tag("level", "" + level);
		if (isDoor) {
			ret += tag("highway", "door");
		}
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
