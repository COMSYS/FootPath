package de.uvwxy.paintbox;

import android.content.Context;
import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * A class managing the creation of a canvas to draw on.
 * 
 * Usage:
 * 
 * 	Create a class which overrides onDraw. Once the surface is created onDraw()
 * 	is called in an infinite loop from a PaintThread. Destroying the surface
 * 	stops the background thread calling onDraw().
 * 
 * @author Paul Smith
 * 
 */
public abstract class PaintBox extends SurfaceView implements SurfaceHolder.Callback {

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

	PaintThread pThread;

	public PaintBox(Context context) {
		super(context);
		getHolder().addCallback(this);
	}
}
