package de.uvwxy.footpath2.drawing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * A class to create a thread to repaint the graphics.
 * 
 * @author Paul
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

	private int height = -1;
	private int width = -1;

	Bitmap buffer = null;
	Canvas bufferedCanvas = null;

	@Override
	public void run() {
		Canvas c;

		while (bRunning) {

			// draw onto back buffered canvas if everything ok. 
			if (height != -1 && width != -1 && bufferedCanvas != null)
				pBox.onDraw(bufferedCanvas);

			c = null;
			try {
				c = surfaceHolder.lockCanvas();
				synchronized (surfaceHolder) {
					if (c != null) {
						// if one of the following components go bozuk, update the buffers
						if (bufferedCanvas == null || buffer == null || c.getWidth() != width
								|| c.getHeight() != c.getHeight()) {
							width = c.getWidth();
							height = c.getHeight();
							buffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
							bufferedCanvas = new Canvas(buffer);
							Log.i("PB", "Created Bitmap");
						}

						c.drawBitmap(buffer, 0, 0, null);
					}
				}
			} finally {
				if (c != null) {
					surfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}