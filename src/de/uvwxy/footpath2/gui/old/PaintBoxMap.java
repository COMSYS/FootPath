package de.uvwxy.footpath2.gui.old;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;
import de.uvwxy.footpath2.drawing.PaintBox;
import de.uvwxy.footpath2.drawing.Tile;
import de.uvwxy.footpath2.map.GraphEdge;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.tools.ToolBox;

/**
 * 
 * @author Paul Smith
 * 
 */
class PaintBoxMap extends PaintBox {
	private static final String MAP_SETTINGS = "PaintBoxMap";

	private final Context context;

	private LinkedList<GraphEdge> edges; // all edges on the path, in right order

	private IndoorLocation lbBound; // left bottom position of bounding box (lat/lon)
	private IndoorLocation rtBound; // rigt top position of bounding box (lat/lon)
	private Tile[] tiles; // array to store osm tiles

	private Bitmap arrow; // png, this is the user position
	private Bitmap arrowred; // user position in red
	private Bitmap stairs; // icon to show stairs on map
	private final Matrix m = new Matrix();

	private float gScale = 1.0f; // global scaling, pressing the zoom buttons will change this
	private final float scaleFactor = 0.6f; // value added/removed when changing zoom level

	private boolean runOnce = true; // needed to create/load resources once

	public PaintBoxMap(Context context) {
		super(context);
		this.context = context;
		// Load saved zoom level
		this.gScale = context.getSharedPreferences(MAP_SETTINGS, 0).getFloat("gScale", 1.0f);
	}

	

	@Override
	protected void onDraw(Canvas canvas) {
		// if (runOnce) {
		// edges = this.navigator.getNavPathEdges();
		// setBoundaries();
		// tiles = loadTiles(18, lbBound, rtBound);
		// arrow = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
		// arrowred = BitmapFactory.decodeResource(getResources(), R.drawable.arrowred);
		// stairs = BitmapFactory.decodeResource(getResources(), R.drawable.stairs);
		// runOnce = false;
		// }
		// double localScale = gScale;
		// IndoorLocation pos = navigator.getPosition(); // get position
		//
		// globalOffsetX = getWidth() / 2.0f - getPosX(pos, localScale); // center position to screen center
		// globalOffsetY = getHeight() / 2.0f - getPosY(pos, localScale); // center position to screen center
		//
		// canvas.drawColor(Color.BLACK); // black background
		// drawTiles(canvas, localScale); // draw map
		// this.drawPath(canvas, localScale); // draw route
		//
		// // draw arrow.png to the screen (user position indicator)
		// if (Positioner
		// .isInRange(navigator.getCompassValue(), navigator.getNavPathDir(), navigator.getAcceptanceWidth())) {
		// m.reset();
		// m.setRotate((float) (navigator.getCompassValue()), arrow.getWidth() / 2.0f, arrow.getHeight() / 2.0f);
		// m.postTranslate(globalOffsetX + getPosX(pos, localScale) - arrow.getWidth() / 2.0f, globalOffsetY
		// + getPosY(pos, localScale) - arrow.getHeight() / 2.0f);
		// canvas.drawBitmap(arrow, m, null); // draw arrow.png to the screen (user position indicator)
		// } else {
		// m.reset();
		// m.setRotate((float) (navigator.getNavPathDir()), arrow.getWidth() / 2.0f, arrow.getHeight() / 2.0f);
		// m.postTranslate(globalOffsetX + getPosX(pos, localScale) - arrow.getWidth() / 2.0f, globalOffsetY
		// + getPosY(pos, localScale) - arrow.getHeight() / 2.0f);
		// canvas.drawBitmap(arrow, m, null);
		//
		// m.reset();
		// m.setRotate((float) (navigator.getCompassValue()), arrow.getWidth() / 2.0f, arrow.getHeight() / 2.0f);
		// m.postTranslate(globalOffsetX + getPosX(pos, localScale) - arrow.getWidth() / 2.0f, globalOffsetY
		// + getPosY(pos, localScale) - arrow.getHeight() / 2.0f);
		// canvas.drawBitmap(arrowred, m, null); // draw arrowred.png to the screen, meaning wrong direction
		// }
		//
		// // draw additional text + background (readability)
		// canvas.drawRect(0, 0, getWidth(), 148, ToolBox.myPaint(1, Color.BLACK, 128));
		// // check if route end reached
		// if (navigator.getNavPathEdgeLenLeft() != -1) {
		// // draw information
		// canvas.drawText("Distance: " + ToolBox.tdp(navigator.getNavPathLen() - navigator.getNavPathLenLeft())
		// + "m of " + ToolBox.tdp(navigator.getNavPathLen()) + "m", 10, 42, ToolBox.greenPaint(32.0f));
		//
		// String nextPath = "";
		// switch (navigator.getNextTurn()) {
		// case -1:
		// nextPath = "turn left";
		// break;
		// case 0:
		// nextPath = "straight on";
		// break;
		// case 1:
		// nextPath = "turn right";
		// break;
		// }
		// canvas.drawText("Go " + ToolBox.tdp(navigator.getNavPathEdgeLenLeft()) + "m then " + nextPath, 10, 74,
		// ToolBox.greenPaint(32.0f));
		// Paint p = ToolBox.greenPaint(32.0f);
		// if (!Positioner.isInRange(navigator.getNavPathDir(), navigator.getCompassValue(),
		// navigator.getAcceptanceWidth())) {
		// p = ToolBox.redPaint(32.0f);
		// }
		// canvas.drawText(
		// "Bearing: " + ToolBox.tdp(navigator.getCompassValue()) + "/"
		// + (ToolBox.tdp(navigator.getNavPathDir())), 10, 106, p);
		// // canvas.drawText(
		// // "Variances: " + ToolBox.tdp(navigator.getVarianceOfX()) + "/" + ToolBox.tdp(navigator.getVarianceOfY())
		// // + "/" + ToolBox.tdp(navigator.getVarianceOfZ()), 10, 138, ToolBox.greenPaint(32.0f));
		// canvas.drawText(
		// "Est. step length: " + ToolBox.tdp(navigator.getEstimatedStepLength()) + " vs "
		// + ToolBox.tdp(navigator.getStepLengthInMeters()) + ", " + navigator.getTotalStepsWalked(),
		// 10, 138, ToolBox.greenPaint(32.0f));
		//
		// } else {
		// canvas.drawText("Destination ( " + navigator.getRouteEnd().getName() + ") reached", 10, 32,
		// ToolBox.redPaint(32.0f));
		// }
	}

