package de.uvwxy.footpath2.tools;

import android.graphics.Bitmap;
import de.uvwxy.footpath2.map.IndoorLocation;

/**
 * This is a class to maintain a tile. The tile is stored in a bitmap.
 * 
 * @author Paul Smith
 * 
 */
public class Tile {
	int zoomlevel;
	int x;
	int y;
	Bitmap bitmap;

	public Tile(int zoomlevel, int x, int y, Bitmap bitmap) {
		this.zoomlevel = zoomlevel;
		this.x = x;
		this.y = y;
		this.bitmap = bitmap;
	}

	public int getZoomlevel() {
		return zoomlevel;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setZoomlevel(int zoomlevel) {
		this.zoomlevel = zoomlevel;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public IndoorLocation getLatLonPosLeftTop() {
		return tileToLoc(x,y,zoomlevel);
	}

	public IndoorLocation getLatLonPosRightBottom() {
		return tileToLoc(x+1,y+1,zoomlevel);
	}

	private IndoorLocation tileToLoc(final int x, final int y, final int zoom) {
		IndoorLocation loc = new IndoorLocation("LeftTop","");
		loc.setLatitude(tile2lat(y,zoom));
		loc.setLongitude(tile2lon(x,zoom));
		return loc;
	}

	static double tile2lon(int x, int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

	static double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}
}