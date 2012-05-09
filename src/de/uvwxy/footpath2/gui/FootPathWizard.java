package de.uvwxy.footpath2.gui;

import android.app.Activity;
import android.os.Bundle;
import de.uvwxy.footpath.R;

/**
 * Author: paul
 * Date: May 9, 2012 2:12:39 PM
 */
public class FootPathWizard extends Activity {
	// ####################################################################
	// Variables & Handles
	// ####################################################################

	// ####################################################################
	// Listener, Callbacks
	// ####################################################################

	// ####################################################################
	// Application Life Cycle
	// ####################################################################
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_wizard_a_start_choice);
		//TODO: Don't forget to add this Activity in your Manifest file!
	}
	// ####################################################################
	// Functions & Methods
	// ####################################################################
}
