package de.uvwxy.footpath2.movement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import de.uvwxy.footpath2.log.AppendWriter;
import de.uvwxy.footpath2.log.Exporter;

public class SensorHistory implements Serializable, Exporter {
	private final List<SensorTriple> list = new ArrayList<SensorTriple>();

	private static final long serialVersionUID = 5777397679052032803L;
	private final float backLogMillis = 3000; // show three seconds of data

	/*
	 * @Override public SensorTriple get(int location) { if (location < 0 || location >= size()) { return null; } return
	 * super.get(location); }
	 */

	private Paint pRed = new Paint();
	private Paint pGreen = new Paint();
	private Paint pBlue = new Paint();
	private Paint pState = new Paint();

	private boolean init = false;
	
	private void initColors() {
		pRed.setColor(Color.RED);
		pGreen.setColor(Color.GREEN);
		pBlue.setColor(Color.BLUE);
		pState.setTextSize(32);
	}

	public void drawToCanvas(Canvas canvas, Location center, int xo, int yo, double pixelsPerMeterOrMaxValue) {
		if (size() < 2) {
			return;
		}
		
		if (!init){
			initColors();
			init = true;
		}

		int i = size() - 2;
		SensorTriple tempRight = get(i + 1);
		SensorTriple tempLeft = get(i);
		long max = System.currentTimeMillis();
		long diff = max - tempLeft.getTs();

		int height = canvas.getHeight();
		int width = canvas.getWidth();

		float pixelsPerMilli = width / getBackLogMillis();
		float pixelsPerValue = (float) ((height / 2) / pixelsPerMeterOrMaxValue);

		float y0r, y0g, y0b, y1r, y1g, y1b, x0, x1;
		int yoffset = height / 2;
		while ((diff) <= getBackLogMillis()) {
			x1 = (-(max - tempRight.getTs()) * pixelsPerMilli) + width;
			x0 = (-diff * pixelsPerMilli) + width;

			y1r = yoffset - (tempRight.getValues()[0] * pixelsPerValue);
			y1g = yoffset - (tempRight.getValues()[1] * pixelsPerValue);
			y1b = yoffset - (tempRight.getValues()[2] * pixelsPerValue);

			y0r = yoffset - (tempLeft.getValues()[0] * pixelsPerValue);
			y0g = yoffset - (tempLeft.getValues()[1] * pixelsPerValue);
			y0b = yoffset - (tempLeft.getValues()[2] * pixelsPerValue);

			canvas.drawLine(x0, y0r, x1, y1r, pRed);
			canvas.drawLine(x0, y0g, x1, y1g, pGreen);
			canvas.drawLine(x0, y0b, x1, y1b, pBlue);

			// Log.i("LOCMOV", "Drawing element: " + temp);
			// Log.i("LOCMOV", "Drawing element: " + get(i+1));

			i--;
			if (i < 0) {
				break;
			}
			tempLeft = get(i);
			tempRight = get(i + 1);
			if (tempLeft == null || tempRight == null) {
				break;
			}
			diff = max - tempLeft.getTs();
		}

		//canvas.drawText("Size: " + size() * 25 + " byte", 16, 16, pGreen);

		// Log.i("LOCMOV", "Added type: " + t.values[0] + "/" + t.values[1] +
		// "/" + t.values[2]);
		// Log.i("LOCMOV", "Elements drawn: " + test);

	}

	public float getBackLogMillis() {
		return backLogMillis;
	}

	/**
	 * convenience method.
	 * 
	 * @return
	 */
	public SensorTriple getLast() {
		if (size() == 0)
			return null;
		return list.get(size() - 1);
	}

	/******************
	 * LIST DELEGATES
	 ******************/

	/**
	 * @param location
	 * @param object
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int location, SensorTriple object) {
		list.add(location, object);
	}

	/**
	 * @param object
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(SensorTriple object) {
		return list.add(object);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends SensorTriple> arg0) {
		return list.addAll(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public boolean addAll(int arg0, Collection<? extends SensorTriple> arg1) {
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
	public SensorTriple get(int location) {
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
	public Iterator<SensorTriple> iterator() {
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
	public ListIterator<SensorTriple> listIterator() {
		return list.listIterator();
	}

	/**
	 * @param location
	 * @return
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator<SensorTriple> listIterator(int location) {
		return list.listIterator(location);
	}

	/**
	 * @param location
	 * @return
	 * @see java.util.List#remove(int)
	 */
	public SensorTriple remove(int location) {
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
	public SensorTriple set(int location, SensorTriple object) {
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
	public List<SensorTriple> subList(int start, int end) {
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

	private int last_i = 0;

	@Override
	public int export_allData(String filename) {
		AppendWriter a = new AppendWriter(filename);
		int size = list.size();
		last_i = size;
		// append = true
		a.openFile(true);
		SensorTriple triple;
		for (int i = 0; i < size; i++) {
			triple = list.get(i);
			a.appendLineToFile(triple.toCSVLine());
		}

		a.closeFile();
		return size;
	}

	@Override
	public int export_recentData(String filename) {
		AppendWriter a = new AppendWriter(filename);
		a.openFile(true);

		int size = list.size();
		int sum = size - last_i;

		SensorTriple triple;

		for (int i = last_i; i < size; i++) {
			triple = list.get(i);
			a.appendLineToFile(triple.toCSVLine());
		}

		a.closeFile();
		last_i = size;
		return sum;
	}

	@Override
	public int export_clearRecentData() {
		int ret = last_i;
		for (int i = 0; i < last_i; i++) {
			list.remove(0);
		}
		last_i = 0;
		return ret;
	}

	@Override
	public int export_clearAllData() {
		int size = list.size();
		list.clear();
		last_i = 0;
		return size;
	}

	@Override
	public int export_consumedBytes() {
		if (list.size() == 0) {
			return 0;
		} else {
			// num of floats in values + float + long
			return list.size() * (list.get(0).getValues().length * 4 + 4 + 8);
		}
	}

}
