package de.uvwxy.footpath2.drawing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.tools.GeoUtils;

public class DrawBitmapToLocation implements DrawToCanvas {
	private Bitmap bmp;
	private float ppm; // pixels per meter
	private float rotation = 0.0f;
	private IndoorLocation loc;
	private Paint p = new Paint();

	public DrawBitmapToLocation(IndoorLocation l, Bitmap b, float ppm, boolean aa) {
		this.bmp = b;
		this.ppm = ppm;
		p.setAntiAlias(aa);
	}

	public Bitmap getBmp() {
		return bmp;
	}

	public void setBmp(Bitmap bmp) {
		this.bmp = bmp;
	}

	public float getPpm() {
		return ppm;
	}

	public void setPpm(float ppm) {
		this.ppm = ppm;
	}

	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public IndoorLocation getLoc() {
		return loc;
	}

	public void setLoc(IndoorLocation loc) {
		this.loc = loc;
	}

	@Override
	public void drawToCanvas(Canvas canvas, IndoorLocation center, Rect boundingBox, double pixelsPerMeterOrMaxValue,
			Paint pLine, Paint pDots) {
		if (loc == null)
			return;
		int w = boundingBox.width() / 2 + boundingBox.left;
		int h = boundingBox.height() / 2 + boundingBox.top;
		int[] apix = GeoUtils.convertToPixelLocation(loc, center, pixelsPerMeterOrMaxValue);

		Matrix m = new Matrix();
		//m.setScale((float) (ppm / pixelsPerMeterOrMaxValue), (float) (ppm / pixelsPerMeterOrMaxValue));
		m.postRotate(rotation, bmp.getWidth() / 2, bmp.getHeight() / 2);
		m.postTranslate(apix[0], apix[1]);
		canvas.drawBitmap(bmp, m, p);
		// canvas.drawCircle(w + apix[0], h + apix[1], (float) (pixelsPerMeterOrMaxValue * 0.5f), pDots);
	}
}
