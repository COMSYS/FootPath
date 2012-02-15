package de.uvwxy.flowpath;

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
	String folder;
	String fileName;

	/**
	 * Constructor
	 * 
	 * @param fileName
	 *            the file name
	 */
	public FileWriter(String folder, String fileName) {
		this.folder = folder;
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
				de.uvwxy.footpath.gui.FlowPathTestGUI.LOG_DIR);
		dir.mkdir();
		File logdir = new File(Environment.getExternalStorageDirectory(),
				de.uvwxy.footpath.gui.FlowPathTestGUI.LOG_DIR + "/" + folder);
		logdir.mkdir();
		p = new PrintWriter(new File(logdir, fileName));
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
