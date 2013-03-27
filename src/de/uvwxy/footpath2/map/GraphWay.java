package de.uvwxy.footpath2.map;

import java.util.ArrayList;

/**
 * A class to maintain a way with given parameters found in OSM/XML data.
 * 
 * @author Paul Smith
 * 
 */
public class GraphWay {
	// all nodes on this path ( ref0 -> ref1 -> ref2 -> ...)
	private ArrayList<Integer> refs = new ArrayList<Integer>();
	private int id;
	private int step_count = 0;
	private float level;
	private String indoor;
	private String buildingpart;
	private String highway;
	private String wheelchair;
	private String area;
	
	public GraphWay() {
		// creating empty GraphWay
	}
	
	public GraphWay(int id) {
		this.id = id;
	}
	
	/**
	 * Constructor to create a coordinate with given parameters.
	 * 
	 * @param refs
	 *            a LinkedList of Integers, references to GraphNodes
	 * @param id
	 *            the id of this way
	 * @param wheelchair
	 *            the value concerning the wheelchair attribute
	 * @param level
	 *            the level of this way
	 */
	public GraphWay(ArrayList<Integer> refs, int id, String wheelchair, float level) {
		this.refs = refs;
		this.id = id;
		this.wheelchair = wheelchair;
		this.level = level;
	}

	public ArrayList<Integer> getRefs() {
		return refs;
	}

	public void setRefs(ArrayList<Integer> refs) {
		this.refs = refs;
	}

	public void addRef(int ref) {
		this.refs.add(Integer.valueOf(ref));
	}

	public String getBuildingpart() {
		return buildingpart;
	}

	public void setBuildingpart(String buildingpart) {
		this.buildingpart = buildingpart;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getWheelchair() {
		return wheelchair;
	}

	public void setWheelchair(String wheelchair) {
		this.wheelchair = wheelchair;
	}

	public String getHighway() {
		return highway;
	}

	public void setHighway(String highway) {
		this.highway = highway;
	}

	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	public int getStepCount() {
		return step_count;
	}

	public void setStepCount(int numSteps) {
		this.step_count = numSteps;
	}

	public int getSteps() {
		return step_count;
	}

	public void setSteps(int numSteps) {
		this.step_count = numSteps;
	}

	public float getLevel() {
		return level;
	}

	public void setLevel(float level) {
		this.level = level;
	}

	public String getIndoor() {
		return indoor;
	}

	public void setIndoor(String indoor) {
		this.indoor = indoor;
	}

	@Override
	public String toString() {
		String ret = "\nWay(" + this.id + "): ";
		ret += "Wheelchair: " + wheelchair;
		ret += "\nRefs:";
		for (Integer ref : refs) {
			ret += "\n    " + ref.intValue();
		}
		return ret;
	}

	public boolean isIndoor() {
		if (indoor == null)
			return false;
		return indoor.equals("yes");
	}
}
