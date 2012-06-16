package de.uvwxy.footpath2.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import android.os.Environment;
import android.util.Log;

public class AppendWriter {
	private ExportManager em = ExportManager.getInstance();
	private String path = null;
	private String filename = null;
	private BufferedOutputStream fos;
	private PrintWriter p;

	public AppendWriter(String filename) {
		this.filename = filename;
		File f = new File(Environment.getExternalStorageDirectory(), em.getDirectory() + em.getSubdirectory());
		path = f.getAbsolutePath();
	}

	public boolean openFile(boolean append) {
		// check if file exists
		try {
			Log.i("FOOTPATH", "Opening file " + path + "/" + filename);
			fos = new BufferedOutputStream(new FileOutputStream(path + "/" + filename, append));
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
		Log.i("FOOTPATH", "Closing file " + path + "/" + filename);
		p.flush();
		p.close();
		try {
			fos.close();
		} catch (IOException e) {
			Log.i("FOOTPATH", "Error closing file: " + e.toString());
			e.printStackTrace();
		}
	}
}
