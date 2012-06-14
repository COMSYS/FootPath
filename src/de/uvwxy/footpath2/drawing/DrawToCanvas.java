package de.uvwxy.footpath2.drawing;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import de.uvwxy.footpath2.map.IndoorLocation;

/**
 * This interface is used as a common interface to drawing stuff onto a canvas. We use this interface, so objects have a
 * common interface to be able to paint themselves on to a canvas.
 * 
 * Example: IndoorLocationList, StepDetectionImpl implement this. We can call both objects to draw themselves onto the
 * same canvas in a layered approach. The center should be given for locations, to adjust their painting location
 * according to this center location and the number of pixels per meter. The bounding box should be an honored space to
 * only which it is allowed to draw, i.e. draw multiple objects into corners of a canvas.
 * 
 * TODO: Not all implementations of this function respect this so far.
 * 
 * If you have to draw a history of values, i.e. in the StepDetectionImplementation you have to give the maximum value
 * that should be represented at the top/bottom of the screen.
 * 
 * The two paints should give a possibility of settings some colors from outside.
 * @author Paul Smith
 * 
 */
public interface DrawToCanvas {
	public void drawToCanvas(Canvas canvas, IndoorLocation center, Rect boundingBox, double pixelsPerMeterOrMaxValue,
			Paint pLine, Paint pDots);
}