	/**
	 * Draw the loaded tiles on the screen.
	 * @param canvas
	 * @param localScale
	 */
	private void drawTiles(Canvas canvas, double localScale) {
		for (int i = 0; i < tiles.length; i++) {
			if (tiles[i] != null) {
				IndoorLocation lt = tiles[i].getLatLonPosLeftTop();
				IndoorLocation rb = tiles[i].getLatLonPosRightBottom();
				float left = globalOffsetX + getPosX(lt, localScale);
				float top = globalOffsetY + getPosY(lt, localScale);
				float right = globalOffsetX + getPosX(rb, localScale);
				float bottom = globalOffsetY + getPosY(rb, localScale);
				RectF destRect = new RectF(left, top, right, bottom);

				if (tiles[i].getBitmap() != null) {
					canvas.drawBitmap(tiles[i].getBitmap(), null, destRect, null);
				}
			}
		}
	}

	/** 
	 * Draw the path on the screen
	 * @param canvas
	 * @param localScale
	 */
	@Deprecated
	private void drawPath(Canvas canvas, double localScale) {
		if (edges.size() == 1) { // special case: only one edge
			Paint wPaint = edges.get(0).getWheelchair() < 0 ? ToolBox.redPaint() : ToolBox.greenPaint();
			canvas.drawLine(globalOffsetX + getPosX(edges.get(0).getNode0(), localScale),
					globalOffsetY + getPosY(edges.get(0).getNode0(), localScale),
					globalOffsetX + getPosX(edges.get(0).getNode1(), localScale),
					globalOffsetY + getPosY(edges.get(0).getNode1(), localScale), wPaint);
			return;
		}

		// try to find node which is not part of next edge:
		int oldNodeId = edges.get(0).getNode0().getId(); // set oldNodeId to node0
		IndoorLocation oldNodePos;
		if (oldNodeId == edges.get(1).getNode0().getId() // oldNodeId is in next edge
				|| oldNodeId == edges.get(1).getNode1().getId()) {
			oldNodeId = edges.get(0).getNode1().getId(); // set oldNodeId to other node
			oldNodePos = new IndoorLocation("", "");
			oldNodePos.setLatitude(edges.get(0).getNode1().getLatitude());
			oldNodePos.setLongitude(edges.get(0).getNode1().getLongitude());
			oldNodePos.setLevel(edges.get(0).getNode1().getLevel());
		} else {
			oldNodePos = new IndoorLocation("", "");
			oldNodePos.setLatitude(edges.get(0).getNode0().getLatitude());
			oldNodePos.setLongitude(edges.get(0).getNode0().getLongitude());
			oldNodePos.setLevel(edges.get(0).getNode0().getLevel());// node is not in next edge (is first node) only
																	// update lat/lon
		}

		IndoorLocation newNodePos;
		for (int i = 0; i < edges.size(); i++) {
			int node0id = edges.get(i).getNode0().getId();
			if (oldNodeId == node0id) {
				newNodePos = new IndoorLocation("", "");
				newNodePos.setLatitude(edges.get(i).getNode1().getLatitude());
				newNodePos.setLongitude(edges.get(i).getNode1().getLongitude());
				newNodePos.setLevel(edges.get(i).getNode1().getLevel());
				oldNodeId = edges.get(i).getNode1().getId();
			} else {
				newNodePos = new IndoorLocation("", "");
				newNodePos.setLatitude(edges.get(i).getNode0().getLatitude());
				newNodePos.setLongitude(edges.get(i).getNode0().getLongitude());
				newNodePos.setLevel(edges.get(i).getNode0().getLevel());

				oldNodeId = edges.get(i).getNode0().getId();
			}
			// set color according to accessibility (wheelchairable -> green, else red)
			Paint wPaint = edges.get(i).getWheelchair() < 0 ? ToolBox.redPaint() : ToolBox.greenPaint();

			if (edges.get(i).isStairs()) { // draw stairs icon
				Matrix m = new Matrix();
				m.setScale(1.0f, 1.0f);
				m.postTranslate(globalOffsetX + getPosX(newNodePos, localScale) - stairs.getWidth() / 2.0f,
						globalOffsetY + getPosY(newNodePos, localScale) - stairs.getHeight() / 2.0f);
				canvas.drawBitmap(stairs, m, null);
			}
			canvas.drawLine(
					globalOffsetX + getPosX(oldNodePos, localScale), // draw path
					globalOffsetY + getPosY(oldNodePos, localScale), globalOffsetX + getPosX(newNodePos, localScale),
					globalOffsetY + getPosY(newNodePos, localScale), wPaint);

			oldNodePos = newNodePos;

		}
	}

