package de.uvwxy.flowpath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * HANDLES: Display of MVD data / Speed
 * 
 * CURRENTLY: Estimation of Speed. (move this somewhere else during refactoring)
 * No automatic redraw. Has to be called manually.
 * 
 * @author paul
 * 
 */
public class PaintBoxMVs extends SurfaceView implements SurfaceHolder.Callback,
		MVDTrigger {
	private float[][][] mvs = null;
	private int mvCount = 0;

	private boolean surface_ok = false;
	private boolean paintMVs = false;

	private final int FRAME_NUM = FlowPathConfig.PIC_FPS * 2;

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		surface_ok = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		surface_ok = false;
	}

	public PaintBoxMVs(Context context) {
		super(context);
		getHolder().addCallback(this);
	}

	public void paintMVs() {
		if (!surface_ok) {
			return;
		}
		Canvas c;
		SurfaceHolder sf = getHolder();
		c = null;
		try {
			c = sf.lockCanvas(null);
			synchronized (sf) {
				onDraw(c);
			}
		} finally {
			if (c != null) {
				sf.unlockCanvasAndPost(c);
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		long tsDiff = System.currentTimeMillis() - tsLast;
		float fps = 1000.0f / tsDiff;

		canvas.drawColor(Color.BLACK);
		Paint p = new Paint();
		p.setColor(Color.WHITE);

		if (mvs == null) {
			canvas.drawText("C (@" + FlowPathConfig.PIC_FPS + "): " + mvCount
					+ " -> " + fps + "fps", 0, 16, p);
			canvas.drawText("NO MVDS!", getWidth() / 2, getHeight() / 2, p);
		} else {

			canvas.drawText("C (@" + FlowPathConfig.PIC_FPS + "): " + mvCount
					+ " -> " + fps + "fps", 32, 16, p);

			if (paintMVs) {
				drawAllMVs(canvas, p, mvs, 16.0f);
			}

			int x_len = mvs.length;
			int y_len = mvs[0].length;
			float[] avg = mvdAverage(mvs);
			float x_sum = avg[0];
			float y_sum = avg[1];

			drawAvgVector(canvas, p, x_sum, y_sum, x_len * 8, y_len * 8);

			drawHistogramm(canvas, p, y_sum);

//			float[][][] f = mvdFields(mvs, 4, 3);
//			paintFields(canvas, p, f, 16.0f, 300, 300);

			int[][] m = mvdHeatMap(mvs);
			int size = 128;
			if (this.getHeight()>512){
				size = 256;
			}
			heatMaps[++hmPtr % numOfHeatMaps] = m;
			paintHeatMap(canvas, p, m, 400, 280, size);
			paintHeatMaps(canvas, p, heatMaps, 660, 280, size);
		}
		tsLast = System.currentTimeMillis();
	}

	private int numOfHeatMaps = 30;
	private int[][][] heatMaps = new int[numOfHeatMaps][][];
	private int hmPtr = 0;

	private void paintHeatMaps(Canvas c, Paint p, int[][][] maps, int xoffset,
			int yoffset, int size) {
		int x_len = maps[hmPtr % numOfHeatMaps].length;
		int y_len = maps[hmPtr % numOfHeatMaps][0].length;

		int f = size / 32;

		c.drawRect(xoffset - 1, yoffset - 1, xoffset + size + 2, yoffset + size
				+ 2, p);
		
		int[][] accumulatedMap = new int[x_len][y_len];
		
		for (int x = 0; x < x_len; x++) {
			for (int y = 0; y < y_len; y++) {
				// c.drawText("" + map[x][y], x*scale + xoffset, y*scale +
				// yoffset, p);
				p.setColor(Color.BLACK);
				int v = 0;
				for (int i = 0; i < numOfHeatMaps; i++) {
					if (maps[i] != null)
						accumulatedMap[x][y] += maps[i][x][y];
				}
			}
		}
		
		int totalSum = x_len*y_len;
		int rowSum = 0;
		
		int s = 0;
		
		for (int y = 0; y < y_len; y++) {
			for (int x = 0; x < x_len; x++) {
				
				
				
				int v = accumulatedMap[x][y];
				rowSum+=v;
				
				if (s == 0 && rowSum > totalSum *4) {
					s = y;
				}
				
				p.setColor(Color.BLACK);
				if (v > 1) {
					p.setColor(Color.DKGRAY);
				}
				if (v > 4) {
					p.setColor(Color.GRAY);
				}
				if (v > 8) {
					p.setColor(Color.LTGRAY);
				}
				if (v > 16) {
					p.setColor(Color.WHITE);
				}
				if (v > 24) {
					p.setColor(Color.RED);
				}
				c.drawRect(xoffset + x * f, yoffset + y * f, xoffset + (x + 1)
						* f, yoffset + (y + 1) * f, p);
				
				
			}
			
		}
		p.setColor(Color.GREEN);
		c.drawLine(xoffset - 2, yoffset + size / 2, xoffset + size + 2, yoffset
				+ size / 2, p);
		p.setColor(Color.RED);
		c.drawLine(xoffset-1,yoffset + + s*f,xoffset+size+2, yoffset  + s*f, p);
	}

	private void paintHeatMap(Canvas c, Paint p, int[][] map, int xoffset,
			int yoffset, int size) {
		int x_len = map.length;
		int y_len = map[0].length;
		int f = size / 32;
		c.drawRect(xoffset - 1, yoffset - 1, xoffset + size + 2, yoffset + size
				+ 2, p);
		for (int x = 0; x < x_len; x++) {
			for (int y = 0; y < y_len; y++) {
				// c.drawText("" + map[x][y], x*scale + xoffset, y*scale +
				// yoffset, p);
				p.setColor(Color.BLACK);
				if (map[x][y] > 1) {
					p.setColor(Color.DKGRAY);
				} else if (map[x][y] > 2) {
					p.setColor(Color.GRAY);
				} else if (map[x][y] > 4) {
					p.setColor(Color.LTGRAY);
				} else if (map[x][y] > 6) {
					p.setColor(Color.WHITE);
				} else if (map[x][y] > 8) {
					p.setColor(Color.RED);
				}
				c.drawRect(xoffset + x * f, yoffset + y * f, xoffset + (x + 1)
						* f, yoffset + (y + 1) * f, p);
			}
		}
	}

	private int[][] mvdHeatMap(float[][][] mvs) {
		int x_len = mvs.length;
		int y_len = mvs[0].length;

		int[][] ret = new int[32][32];

		for (int x = 0; x < x_len; x++) {
			for (int y = 0; y < y_len; y++) {
				int mvx = (int) mvs[x][y][0];
				int mvy = (int) mvs[x][y][1];
				mvx += 16;
				mvy += 16;
				ret[mvx][mvy]++;
			}
		}

		return ret;
	}

	private void paintFields(Canvas c, Paint p, float[][][] fields,
			float scale, int xoffset, int yoffset) {
		if (fields == null)
			return;

		int x_len = fields.length;
		int y_len = fields[0].length;

		for (int x = 0; x < x_len; x++) {
			for (int y = 0; y < y_len; y++) {
				c.drawLine(xoffset + x * scale, yoffset + y * scale, xoffset
						+ x * scale + fields[x][y][0], yoffset + y * scale
						+ fields[x][y][1], p);
			}
		}
	}

	private float[][][] mvdFields(float[][][] mvs, int blockDivX, int blockDivY) {
		int x_len = mvs.length;
		int y_len = mvs[0].length;

		if (x_len % blockDivX != 0 || y_len % blockDivY != 0) {
			Log.i("FLOWPATH", "ÖRKS " + x_len + " " + y_len + " "
					+ (x_len % blockDivX) + " " + (y_len % blockDivY));
			return null;
		}

		if (blockDivX == 1 && blockDivY == 1) {
			return mvs;
		}

		float[][][] ret = new float[blockDivX][blockDivY][2];

		int blockWidth = x_len / blockDivX;
		int blockHeight = y_len / blockDivY;

		for (int y = 0; y < blockDivY; y++) {
			for (int x = 0; x < blockDivX; x++) {
				float sumx = 0.0f;
				float sumy = 0.0f;
				for (int yr = 0; yr < blockHeight; yr++) {
					for (int xr = 0; xr < blockWidth; xr++) {
						sumx += mvs[x * blockWidth + xr][y * blockHeight + yr][0];
						sumy += mvs[x * blockWidth + xr][y * blockHeight + yr][1];
					}
				}
				ret[x][y][0] = sumx / (blockWidth * blockHeight);
				ret[x][y][1] = sumy / (blockWidth * blockHeight);
			}
		}

		return ret;
	}

	private void drawAvgVector(Canvas canvas, Paint p, float x_sum,
			float y_sum, int sizex, int sizey) {
		if (y_sum < 0) {
			p.setColor(Color.GREEN);
			canvas.drawText("MOVING ", 256, 16, p);
		} else {
			p.setColor(Color.RED);
			canvas.drawText("NOT MOVING ", 256, 16, p);
		}
		// paint average motion vector
		canvas.drawLine(sizex + 16.0f, 16 + sizex + 16.0f, sizex + x_sum * 16
				+ 16.0f, 16 + sizey + y_sum * 16 + 16.0f, p);
	}

	private float[] mvdAverage(float[][][] mvs) {
		int x_len = mvs.length;
		int y_len = mvs[0].length;

		float x_sum = 0.0f;
		float y_sum = 0.0f;

		for (int x = 0; x < x_len; x++) {
			for (int y = 0; y < y_len; y++) {
				x_sum += mvs[x][y][0];
				y_sum += mvs[x][y][1];
			}
		}

		x_sum /= x_len * y_len;
		y_sum /= x_len * y_len;

		float[] ret = { x_sum, y_sum };
		return ret;
	}

	private long tsLast = 0;
	float[] y_sum_avgs = new float[FRAME_NUM]; // average speed over the last
												// FRAME_NUM frames
	int y_sum_avgs_ptr = 0;

	private void drawHistogramm(Canvas c, Paint p, float y_sum) {
		y_sum_avgs[++y_sum_avgs_ptr % FRAME_NUM] = y_sum;

		float y_sec_sum_avg = 0;

		for (float f : y_sum_avgs) {
			y_sec_sum_avg += f;
		}

		y_sec_sum_avg /= FlowPathConfig.PIC_FPS;

		int barWidth = 8;
		int barHeight = 120;
		// draw average
		drawSpeedBar(c, p, y_sec_sum_avg, barWidth * 2, barHeight, 16, 16);

		// draw histogram
		for (int i = 0; i < FRAME_NUM; i++) {
			drawSpeedBar(c, p,
					y_sum_avgs[(y_sum_avgs_ptr + 1 + i) % FRAME_NUM], barWidth,
					barHeight, 40 + (barWidth + 2) * (i + 1), 16);

		}
	}

	private void drawAllMVs(Canvas c, Paint p, float[][][] mvs, float scale) {
		int x_len = mvs.length;
		int y_len = mvs[0].length;

		float mvx = 0;
		float mvy = 0;

		for (int x = 0; x < x_len; x++) {
			for (int y = 0; y < y_len; y++) {

				if (mvy < 0) {
					p.setColor(Color.GREEN);
				} else {
					p.setColor(Color.GRAY);
				}
				c.drawLine(x * scale + scale, 16 + y * scale + scale, x * scale
						+ mvx + scale, scale + y * scale + mvy + scale, p);

			}
		}

	}

	private void drawSpeedBar(Canvas canvas, Paint p, float value, int width,
			int height, int xOffset, int yOffset) {

		if (value < 0) {
			p.setColor(Color.GREEN);
		} else {
			p.setColor(Color.BLUE);
		}

		// draw vertical bar:
		p.setStrokeWidth(width);
		canvas.drawLine(xOffset, yOffset + height, xOffset, yOffset + height
				+ value * (height / 16), p);

		p.setStrokeWidth(1.0f);
		p.setColor(Color.BLUE);

		canvas.drawLine(xOffset, yOffset + height + height, xOffset + width,
				yOffset + height + height, p);

		p.setColor(Color.GREEN);
		canvas.drawLine(xOffset, yOffset, xOffset + width, yOffset, p);
	}

	@Override
	public void processMVData(long now_ms, float[][][] mvds) {
		this.mvs = mvds;
		paintMVs();
		mvCount++;
	}
}
