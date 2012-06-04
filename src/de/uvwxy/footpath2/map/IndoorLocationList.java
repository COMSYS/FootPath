package de.uvwxy.footpath2.map;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.os.Environment;

/**
 * This class extends LinkedList<Location> to provide a means to collect GPS Locations, return the total distance
 * between point 1 to point n, and helper functions to draw a path and export the data.
 * 
 * @author Paul Smith
 * 
 */
public class IndoorLocationList extends LinkedList<IndoorLocation> {
	private static final long serialVersionUID = 3297314826220855327L;
	private static final double ARC_DISTANCE_PER_DEGREE = 60 * 1852;

	/**
	 * Get the last, i.e. current, location.
	 * 
	 * @return
	 */
	public Location getLocation() {
		if (size() == 0) {
			return null;
		} else {
			return getLast();
		}
	}

	/**
	 * Get the length of the walked distance.
	 * 
	 * @return walked distance in meters.
	 */
	public double getTotalDistance() {
		double res = 0;
		for (int i = 0; i < this.size() - 1; i++) {
			res += this.get(i).distanceTo(this.get(i + 1));
		}
		return res;
	}
	
	//TODO: crate a class for these functions!! call them from there
	private int[] convertToPixelLocation(IndoorLocation gpsLocation, IndoorLocation center, double pixelsPerMeter) {
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
	public IndoorLocation convertPixelToGPSLocation(double x, double y, IndoorLocation center, double pixelsPerMeter) {
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

	public synchronized void drawToCanvas(Canvas canvas, IndoorLocation center, Rect boundingBox,
			double pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots) {
		int w = boundingBox.width() / 2 + boundingBox.left;
		int h = boundingBox.height() / 2 + boundingBox.top;

		if (canvas == null || center == null || pLine == null || pDots == null) {
			return;
		}

		for (int i = 0; i < this.size() - 1; i++) {
			// draw line between nodes
			IndoorLocation a = this.get(i);
			IndoorLocation b = this.get(i + 1);
			int[] apix = convertToPixelLocation(a, center, pixelsPerMeterOrMaxValue);
			int[] bpix = convertToPixelLocation(b, center, pixelsPerMeterOrMaxValue);
			canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pLine);
		}

		for (int i = 0; i < this.size(); i++) {
			// draw nodes
			IndoorLocation a = this.get(i);
			int[] apix = convertToPixelLocation(a, center, pixelsPerMeterOrMaxValue);
			canvas.drawCircle(w + apix[0], h + apix[1], 2, pDots);
			if (a.getName() != null) {
				canvas.drawText(a.getName(), w + apix[0], h + apix[1], pDots);
			}
		}

	}

	public String exportData(String prefix, String postfix) {
		String ret = "";

		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
			ret = "External storage not writable. Nothing exported.";
		} else {
			// Something else is wrong. It may be one of many other states,
			// but all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
			ret = "External storage error. Nothing exported.";
		}

		if (mExternalStorageAvailable && mExternalStorageWriteable) {
			File dir = Environment.getExternalStorageDirectory();
			String filePath = dir.toString() + "/" + prefix + ".0." + postfix;

			File f = new File(filePath);

			int index = 0;
			while (f.exists()) {
				index++;
				filePath = dir.toString() + "/" + prefix + "." + index + "." + postfix;
				;
				f = new File(filePath);
			}
			StringBuilder builder = new StringBuilder();

			long ms_init = this.getFirst().getTime();

			for (int i = 0; i < this.size(); i++) {
				Location l = this.get(i);
				if (l != null) {
					builder.append("" + (l.getTime() - ms_init) + "," + l.getLatitude() + "," + l.getLongitude() + ","
							+ l.getAltitude() + "\n");
					ms_init = l.getTime();
				}

			}

			try {

				FileWriter fw = new FileWriter(f);

				fw.write(builder.toString());
				fw.flush();
				fw.close();
				ret = "Exported " + this.size() + " entries to\n" + f.toString();
			} catch (IOException e) {
				ret = "Error writing file:\n" + e.getLocalizedMessage();
			}
		}

		return ret;
	}
}
