package de.uvwxy.footpath.gui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import de.uvwxy.paintbox.PaintBox;

public class PaintBoxMVs extends PaintBox {
	float[][][] mvs = null;
	private int mvCount = 0;

	public PaintBoxMVs(Context context) {
		super(context);

	}

	public void updateMVs(float[][][] mvs) {
		this.mvs = mvs;
		mvCount++;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(Color.BLACK);
		Paint p = new Paint();
		p.setColor(Color.WHITE);

		if (mvs == null) {
			canvas.drawText("NO MVDS!", getWidth() / 2, getHeight() / 2, p);
		} else {

			int x_len = mvs.length;
			int y_len = mvs[0].length;

			float mvx = 0;
			float mvy = 0;

			canvas.drawText("C (@" + FlowPath.PIC_FPS + "): " + mvCount, 512,
					512, p);

			for (int x = 0; x < x_len; x++) {
				for (int y = 0; y < y_len; y++) {
					mvx = mvs[x][y][0];
					mvy = mvs[x][y][1];

					canvas.drawLine(x * 16 + 16.0f, y * 16 + 16.0f, x * 16
							+ mvx + 16.0f, y * 16 + mvy + 16.0f, p);
				}
			}

		}
	}

}
