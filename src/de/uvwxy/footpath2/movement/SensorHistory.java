package de.uvwxy.footpath2.movement;

import java.util.LinkedList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.util.Log;

public class SensorHistory extends LinkedList<SensorTriple> {
	private static final long serialVersionUID = 5777397679052032803L;
	private final float backLogMillis = 3000; // show three seconds of data

	@Override
	public SensorTriple get(int location) {
		if (location < 0 || location >= size()){
			return null;
		}
		return super.get(location);
	}


	public void drawToCanvas(Canvas canvas, Location center, Rect boundingBox,
			double pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots) {
		if (boundingBox == null || size() < 2) {
			return;
		}

		int i = size() - 2;
		SensorTriple tempRight = get(i + 1);
		SensorTriple tempLeft = get(i);
		long max = System.currentTimeMillis();
		long diff = max - tempLeft.ts;

		int height = boundingBox.height();
		int width = boundingBox.width();

		float pixelsPerMilli = width / getBackLogMillis();
		float pixelsPerValue = (float) ((height / 2) / pixelsPerMeterOrMaxValue);

		float y0r, y0g, y0b, y1r, y1g, y1b, x0, x1;
		int yoffset = height / 2 + boundingBox.top;
		while ((diff) <= getBackLogMillis()) {
			x1 = (-(max - tempRight.ts) * pixelsPerMilli) + boundingBox.right;
			x0 = (-diff * pixelsPerMilli) + boundingBox.right;

			y1r = yoffset - (tempRight.values[0] * pixelsPerValue);
			y1g = yoffset - (tempRight.values[1] * pixelsPerValue);
			y1b = yoffset - (tempRight.values[2] * pixelsPerValue);

			y0r = yoffset - (tempLeft.values[0] * pixelsPerValue);
			y0g = yoffset - (tempLeft.values[1] * pixelsPerValue);
			y0b = yoffset - (tempLeft.values[2] * pixelsPerValue);

			pLine.setColor(Color.RED);
			canvas.drawLine(x0, y0r, x1, y1r, pLine);
			pLine.setColor(Color.GREEN);
			canvas.drawLine(x0, y0g, x1, y1g, pLine);
			pLine.setColor(Color.BLUE);
			canvas.drawLine(x0, y0b, x1, y1b, pLine);

			// Log.i("LOCMOV", "Drawing element: " + temp);
			// Log.i("LOCMOV", "Drawing element: " + get(i+1));

			i--;
			if (i < 0) {
				break;
			}
			tempLeft = get(i);
			tempRight = get(i+1);
			if (tempLeft == null || tempRight == null){
				break;
			}
			diff = max - tempLeft.ts;
		}

		canvas.drawText("Size: " + size()*25 + " byte", 16, 16, pDots);

		// Log.i("LOCMOV", "Added type: " + t.values[0] + "/" + t.values[1] +
		// "/" + t.values[2]);
		// Log.i("LOCMOV", "Elements drawn: " + test);

	}


	public float getBackLogMillis() {
		return backLogMillis;
	}
}
