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
	private float sizeInMeters; // pixels per meter
	private float rotation = 0.0f;
	private IndoorLocation loc;
	private Paint p = new Paint();

	public DrawBitmapToLocation(IndoorLocation l, Bitmap b, float ppm, boolean aa) {
		this.bmp = b;
		this.sizeInMeters = ppm;
		p.setAntiAlias(aa);
	}

	public Bitmap getBmp() {
		return bmp;
	}

	public void setBmp(Bitmap bmp) {
		this.bmp = bmp;
	}

	public float getPpm() {
		return sizeInMeters;
	}

	public void setPpm(float ppm) {
		this.sizeInMeters = ppm;
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

	private float scale_x, scale_y;
	private int[] apix = { 0, 0 };

	private Matrix move = new Matrix();
	private Matrix m = new Matrix();

	@Override
	public void drawToCanvas(Canvas canvas, IndoorLocation center, int ox, int oy, float pixelsPerMeterOrMaxValue,
			Paint pLine, Paint pDots) {
		if (loc == null)
			return;

		scale_x = sizeInMeters * (pixelsPerMeterOrMaxValue / bmp.getWidth());
		scale_y = sizeInMeters * (pixelsPerMeterOrMaxValue / bmp.getHeight());
		apix = GeoUtils.convertToPixelLocation(loc, center, pixelsPerMeterOrMaxValue);

		move.setTranslate(ox + apix[0] - bmp.getWidth() * scale_x * 0.5f, oy + apix[1] - bmp.getHeight() * scale_y * 0.5f);

		m.setRotate(rotation, bmp.getWidth() / 2, bmp.getHeight() / 2);
		m.postScale(scale_x, scale_y);
		m.postConcat(move);

		canvas.drawBitmap(bmp, m, p);
		// canvas.drawCircle(w + apix[0], h + apix[1], (float) (pixelsPerMeterOrMaxValue * 0.5f), pDots);
	}
}
