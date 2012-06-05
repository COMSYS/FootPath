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
	private short wheelchair;
	private boolean isStairs = false;
	private boolean isElevator = false;
	
	// >0 := number correct steps given
	//  0 := no steps
	// -1 := undefined number of steps
	// -2 := elevator
	private int numSteps = 0;
	
	private float level;
	private boolean isIndoor;
	
	/**
	 * Constructor to create an empty edge with everything set to 0/null/false
	 */
	public GraphEdge() {
		this.node0 = null;
		this.node1 = null;
		this.len = 0.0;
		this.wheelchair = 1;
		this.level = Float.MAX_VALUE;
		this.isIndoor = false;
	}
	
	/**
	 * Constructor to create an edge with given parameters.
	 * 
	 * @param node0 the first GraphNode
	 * @param node1 the second GraphNode
	 * @param len the length of this edge
	 * @param compDir the direction of this edge (node0 -> node1)
	 * @param wheelchair the value concerning the wheelchair attribute
	 * @param level the level of this edge
	 * @param isIndoor true if is indoor
	 */
	public GraphEdge(IndoorLocation node0, IndoorLocation node1, double len, double compDir, short wheelchair, float level, boolean isIndoor) {
		this.node0 = node0;
		this.node1 = node1;
		this.len = len;
		this.bearing = compDir;
		this.wheelchair = wheelchair;
		this.level = level;
		this.isIndoor = isIndoor;
	}
	
	public double getCompDir() {
		return bearing;
	}
	
	public IndoorLocation getNode0() {
		return node0;
	}
	
	public IndoorLocation getNode1() {
		return node1;
	}
	
	public double getLen() {
		return len;
	}
	
	public short getWheelchair() {
		return wheelchair;
	}
	
	public boolean isStairs(){
		return isStairs;
	}
	
	public boolean isElevator(){
		return isElevator;
	}
	
	public int getSteps(){
		return numSteps;
	}
	
	public float getLevel() {
		return level;
	}
	
	public boolean isIndoor(){
		return isIndoor;
	}
	
	public void setCompDir(double compDir) {
		this.bearing = compDir;
	}
	
	public void setNode0(IndoorLocation node0) {
		this.node0 = node0;
	}
	
	public void setNode1(IndoorLocation node1) {
		this.node1 = node1;
	}
	
	public void setLen(double len) {
		this.len = len;
	}
	
	public void setWheelchair(short wheelchair) {
		this.wheelchair = wheelchair;
	}
	
	public void setStairs(boolean isStairs) {
		this.isStairs = isStairs;
	}
	
	public void setElevator(boolean isElevator) {
		this.isElevator = isElevator;
	}
	
	public void setSteps(int numSteps){
		this.numSteps = numSteps;
		if(numSteps>0 || numSteps==-1)
			this.setWheelchair((short)-1);//if steps, NO wheelchair
	}
	
	public void setLevel(float level) {
		this.level = level;
	}
	
	public void setLevel(boolean isIndoor){
		this.isIndoor = isIndoor;
	}

	public boolean equals(GraphEdge edge){
		if(edge == null)
			return false;
		return this.node0.equals(edge.getNode0()) && this.node1.equals(edge.getNode1())
				|| this.node0.equals(edge.getNode1()) && this.node1.equals(edge.getNode0());
	}
	
	public boolean contains(IndoorLocation node){
		return getNode0().equals(node) || getNode1().equals(node);
	}
	
	public String toString(){
		String ret = "\nEdge(" + this.node0.getId() + " to " + this.node1.getId() + "): ";
		ret += "\n    Length: " + this.len;
		ret += "\n    Bearing: " + this.bearing;
		if(isStairs()){
			ret += "\n    Staircase with: " + this.getSteps() + " steps";
		}
		if(isElevator()){
			ret += "\n    Elevator: yes";
		}
		ret+="\n    Level: " + level;
		return ret;
	}
	
	public String toXML(){
		// I'm not quite sure if this ID creating is considered ugly =) 
		// (but it works)
		String ret = "\n  <way id='" + (--GraphEdge.ID) + "' action='modify' visible='true'>";
		ret += nd (this.node0.getId());
		ret += nd (this.node1.getId());
		ret += tag("indoor", this.isIndoor() ? "yes" : "no");
		ret += tag("level", "" + level);
		switch (wheelchair) {
		case -1:
			ret += tag("wheelchair", "no");
			break;
		case 0:
			ret += tag("wheelchair", "limited");
			break;
		case 1:
			ret += tag("wheelchair", "yes");
			break;
		default:
		}
		
		if (this.isElevator){
			ret += tag("highway", "elevator");
		}
		
		if (this.isStairs){
			ret += tag("highway", "steps");
			ret += tag("step_count", "" + numSteps);
		}
		ret += "\n  </way>";
		return ret;
	}
	
	private String nd(int iD){
		return "\n    <nd ref='" + iD + "' />";
	}
	
	private String tag(String k, String v){
		return "\n    <tag k='" + k + "' v='" + v + "' />";
	}
}
