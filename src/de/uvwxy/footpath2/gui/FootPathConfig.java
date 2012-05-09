package de.uvwxy.footpath2.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import de.uvwxy.footpath.R;

/**
 * Author: paul
 * Date: May 9, 2012 2:13:56 PM
 */
public class FootPathConfig extends Activity {
	private SeekBar sbPeakControl = null;
	private SeekBar sbJumpPeakControl = null;
	private SeekBar sbStepTimeOutControl = null;
	private SeekBar sbStandingTimeOutControl = null;
	private TextView lblPeak = null;
	private TextView lblStepTimeOut = null;
	private TextView lblJumpPeak = null;
	private TextView lblStandingTimeOut = null;
	private PaintBoxMovementStepDetection svSensors = null;

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
		svSensors = new PaintBoxMovementStepDetection(this, 3000);

		// and remove surface views from layout
		linLayoutHistory.removeView(svOldHistory);
		// add surface views with cloned parameters to layout
		linLayoutHistory.addView(svSensors, lpHistory);

		sbStepTimeOutControl = (SeekBar) findViewById(R.id.sbStepTimeOutControl);
		sbPeakControl = (SeekBar) findViewById(R.id.sbPeakControl);
		sbJumpPeakControl = (SeekBar) findViewById(R.id.sbJumpPeakControl);
		sbStandingTimeOutControl = (SeekBar) findViewById(R.id.sbStandingTimeOutControl);

		sbStepTimeOutControl
				.setOnSeekBarChangeListener(seekbarOnChangeListener);
		sbPeakControl.setOnSeekBarChangeListener(seekbarOnChangeListener);
		sbJumpPeakControl.setOnSeekBarChangeListener(seekbarOnChangeListener);
		sbStandingTimeOutControl
				.setOnSeekBarChangeListener(seekbarOnChangeListener);

		lblStepTimeOut = (TextView) findViewById(R.id.lblStepTimeOut);
		lblPeak = (TextView) findViewById(R.id.lblPeak);

		lblStandingTimeOut = (TextView) findViewById(R.id.lblStandingTimeOut);
		lblJumpPeak = (TextView) findViewById(R.id.lblJumpPeak);

		initTest();
	}

	public void onResume() {
		super.onResume();
		if (pipeFeeder != null) {
			pipeFeeder.registerSensors();
			svSensors.setPipeFeeder(pipeFeeder);
		}
		
		seekbarOnChangeListener.onProgressChanged(sbJumpPeakControl,
				(int)(pipeFeeder.locationPipe.stepDetectionHistory.getJumpPeak()*100), false);
		seekbarOnChangeListener.onProgressChanged(sbPeakControl,
				(int)(pipeFeeder.locationPipe.stepDetectionHistory.getPeak()*100), false);
		seekbarOnChangeListener.onProgressChanged(sbStepTimeOutControl,
				(int)(pipeFeeder.locationPipe.stepDetectionHistory.getStepTimeOut()), false);
		seekbarOnChangeListener.onProgressChanged(sbStandingTimeOutControl,
				(int)(pipeFeeder.locationPipe.stepDetectionHistory.getStandingTimeOut()), false);
	}

	public void onPause() {
		super.onPause();
		if (pipeFeeder != null) {
			pipeFeeder.unregisterSensors();
		}
	}

	public void onDestroy() {
		super.onDestroy();
		if (pipeFeeder != null) {
			pipeFeeder.unregisterSensors();
		}
	}

	private void initTest() {
		if (pipeFeeder == null) {
			pipeFeeder = new PipeFeeder(this);
			svSensors.setPipeFeeder(pipeFeeder);
			pipeFeeder.initSensors();
		}
	}

	private OnSeekBarChangeListener seekbarOnChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (seekBar.equals(sbPeakControl)) {
				pipeFeeder.locationPipe.stepDetectionHistory
						.setPeak(progress / 100.0);
				lblPeak.setText("Peak: (" + progress / 100.0 + ")");
				seekBar.setProgress(progress);
			} else if (seekBar.equals(sbStepTimeOutControl)) {
				pipeFeeder.locationPipe.stepDetectionHistory
						.setStepTimeout(progress);
				lblStepTimeOut.setText("Step timeout: (" + progress + ")");
			} else if (seekBar.equals(sbStandingTimeOutControl)) {
				pipeFeeder.locationPipe.stepDetectionHistory
						.setStandingTimeout(progress);
				lblStandingTimeOut.setText("Standing timeout: (" + progress
						+ ")");
			} else if (seekBar.equals(sbJumpPeakControl)) {
				pipeFeeder.locationPipe.stepDetectionHistory
						.setJumpPeak(progress / 100.0);
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
}
