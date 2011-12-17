package de.uvwxy.footpath.h263;

import java.io.File;
import java.io.IOException;

import de.uvwxy.footpath.gui.FlowPath;

import android.media.MediaRecorder;
import android.os.Environment;

/**
 * A class for recording of an audio/video file, with input from the
 * microphone/camera . The directory written to is LOG_DIR in the directory
 * retrieved from getExternalStorageDirectory(). The file name is given to the
 * constructor.
 * 
 * @author Paul
 * 
 */
public class AudioVideoWriter {
	// Capture object
	MediaRecorder recorder;

	// File string
	String filePath;
	
	public String getFilePath(){
		return filePath;
	}

	/**
	 * Constructor
	 * 
	 * @param fileName
	 *            the file name
	 */
	public AudioVideoWriter(String fileName) {
		File dir = new File(Environment.getExternalStorageDirectory(),
				FlowPath.LOG_DIR);
		dir.mkdir();
		this.filePath = dir.getAbsolutePath() + "/" + fileName;
	}

	/**
	 * Initializes everything needed to capture from AudioSource.MIC. If this
	 * succeeds the capture device is ready and can be started.
	 * 
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void registerCapture() throws IllegalStateException, IOException {
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
		recorder.setVideoFrameRate(24);
		recorder.setVideoSize(320, 240);
		// Note: Camera orientation can only be changed since API level 8 (2.2/Froyo)
//		recorder.setOrientationHint(270);
		recorder.setOutputFile(filePath);
		recorder.setPreviewDisplay(FlowPath.sh01.getSurface());
		recorder.prepare();
	}

	/**
	 * Starts the capture
	 */
	public void startCapture() {
		recorder.start();
	}

	/**
	 * Stops the capture
	 */
	public void stopCapture() {
		recorder.stop();
		recorder.reset();
	}

	/**
	 * Resleases the capture device
	 */
	public void unregisterCapture() {
		recorder.release();
	}

}
