package de.uvwxy.footpath2.tools;

import android.graphics.PointF;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class PanZoomListener implements OnTouchListener {
	PointF start = new PointF();
	float oldZoomDistPixels = 1f;

	// use this flag to prevent the initial scale value destroy everything
	// usualy scale is somewhere around 1.0f, but initial around half screen size.
	boolean reset_first_scale_value = true;
	
	private PanZoomType state = PanZoomType.NONE;
	private boolean onTouchReturn = true;
	private PanZoomResult panZoomResult = new PanZoomResult();
	
	public PanZoomResult getPanZoomResult() {
		return panZoomResult;
	}
	
	public void setOnTouchReturn(boolean onTouchReturn) {
		this.onTouchReturn = onTouchReturn;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// Handle touch events here...
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			state = PanZoomType.PAN;
//			Log.i("FOOTPATH", "PanZoomListener: primary: " + event.getX() + "/" + event.getY());
			start.set(event.getX(), event.getY());
			
			panZoomResult.resetResult();
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			// second finger down
//			Log.i("FOOTPATH", "PanZoomListener: secondary: " + event.getX() + "/" + event.getY());
			state = PanZoomType.ZOOM;
			oldZoomDistPixels = 1f;
			reset_first_scale_value = true;
			panZoomResult.resetResult();
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			state = PanZoomType.NONE;

			panZoomResult.resetResult();
			break;
		case MotionEvent.ACTION_MOVE:
			switch (state) {
			case PAN:
				PointF pan = new PointF(event.getX() - start.x, event.getY() - start.y);
//				Log.i("FOOTPATH", "PanZoomListener: pan: " + pan.x + "/" + pan.y);
				start.set(event.getX(), event.getY());
				
				panZoomResult.type = PanZoomType.PAN;
				panZoomResult.x = pan.x;
				panZoomResult.y = pan.y;
				break;
			case ZOOM:
				float x = event.getX(0) - event.getX(1);
				float y = event.getY(0) - event.getY(1);
				float d = FloatMath.sqrt(x * x + y * y);
				float s = d / oldZoomDistPixels;
				if (reset_first_scale_value){
					s = 1f;
					reset_first_scale_value = false;
				}
//				Log.i("FOOTPATH", "PanZoomListener: scaled: " + (s));
				oldZoomDistPixels = d;
				
				panZoomResult.type = PanZoomType.ZOOM;
				panZoomResult.scale = s;
				break;
			}
			break;
		}
		return onTouchReturn; // indicate false, as this is to be done by calling function
	}
	


}
