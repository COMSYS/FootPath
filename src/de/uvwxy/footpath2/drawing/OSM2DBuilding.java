package de.uvwxy.footpath2.drawing;

import java.util.ArrayList;
import java.util.LinkedList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import de.uvwxy.footpath2.map.GraphEdge;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.tools.GeoUtils;

public class OSM2DBuilding implements DrawToCanvas {
	public ArrayList<GraphEdge> walls_outer = new ArrayList<GraphEdge>();
	public ArrayList<GraphEdge> walls_inner = new ArrayList<GraphEdge>();
	public ArrayList<ArrayList<GraphEdge>> walls_outer_area = new ArrayList<ArrayList<GraphEdge>>();
	public ArrayList<ArrayList<GraphEdge>> walls_inner_area = new ArrayList<ArrayList<GraphEdge>>();

	public ArrayList<GraphEdge> stairs = new ArrayList<GraphEdge>();
	public ArrayList<GraphEdge> elevators = new ArrayList<GraphEdge>();
	public ArrayList<ArrayList<GraphEdge>> stairs_area = new ArrayList<ArrayList<GraphEdge>>();
	public ArrayList<ArrayList<GraphEdge>> elevators_area = new ArrayList<ArrayList<GraphEdge>>();

	private float current_level = 2.0f;

	private float inner_wall_width = 0.10f;
	private float outer_wall_width = 0.25f;

	public void setCurrent_level(float current_level) {
		this.current_level = current_level;
	}

	public float getCurrent_level() {
		return current_level;
	}

	@Override
	public synchronized void drawToCanvas(Canvas canvas, IndoorLocation center, int ox, int oy,
			float pixelsPerMeterOrMaxValue) {
		if (!init) {
			initColors();
			init = true;
		}

		resizeColors(pixelsPerMeterOrMaxValue);

		if (canvas == null || center == null) {
			return;
		}

		drawLevel(canvas, center, pixelsPerMeterOrMaxValue, ox, oy, current_level);

	}

	private int transparency = 240;
	private int color0 = Color.argb(transparency, 189, 174, 173);
	private int color1 = Color.argb(transparency, 255, 192, 203);
	private int color2 = Color.argb(transparency, 231, 78, 78);
	private int color3 = Color.argb(transparency, 136, 217, 114);
	private Paint pWallsInner = new Paint();
	private Paint pWallsOuter = new Paint();
	private Paint pStairs = new Paint();
	private Paint pElevators = new Paint();

	private boolean init = false;

	private void initColors() {
		// #ffc0cb
		pWallsInner.setColor(Color.argb(255, 255, 192, 203));
		// #e4b334
		pWallsOuter.setColor(Color.argb(255, 228, 179, 52));
		// #e74e4e
		pStairs.setColor(Color.argb(255, 231, 78, 78));
		// #e4b334
		pElevators.setColor(Color.argb(255, 136, 217, 114));

	}

	private void resizeColors(float pixelsPerMeterOrMaxValue) {
		pWallsInner.setStrokeWidth(pixelsPerMeterOrMaxValue * inner_wall_width);
		pWallsOuter.setStrokeWidth(pixelsPerMeterOrMaxValue * outer_wall_width);
		pStairs.setStrokeWidth(pixelsPerMeterOrMaxValue * inner_wall_width);
		pElevators.setStrokeWidth(pixelsPerMeterOrMaxValue * inner_wall_width);

	}

	/**
	 * @param canvas
	 * @param center
	 * @param pixelsPerMeterOrMaxValue
	 * @param pLine
	 * @param w
	 * @param h
	 */
	private void drawLevel(Canvas canvas, IndoorLocation center, float pixelsPerMeterOrMaxValue, int w, int h,
			float level) {

		paintWallPathArea(canvas, center, pixelsPerMeterOrMaxValue, w, h, color0, walls_outer_area, level);
		paintWallPathArea(canvas, center, pixelsPerMeterOrMaxValue, w, h, color1, walls_inner_area, level);
		paintWallPathArea(canvas, center, pixelsPerMeterOrMaxValue, w, h, color2, stairs_area, level);
		paintWallPathArea(canvas, center, pixelsPerMeterOrMaxValue, w, h, color3, elevators_area, level);

		for (int i = 0; i < walls_inner.size() - 1; i++) {
			if (walls_inner.get(i).getLevel() == level) {
				int[] apix = GeoUtils.convertToPixelLocation(walls_inner.get(i).getNode0(), center,
						pixelsPerMeterOrMaxValue);
				int[] bpix = GeoUtils.convertToPixelLocation(walls_inner.get(i).getNode1(), center,
						pixelsPerMeterOrMaxValue);
				canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pWallsInner);
			}
		}

		for (int i = 0; i < walls_outer.size() - 1; i++) {
			if (walls_outer.get(i).getLevel() == level) {
				int[] apix = GeoUtils.convertToPixelLocation(walls_outer.get(i).getNode0(), center,
						pixelsPerMeterOrMaxValue);
				int[] bpix = GeoUtils.convertToPixelLocation(walls_outer.get(i).getNode1(), center,
						pixelsPerMeterOrMaxValue);
				canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pWallsOuter);
			}
		}

		for (int i = 0; i < stairs.size() - 1; i++) {
			if (walls_inner.get(i).getLevel() == level) {
				int[] apix = GeoUtils
						.convertToPixelLocation(stairs.get(i).getNode0(), center, pixelsPerMeterOrMaxValue);
				int[] bpix = GeoUtils
						.convertToPixelLocation(stairs.get(i).getNode1(), center, pixelsPerMeterOrMaxValue);
				canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pStairs);
			}
		}

		for (int i = 0; i < elevators.size() - 1; i++) {
			if (walls_inner.get(i).getLevel() == level) {
				int[] apix = GeoUtils.convertToPixelLocation(elevators.get(i).getNode0(), center,
						pixelsPerMeterOrMaxValue);
				int[] bpix = GeoUtils.convertToPixelLocation(elevators.get(i).getNode1(), center,
						pixelsPerMeterOrMaxValue);
				canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pElevators);
			}
		}

	}

	Paint wallpaint = new Paint();
	ArrayList<GraphEdge> es;
	int[] apix;
	
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
			int color, ArrayList<ArrayList<GraphEdge>> walls_area, float level) {


		wallpaint.setColor(color);
		wallpaint.setStyle(Style.FILL);

		Path wallpath = new Path();
		for (int i = 0; i < walls_area.size(); i++) {
			if (walls_area.get(i) != null && walls_area.get(i).get(0) != null
					&& walls_area.get(i).get(0).getLevel() == level) {

				es = walls_area.get(i);
				wallpath.reset(); // only needed when reusing this path for a new build

				if (es.size() > 0) {
					for (int j = 0; j < es.size(); j++) {
						if (j == 0) {
							apix = GeoUtils.convertToPixelLocation(es.get(j).getNode0(), center,
									pixelsPerMeterOrMaxValue);
							wallpath.moveTo(w + apix[0], h + apix[1]); // used for first point
							// Log.i("FOOTPATH", "Draw first line from " + apix[0] + "/" + apix[1]);
						} else {
							apix = GeoUtils.convertToPixelLocation(es.get(j).getNode0(), center,
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
