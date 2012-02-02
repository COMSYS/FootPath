package de.uvwxy.footpath.h263;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.media.MediaRecorder;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import de.uvwxy.footpath.gui.FlowPath;

/**
 * A class for recording of an audio/video file, with input from the
 * microphone/camera . The directory written to is LOG_DIR in the directory
 * retrieved from getExternalStorageDirectory(). The file name is given to the
 * constructor.
 * 
 * @author Paul
 * 
 */
public class SocketAudioVideoWriter {
	// Capture object
	MediaRecorder recorder;

	
	// Socket + Functions:
	private Socket sckCltSend = null;
	
	private void startClient(String hostname, int port){
		try {
			sckCltSend = new Socket(InetAddress.getByName(hostname), port);
			sckCltSend.setTcpNoDelay(true);
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	
	private FileDescriptor getFileDescriptorFromClientSocket(){
		return ParcelFileDescriptor.fromSocket(sckCltSend).getFileDescriptor();
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
		recorder.setVideoFrameRate(FlowPath.PIC_FPS);
		recorder.setVideoSize(FlowPath.PIC_SIZE_WIDTH, FlowPath.PIC_SIZE_HEIGHT);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
		
		startClient("127.0.0.1", 1337);
		
		recorder.setOutputFile(getFileDescriptorFromClientSocket());		
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
