package de.uvwxy.footpath2.tools.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import android.os.Environment;
import android.util.Log;

public class AppendWriter {
	private static final String directory = "footpath_exports/";
	private String path = null;
	private String filename = null;
	private BufferedOutputStream fos;
	private PrintWriter p;

	public AppendWriter(String subdirectory, String filename) {

		// check if first dir exists
		File dir = new File(Environment.getExternalStorageDirectory(), directory);
		if (!dir.exists()) {
			dir.mkdir();
		}

		// check if second dir exists
		dir = new File(Environment.getExternalStorageDirectory(), directory + subdirectory);
		if (!dir.exists()) {
			dir.mkdir();
		}

		path = dir.getAbsolutePath();

		this.filename = filename;
	}

	public boolean openFile(boolean append) {
		// check if file exists
		try {
			fos = new BufferedOutputStream(new FileOutputStream(path + filename));
			p = new PrintWriter(fos);
		} catch (IOException e) {
			Log.i("FOOTPATH", "ERROR: " + e.toString());
			return false;
		}

		return true;
	}
	
	public void forceOpenFile(boolean append){
		while(!openFile(append)){
			
		}
	}

	/**
	 * Appends a string to a text file. This line will be followed by a new line
	 * 
	 * @param data
	 *            the line to append
	 */
	public void appendLineToFile(String data) {
		p.append(data + "\n");
	}

	public void closeFile() {
		p.close();
		try {
			fos.close();
		} catch (IOException e) {
			Log.i("FOOTPATH", "Error closing file: " + e.toString());
			e.printStackTrace();
		}
	}
}
