package de.uvwxy.footpath2.tools;

import android.graphics.Canvas;

/**
 * Add a class implementing this interface to the PaintBoxDrawToCanvasWrapper. Then set the class as the canvas painter
 * of the paint box (setCanvasPainter(..)).
 * 
 * The drawToCanvas(Canvas canvas) function will be called by a background thread as "fast as possible".
 * 
 * Draw on the supplied canvas object, i.e. passing the canvas to a DrawToCanvas interface.
 * 
 * @author Paul Smith
 * 
 */
public interface DrawToCanvasWrapper {
	public void drawToCanvas(Canvas canvas);
}
