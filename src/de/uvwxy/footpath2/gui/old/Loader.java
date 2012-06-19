package de.uvwxy.footpath2.gui.old;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import de.uvwxy.footpath.R;
import de.uvwxy.footpath2.gui.FlowPathTestGUI;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.map.Map;
import de.uvwxy.footpath2.tools.FileNameFilterFromStringArray;

/**
 * 
 * @author Paul Smith
 *
 */
public class Loader extends Activity {

	public static final String LOADER_SETTINGS = "FootPathSettings";
	public static final String FOOTPATH_BASE_DIR = Environment.getExternalStorageDirectory() + "/footpath/";
	
	// GRAPH
	private static Map g;					// Holds the data structure
	private String nodeFrom;				// Node name to start from, i.e. "5052"
	private int closestNodeID;				// Node ID if closest node was found via GPS
	private String nodeTo;					// Node name to navigate to
	private int iNodeFrom = 0;				// Selected index from spFrom
	private int iNodeTo = 0;				// Selected index from spTo
	private String[] rooms = null;			// Array of all room names added to drop down lists
	private LocationManager locationManager;
	
	// GUI
	private Spinner spFrom = null;			// Drop down lists
	private Spinner spTo = null;
	private Button bGo = null;				// Buttons
	private Button bFlow = null;
	private Button bLoad = null;
	private Button bSave = null;
	private Button bCalibrate = null;
	private Button bGPS = null;
	private Button bQRCode = null;
	private EditText et01 = null;			// EditText, body height
	private CheckBox cbStairs = null;		// Check boxes, for route selection mode
	private CheckBox cbElevator = null;
	private CheckBox cbOutside = null;
	private CheckBox cbLog = null;
//	private CheckBox cbAudio = null;
	private ArrayAdapter<String> adapter1 = null;	// Adapter to manage drop down lists
	private ArrayAdapter<String> adapter2 = null;
	
	
	// DialogSelctionClickHandler needs access to this
	private boolean[] selectedFilesMask =  null;	
	private String[] filePaths = null;
	
