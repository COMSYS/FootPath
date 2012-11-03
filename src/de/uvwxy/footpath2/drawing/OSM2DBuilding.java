package de.uvwxy.footpath2.drawing;

import java.util.LinkedList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import de.uvwxy.footpath2.map.GraphEdge;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.tools.GeoUtils;

public class OSM2DBuilding implements DrawToCanvas {
	public LinkedList<GraphEdge> walls_outer = new LinkedList<GraphEdge>();
	public LinkedList<GraphEdge> walls_inner = new LinkedList<GraphEdge>();
	public LinkedList<LinkedList<GraphEdge>> walls_outer_area = new LinkedList<LinkedList<GraphEdge>>();
	public LinkedList<LinkedList<GraphEdge>> walls_inner_area = new LinkedList<LinkedList<GraphEdge>>();

	public LinkedList<GraphEdge> stairs = new LinkedList<GraphEdge>();
	public LinkedList<GraphEdge> elevators = new LinkedList<GraphEdge>();
	public LinkedList<LinkedList<GraphEdge>> stairs_area = new LinkedList<LinkedList<GraphEdge>>();
	public LinkedList<LinkedList<GraphEdge>> elevators_area = new LinkedList<LinkedList<GraphEdge>>();

	private float current_level = 2.0f;

	private float inner_wall_width = 0.10f;
	private float outer_wall_width = 0.25f;

	public void setCurrent_level(float current_level) {
		this.current_level = current_level;
	}

	public float getCurrent_level() {
		return current_level;
	}

	private int w, h;

	@Override
	public synchronized void drawToCanvas(Canvas canvas, IndoorLocation center, Rect boundingBox,
			float pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots) {
		w = boundingBox.width() / 2 + boundingBox.left;
		h = boundingBox.height() / 2 + boundingBox.top;

		if (canvas == null || center == null || pLine == null || pDots == null) {
			return;
		}

		drawLevel(canvas, center, pixelsPerMeterOrMaxValue, pLine, w, h, current_level);

	}

	/**
	 * @param canvas
	 * @param center
	 * @param pixelsPerMeterOrMaxValue
	 * @param pLine
	 * @param w
	 * @param h
	 */
	private void drawLevel(Canvas canvas, IndoorLocation center, float pixelsPerMeterOrMaxValue, Paint pLine, int w,
			int h, float level) {
		float oldWidth = pLine.getStrokeWidth();

		int t = 192;
		paintWallPathArea(canvas, center, pixelsPerMeterOrMaxValue, w, h, Color.argb(t, 189, 174, 173),
				walls_outer_area, level);
		paintWallPathArea(canvas, center, pixelsPerMeterOrMaxValue, w, h, Color.argb(t, 255, 192, 203),
				walls_inner_area, level);
		paintWallPathArea(canvas, center, pixelsPerMeterOrMaxValue, w, h, Color.argb(t, 231, 78, 78), stairs_area,
				level);
		paintWallPathArea(canvas, center, pixelsPerMeterOrMaxValue, w, h, Color.argb(t, 136, 217, 114), elevators_area,
				level);

		// #ffc0cb
		pLine.setColor(Color.argb(255, 255, 192, 203));
		pLine.setStrokeWidth(pixelsPerMeterOrMaxValue * inner_wall_width);
		for (int i = 0; i < walls_inner.size() - 1; i++) {
			if (walls_inner.get(i).getLevel() == level) {
				int[] apix = GeoUtils.convertToPixelLocation(walls_inner.get(i).getNode0(), center,
						pixelsPerMeterOrMaxValue);
				int[] bpix = GeoUtils.convertToPixelLocation(walls_inner.get(i).getNode1(), center,
						pixelsPerMeterOrMaxValue);
				canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pLine);
			}
		}

