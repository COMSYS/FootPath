package de.uvwxy.footpath2.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import de.uvwxy.footpath2.gui.NavigatorFootPath;

import android.os.Environment;
import android.util.Log;

/**
 * A class for opening, writing to and closing of a text file. The directory
 * written to is LOG_DIR in the directory retrieved from
 * getExternalStorageDirectory(). The file name is given to the constructor.
 * 
 * @author Paul Smith
 * 
 */
public class FWriter {
	PrintWriter p;
	FileOutputStream fos;
	String fileName;
	boolean open = false;
	
	String sub_directory;
	/**
	 * Constructor
	 * 
	 * @param fileName
	 *            the file name
	 */
	public FWriter(String sub_directory, String fileName) {
		this.fileName = fileName;
		this.sub_directory = sub_directory;
	}

	/**
	 * Creates the directory LOG_DIR on the external storage and opens the file
	 * for writing. There is no problem if LOG_DIR already exists.
	 * 
	 * @throws FileNotFoundException
	 */
	public void openFileOnCard() throws FileNotFoundException {
		// check if first dir exists
		File dir = new File(Environment.getExternalStorageDirectory(),NavigatorFootPath.LOG_DIR);
		if(!dir.exists()){
			dir.mkdir();
		}
		// check if second dir exists
		dir = new File(Environment.getExternalStorageDirectory(),
				NavigatorFootPath.LOG_DIR + sub_directory);
		if(!dir.exists()){
			dir.mkdir();
		}
		// check if file exists
		File f = new File (dir, fileName);
		if(!f.exists()){
			try {
				// create it
				f.createNewFile();
			} catch (IOException e) {
				Log.i("FOOTPATH", "ERROR: " + e.toString());
				throw new FileNotFoundException("IOException");
			}
		}
		try {
			fos = new FileOutputStream(f, true);
			p = new PrintWriter(fos);
		} catch (IOException e) {
			Log.i("FOOTPATH", "ERROR: " + e.toString());
			throw new FileNotFoundException("IOException");
		}
		
		open = true;
	}

	/**
	 * Appends a string to a text file followed by a new line
	 * 
	 * @param data
	 *            the line to append
	 */
	public void appendLineToFile(String data) {
		p.append(data + "\n");
	}

	/**
	 * Closes the previously opened file.F
	 */
	public void closeFileOnCard() {
		if(open){
			p.close();
			open = false;
		}
	}
}