	// LISTENERS
	OnItemSelectedListener spinnerListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			// List from: save selected name and position in list
			if (parent.equals(spFrom)) {
				nodeFrom = (String) spFrom.getSelectedItem();
				iNodeFrom = position;
			}
			// List from: save selected name and position in list
			if (parent.equals(spTo)) {				
				nodeTo = (String) spTo.getSelectedItem();
				iNodeTo = position;
			}
		
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			shortToast("You have to selected nothing");
		}
	};
	
	OnClickListener onListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// Distinguish which button was pressed
			if (v.equals(bGo)) {
				if(nodeFrom.equals(nodeTo)){
					shortToast("Please.... " + nodeFrom + " to " + nodeTo + "?");
					return;
				}
				startNavigationFootPath();			
			} else if (v.equals(bFlow)) { 
				if(nodeFrom.equals(nodeTo)){
					shortToast("Please.... " + nodeFrom + " to " + nodeTo + "?");
					return;
				}
				startNavigationFlowPath();		
		    } else if(v.equals(bLoad)){
				// Load values from settings, second parameter is passed if value is not found
				int tNodeFrom 	 = 	getSharedPreferences(LOADER_SETTINGS,0).getInt("nodeFrom", 		0);
				int tNodeTo 	 = 	getSharedPreferences(LOADER_SETTINGS,0).getInt("nodeTo", 		0);
				float sizeIncm 	 = 	getSharedPreferences(LOADER_SETTINGS,0).getFloat("sizeIncm",	191.0f);
				boolean stairs 	 = 	getSharedPreferences(LOADER_SETTINGS,0).getBoolean("stairs",	true);
				boolean elevator = 	getSharedPreferences(LOADER_SETTINGS,0).getBoolean("elevator",	true);
				boolean outside  = 	getSharedPreferences(LOADER_SETTINGS,0).getBoolean("outside",	true);
				// Update GUI elements corresponding to variables
				cbStairs.setChecked(stairs);
				cbElevator.setChecked(elevator);
				cbOutside.setChecked(outside);					
				spFrom.setSelection(tNodeFrom, true);
				spTo.setSelection(tNodeTo, true);
				et01.setText("" + sizeIncm);
			} else if(v.equals(bSave)){
				// Save current values to settings
				SharedPreferences settings = getSharedPreferences(LOADER_SETTINGS, 0);
			    SharedPreferences.Editor editor = settings.edit();
			    editor.putInt("nodeFrom", 		iNodeFrom);
			    editor.putInt("nodeTo", 		iNodeTo);
			    editor.putFloat("sizeIncm", 	Float.parseFloat(et01.getText().toString()));
			    editor.putBoolean("stairs", 	cbStairs.isChecked());
			    editor.putBoolean("elevator", 	cbElevator.isChecked());
			    editor.putBoolean("outside", 	cbOutside.isChecked());
			    // Apply changes
			    editor.commit();
			} else if(v.equals(bCalibrate)){
				Intent intenCalibrator = new Intent(Loader.this, Calibrator.class);
				startActivityForResult(intenCalibrator, 2);
			} else if(v.equals(bGPS)){
				bGPS.setEnabled(false);
				bGPS.setText("Wait for fix");
				initGPS();
			} else if(v.equals(bQRCode)){
				// Source: http://code.google.com/p/zxing/wiki/ScanningViaIntent
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
		        intent.setPackage("com.google.zxing.client.android");
		        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
		        startActivityForResult(intent, 0);
			}
		}
	};
	
	LocationListener locationListener = new LocationListener() {
		// Called when a new location is found by the GPS location provider.
		public void onLocationChanged(Location location) {
			IndoorLocation closestNode;
			IndoorLocation pos;
			String nodeName;
			longToast("GPS location found, searching for nearest node");
			
			pos = new IndoorLocation(location);
			pos.setLevel(0);
			
			// Third parameter set to false, such that only outdoor nodes are accepted
			closestNode = g.getClosestNodeToLatLonPos(pos, 0, false, 17);
			
			if(closestNode != null){
				closestNodeID = closestNode.getId();
				nodeName = (closestNode.getName()==null)?"" + closestNodeID : closestNode.getName();
				longToast("Closest Node is " + nodeName + "\n\n Please select target\n" +
						"The selected room as lcation will be ignored.");
				bGPS.setText(nodeName);
				locationManager.removeUpdates(this);
				bGPS.setEnabled(true);
			} else {
				// As the level is hard coded to 0, this should never appear,
				// as long as all maps have a level beginning at 0.
				double dist = g.getClosestDistanceToNode(pos, 0, false);
				longToast("No node found on this level (0)!\n\nDistance is " + dist + " meters");
			}
			
		}
			
	    public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	    public void onProviderEnabled(String provider) {}
	
	    public void onProviderDisabled(String provider) {}
	};

	private class DialogSelectionClickHandler implements
			DialogInterface.OnMultiChoiceClickListener {
		public void onClick(DialogInterface dialog, int clicked,
				boolean selected) {
			Log.i("FOOTPATH", "selectedFilesMask[" + clicked + "]" + " selected: "
					+ selectedFilesMask[clicked]);
		}
	}
	
	private class ButtonClickHandler implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			finishGraphLoading();
		}
		
	}

	private static final int MENU_GRP_ID = 1337;
	private static final int MENU_ITEM_0 = 10;
	private static final int MENU_ITEM_1 = 20;
	
	private static final String menu_str_0 = "Select Maps";
	private static final String menu_str_1 = "Test Flow Path";
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(MENU_GRP_ID, MENU_ITEM_0, Menu.NONE, menu_str_0);
		menu.add(MENU_GRP_ID, MENU_ITEM_1, Menu.NONE, menu_str_1);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_0:
			showFileListSelector();
			break;
		case MENU_ITEM_1:
			Intent intentFlowTest = new Intent(Loader.this,
					FlowPathTestGUI.class);
