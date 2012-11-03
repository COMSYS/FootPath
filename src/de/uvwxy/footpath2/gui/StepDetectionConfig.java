package de.uvwxy.footpath2.gui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import de.uvwxy.footpath.R;
import de.uvwxy.footpath2.drawing.DrawToCanvasWrapper;
import de.uvwxy.footpath2.drawing.PaintBoxDrawToCanvasWrapper;
import de.uvwxy.footpath2.log.ExportManager;
import de.uvwxy.footpath2.log.ExportManager.IntervalExportBehavior;
import de.uvwxy.footpath2.movement.SensorEventDistributor;
import de.uvwxy.footpath2.movement.steps.StepDetectionImpl;

/**
 * Author: paul Date: May 9, 2012 2:13:56 PM
 */
public class StepDetectionConfig extends Activity implements DrawToCanvasWrapper {
	private SeekBar sbPeakControl = null;
	private SeekBar sbJumpPeakControl = null;
	private SeekBar sbStepTimeOutControl = null;
	private SeekBar sbStandingTimeOutControl = null;
	private TextView lblPeak = null;
	private TextView lblStepTimeOut = null;
	private TextView lblJumpPeak = null;
	private TextView lblStandingTimeOut = null;
	private PaintBoxDrawToCanvasWrapper paintBoxDTC = null;

	private SensorEventDistributor sensorEventDistributor;
	private StepDetectionImpl stepDetection;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.movement_stepdetection_config);

		// get pointer to layout
		LinearLayout linLayoutHistory = (LinearLayout) findViewById(R.id.llPaintBoxHistory);
		// get SurfaceView defined in xml
		SurfaceView svOldHistory = (SurfaceView) findViewById(R.id.svPaintBoxHistory);

		// get its layout params
		LayoutParams lpHistory = svOldHistory.getLayoutParams();

		// create PaintBoxes
		paintBoxDTC = new PaintBoxDrawToCanvasWrapper(this);

		// and remove surface views from layout
		linLayoutHistory.removeView(svOldHistory);
		// add surface views with cloned parameters to layout
		linLayoutHistory.addView(paintBoxDTC, lpHistory);

		sbStepTimeOutControl = (SeekBar) findViewById(R.id.sbStepTimeOutControl);
		sbPeakControl = (SeekBar) findViewById(R.id.sbPeakControl);
		sbJumpPeakControl = (SeekBar) findViewById(R.id.sbJumpPeakControl);
		sbStandingTimeOutControl = (SeekBar) findViewById(R.id.sbStandingTimeOutControl);

		sbStepTimeOutControl.setOnSeekBarChangeListener(seekbarOnChangeListener);
		sbPeakControl.setOnSeekBarChangeListener(seekbarOnChangeListener);
		sbJumpPeakControl.setOnSeekBarChangeListener(seekbarOnChangeListener);
		sbStandingTimeOutControl.setOnSeekBarChangeListener(seekbarOnChangeListener);

		lblStepTimeOut = (TextView) findViewById(R.id.lblStepTimeOut);
		lblPeak = (TextView) findViewById(R.id.lblPeak);

		lblStandingTimeOut = (TextView) findViewById(R.id.lblStandingTimeOut);
		lblJumpPeak = (TextView) findViewById(R.id.lblJumpPeak);

		initTest();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (stepDetection != null) {
			sensorEventDistributor._b2_unPauseSensorUpdates();
			stepDetection._b2_unPauseMovementDetection();
			Log.i("FOOTPATH2", "Test");
		}
		seekbarOnChangeListener.onProgressChanged(sbJumpPeakControl, (int) (stepDetection.getJumpPeak() * 100), false);
		seekbarOnChangeListener.onProgressChanged(sbPeakControl, (int) (stepDetection.getPeak() * 100), false);
		seekbarOnChangeListener.onProgressChanged(sbStepTimeOutControl, (stepDetection.getStepTimeOut()), false);
		seekbarOnChangeListener
				.onProgressChanged(sbStandingTimeOutControl, (stepDetection.getStandingTimeOut()), false);
		// ExportManager em = ExportManager.getInstance();
		// em.setBehavior(IntervalExportBehavior.EXPORT_RECENTDATA); // only exporting, no clearing of data
		// em.startIntervalExporting(15 * 1000); // save data each 10 seconds
		// em.setByteThreshold(0); // direct saving
	}

	@Override
	public void onPause() {
		super.onPause();
		if (stepDetection != null) {
			stepDetection._b1_pauseMovementDetection();
			sensorEventDistributor._b1_pauseSensorUpdates();
			// ExportManager em = ExportManager.getInstance();
			// em.stopIntervalExporting();
			// em.export_recentData(); // save new data on exit.
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (stepDetection != null) {
			stepDetection._c_stopMovementDetection();
			sensorEventDistributor._c_stopSensorUpdates();
		}
	}

	private void initTest() {
		if (stepDetection == null) {
			sensorEventDistributor = SensorEventDistributor.getInstance(this);
			stepDetection = new StepDetectionImpl(this);
			paintBoxDTC.setCanvasPainter(this);
			sensorEventDistributor.addLinearAccelerometerListener(stepDetection);
			sensorEventDistributor.addGravityListener(stepDetection);
			sensorEventDistributor._a_startSensorUpdates();
			sensorEventDistributor.registerExportData();
		}
	}

	private final OnSeekBarChangeListener seekbarOnChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (seekBar.equals(sbPeakControl)) {
				stepDetection.setPeak(progress / 100.0);
				lblPeak.setText("Peak: (" + progress / 100.0 + ")");
				seekBar.setProgress(progress);
			} else if (seekBar.equals(sbStepTimeOutControl)) {
				stepDetection.setStepTimeout(progress);
				lblStepTimeOut.setText("Step timeout: (" + progress + ")");
			} else if (seekBar.equals(sbStandingTimeOutControl)) {
				stepDetection.setStandingTimeout(progress);
				lblStandingTimeOut.setText("Standing timeout: (" + progress + ")");
			} else if (seekBar.equals(sbJumpPeakControl)) {
				stepDetection.setJumpPeak(progress / 100.0);
				lblJumpPeak.setText("Jump Peak: (" + progress / 100.0 + ")");
			}
			seekBar.setProgress(progress);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub

		}

	};

	private void longToast(String msg) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, msg, duration);
		toast.show();
	}

	@Override
	public void drawToCanvas(Canvas canvas) {
		Rect bb = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
		Paint black = new Paint();
		canvas.drawRect(bb, black);

		Paint pLine = new Paint();
		Paint pDots = new Paint();
		pLine.setColor(Color.RED);
		pDots.setColor(Color.GREEN);
		stepDetection.drawToCanvas(canvas, null, canvas.getWidth() / 2, canvas.getHeight() / 2, 9);
	}
}