	float scaleWidth; // displacement to center path: x
	float scaleHeight; // displacement to center path: y
	float globalOffsetX = 0.0f; // in case we want to "move" path in x
	float globalOffsetY = 0.0f; // in case we want to "move" path in y

	private float getPosX(IndoorLocation p, double localScale) {
		return mercatXToScreen(p.getMercatorX(), localScale);
	}

	private float getPosY(IndoorLocation p, double localScale) {
		return mercatYToScreen(p.getMercatorY(), localScale);
	}

	private float mercatXToScreen(double x, double localScale) { // this returns the position relative to the middle of
																	// the
		double highX = rtBound.getMercatorX(); // width of all data
		double lowX = lbBound.getMercatorX();
		double w = (highX - lowX);
		double xs = w / 2 - (highX - x); // xs = relative position to center of data
		xs *= localScale;
		return (float) xs;
	}

	private float mercatYToScreen(double y, double localScale) { // this returns the position relative to the middle of
																	// the
		double highY = rtBound.getMercatorY(); // height of all data
		double lowY = lbBound.getMercatorY();
		double h = (highY - lowY);
		double ys = h / 2 - (highY - y); // ys = relative position to center of data
		ys *= -1.0 * localScale;
		return (float) ys;
	}

	public void zoomOut() {
		if (gScale <= scaleFactor) {
		} else {
			gScale -= scaleFactor;
			SharedPreferences settings = context.getSharedPreferences(MAP_SETTINGS, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putFloat("gScale", gScale);
			editor.commit();
		}
	}

	public void zoomIn() {
		if (gScale >= 100.0f) {
		} else {
			gScale += scaleFactor;
			SharedPreferences settings = context.getSharedPreferences(MAP_SETTINGS, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putFloat("gScale", gScale);
			editor.commit();
		}
	}
}