//			intentFlowTest.putExtra("my_variable", my_variable);
			startActivityForResult(intentFlowTest, 0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selectroom);
		
		// If you want to know how to load a map from xml resource:
		// staticLoadGraphFromResource();
		
		// GUI - Create references to elements on the screen
		spFrom 		= (Spinner)  findViewById(R.id.Spinner01);
		spTo 		= (Spinner)  findViewById(R.id.Spinner02);
		bGo 		= (Button)   findViewById(R.id.btnGo);
		bFlow 		= (Button)   findViewById(R.id.btnFlow);
		bLoad	 	= (Button)   findViewById(R.id.btnLoad);
		bSave 		= (Button)   findViewById(R.id.btnSave);
		bCalibrate 	= (Button) 	 findViewById(R.id.btnCalibrate);
		bGPS		= (Button)	 findViewById(R.id.btnGPS);
		bQRCode		= (Button)	 findViewById(R.id.btnQR);
		et01 		= (EditText) findViewById(R.id.EditText01);
		cbStairs	= (CheckBox) findViewById(R.id.cbStairs);
		cbElevator 	= (CheckBox) findViewById(R.id.cbElevators);
		cbOutside 	= (CheckBox) findViewById(R.id.cbOutside);
		cbLog		= (CheckBox) findViewById(R.id.cbLog);
