package de.uvwxy.footpath2.drawing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.tools.GeoUtils;

public class TileLoaderAndPainter implements DrawToCanvas {

	private IndoorLocation lbBound; // left bottom position of bounding box (lat/lon)
	private IndoorLocation rtBound; // rigt top position of bounding box (lat/lon)

	private List<IndoorLocation> nodes; // all nodes on the path
	private Tile[] tiles; // array to store osm tiles
	private int zoomlevel; // zoomlevel of tiles [1..18]

	public void _a_setNodes(List<IndoorLocation> nodes) {
		this.nodes = nodes;
	}

	/**
	 * Call this function to set lbBound and rtBound.
	 */
	public void _b_setBoundaries() {
		double latMin = Double.POSITIVE_INFINITY;
		double latMax = Double.NEGATIVE_INFINITY;
		double lonMin = Double.POSITIVE_INFINITY;
		double lonMax = Double.NEGATIVE_INFINITY;
		for (IndoorLocation node : nodes) {
			double n0lat = node.getLatitude();
			double n0lon = node.getLongitude();

			if (n0lat < latMin) // find minimum latitude
				latMin = n0lat;
			if (n0lon < lonMin) // find minimum longitude
				lonMin = n0lon;

			if (n0lat > latMax) // find maximum latitude
				latMax = n0lat;
			if (n0lon > lonMax) // find maximum longitude
				lonMax = n0lon;
		}

		lbBound = new IndoorLocation("left,bottom", "");
		lbBound.setLatitude(latMin);
		lbBound.setLongitude(lonMin);

		rtBound = new IndoorLocation("right,top", "");
		rtBound.setLatitude(latMax);
		rtBound.setLongitude(lonMax);
	}

	/**
	 * Load tiles for given zoom level, from sdcard or http. Create a tile object for each visible tile, and store it in
	 * a bitmap in memory.
	 * 
	 * @param zoomlevel
	 *            the zoom level of the tiles to fetch
	 * @param lbBoundary
	 *            left bottom boundary of the path, a GPS coordinate
	 * @param rtBoundary
	 *            right top boundary of the path, a GPS coordinate
	 * @return an array of loaded tiles if successful
	 */
	public void _c_loadTiles(int zoomlevel) {
		this.zoomlevel = zoomlevel;
		new DownloadTilesTask().execute();
	}

