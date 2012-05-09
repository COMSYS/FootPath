package de.uvwxy.footpath2.tools;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;

public interface DrawToCanvas {
	public void drawToCanvas(Canvas canvas, Location center, Rect boundingBox,
			double pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots);
}
