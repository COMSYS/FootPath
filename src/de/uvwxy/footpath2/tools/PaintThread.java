package de.uvwxy.footpath2.tools;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * A class to create a thread to repaint the graphics.
 * 
 * @author Paul Smith
 * 
 */
class PaintThread extends Thread {
	private SurfaceHolder surfaceHolder;
	private PaintBox pBox;
	private boolean bRunning = false;

	public PaintThread(SurfaceHolder surfaceHolder, PaintBox pBox) {
		this.surfaceHolder = surfaceHolder;
		this.pBox = pBox;
	}

	public void setRunning(boolean run) {
		bRunning = run;
	}

	@Override
	public void run() {
		Canvas c;
		while (bRunning) {
			c = null;
			try {
				c = surfaceHolder.lockCanvas(null);
				synchronized (surfaceHolder) {
					pBox.onDraw(c);
				}
			} finally {
				if (c != null) {
					surfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}