//		cbAudio		= (CheckBox) findViewById(R.id.cbAudio);
//		this.setTitle("Footpath r(" +  + ")");
						
		// Set select/click listeners
		spFrom.setOnItemSelectedListener(spinnerListener);
		spTo.setOnItemSelectedListener(spinnerListener);
		bGo.setOnClickListener(onListener);
		bFlow.setOnClickListener(onListener);
		bLoad.setOnClickListener(onListener);
		bSave.setOnClickListener(onListener);
		bCalibrate.setOnClickListener(onListener);
		bGPS.setOnClickListener(onListener);
		bQRCode.setOnClickListener(onListener);
		
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		showFileListSelector();
	}

	@Override
	public void onResume() {
		super.onResume();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Source: http://code.google.com/p/zxing/wiki/ScanningViaIntent
		
		Log.i("FOOTPATH", "requestCode = " + requestCode);
		Log.i("FOOTPATH", "resultCode = " + resultCode);
		
	    if (requestCode == 0) {
	        if (resultCode == RESULT_OK) {
	            String contents = intent.getStringExtra("SCAN_RESULT");
	            String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

	            // Handle successful scan
	            // FORMAT: "http://......?....&fN=<roomname>&tN=<roomname>&.....
	            
	            if(!format.equals("QR_CODE")){
	            	this.longToast("Scan result was not a QR Code");
	            	return;
	            }
	            
	            String[] split = contents.split("\\?");
	            
	            if(split.length!=2){
	            	this.longToast("URL: " + contents + "\n\n is in the wrong format!");
	            	return;
	            }
	            
	            int progress = 0;
	            String sVarString = split[1];
	            String[] sVars = sVarString.split("\\&");
	            
	            for(String s : sVars){
	            	if(s.split("=")[0].equals("fN")){
	            		this.nodeFrom = s.split("=")[1];
	            		progress++;
	            	} else if(s.split("=")[0].equals("tN")){
	            		this.nodeTo = s.split("=")[1];
	            		progress++;
	            	}
	            }
	            
	            for(int i = 0; i < rooms.length; i++){
	            	if(rooms[i].equals(nodeFrom)){
	            		this.spFrom.setSelection(i);
	            	}
	            	if(rooms[i].equals(nodeTo)){
	            		this.spTo.setSelection(i);
	            	}
	            }
	            
	            if(progress==2){
	            	if(nodeFrom.equals(nodeTo)){
	            		longToast("Even if the URL was correct, you will not be going far from " + nodeFrom + " to " + nodeTo);
	            	}
	            	longToast("Starting navigation from " + nodeFrom + " to " + nodeTo);
	            	startNavigationFootPath();
	            }
	        } else if (resultCode == RESULT_CANCELED) {
	            // Handle cancel
	        }
	    } else if (requestCode == 1){
	    	if(resultCode == RESULT_CANCELED){
	    		longToast("Navigation Over!");
	    	}
	    }
	}
	
	@Override
	public void onPause() {
		super.onPause();
		bGPS.setEnabled(true);
		locationManager.removeUpdates(locationListener);
	}
	
	/**
	 * This function calls findOSMFiles(FOOTPATH_BASE_DIR) to find all files in
	 * the configured footpath/ folder and displays the filenames in a list.
	 * 
	 * The ButtonClickHandler() is used to finish the Graph Loading process by
	 * calling finishGraphLoading().
	 */
	private void showFileListSelector(){
		filePaths = findOSMFiles(FOOTPATH_BASE_DIR);

		
		if (filePaths == null || filePaths.length == 0) {
			longToast("No Maps found!\nLoading demo...");
			staticLoadGraphFromResource();
			// Drop down lists: create entries of room names from rooms
			adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, rooms);
			adapter1.setDropDownViewResource(android.R.layout.simple_spinner_item);
			adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, rooms);
			adapter2.setDropDownViewResource(android.R.layout.simple_spinner_item);
		
			spFrom.setAdapter(adapter1);
			spTo.setAdapter(adapter2);
			return;
		}
		
		selectedFilesMask = new boolean[filePaths.length];
		
		
		// Create dialog with list of file names, i.e. with "super_c.osm"
		AlertDialog d = new AlertDialog.Builder( this )
				.setTitle( "Select maps to load:" )
				.setMultiChoiceItems( filePaths, selectedFilesMask, new DialogSelectionClickHandler() )
				.setPositiveButton("OK", new ButtonClickHandler() )
				.create();
		d.show();
	}
	
	/**
	 * This function is called when the user hits "OK" on the list dialog with
	 * the found .osm/.xml files. This function finishes the Graph-loading by
	 * calling load GraphFromFiles(files[]..). (This also merges merge_id's)
	 */
	private void finishGraphLoading() {
		String[] selectedFilePaths = null;
		
		int posBitCount = 0;
		
		// count number of selected items
		for (int i = 0; i < selectedFilesMask.length; i++) {
			if (selectedFilesMask[i]) {
				posBitCount++;
			}
		}

		selectedFilePaths = new String[filePaths.length];

		// fill selectedFilePaths backwards with selected files
		for (int i = 0; i < filePaths.length; i++) {
			if (selectedFilesMask[i]) {
				// add correct absolute path
				selectedFilePaths[--posBitCount] = FOOTPATH_BASE_DIR + filePaths[i];
			}
		}
		
		loadGraphFromFiles(selectedFilePaths);
	}
	
	/**
	 * This function returns a String array of file paths of .osm/.xml files
	 * found non-recursively in the directory prefixDir.
	 * @param prefixDir the directory too look for files in
	 * @return a String array of file paths/names? TODO: make this clear
	 */
	private String[] findOSMFiles(String prefixDir){
		switch(checkStorageEnvironment()) {
		case -1:
			longToast("External storage not availiable");
			return null;
		case 0:
			longToast("External storage not writeable");
			break;
		case 1:
			// here we are fine ;)
			break;
		}
				
		File searchFolder = new File(prefixDir);
		
		String[] endingsToFilter = {"osm","xml"};
		FileNameFilterFromStringArray fileFilter = new FileNameFilterFromStringArray(endingsToFilter);
		File[] files = searchFolder.listFiles(fileFilter);
		
		if (files != null) {
			String[] ret = new String[files.length];
			for (int i = 0; i < files.length; i++) {
				if (files[i] != null) {
					ret[i] = files[i].getName();
				}
			}
	
			return ret;
		} else { // -> if(files!=null)
			return null;
		} // else -> if(files!=null)
	}

	/**
	 * This creates a Graph from the given files. Some Toast message are given
	 * on error. After the files have been loaded merge_id's are merged, and the
	 * spinners are updated with their new list of rooms.
	 * @param filePaths an array of absolute paths to .osm/.xml files
	 */
	private void loadGraphFromFiles(String[] filePaths ){
		// Cancel on null or empty array.
		if (filePaths == null || filePaths.length == 0) {
			return;
		}
		
		longToast("Loading can take a while...\n\n Please wait...");
		
		// TODO: We create a new graph, do we need to clean up sth.?
		g = new Map();
		
		// Add new layer(s) of ways from XML-file from sdcard
		for (String file : filePaths){
			try {
				g.addToGraphFromXMLFile(file);
			} catch (FileNotFoundException e1) {
				longToast("Could not open\n" + file);
			} catch (ParserConfigurationException e1) {
				longToast("Parser error with\n" + file);
			} catch (SAXException e1) {
				longToast("SAX error with\n" + file);
			} catch (IOException e1) {
				longToast("IO error with\n" + file);
			}
		}
		
		g.mergeNodes();
		
		rooms = g.getRoomList();
		
		// Drop down lists: create entries of room names from rooms
		adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, rooms);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_item);
		adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, rooms);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_item);
	
		spFrom.setAdapter(adapter1);
		spTo.setAdapter(adapter2);
		
		return;
	}
	
	private void staticLoadGraphFromResource(){
//		 Old Static Code:
		 g = new Map();
////		 And add layer(s) of ways
//		 try {
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.stone_henge_demo));
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.sc_floor_u2));
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.sc_floor_u1));
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.sc_floor_0));
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.sc_floor_1));
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.sc_floor_2));
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.sc_floor_3));
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.sc_floor_4));
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.sc_floor_5));
////		 g.addToGraphFromXMLResourceParser(this.getResources().getXml(R.xml.sc_floor_6));
//		 g.mergeNodes();
//		 	rooms = g.getRoomList();
//		 } catch (NotFoundException e) {
//			 longToast("Error: resource not found:\n\n" + e);
//		 } catch (XmlPullParserException e) {
//			 longToast("Error: xml error:\n\n" + e);
//		 } catch (IOException e) {
//			 longToast("Error: io error:\n\n" + e);
//		 }
	}

	// Navigator needs static access to graph
	public static Map getGraph(){
		return g;
	}
	
	private void shortToast(String s) {
		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}

	private void longToast(String s) {
		Toast.makeText(this, s, Toast.LENGTH_LONG).show();
	}

	private void initGPS(){
		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		longToast("Waiting for GPS fix\n\nPlease wait for a notification to continue.\n\nYou can select a destination.");
	}
	private void startNavigationFootPath(){
		Log.i("FOOTPATH", "Starting FootPath navigation intent");
		// Create intent for navigation
		Intent intentNavigator = new Intent(Loader.this, NavigatorFootPath.class);
		// Add values to be passed to navigator
		intentNavigator.putExtra("from",		nodeFrom);
		intentNavigator.putExtra("fromId",		closestNodeID);
		intentNavigator.putExtra("to",			nodeTo);
		intentNavigator.putExtra("stairs",		cbStairs.isChecked());
		intentNavigator.putExtra("elevator",	cbElevator.isChecked());
		intentNavigator.putExtra("outside",		cbOutside.isChecked());
		intentNavigator.putExtra("log", 		cbLog.isChecked());
//		intentNavigator.putExtra("audio",		cbAudio.isChecked());
		// Source: http://www.pedometersaustralia.com/g/13868/measure-step-length-.html
		intentNavigator.putExtra("stepLength", 	Float.parseFloat(et01.getText().toString()) * 0.415f);
		// Start intent for navigation
		startActivityForResult(intentNavigator, 1);
	}
	
	private void startNavigationFlowPath(){
		Log.i("FOOTPATH", "Starting FlowPath navigation intent");
		// Create intent for navigation
		Intent intentNavigator = new Intent(Loader.this, NavigatorFlowPath.class);
		// Add values to be passed to navigator
		intentNavigator.putExtra("from",		nodeFrom);
		intentNavigator.putExtra("fromId",		closestNodeID);
		intentNavigator.putExtra("to",			nodeTo);
		intentNavigator.putExtra("stairs",		cbStairs.isChecked());
		intentNavigator.putExtra("elevator",	cbElevator.isChecked());
		intentNavigator.putExtra("outside",		cbOutside.isChecked());
		intentNavigator.putExtra("log", 		cbLog.isChecked());
//		intentNavigator.putExtra("audio",		cbAudio.isChecked());
		// Source: http://www.pedometersaustralia.com/g/13868/measure-step-length-.html
		intentNavigator.putExtra("stepLength", 	30f);
		// Start intent for navigation
		startActivityForResult(intentNavigator, 1);
	}
	
	/**
	 * This checks if the external storage is present/read/writeable
	 * @return -1 = not present, 0 = readable, 1 = writeable
	 */
	private short checkStorageEnvironment(){
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		if(!mExternalStorageAvailable){
			return -1;
		}
		
		if (!mExternalStorageWriteable){
			return 0;
		}
		
		return 1;
	}
}
