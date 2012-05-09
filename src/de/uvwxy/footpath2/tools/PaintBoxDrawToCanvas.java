package de.uvwxy.footpath2.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class PaintBoxDrawToCanvas extends PaintBox {
	private DrawToCanvas canvasPainter;

	public PaintBoxDrawToCanvas(Context context) {
		super(context);
	}

	public void setCanvasPainter(DrawToCanvas dtc) {
		this.canvasPainter = dtc;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Rect boundingBox = new Rect(0, 0, getWidth(), getHeight());
		Paint blackPaint = new Paint();
		blackPaint.setColor(Color.BLACK);
		canvas.drawRect(boundingBox, blackPaint);
		Paint pLine = new Paint();
		pLine.setColor(Color.RED);
		Paint pDots = new Paint();
		if (canvasPainter != null) {
			canvasPainter.drawToCanvas(canvas, null, boundingBox, 9, pLine,
					pDots);

		} else {
			canvas.drawText("Loading", getWidth() / 2, getHeight() / 2, pLine);
		}
	}

}
