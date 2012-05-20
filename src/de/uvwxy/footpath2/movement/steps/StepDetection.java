package de.uvwxy.footpath2.movement.steps;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.Location;

/**
 * generic interface for step detection - there may be more than one method to do this job in the future.
 * 
 * @author helge
 * 
 */
public interface StepDetection {

	public abstract void _a_startMovementDetection();

	public abstract void _b1_pauseMovementDetection();

	public abstract void _b2_unPauseMovementDetection();

	public abstract void _c_stopMovementDetection();

	public abstract void onAccuracyChanged(Sensor sensor, int accuracy);

	public abstract void onSensorChanged(SensorEvent event);

	public abstract double getPeak();

	public abstract double getJumpPeak();

	public abstract int getStepTimeOut();

	public abstract int getStandingTimeOut();

	public abstract void setPeak(double peak);

	public abstract void setJumpPeak(double peak);

	public abstract void setStepTimeout(int timeout);

	public abstract void setStandingTimeout(int timeout);

	public abstract long getLastStepTimeDelta();

	public abstract int getNumSteps();

	public abstract int getNumJumps();

	public abstract MovementType getCurrentMovement();

	public abstract void drawToCanvas(Canvas canvas, Location center, Rect boundingBox,
			double pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots);

}