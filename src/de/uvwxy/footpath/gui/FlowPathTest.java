package de.uvwxy.footpath.gui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import de.uvwxy.footpath.R;
import de.uvwxy.footpath.core.FlowDetection;
import de.uvwxy.footpath.core.FlowDetectionParser;

/**
 * Author: paul Date: Nov 18, 2011 4:23:52 PM
 */
public class FlowPathTest extends Activity {
	// ####################################################################
	// Variables & Handles
	// ####################################################################
	private Button btnFlowPath = null;
	// ####################################################################
	// Listener, Callbacks
	// ####################################################################

	private OnClickListener onClick = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (view.equals(btnFlowPath)) {
				Log.i("FOOTPATH", "new FlowDetection()");
				FlowDetection flowDetection = new FlowDetection();
				Log.i("FOOTPATH", ".loadFlowDetection()");
				SurfaceView sv01 = (SurfaceView) findViewById(R.id.svCam);
				SurfaceHolder sh01 = sv01.getHolder();
//				sh01.setSizeFromLayout();
				sh01.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
				if (flowDetection.loadFlowDetection(sh01.getSurface())) {
					Log.i("FOOTPATH", "parse()");
					FlowDetectionParser fdp = new FlowDetectionParser(
							flowDetection);
					fdp.start();

				}
			}
		}
	};

	// ####################################################################
	// Application Life Cycle
	// ####################################################################
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.flowpathtest);
		// TODO: Don't forget to add this Activity in your Manifest file!

		btnFlowPath = (Button) findViewById(R.id.btnFlowPath);
		btnFlowPath.setOnClickListener(onClick);
	}
	// ####################################################################
	// Functions & Methods
	// ####################################################################
}
