package de.uvwxy.footpath.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import android.os.Environment;

/**
 * A class for opening, writing to and closing of a text file. The directory
 * written to is LOG_DIR in the directory retrieved from
 * getExternalStorageDirectory(). The file name is given to the constructor.
 * 
 * @author Paul
 * 
 */
public class FileWriter {
	PrintWriter p;
	String fileName;

	/**
	 * Constructor
	 * 
	 * @param fileName
	 *            the file name
	 */
	public FileWriter(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Creates the directory LOG_DIR on the external storage and opens the file
	 * for writing. There is no problem if LOG_DIR already exists.
	 * 
	 * @throws FileNotFoundException
	 */
	public void createFileOnCard() throws FileNotFoundException {
		File dir = new File(Environment.getExternalStorageDirectory(),
				de.uvwxy.footpath.gui.FlowPath.LOG_DIR);
		dir.mkdir();
		p = new PrintWriter(new File(dir, fileName));
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
		p.close();
	}
}
