package de.uvwxy.footpath2.movement.steps;

import java.util.LinkedList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import de.uvwxy.footpath2.movement.MovementDetection;
import de.uvwxy.footpath2.movement.SensorHistory;
import de.uvwxy.footpath2.movement.SensorTriple;
import de.uvwxy.footpath2.tools.DrawToCanvas;

public class StepDetection extends MovementDetection implements
		SensorEventListener, DrawToCanvas {
	private static final String PREF_ID = "SENSOR_STEP_HISTORY_SETTINS";
	private Handler mHandler = new Handler();
	private long delayMillis = 1000 / 30;

	private double stepPeak = 0.8;
	private double jumpPeak = 10;
	private int colorStep = Color.MAGENTA;
	private int colorJump = Color.YELLOW;
	private LinkedList<Step> steps = new LinkedList<Step>();
	private LinkedList<Jump> jumps = new LinkedList<Jump>();
	private SensorHistory linAccHistory = new SensorHistory();

	private int step_timeout_ms = 666;
	private int standing_timeout_ms = 1234;

	private static final int vhSize = 6;
	private float[][] stepDetectionWindow = new float[vhSize][];
	private int vhPointer = 0;

	private MovementType currentMovement = MovementType.STANDING;

	private Context context;

	public StepDetection(Context context) {
		super();
		this.context = context;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		stepPeak = settings.getFloat("stepPeak", 0.8f);
		jumpPeak = settings.getFloat("jumpPeak", 8.0f);
		step_timeout_ms = settings.getInt("step_timeout_ms", 666);
		standing_timeout_ms = settings.getInt("standing_timeout_ms", 1234);

	}

	public synchronized void _a_startMovementDetection() {
		super._a_startMovementDetection();
		// TODO: linAccHistory do sth
		handlerStepDetection.run();
	};

	public synchronized void _b1_pauseMovementDetection() {
		super._b1_pauseMovementDetection();
		// TODO: linAccHistory do sth
		mHandler.removeCallbacks(handlerStepDetection);

	};

	public synchronized void _b2_unPauseMovementDetection() {
		super._b2_unPauseMovementDetection();
		// TODO: linAccHistory do sth
		mHandler.removeCallbacks(handlerStepDetection);
		mHandler.postDelayed(handlerStepDetection, delayMillis);
	};

	public synchronized void _c_stopMovementDetection() {
		super._c_stopMovementDetection();
		// TODO: linAccHistory do sth
		mHandler.removeCallbacks(handlerStepDetection);

	};

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			linAccHistory.add(new SensorTriple(event.values, event.timestamp,
					event.sensor.getType()));
			
		}
	}

	public double getPeak() {
		return stepPeak;
	}

	public double getJumpPeak() {
		return jumpPeak;
	}

	public int getStepTimeOut() {
		return step_timeout_ms;
	}

	public int getStandingTimeOut() {
		return standing_timeout_ms;
	}

	public void setPeak(double peak) {
		stepPeak = peak;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putFloat("stepPeak", (float) peak);
		editor.commit();
	}

	public void setJumpPeak(double peak) {
		jumpPeak = peak;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putFloat("jumpPeak", (float) jumpPeak);
		editor.commit();
	}

	public void setStepTimeout(int timeout) {
		step_timeout_ms = timeout;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("step_timeout_ms", step_timeout_ms);
		editor.commit();
	}

	public void setStandingTimeout(int timeout) {
		standing_timeout_ms = timeout;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("standing_timeout_ms", standing_timeout_ms);
		editor.commit();
	}

	public long getLastStepTimeDelta() {
		return System.currentTimeMillis() - jumps.getLast().ts;
	}

	public int getNumSteps() {
		return steps.size();
	}

	public int getNumJumps() {
		return jumps.size();
	}

	public MovementType getCurrentMovement() {
		return currentMovement;
	}

	private boolean checkForStep(double peakSize, double lim) {
		// Add value to values_history

		int lookahead = 5;

		for (int t = 1; t <= lookahead; t++) {
			// catch nullpointer exception at the beginning
			if (stepDetectionWindow[(vhPointer - 1 - t + vhSize + vhSize)
					% vhSize] != null) {
				double check = stepDetectionWindow[(vhPointer - 1 - t + vhSize + vhSize)
						% vhSize][2]
						- stepDetectionWindow[(vhPointer - 1 + vhSize) % vhSize][2];
				if (check >= peakSize && check < lim) {
					// Log.i("JNR_LOCMOV", "Detected step with t = " + t
					// + ", peakSize = " + peakSize + " < " + check);
					return true;
				}
			}
		}
		return false;
	}

	private void addSensorData(float[] value) {
		stepDetectionWindow[vhPointer % vhSize] = value;
		vhPointer++;
		vhPointer = vhPointer % vhSize;
	}

	public void drawToCanvas(Canvas canvas, Location center, Rect boundingBox,
			double pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots) {
		long max = System.currentTimeMillis();
		drawSteps(canvas, boundingBox, pixelsPerMeterOrMaxValue, pLine, pDots,
				max);
		drawJumps(canvas, boundingBox, pixelsPerMeterOrMaxValue, pLine, pDots,
				max);
		canvas.drawText("Steps: " + steps.size() + " Jumps: " + jumps.size(),
				16, 48, pDots);

		String strState = "";
		switch (currentMovement) {
		case JUMPING:
			pDots.setColor(Color.YELLOW);
			strState = "JUMPING";
			break;
		case STANDING:
			pDots.setColor(Color.BLUE);
			strState = "STANDING";
			break;
		case WALKING:
			pDots.setColor(Color.GREEN);
			strState = "WALKING";
			break;
		}

		linAccHistory.drawToCanvas(canvas, center, boundingBox,
				pixelsPerMeterOrMaxValue, pLine, pDots);

		pDots.setTextSize(32);
		canvas.drawText("State: " + strState, 16, 96, pDots);
		// Log.i("LOCMOV", "Added type: " + t.values[0] + "/" + t.values[1] +
		// "/" + t.values[2]);
		// Log.i("LOCMOV", "Elements drawn: " + test);

	}

	private void drawSteps(Canvas canvas, Rect boundingBox,
			double pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots, long max) {
		if (steps.size() == 0) {
			return;
		}
		int height = boundingBox.height();
		int width = boundingBox.width();

		float pixelsPerMilli = width / linAccHistory.getBackLogMillis();

		float x0, x1;
		int i = steps.size() - 1;
		long diff = max - steps.getLast().ts;
		Step temp = steps.getLast();
		while ((diff) <= linAccHistory.getBackLogMillis()) {
			x0 = (-diff * pixelsPerMilli) + boundingBox.right;

			// Log.i("LOCMOV", "Drawing element: " + temp);
			// Log.i("LOCMOV", "Drawing element: " + get(i+1));
			pDots.setColor(colorStep);
			canvas.drawLine(x0, boundingBox.top, x0, boundingBox.bottom, pDots);

			i--;
			if (i < 0) {
				break;
			}
			temp = steps.get(i);
			if (temp == null) {
				break;
			}
			diff = max - temp.ts;
		}
	}

	private void drawJumps(Canvas canvas, Rect boundingBox,
			double pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots, long max) {
		if (jumps.size() == 0) {
			return;
		}
		int height = boundingBox.height();
		int width = boundingBox.width();

		float pixelsPerMilli = width / linAccHistory.getBackLogMillis();

		float x0, x1;
		int i = jumps.size() - 1;
		long diff = max - jumps.getLast().ts;
		Jump temp = jumps.getLast();
		while ((diff) <= linAccHistory.getBackLogMillis()) {
			x0 = (-diff * pixelsPerMilli) + boundingBox.right;

			// Log.i("LOCMOV", "Drawing element: " + temp);
			// Log.i("LOCMOV", "Drawing element: " + get(i+1));
			pDots.setColor(colorJump);
			canvas.drawLine(x0, boundingBox.top, x0, boundingBox.bottom, pDots);

			i--;
			if (i < 0) {
				break;
			}
			temp = jumps.get(i);
			if (temp == null) {
				break;
			}
			diff = max - temp.ts;
		}
	}

	private Runnable handlerStepDetection = new Runnable() {

		public void run() {
			// if start is called twice: we have "two" threads, i.e clean this
			mHandler.removeCallbacks(handlerStepDetection);
			if (linAccHistory.size() != 0) {
				addSensorData(linAccHistory.getLast().values);

				long t = System.currentTimeMillis();

				// Fixed Peak size.. bad bad bad! .. i.e. no per user/device
				// configuration
				if (checkForStep(jumpPeak, 10 * jumpPeak)) {
					// jump if no jumps before or outside of interval
					if (jumps.size() == 0
							|| t - jumps.getLast().ts > step_timeout_ms) {
						// detected a jump
						jumps.add(new Jump(t));
						currentMovement = MovementType.JUMPING;
					}
				} else if (checkForStep(stepPeak, jumpPeak)) {
					// step if no steps before or outside of interval
					if (steps.size() == 0
							|| t - steps.getLast().ts > step_timeout_ms) {
						// detected a step
						steps.add(new Step(t));
						currentMovement = MovementType.WALKING;
					}
				}

				// no movements if no steps/jumps detected at all, or not in the
				// standing_timeout_ms intervall

				if (currentMovement != MovementType.STANDING) {
					if (steps.size() != 0
							&& standing_timeout_ms <= (t - steps.getLast().ts)) {
						currentMovement = MovementType.STANDING;
					}
				}
			}
			mHandler.postDelayed(this, delayMillis);
		}
	};

}
