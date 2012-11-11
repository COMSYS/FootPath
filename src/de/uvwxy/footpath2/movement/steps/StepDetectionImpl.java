package de.uvwxy.footpath2.movement.steps;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.util.Log;
import de.uvwxy.footpath2.drawing.DrawToCanvas;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.movement.MovementDetection;
import de.uvwxy.footpath2.movement.SensorHistory;
import de.uvwxy.footpath2.movement.SensorTriple;
import de.uvwxy.footpath2.movement.StepEventListener;

public class StepDetectionImpl extends MovementDetection implements SensorEventListener, StepDetection, DrawToCanvas {
	private static final String PREF_ID = "SENSOR_STEP_HISTORY_SETTINS";
	private final Handler mHandler = new Handler();
	private final long delayMillis = 1000 / 30;

	private double stepPeak = 0.8;
	private double jumpPeak = 10;
	private final int colorStep = Color.MAGENTA;
	private final int colorJump = Color.YELLOW;
	private final List<Step> steps = new LinkedList<Step>();
	private final List<Jump> jumps = new LinkedList<Jump>();
	private final SensorHistory linAccHistory = new SensorHistory();

	private int step_timeout_ms = 666;
	private int standing_timeout_ms = 1234;

	private static final int vhSize = 6;
	private final float[][] stepDetectionWindow = new float[vhSize][];
	private int vhPointer = 0;

	private float[] bearing;

	private MovementType currentMovement = MovementType.STANDING;

