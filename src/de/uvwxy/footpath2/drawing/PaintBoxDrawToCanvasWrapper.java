package de.uvwxy.footpath2.drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

public class PaintBoxDrawToCanvasWrapper extends PaintBox {
	private DrawToCanvasWrapper canvasPainter;
	private final Paint p = new Paint();

	public PaintBoxDrawToCanvasWrapper(Context context) {
		super(context);
	}

	public PaintBoxDrawToCanvasWrapper(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PaintBoxDrawToCanvasWrapper(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setCanvasPainter(DrawToCanvasWrapper dtc) {
		this.canvasPainter = dtc;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (canvasPainter != null) {
			canvasPainter.drawToCanvas(canvas);

		} else {
			p.setColor(Color.RED);
			canvas.drawText("Loading", getWidth() / 2, getHeight() / 2, p);
		}
	}

}
