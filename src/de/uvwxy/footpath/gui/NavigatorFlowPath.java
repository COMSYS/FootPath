package de.uvwxy.footpath.gui;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Stack;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ZoomControls;
import de.uvwxy.footpath.R;
import de.uvwxy.footpath.ToolBox;
import de.uvwxy.footpath.core.NPConfig;
import de.uvwxy.footpath.core.Positioner;
import de.uvwxy.footpath.core.Positioner_OnlineBestFit;
import de.uvwxy.footpath.core.Positioner_OnlineFirstFit;
import de.uvwxy.footpath.core.StepDetection;
import de.uvwxy.footpath.core.StepTrigger;
import de.uvwxy.footpath.graph.Graph;
import de.uvwxy.footpath.graph.GraphEdge;
import de.uvwxy.footpath.graph.GraphNode;
import de.uvwxy.footpath.graph.LatLonPos;
import de.uvwxy.footpath.log.AudioWriter;
import de.uvwxy.footpath.log.DataLogger;

/**
 * 
 * @author Paul Smith
 * 
 */
public class NavigatorFlowPath extends Navigator {

	// #########################################################################
	// ############################## Functions ################################
	// #########################################################################

	// #########################################################################
	// ######################## Activity Life Cycle ############################
	// #########################################################################

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// route calculation and so on..
		super.onCreate(savedInstanceState);

		setNavigating(true);
	}
}