	private final Context context;
	private float[] orientationGravityVals = { 1.0f, 0.0f, 0.0f };

	
	public StepDetectionImpl(Context context) {
		super();
		this.context = context;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		stepPeak = settings.getFloat("stepPeak", 0.8f);
		jumpPeak = settings.getFloat("jumpPeak", 8.0f);
		step_timeout_ms = settings.getInt("step_timeout_ms", 666);
		standing_timeout_ms = settings.getInt("standing_timeout_ms", 1234);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#_a_startMovementDetection()
	 */
	@Override
	public synchronized void _a_startMovementDetection() {
		// TODO: linAccHistory do sth
		handlerStepDetection.run();
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#_b1_pauseMovementDetection()
	 */
	@Override
	public synchronized void _b1_pauseMovementDetection() {
		// TODO: linAccHistory do sth
		mHandler.removeCallbacks(handlerStepDetection);

	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#_b2_unPauseMovementDetection()
	 */
	@Override
	public synchronized void _b2_unPauseMovementDetection() {
		// TODO: linAccHistory do sth
		mHandler.removeCallbacks(handlerStepDetection);
		mHandler.postDelayed(handlerStepDetection, delayMillis);
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#_c_stopMovementDetection()
	 */
	@Override
	public synchronized void _c_stopMovementDetection() {
		// TODO: linAccHistory do sth
		mHandler.removeCallbacks(handlerStepDetection);

	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#onSensorChanged(android.hardware.SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
			orientationGravityVals = event.values.clone();
		} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			linAccHistory.add(new SensorTriple(event.values.clone(), event.timestamp, event.sensor.getType()));
		} else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) { // TODO: TYPE_ORIENTATION is deprecated.
			bearing = event.values;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#getPeak()
	 */
	@Override
	public double getPeak() {
		return stepPeak;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#getJumpPeak()
	 */
	@Override
	public double getJumpPeak() {
		return jumpPeak;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#getStepTimeOut()
	 */
	@Override
	public int getStepTimeOut() {
		return step_timeout_ms;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#getStandingTimeOut()
	 */
	@Override
	public int getStandingTimeOut() {
		return standing_timeout_ms;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#setPeak(double)
	 */
	@Override
	public void setPeak(double peak) {
		stepPeak = peak;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putFloat("stepPeak", (float) peak);
		editor.commit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#setJumpPeak(double)
	 */
	@Override
	public void setJumpPeak(double peak) {
		jumpPeak = peak;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putFloat("jumpPeak", (float) jumpPeak);
		editor.commit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#setStepTimeout(int)
	 */
	@Override
	public void setStepTimeout(int timeout) {
		step_timeout_ms = timeout;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("step_timeout_ms", step_timeout_ms);
		editor.commit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#setStandingTimeout(int)
	 */
	@Override
	public void setStandingTimeout(int timeout) {
		standing_timeout_ms = timeout;
		SharedPreferences settings = context.getSharedPreferences(PREF_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("standing_timeout_ms", standing_timeout_ms);
		editor.commit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#getLastStepTimeDelta()
	 */
	@Override
	public long getLastStepTimeDelta() {
		int size = jumps.size();
		if (jumps.size() == 0) {
			return -1;
		}
		Jump last = jumps.get(size - 1);
		return System.currentTimeMillis() - last.ts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#getNumSteps()
	 */
	@Override
	public int getNumSteps() {
		return steps.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#getNumJumps()
	 */
	@Override
	public int getNumJumps() {
		return jumps.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uvwxy.footpath2.movement.steps.StepDetection#getCurrentMovement()
	 */
	@Override
	public MovementType getCurrentMovement() {
		return currentMovement;
	}

	/**
	 * compute normalized orientation of phone<br>
	 * the axis are the same as at the linear accelerometer
	 * 
	 * @return
	 */
	private float[] getOrientation() {
		// TOOD: paul: I encountered a null pointer here (twice):
		final float sum = orientationGravityVals[0] + orientationGravityVals[1] + orientationGravityVals[2];
		final float[] retVal = new float[3];

		retVal[0] = orientationGravityVals[0] / sum;
		retVal[1] = orientationGravityVals[1] / sum;
		retVal[2] = orientationGravityVals[2] / sum;

		return retVal;
	}

	/**
	 * get diffs for each axis & multiply with orientation this will be checked against the threshhold
	 * 
	 * @param peakSize
	 * @param lim
	 * @return
	 */
	private boolean checkForStep(double peakSize, double lim) {
		float[] orientationVals = getOrientation();
		// no orientation, fallback to only z-axis.
		if (orientationVals == null) {
			orientationVals = new float[3];
			orientationVals[0] = 0;
			orientationVals[1] = 0;
			orientationVals[2] = 1;
		}

		// Add value to values_history

		int lookahead = 5;

		for (int t = 1; t <= lookahead; t++) {
			// catch nullpointer exception at the beginning
			if (stepDetectionWindow[(vhPointer - 1 - t + vhSize + vhSize) % vhSize] != null) {
				// simple approach - use AVG of all 3 directions
				double check_tmp[] = new double[3];
				int pos_new = (vhPointer - 1 - t + vhSize + vhSize) % vhSize;
				int pos_old = (vhPointer - 1 + vhSize) % vhSize;
				check_tmp[0] = stepDetectionWindow[pos_new][0] - stepDetectionWindow[pos_old][0];
				check_tmp[1] = stepDetectionWindow[pos_new][1] - stepDetectionWindow[pos_old][1];
				check_tmp[2] = stepDetectionWindow[pos_new][2] - stepDetectionWindow[pos_old][2];

				// sum there values
				double check = (check_tmp[0] * orientationVals[0] + check_tmp[1] * orientationVals[1] + check_tmp[2]
						* orientationVals[2]);

				if (check >= peakSize && check < lim) {
					// Log.i("JNR_LOCMOV", "Detected step with t = " + t
					// + ", peakSize = " + peakSize + " < " + check);
					return true;
				}
			}
		}
		return false;
	}

	private void addSensorData(SensorTriple sensorData) {
		stepDetectionWindow[vhPointer % vhSize] = sensorData.getValues();
		vhPointer++;
		vhPointer = vhPointer % vhSize;
	}

	private Paint pWhite = new Paint();
	private Paint pStep = new Paint();
	private Paint pJump = new Paint();
	private Paint pState = new Paint();

	private void initColors() {
		pWhite.setColor(Color.WHITE);
		pStep.setColor(colorStep);
		pJump.setColor(colorJump);
		pState.setTextSize(32);
	}

	private boolean init = false;

	@Override
	public void drawToCanvas(Canvas canvas, IndoorLocation center, int ox, int oy, float pixelsPerMeterOrMaxValue) {
		if (!init) {
			initColors();
			init = true;
		}

		long max = System.currentTimeMillis();
		drawSteps(canvas, ox, oy, pixelsPerMeterOrMaxValue, max);
		drawJumps(canvas, ox, oy, pixelsPerMeterOrMaxValue, max);
		canvas.drawText("Steps: " + steps.size() + " Jumps: " + jumps.size(), 16, 48, pWhite);

		String strState = "";
		switch (currentMovement) {
		case JUMPING:
			pState.setColor(Color.YELLOW);
			strState = "JUMPING";
			break;
		case STANDING:
			pState.setColor(Color.BLUE);
			strState = "STANDING";
			break;
		case WALKING:
			pState.setColor(Color.GREEN);
			strState = "WALKING";
			break;
		}

		linAccHistory.drawToCanvas(canvas, center, ox, oy, pixelsPerMeterOrMaxValue);

		canvas.drawText("State: " + strState, 16, 96, pState);
		// Log.i("LOCMOV", "Added type: " + t.values[0] + "/" + t.values[1] +
		// "/" + t.values[2]);
		// Log.i("LOCMOV", "Elements drawn: " + test);

	}

	private void drawSteps(Canvas canvas, int ox, int oy, float pixelsPerMeterOrMaxValue, long max) {
		if (steps.size() == 0) {
			return;
		}

		float pixelsPerMilli = canvas.getWidth() / linAccHistory.getBackLogMillis();

		float x0, x1;
		int i = steps.size() - 1;
		Step last = steps.get(i);
		long diff = max - last.ts;
		Step temp = last;
		while ((diff) <= linAccHistory.getBackLogMillis()) {
			x0 = (-diff * pixelsPerMilli) + canvas.getWidth();

			// Log.i("LOCMOV", "Drawing element: " + temp);
			// Log.i("LOCMOV", "Drawing element: " + get(i+1));
			canvas.drawLine(x0, 0, x0, canvas.getHeight(), pStep);

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

	private void drawJumps(Canvas canvas, int ox, int oy, float pixelsPerMeterOrMaxValue, long max) {
		int size = jumps.size();
		if (size == 0) {
			return;
		}

		int height = canvas.getHeight();
		int width = canvas.getWidth();

		float pixelsPerMilli = width / linAccHistory.getBackLogMillis();

		float x0, x1;
		int i = size - 1;
		Jump last = jumps.get(i);
		long diff = max - last.ts;
		Jump temp = jumps.get(i);
		while ((diff) <= linAccHistory.getBackLogMillis()) {
			x0 = (-diff * pixelsPerMilli) + width;

			// Log.i("LOCMOV", "Drawing element: " + temp);
			// Log.i("LOCMOV", "Drawing element: " + get(i+1));

			canvas.drawLine(x0, 0, x0, height, pJump);

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

	private final Runnable handlerStepDetection = new Runnable() {

		@Override
		public void run() {
			// if start is called twice: we have "two" threads, i.e clean this
			mHandler.removeCallbacks(handlerStepDetection);
			if (linAccHistory.size() != 0) {
				addSensorData(linAccHistory.getLast());
				long t = System.currentTimeMillis();

				// TODO: we can remove jump detection again i suppose

				// Fixed Peak size.. bad bad bad! .. i.e. no per user/device
				// configuration
				if (checkForStep(jumpPeak, 10 * jumpPeak)) {
					// jump if no jumps before or outside of intervals
					int size = jumps.size();
					if (size == 0 || t - jumps.get(size - 1).ts > step_timeout_ms) {
						// detected a jump
						jumps.add(new Jump(t));
						currentMovement = MovementType.JUMPING;
					}
				} else if (checkForStep(stepPeak, jumpPeak)) {
					// step if no steps before or outside of interval
					int size = steps.size();
					if (size == 0 || (size > 0 && t - steps.get(size - 1).ts > step_timeout_ms)) {
						// detected a step
						steps.add(new Step(t));
						currentMovement = MovementType.WALKING;
//						Log.i("FOOTPATH", "StepDetectionImpl: detected step, distributing");

						// prevent NPE
						if (onStepListenerList != null && onStepListenerList.size() > 0) {
							for (StepEventListener l : onStepListenerList) {
								// l.onStepUpdate(bearing, steplength, timestamp, estimatedStepLengthError,
								// estimatedBearingError)
								// TODO: Documentation 0.0 = not defined similar to accuracy in Location
								l.onStepUpdate(bearing[0], initialStepLength, t, 0.0f, 0.0f);
							}
						}
					}
				}

				// no movements if no steps/jumps detected at all, or not in the
				// standing_timeout_ms intervall

				if (currentMovement != MovementType.STANDING) {
					int size = steps.size();
					if (size != 0 && standing_timeout_ms <= (t - steps.get(size - 1).ts)) {
						currentMovement = MovementType.STANDING;
					}
				}
			}
			mHandler.postDelayed(this, delayMillis);
		}
	};

}
