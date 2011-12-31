package de.uvwxy.footpath.gui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import de.uvwxy.paintbox.PaintBox;

public class PaintBoxMVs extends PaintBox {
	double[][][] mvs = null;
	private int mvCount = 0;
	public PaintBoxMVs(Context context) {
		super(context);

	}

	public void updateMVs(double[][][] mvs) {
		this.mvs = mvs;
		mvCount++;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(Color.BLACK);
		Paint p = new Paint();
		p.setColor(Color.WHITE);
		
		if (mvs == null) {
			canvas.drawText("NO MVDS!", getWidth()/2, getHeight()/2, p);
		} else {
		
//			int x_len = mvs.length;
//			int y_len = mvs[0].length;
//
//			double mvx = 0;
//			double mvy = 0;
			
			canvas.drawText("C: " + mvCount , 512, 512, p);
			
//			for (int x = 0; x < x_len; x++) {
//				for (int y = 0; y < y_len; y++) {
//					mvx = mvs[x][y][0];
//					mvy = mvs[x][y][1];
//
//					canvas.drawLine((float) (x * 16) + 16.0f,
//							(float) (y * 16) + 16.0f,
//							(float) (x * 16 + mvx) + 16.0f,
//							(float) (y * 16 + mvy) + 16.0f, p);
//				}
//			}

		}
	}

}
