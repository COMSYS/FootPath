package de.uvwxy.footpath2.drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * A class managing the creation of a canvas to draw on. This is abstract.
 * Create a class which overrides onDraw. The result is a clean class where the
 * drawing-only code will reside.
 * 
 * @author Paul
 * 
 */
public abstract class PaintBox extends SurfaceView implements SurfaceHolder.Callback {

	PaintThread pThread;
	
	public PaintBox(Context context) {
		super(context);
		getHolder().addCallback(this);
	}

	public PaintBox(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
	}

	public PaintBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		getHolder().addCallback(this);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		pThread = new PaintThread(getHolder(), this);
		pThread.setRunning(true);
		pThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		boolean retry = true;
		pThread.setRunning(false);
		while (retry) {
			try {
				pThread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	protected abstract void onDraw(Canvas canvas);
	

	

}
