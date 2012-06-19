package de.uvwxy.footpath2.map;

/**
 * A class to maintain an edge in the graph.
 * 
 * @author Paul Smith
 * 
 */
public class GraphEdge {
	private static int ID = -1337;
	private IndoorLocation node0;
	private IndoorLocation node1;
	private double len;
	private double bearing;
	
	private String indoor;
	private String buildingpart;
	private String highway;
	private String wheelchair;

	// >0 := number correct steps given
	// 0 := no steps
	// -1 := undefined number of steps
	// -2 := elevator
	private int numSteps = 0;

	private float level;

	/**
	 * Constructor to create an empty edge with everything set to 0/null/false
	 */
	public GraphEdge() {
		this.node0 = null;
		this.node1 = null;
		this.len = 0.0;
		this.wheelchair = "yes";
		this.level = Float.MAX_VALUE;
	}

	/**
	 * Constructor to create an edge with given parameters.
	 * 
	 * @param node0
	 *            the first GraphNode
	 * @param node1
	 *            the second GraphNode
	 * @param len
	 *            the length of this edge
	 * @param compDir
	 *            the direction of this edge (node0 -> node1)
	 * @param wheelchair
	 *            the value concerning the wheelchair attribute
	 * @param level
	 *            the level of this edge
	 * @param isIndoor
	 *            true if is indoor
	 */
	public GraphEdge(IndoorLocation node0, IndoorLocation node1, double len, double compDir, String wheelchair,
			float level, String indoor) {
		this.node0 = node0;
		this.node1 = node1;
		this.len = len;
		this.bearing = compDir;
		this.wheelchair = wheelchair;
		this.level = level;
		this.indoor = indoor;
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

	public String getIndoor() {
		return indoor;
	}

	public void setIndoor(String indoor) {
		this.indoor = indoor;
	}

	public double getCompDir() {
		return bearing;
	}

	public IndoorLocation getNode0() {
		return node0;
	}

	public void setNode0(IndoorLocation node0) {
		this.node0 = node0;
	}

	public IndoorLocation getNode1() {
		return node1;
	}

	public void setNode1(IndoorLocation node1) {
		this.node1 = node1;
	}

	public double getLen() {
		return len;
	}

	public void setLen(double len) {
		this.len = len;
	}


	public boolean isStairs() {
		if (highway == null)
			return false;
		if (highway.equals("steps"))
			return true;
		else 
			return false;	}


	public boolean isElevator() {
		if (highway == null)
			return false;
		if (highway.equals("elevator"))
			return true;
		else 
			return false;
	}
	public int getSteps() {
		return numSteps;
	}

	public void setSteps(int numSteps) {
		this.numSteps = numSteps;
		if (numSteps > 0 || numSteps == -1)
			this.setWheelchair("no");// if steps, NO wheelchair
	}

	public float getLevel() {
		return level;
	}

	public void setLevel(float level) {
		this.level = level;
	}


	public boolean isIndoor() {
		if(indoor == null)
			return false;
		if (indoor.equals("yes"))
			return true;
		else 
			return false;
	}

	public void setCompDir(double compDir) {
		this.bearing = compDir;
	}

	public boolean equals(GraphEdge edge) {
		if (edge == null)
			return false;
		return this.node0.equals(edge.getNode0()) && this.node1.equals(edge.getNode1())
				|| this.node0.equals(edge.getNode1()) && this.node1.equals(edge.getNode0());
	}

	public boolean contains(IndoorLocation node) {
		return getNode0().equals(node) || getNode1().equals(node);
	}

	@Override
	public String toString() {
		String ret = "\nEdge(" + this.node0.getId() + " to " + this.node1.getId() + "): ";
		ret += "\n    Length: " + this.len;
		ret += "\n    Bearing: " + this.bearing;
		if (isStairs()) {
			ret += "\n    Staircase with: " + this.getSteps() + " steps";
		}
		if (isElevator()) {
			ret += "\n    Elevator: yes";
		}
		ret += "\n    Level: " + level;
		return ret;
	}

	public String toXML() {
		// I'm not quite sure if this ID creating is considered ugly =)
		// (but it works)
		String ret = "\n  <way id='" + (--GraphEdge.ID) + "' action='modify' visible='true'>";
		ret += nd(this.node0.getId());
		ret += nd(this.node1.getId());
		ret += tag("indoor", this.isIndoor() ? "yes" : "no");
		ret += tag("level", "" + level);
		
		ret += tag("wheelchair", wheelchair);
		// old: -1 = no, 0 = limited, 1 = yes
		

		if (isElevator()) {
			ret += tag("highway", "elevator");
		}

		if (isStairs()) {
			ret += tag("highway", "steps");
			ret += tag("step_count", "" + numSteps);
		}
		ret += "\n  </way>";
		return ret;
	}

	private String nd(int iD) {
		return "\n    <nd ref='" + iD + "' />";
	}

	private String tag(String k, String v) {
		return "\n    <tag k='" + k + "' v='" + v + "' />";
	}
}