		// #e4b334
		pLine.setColor(Color.argb(255, 228, 179, 52));
		pLine.setStrokeWidth(pixelsPerMeterOrMaxValue * outer_wall_width);
		for (int i = 0; i < walls_outer.size() - 1; i++) {
			if (walls_outer.get(i).getLevel() == level) {
				int[] apix = GeoUtils.convertToPixelLocation(walls_outer.get(i).getNode0(), center,
						pixelsPerMeterOrMaxValue);
				int[] bpix = GeoUtils.convertToPixelLocation(walls_outer.get(i).getNode1(), center,
						pixelsPerMeterOrMaxValue);
				canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pLine);
			}
		}

		// #e74e4e
		pLine.setColor(Color.argb(255, 231, 78, 78));
		pLine.setStrokeWidth(pixelsPerMeterOrMaxValue * inner_wall_width);
		for (int i = 0; i < stairs.size() - 1; i++) {
			if (walls_inner.get(i).getLevel() == level) {
				int[] apix = GeoUtils
						.convertToPixelLocation(stairs.get(i).getNode0(), center, pixelsPerMeterOrMaxValue);
				int[] bpix = GeoUtils
						.convertToPixelLocation(stairs.get(i).getNode1(), center, pixelsPerMeterOrMaxValue);
				canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pLine);
			}
		}

		// #e4b334
		pLine.setColor(Color.argb(255, 136, 217, 114));
		pLine.setStrokeWidth(pixelsPerMeterOrMaxValue * inner_wall_width);
		for (int i = 0; i < elevators.size() - 1; i++) {
			if (walls_inner.get(i).getLevel() == level) {
				int[] apix = GeoUtils.convertToPixelLocation(elevators.get(i).getNode0(), center,
						pixelsPerMeterOrMaxValue);
				int[] bpix = GeoUtils.convertToPixelLocation(elevators.get(i).getNode1(), center,
						pixelsPerMeterOrMaxValue);
				canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pLine);
			}
		}

		pLine.setStrokeWidth(oldWidth);
	}

	/**
	 * @param canvas
	 * @param center
	 * @param pixelsPerMeterOrMaxValue
	 * @param w
	 * @param h
	 * @param wallpaint
	 * @param wallpath
	 */
	private void paintWallPathArea(Canvas canvas, IndoorLocation center, float pixelsPerMeterOrMaxValue, int w, int h,
			int color, LinkedList<LinkedList<GraphEdge>> walls_area, float level) {

		Paint wallpaint = new Paint();

		wallpaint.setColor(color);
		wallpaint.setStyle(Style.FILL);

		Path wallpath = new Path();
		for (int i = 0; i < walls_area.size(); i++) {
			if (walls_area.get(i) != null && walls_area.get(i).getFirst() != null
					&& walls_area.get(i).getFirst().getLevel() == level) {

				LinkedList<GraphEdge> es = walls_area.get(i);
				wallpath.reset(); // only needed when reusing this path for a new build

				if (es.size() > 0) {
					for (int j = 0; j < es.size(); j++) {
						if (j == 0) {
							int[] apix = GeoUtils.convertToPixelLocation(es.get(j).getNode0(), center,
									pixelsPerMeterOrMaxValue);
							wallpath.moveTo(w + apix[0], h + apix[1]); // used for first point
							// Log.i("FOOTPATH", "Draw first line from " + apix[0] + "/" + apix[1]);
						} else {
							int[] apix = GeoUtils.convertToPixelLocation(es.get(j).getNode0(), center,
									pixelsPerMeterOrMaxValue);
							wallpath.lineTo(w + apix[0], h + apix[1]);
							if (j == es.size() - 1) {
								// draw line to start point
								apix = GeoUtils.convertToPixelLocation(es.get(0).getNode0(), center,
										pixelsPerMeterOrMaxValue);
								wallpath.lineTo(w + apix[0], h + apix[1]);
								// Log.i("FOOTPATH", "Draw last line to " + apix[0] + "/" + apix[1]);
							}
						}
					}
					canvas.drawPath(wallpath, wallpaint);
				}
			}
		}
	}
}
