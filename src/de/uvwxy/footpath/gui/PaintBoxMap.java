package de.uvwxy.footpath.gui;

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
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;
import de.uvwxy.footpath.R;
import de.uvwxy.footpath.ToolBox;
import de.uvwxy.footpath.core.Positioner;
import de.uvwxy.footpath.graph.GraphEdge;
import de.uvwxy.footpath.graph.LatLonPos;
import de.uvwxy.paintbox.PaintBox;

/**
 * 
 * @author Paul Smith
 * 
 */
class PaintBoxMap extends PaintBox {
	private static final String MAP_SETTINGS = "PaintBoxMap";
	
	private Tile[] tiles;								// array to store osm tiles
	private Bitmap arrow;								// png, this is the user position
	private Bitmap arrowred;							// user position in red
	private Bitmap stairs;								// icon to show stairs on map
	
	private Context context;							
	private Navigator navigator;						// object to get data from (location, bearing,..)

	private LinkedList<GraphEdge> edges;				// all edges on the path, in right order
	private LatLonPos lbBound;							// left bottom position of bounding box (lat/lon)
	private LatLonPos rtBound;							// rigt top position of bounding box (lat/lon)

	private float gScale = 1.0f;						// global scaling, pressing the zoom buttons will change this
	private float scaleFactor = 0.6f;					// value added/removed when changing zoom level
	
	private boolean runOnce = true;						// needed to create/load resources once
	
	public PaintBoxMap(Context context, Navigator navigator) {
		super(context);
		this.context = context;
		this.navigator = navigator;
		// Load saved zoom level
		this.gScale = context.getSharedPreferences(MAP_SETTINGS,0).getFloat("gScale",1.0f); 
	}

	// create lbBound and rtBound
	private void setBoundaries() {
		double latMin = Double.POSITIVE_INFINITY;
		double latMax = Double.NEGATIVE_INFINITY;
		double lonMin = Double.POSITIVE_INFINITY;
		double lonMax = Double.NEGATIVE_INFINITY;
		for(GraphEdge edge : edges){			// edges contain only edges from path
												// but still, almost every node is searched twice ([a,b][b,c][c,d]...)
			double n0lat = edge.getNode0().getLat();
			double n1lat = edge.getNode1().getLat();
			double n0lon = edge.getNode0().getLon();
			double n1lon = edge.getNode1().getLon();
			
			if(n0lat < latMin)			// find minimum lat
				latMin = n0lat;
			if(n1lat < latMin)
				latMin = n1lat;
			if(n0lon < lonMin)			// find minimum lon
				lonMin = n0lon;
			if(n1lon < lonMin)
				lonMin = n1lon;
			
			if(n0lat > latMax)			// find maximum lat
				latMax = n0lat;
			if(n1lat > latMax)
				latMax = n1lat;
			if(n0lon > lonMax)			// find maximum lon
				lonMax = n0lon;
			if(n1lon > lonMax)
				lonMax = n1lon;
		}
		
		lbBound = new LatLonPos(latMin, lonMin, -1337);
		rtBound = new LatLonPos(latMax, lonMax, -1337);
	}
	
