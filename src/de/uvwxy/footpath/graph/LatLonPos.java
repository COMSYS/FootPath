package de.uvwxy.footpath.graph;

/**
 * A class to maintain a coordinate consisting of latitude and longitude, with
 * a given level.
 *  
 * @author Paul Smith
 *
 */
public class LatLonPos {
	private double lat;
	private double lon;
	
	// Float.MAX_VALUE == undefined!
	private float level; 
	
	// planet radius in meters
	private static final int r = 6378137; 					
	// meters per degree
	private static final double scale = (Math.PI * r)/180.0;
	
	/**
	 * Constructor to create a 0/0/undefined level coordinate.
	 */
	public LatLonPos() {
		this.lat = 0.0;
		this.lon = 0.0;
		this.level = Float.MAX_VALUE;
	}
	
	/**
	 * Constructor to create a coordinate with given parameters.
	 * 
	 * @param lat the latitude
	 * @param lon the longitude
	 * @param level the level
	 */
	public LatLonPos(double lat, double lon, float level) {
		super();
		this.lat = lat;
		this.lon = lon;
		this.level = level;
	}

	public double getLat() {
		return lat;
	}
	
	public double getLon() {
		return lon;
	}
	
	public float getLevel() {
		return level;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public void setLon(double lon) {
		this.lon = lon;
	}
	
	public void setLevel(float level) {
		this.level = level;
	}
	
	/**
	 * Return the x value of this coordinate in meters concerning the mercator
	 * projection.
	 * 
	 * @return x value in meters
	 */
	public double getMercatorX(){
		// source: http://mathworld.wolfram.com/MercatorProjection.html
		double x = lon;
		// translate into meters
		x*=scale; 
		return x;
	}
	
	/**
	 * Return the y value of this coordinate in meters concerning the mercator
	 * projection.
	 * 
	 * @return y value in meters
	 */
	public double getMercatorY(){
		// source: http://mathworld.wolfram.com/MercatorProjection.html
		double y = 0.5*Math.log(
				 (1+Math.sin(Math.toRadians(lat)))
				/(1-Math.sin(Math.toRadians(lat)))   );
		// rad to degrees
		y = Math.toDegrees(y);
		// translate into meters
		y*=scale;
		return y;
	}
	
	public void moveIntoDirection(LatLonPos nextNode, double factor){
		// First step: Do Mercator Projection with latitude.
		lat = lat + (nextNode.lat - lat)*factor;
		lon = lon + (nextNode.lon - lon)*factor;
	}
	
	/**
	 * Create a clone of this Object.
	 * 
	 * @return the new clone
	 */
	public LatLonPos clone(){
		LatLonPos ret = new LatLonPos();
		ret.setLat(lat);
		ret.setLon(lon);
		ret.setLevel(level);
		return ret;
	}
}
