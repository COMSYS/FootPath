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
public class IndoorLocationHistory {
	private final List<IndoorLocation> list = new LinkedList<IndoorLocation>();
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

	private IndoorLocation convertPixelToGPSLocation(double x, double y, IndoorLocation center, double pixelsPerMeter) {
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

	/**
	 * convenience method
	 * 
	 * @return
	 */
	public IndoorLocation getLast() {
		if (list.size() == 0)
			return null;
		return list.get(size() - 1);
	}

	/**
	 * convenience method
	 * 
	 * @return
	 */
	public IndoorLocation getFirst() {
		if (list.size() == 0)
			return null;
		return list.get(0);
	}

	/*
	 * list delegates
	 */

	/**
	 * @param object
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(IndoorLocation object) {
		return list.add(object);
	}

	/**
	 * @param location
	 * @param object
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int location, IndoorLocation object) {
		list.add(location, object);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends IndoorLocation> arg0) {
		return list.addAll(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public boolean addAll(int arg0, Collection<? extends IndoorLocation> arg1) {
		return list.addAll(arg0, arg1);
	}

	/**
	 * 
	 * @see java.util.List#clear()
	 */
	public void clear() {
		list.clear();
	}

	/**
	 * @param object
	 * @return
	 * @see java.util.List#contains(java.lang.Object)
	 */
	public boolean contains(Object object) {
		return list.contains(object);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.List#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> arg0) {
		return list.containsAll(arg0);
	}

	/**
	 * @param object
	 * @return
	 * @see java.util.List#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object object) {
		return list.equals(object);
	}

	/**
	 * @param location
	 * @return
	 * @see java.util.List#get(int)
	 */
	public IndoorLocation get(int location) {
		return list.get(location);
	}

	/**
	 * @return
	 * @see java.util.List#hashCode()
	 */
	@Override
	public int hashCode() {
		return list.hashCode();
	}

	/**
	 * @param object
	 * @return
	 * @see java.util.List#indexOf(java.lang.Object)
	 */
	public int indexOf(Object object) {
		return list.indexOf(object);
	}

	/**
	 * @return
	 * @see java.util.List#isEmpty()
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * @return
	 * @see java.util.List#iterator()
	 */
	public Iterator<IndoorLocation> iterator() {
		return list.iterator();
	}

	/**
	 * @param object
	 * @return
	 * @see java.util.List#lastIndexOf(java.lang.Object)
	 */
	public int lastIndexOf(Object object) {
		return list.lastIndexOf(object);
	}

	/**
	 * @return
	 * @see java.util.List#listIterator()
	 */
	public ListIterator<IndoorLocation> listIterator() {
		return list.listIterator();
	}

	/**
	 * @param location
	 * @return
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator<IndoorLocation> listIterator(int location) {
		return list.listIterator(location);
	}

	/**
	 * @param location
	 * @return
	 * @see java.util.List#remove(int)
	 */
	public IndoorLocation remove(int location) {
		return list.remove(location);
	}

	/**
	 * @param object
	 * @return
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean remove(Object object) {
		return list.remove(object);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.List#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> arg0) {
		return list.removeAll(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.List#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> arg0) {
		return list.retainAll(arg0);
	}

	/**
	 * @param location
	 * @param object
	 * @return
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	public IndoorLocation set(int location, IndoorLocation object) {
		return list.set(location, object);
	}

	/**
	 * @return
	 * @see java.util.List#size()
	 */
	public int size() {
		return list.size();
	}

	/**
	 * @param start
	 * @param end
	 * @return
	 * @see java.util.List#subList(int, int)
	 */
	public List<IndoorLocation> subList(int start, int end) {
		return list.subList(start, end);
	}

	/**
	 * @return
	 * @see java.util.List#toArray()
	 */
	public Object[] toArray() {
		return list.toArray();
	}

	/**
	 * @param array
	 * @return
	 * @see java.util.List#toArray(T[])
	 */
	public <T> T[] toArray(T[] array) {
		return list.toArray(array);
	}
}