	// load tiles for given zoom level, from sdcard or http
	private Tile[] loadTiles(int zoomlevel, LatLonPos lbBoundary, LatLonPos rtBoundary){
		// source: http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
		
		// find out which tiles to get
		int x0 = getTileX(lbBoundary.getLon(), zoomlevel);			// point 0 left top
		int y0 = getTileY(rtBoundary.getLat(), zoomlevel);
		int x1 = getTileX(rtBoundary.getLon(), zoomlevel);			// point 1 right top
		int y2 = getTileY(lbBoundary.getLat(), zoomlevel);			// point 2 left bottom
		
		int diffX = x1 - x0;
		int diffY = y2 - y0;
		
		int arraySize = (diffX+3)*(diffY+3);
		arraySize = arraySize <= 0 ? 1 : arraySize;			// fix size if only one tile needed
		Tile[] res = new Tile[arraySize];
		
		// check if dir exists
		File dir = new File(Environment.getExternalStorageDirectory(),"footpath/");
		if(!dir.exists()){
			dir.mkdir();
		}
		int counter = 0;
		boolean downloadFailed = false;
		for(int x = -1; x <= diffX+1; x++){			// x/y =  -1 to have some more tiles around the building 
			for(int y = -1; y <= diffY+1; y++){   	// because some parts of the building might be overlapping
													// and thus not visible (nicer graphics)
				File f = new File(Environment.getExternalStorageDirectory(),"footpath/tile." 
						+ zoomlevel + "." + (x0+x) + "." + (y0+y) + ".png");
				if(f.exists()){
					// file existed -> read it
					res[counter] = new Tile(zoomlevel, x0+x, y0+y, BitmapFactory.decodeFile(f.getPath()));
					Log.i("FOOTPATH", "Loading from sd footpath/tile."  + zoomlevel + "." + (x0+x) + "." + (y0+y) + ".png)");
				} else {
					// file did not exist -> download it
					URL u = null;
					try {
						u= new URL("http://tile.openstreetmap.org/" + zoomlevel + "/" + (x0+x) + "/" + (y0+y) + ".png");
					} catch (MalformedURLException e) {
						Log.i("FOOTPATH", "URL creation failed (http://tile.openstreetmap.org/"  + zoomlevel + "/" + (x0+x) + "/" + (y0+y) + ".png)" + "\n" + e);
					}    
					try {
						HttpURLConnection c = (HttpURLConnection)u.openConnection();
						c.setDoInput(true);
						InputStream is = c.getInputStream();    
						res[counter] =  new Tile(zoomlevel, x0+x, y0+y, BitmapFactory.decodeStream(is));
						Log.i("FOOTPATH", "Download succeeded (" + u.toString() + ")");
						// -> and save it
						try {
						       FileOutputStream out = new FileOutputStream(f);
						       res[counter].getBitmap().compress(Bitmap.CompressFormat.PNG, 90, out);
						       Log.i("FOOTPATH", "Writing of file suceeded (" + f.toString() + ")");
						} catch (Exception e) {
						      Log.i("FOOTPATH", "Writing of file failed (" + f.toString() + ")" + "\n" + e);
						}
					} catch (IOException e) {
						Log.i("FOOTPATH", "Download failed (" + u.toString() + ")" + "\n" + e);
						downloadFailed = true;
					}
				}
				counter++;
			}
		}
		if(downloadFailed){
			// TODO: Give feedback on failed download of map tiles
		}
		return res;
	}
	
