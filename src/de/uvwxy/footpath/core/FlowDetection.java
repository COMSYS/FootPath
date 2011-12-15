package de.uvwxy.footpath.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

/**
 * 
 * @author paul
 * 
 */
public class FlowDetection {
	private static String outputFileName = "out.3gp";
	// Capture object
	MediaRecorder recorder;

	// File string
	String filePath;

	/**
	 * Initializes everything needed to capture from AudioSource.MIC. If this
	 * succeeds the capture device is ready and can be started.
	 * 
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	private void registerCapture(Surface surface) throws IllegalStateException,
			IOException {
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
//		recorder.setVideoFrameRate(24);
		recorder.setVideoSize(640, 480);
		// Note: Camera orientation can only be changed since API level 8
		// (2.2/Froyo)
		// recorder.setOrientationHint(270);
		recorder.setOutputFile(filePath);
		recorder.setPreviewDisplay(surface);
		recorder.prepare();
	}

	/**
	 * Starts the capture
	 */
	private void startCapture() {
		recorder.start();
	}

	/**
	 * Stops the capture
	 */
	private void stopCapture() {
		recorder.stop();
		recorder.reset();
	}

	/**
	 * Resleases the capture device
	 */
	private void unregisterCapture() {
		recorder.release();
	}

	public boolean loadFlowDetection(Surface surface) {
		this.filePath = Environment.getExternalStorageDirectory() + "/" + outputFileName;
		File file = new File(filePath);
		file.delete();
		Log.i("FOOTPATH", "FilePath for video file: " + filePath);
		try {
			registerCapture(surface);
			Log.i("FOOTPATH", "Starting Capture");
			startCapture();
			Log.i("FOOTPATH", "Started Capture");
			return true;
		} catch (IllegalStateException e) {
			unregisterCapture();
			Log.i("FOOTPATH", "Och nööö, is kaputt\n" + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			unregisterCapture();
			Log.i("FOOTPATH", "Och nööö, is kaputt\n" + e.getLocalizedMessage());
			e.printStackTrace();
		}
		return false;
	}

	public void parse() {
		// now try accessing video file written to sd // it DOES work (v2.1
		// /Milestone)
		FileInputStream in = null;
		try {
			in = new FileInputStream(new File(
					Environment.getExternalStorageDirectory(), outputFileName));
		} catch (FileNotFoundException ex) {
			Log.i("FOOTPATH", "File not found: " + ex.getLocalizedMessage());
		}

		InputStreamReader inReader = new InputStreamReader(in);
		BufferedReader inBuffer = new BufferedReader(inReader);

		try {
			int inBuf = inBuffer.read();
			Log.i("FOOTPATH", "Reading from video file: "
					+ ((inBuf == -1) ? " EOS." : inBuf));
		} catch (IOException e) {
			Log.i("FOOTPATH",
					"Could not read from buffer/video file\n"
							+ e.getLocalizedMessage());
		}

	}

	public void undloadFlowDetection() {
		stopCapture();
		unregisterCapture();
		File f = new File(filePath);
		// TODO: don't do this for testing?
		f.delete();
	}
}
