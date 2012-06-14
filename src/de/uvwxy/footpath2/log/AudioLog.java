package de.uvwxy.footpath2.log;

import java.io.File;
import java.io.IOException;

import de.uvwxy.footpath2.gui.old.NavigatorFootPath;

import android.media.MediaRecorder;
import android.os.Environment;

/**
 * A class for recording of an audio/video file, with input from the
 * microphone/camera . The directory written to is LOG_DIR within the directory
 * retrieved from getExternalStorageDirectory(). The file name is given to the
 * constructor.
 * 
 * Usage:
 * 
 * 	o = new AudioLog()
 * 	o.registerCapture()
 * 	o.startCapture()
 * 	[... magic moment in time ...]
 * 	o.stopCapture()
 * 	o.unregisterCapture()
 * 
 * 
 * @author Paul Smith
 * 
 */
public class AudioLog {
	private MediaRecorder recorder;
	private String filePath;
		
	/**
	 * The only constructor
	 * 
	 * @param fileName  the file name
	 */
	public AudioLog(String subdirectory, String fileName) {
		File dir = new File(Environment.getExternalStorageDirectory(),
				"routelog/" + subdirectory);
		dir.mkdir();
		this.filePath = dir.getAbsolutePath() + "/" + fileName;
	}

	/**
	 * Call this to initialize everything needed to capture from AudioSource.MIC
	 * 
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void _a_registerCapture() throws IllegalStateException, IOException {
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		recorder.setOutputFile(filePath);
		recorder.prepare();
	}

	/**
	 * Call this to start capture
	 */
	public void _b_startCapture() {
		recorder.start();
	}

	/**
	 * Call this to stop capture
	 */
	public void _c_stopCapture() {
		recorder.stop();
		recorder.reset();
	}

	/**
	 * Call this to release capture device
	 */
	public void _d_unregisterCapture() {
		recorder.release();
	}

}