	private int getTileX(double lon, int zoom) {
		return (int)Math.floor( (lon + 180) / 360 * (1<<zoom));
	}
	private int getTileY(double lat, int zoom) {
		return (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if(runOnce){
			edges = this.navigator.getNavPathEdges();
			setBoundaries();
			tiles = loadTiles(18,lbBound, rtBound);
			arrow = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
			arrowred = BitmapFactory.decodeResource(getResources(), R.drawable.arrowred);
			stairs = BitmapFactory.decodeResource(getResources(), R.drawable.stairs);
			runOnce = false; 
		}		
		double localScale = gScale;
		LatLonPos pos = navigator.getPosition();			// get position
		
		globalOffsetX = getWidth()/2.0f - getPosX(pos, localScale);		// center position to screen center
		globalOffsetY = getHeight()/2.0f - getPosY(pos, localScale);	// center position to screen center
		
		canvas.drawColor(Color.BLACK); 						// black background
		drawTiles(canvas, localScale);									// draw map
		this.drawPath(canvas, localScale);								// draw route

		// draw arrow.png to the screen (user position indicator)
				
		if(Positioner.isInRange(navigator.getCompassValue(), navigator.getNavPathDir(), navigator.getAcceptanceWidth())){
			Matrix m = new Matrix();
			m = new Matrix();
			m.setRotate((float) (navigator.getCompassValue()),arrow.getWidth()/2.0f,arrow.getHeight()/2.0f);
			m.postTranslate(globalOffsetX + getPosX(pos, localScale) - arrow.getWidth()/2.0f , globalOffsetY + getPosY(pos, localScale) - arrow.getHeight()/2.0f);
			canvas.drawBitmap(arrow,m,null);					// draw arrow.png to the screen (user position indicator)
		}else {
			Matrix m = new Matrix();
			m.setRotate((float) (navigator.getNavPathDir()),arrow.getWidth()/2.0f,arrow.getHeight()/2.0f);
			m.postTranslate(globalOffsetX + getPosX(pos, localScale) - arrow.getWidth()/2.0f , globalOffsetY + getPosY(pos, localScale) - arrow.getHeight()/2.0f);
			canvas.drawBitmap(arrow,m,null);	
			m = new Matrix();
			m.setRotate((float) (navigator.getCompassValue()),arrow.getWidth()/2.0f,arrow.getHeight()/2.0f);
			m.postTranslate(globalOffsetX + getPosX(pos, localScale) - arrow.getWidth()/2.0f , globalOffsetY + getPosY(pos, localScale) - arrow.getHeight()/2.0f);
			canvas.drawBitmap(arrowred,m,null);					// draw arrowred.png to the screen, meaning wrong direction
		}
		
		
		// draw additional text + background (readability)
		canvas.drawRect(0, 0, getWidth(), 148, ToolBox.myPaint(1, Color.BLACK, 128));
		// check if route end reached
		if (navigator.getNavPathEdgeLenLeft() != -1) {
			// draw information
			canvas.drawText(
					"Distance: " + ToolBox.tdp(navigator.getNavPathLen() - navigator.getNavPathLenLeft()) + "m of " +
					ToolBox.tdp(navigator.getNavPathLen()) + "m" , 10, 42, ToolBox.greenPaint(32.0f));
			
			String nextPath = "";
			switch(navigator.getNextTurn()){
			case -1:
				nextPath = "turn left";
				break;
			case 0:
				nextPath = "straight on";
				break;
			case 1:
				nextPath = "turn right";
				break;
			}
			canvas.drawText(
					"Go " + ToolBox.tdp(navigator.getNavPathEdgeLenLeft())
					+ "m then " + nextPath, 10, 74, ToolBox.greenPaint(32.0f));
			Paint p = ToolBox.greenPaint(32.0f);
			if(!Positioner.isInRange(navigator.getNavPathDir(),
					navigator.getCompassValue(),
					navigator.getAcceptanceWidth())){
				p = ToolBox.redPaint(32.0f);
			}
			canvas.drawText(
					"Bearing: " + ToolBox.tdp(navigator.getCompassValue()) + "/" + (ToolBox.tdp(navigator.getNavPathDir())), 10, 106, p);
//			canvas.drawText(
//					"Variances: " + ToolBox.tdp(navigator.getVarianceOfX()) + "/" + ToolBox.tdp(navigator.getVarianceOfY()) 
//					+ "/" + ToolBox.tdp(navigator.getVarianceOfZ()), 10, 138, ToolBox.greenPaint(32.0f));
			canvas.drawText("Est. step length: " 
					+ ToolBox.tdp(navigator.getEstimatedStepLength()) + " vs " + ToolBox.tdp(navigator.getStepLengthInMeters()) ,10, 138, ToolBox.greenPaint(32.0f));

		} else {
			canvas.drawText("Destination ( " + navigator.getRouteEnd().getName() + ") reached", 10, 32, ToolBox.redPaint(32.0f));
		}
	}

	private void drawTiles(Canvas canvas, double localScale){
		for(int i = 0; i < tiles.length; i++){
			if(tiles[i]!=null){
				LatLonPos lt = tiles[i].getLatLonPosLeftTop();
				LatLonPos rb = tiles[i].getLatLonPosRightBottom();
				float left = globalOffsetX + getPosX(lt, localScale);
				float top = globalOffsetY + getPosY(lt, localScale);
				float right = globalOffsetX + getPosX(rb, localScale);
				float bottom = globalOffsetY + getPosY(rb, localScale);
				RectF destRect = new RectF(left, top, right, bottom);
				
				if(tiles[i].getBitmap()!=null){
					canvas.drawBitmap(tiles[i].getBitmap(), null, destRect, null);
				}
			}
		}
	}
	
	private void drawPath(Canvas canvas, double localScale) {
		if(edges.size() == 1){				// special case: only one edge
			Paint wPaint = edges.get(0).getWheelchair()<0 ? ToolBox.redPaint() : ToolBox.greenPaint();
			canvas.drawLine(globalOffsetX + getPosX(edges.get(0).getNode0().getPos(), localScale),
					globalOffsetY + getPosY(edges.get(0).getNode0().getPos(), localScale),
					globalOffsetX + getPosX(edges.get(0).getNode1().getPos(), localScale),
					globalOffsetY + getPosY(edges.get(0).getNode1().getPos(), localScale), wPaint);
			return;
		}
		
		// try to find node which is not part of next edge:
		int oldNodeId = edges.get(0).getNode0().getId();					// set oldNodeId to node0
		LatLonPos oldNodePos;
		if(oldNodeId == edges.get(1).getNode0().getId()						// oldNodeId is in next edge
				|| oldNodeId == edges.get(1).getNode1().getId()){
			oldNodeId = edges.get(0).getNode1().getId();					// set oldNodeId to other node
			oldNodePos = new LatLonPos(edges.get(0).getNode1().getLat(),		
					edges.get(0).getNode1().getLon(),
					edges.get(0).getNode1().getLevel());
		} else {
			oldNodePos = new LatLonPos(edges.get(0).getNode0().getLat(),	// node is not in next edge (is first node)
					edges.get(0).getNode0().getLon(),
					edges.get(0).getNode0().getLevel());					// only update lat/lon
		}

		LatLonPos newNodePos;
		for(int i = 0; i < edges.size(); i++){
			int node0id = edges.get(i).getNode0().getId();
			if(oldNodeId == node0id){
				newNodePos = new LatLonPos(edges.get(i).getNode1().getLat(),
						edges.get(i).getNode1().getLon(),
						edges.get(i).getNode1().getLevel());
				oldNodeId = edges.get(i).getNode1().getId();
			} else {
				newNodePos = new LatLonPos(edges.get(i).getNode0().getLat(),
					edges.get(i).getNode0().getLon(),
					edges.get(i).getNode0().getLevel());
				oldNodeId = edges.get(i).getNode0().getId();
			}
			// set color according to accessibility (wheelchairable -> green, else red)
			Paint wPaint = edges.get(i).getWheelchair()<0 ? ToolBox.redPaint() : ToolBox.greenPaint();
			
			if(edges.get(i).isStairs()){				// draw stairs icon
				Matrix m = new Matrix();
				m.setScale(1.0f, 1.0f);
				m.postTranslate(globalOffsetX + getPosX(newNodePos, localScale) - stairs.getWidth()/2.0f ,
						globalOffsetY + getPosY(newNodePos, localScale) - stairs.getHeight()/2.0f);
				canvas.drawBitmap(stairs,m,null);
			}
			canvas.drawLine(globalOffsetX + getPosX(oldNodePos, localScale), 			// draw path
					globalOffsetY + getPosY(oldNodePos, localScale),
					globalOffsetX + getPosX(newNodePos, localScale),
					globalOffsetY + getPosY(newNodePos, localScale),
					wPaint);
			
			oldNodePos = newNodePos;

		}
	}

	float scaleWidth;									// displacement to center path: x
	float scaleHeight;									// displacement to center path: y
	float globalOffsetX = 0.0f;							// in case we want to "move" path in x
	float globalOffsetY = 0.0f;							// in case we want to "move" path in y
	
	private float getPosX(LatLonPos p, double localScale){
		return mercatXToScreen(p.getMercatorX(), localScale);		
	}
	
	private float getPosY(LatLonPos p, double localScale){
		return mercatYToScreen(p.getMercatorY(), localScale);
	}
	
	private float mercatXToScreen(double x, double localScale){			// this returns the position relative to the middle of the
		double highX = rtBound.getMercatorX();			// width of all data
		double lowX = lbBound.getMercatorX();
		double w = (highX - lowX);
		double xs = w/2 - (highX - x);					// xs = relative position to center of data
		xs *=localScale;
		return (float)xs;
	}
	
	private float mercatYToScreen(double y, double localScale){			// this returns the position relative to the middle of the
		double highY = rtBound.getMercatorY();			// height of all data
		double lowY = lbBound.getMercatorY();
		double h = (highY - lowY);
		double ys = h/2 - (highY - y);					// ys = relative position to center of data
		ys *= -1.0*localScale;
		return (float)ys;
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
