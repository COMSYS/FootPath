package de.uvwxy.footpath2.map;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.os.Environment;
import de.uvwxy.footpath2.drawing.DrawToCanvas;
import de.uvwxy.footpath2.tools.GeoUtils;

/**
 * This class extends LinkedList<Location> to provide a means to collect GPS Locations, return the total distance
 * between point 1 to point n, and helper functions to draw a path and export the data.
 * 
 * @author Paul Smith
 * 
 */
public class IndoorLocationList extends LinkedList<IndoorLocation> implements DrawToCanvas {
	private static final long serialVersionUID = 3297314826220855327L;

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

	public synchronized void drawToCanvas(Canvas canvas, IndoorLocation center, int ox, int oy,
			float pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots) {

		if (canvas == null || center == null || pLine == null || pDots == null) {
			return;
		}

		for (int i = 0; i < this.size() - 1; i++) {
			// draw line between nodes
			IndoorLocation a = this.get(i);
			IndoorLocation b = this.get(i + 1);
			int[] apix = GeoUtils.convertToPixelLocation(a, center, pixelsPerMeterOrMaxValue);
			int[] bpix = GeoUtils.convertToPixelLocation(b, center, pixelsPerMeterOrMaxValue);
			canvas.drawLine(ox + apix[0], oy + apix[1], ox + bpix[0], oy + bpix[1], pLine);
		}

		for (int i = 0; i < this.size(); i++) {
			// draw nodes
			IndoorLocation a = this.get(i);
			int[] apix = GeoUtils.convertToPixelLocation(a, center, pixelsPerMeterOrMaxValue);
			canvas.drawCircle(ox + apix[0], oy + apix[1], 2, pDots);
			if (a.getName() != null) {
				canvas.drawText(a.getName(), ox + apix[0], oy + apix[1], pDots);
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