	/**
	 * To avoid android.os.NetworkOnMainThreadException
	 * 
	 * @author Paul Smith, code@uvwxy.de: Jun 6, 2012
	 * 
	 */
	private class DownloadTilesTask extends AsyncTask<URL, Integer, Long> {
		protected Long doInBackground(URL... urls) {
			// TODO: exception if not called a & b
			// source: http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames

			// find out which tiles to get
			int x0 = getTileX(lbBound.getLongitude(), zoomlevel); // point 0 left top
			int y0 = getTileY(rtBound.getLatitude(), zoomlevel);
			int x1 = getTileX(rtBound.getLongitude(), zoomlevel); // point 1 right top
			int y2 = getTileY(lbBound.getLatitude(), zoomlevel); // point 2 left bottom

			int diffX = x1 - x0;
			int diffY = y2 - y0;

			int arraySize = (diffX + 3) * (diffY + 3);
			arraySize = arraySize <= 0 ? 1 : arraySize; // fix size if only one tile needed
			tiles = new Tile[arraySize];

			// check if dir exists
			File dir = new File(Environment.getExternalStorageDirectory(), "footpath/");
			if (!dir.exists()) {
				dir.mkdir();
			}
			int counter = 0;
			int downloadedTiles = 0;
			boolean downloadFailed = false;
			for (int x = -1; x <= diffX + 1; x++) { // x/y = -1 to have some more tiles around the building
				for (int y = -1; y <= diffY + 1; y++) { // because some parts of the building might be overlapping
														// and thus not visible (nicer graphics)
					File f = new File(Environment.getExternalStorageDirectory(), "footpath/tile." + zoomlevel + "."
							+ (x0 + x) + "." + (y0 + y) + ".png");
					if (f.exists()) {
						// file existed -> read it
						tiles[counter] = new Tile(zoomlevel, x0 + x, y0 + y, BitmapFactory.decodeFile(f.getPath()));
						Log.i("FOOTPATH", "Loading from sd footpath/tile." + zoomlevel + "." + (x0 + x) + "."
								+ (y0 + y) + ".png)");
					} else {
						// file did not exist -> download it
						URL u = null;
						try {
							u = new URL("http://tile.openstreetmap.org/" + zoomlevel + "/" + (x0 + x) + "/" + (y0 + y)
									+ ".png");
						} catch (MalformedURLException e) {
							Log.i("FOOTPATH", "URL creation failed (http://tile.openstreetmap.org/" + zoomlevel + "/"
									+ (x0 + x) + "/" + (y0 + y) + ".png)" + "\n" + e);
						}
						try {
							HttpURLConnection c = (HttpURLConnection) u.openConnection();
							c.setDoInput(true);
							InputStream is = c.getInputStream();
							tiles[counter] = new Tile(zoomlevel, x0 + x, y0 + y, BitmapFactory.decodeStream(is));
							Log.i("FOOTPATH", "Download succeeded (" + u.toString() + ")");
							downloadedTiles++;
							// -> and save it
							try {
								FileOutputStream out = new FileOutputStream(f);
								tiles[counter].getBitmap().compress(Bitmap.CompressFormat.PNG, 90, out);
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
			if (downloadFailed) {
				// TODO: Use Exception: Give feedback on failed download of map tiles
			}

			return Long.valueOf(downloadedTiles);
		}

		protected void onProgressUpdate(Integer... progress) {
			// setProgressPercent(progress[0]);
		}

		protected void onPostExecute(Long result) {
			// showDialog("Downloaded " + result + " bytes");
		}
	}

	/**
	 * Calculate the tile x coordinate from longitude
	 * 
	 * @param lon
	 *            longitude
	 * @param zoom
	 *            zoom level
	 * @return the x coordinate of the longitude on the tile server
	 */
	private int getTileX(double lon, int zoom) {
		return (int) Math.floor((lon + 180) / 360 * (1 << zoom));
	}

	/**
	 * Calculate the tile x coordinate from latitude
	 * 
	 * @param lat
	 *            latitude
	 * @param zoom
	 *            zoom level
	 * @return the x coordinate of the latitude on the tile server
	 */
	private int getTileY(double lat, int zoom) {
		return (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat)))
				/ Math.PI)
				/ 2 * (1 << zoom));
	}

	@Override
	public void drawToCanvas(Canvas canvas, IndoorLocation center, Rect boundingBox, double pixelsPerMeter,
			Paint pLine, Paint pDots) {

		int w = boundingBox.width() / 2 + boundingBox.left;
		int h = boundingBox.height() / 2 + boundingBox.top;

		if (tiles != null) {
			for (int i = 0; i < tiles.length; i++) {
				if (tiles[i] != null) {
					int[] iLT = GeoUtils.convertToPixelLocation(tiles[i].getLatLonPosLeftTop(), center, pixelsPerMeter);
					int[] iRB = GeoUtils.convertToPixelLocation(tiles[i].getLatLonPosRightBottom(), center,
							pixelsPerMeter);

					RectF destRect = new RectF(w + iLT[0], h + iLT[1], w + iRB[0], h + iRB[1]);
					if (i == 0)
						Log.i("FOOTPATH", "ATile: " + i + " : " + destRect);
					if (i == tiles.length - 1)
						Log.i("FOOTPATH", "BTile: " + i + " : " + destRect);

					if (tiles[i].getBitmap() != null) {
						canvas.drawBitmap(tiles[i].getBitmap(), null, destRect, null);
					}
				}
			}
		} else {
			// TODO: draw empty tile symbol?
		}
	}
}
