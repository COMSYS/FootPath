package de.uvwxy.footpath2.movement.h263;

import java.io.FileDescriptor;
import java.io.IOException;

import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import de.uvwxy.footpath2.gui.FlowPathTestGUI;

/**
 * Start video capture and write video stream to TCP/IP socket. The following configuration variables are used:
 * FlowPath.PIC_SIZE_WIDTH, FlowPath.PIC_SIZE_HEIGHT, FlowPath.PIC_FPS, FlowPath.PORT, Port of localhost to connect to
 * FlowPath.sh01 SurfaceView for preview and H.263 encoding.
 * 
 * CURRENTLY: Starts the TCP/IP client to send data into.
 * 
 * @author Paul Smith
 * 
 */
public class SocketAudioVideoWriter {
	private MediaRecorder recorder;
	private FileDescriptor fd = null;
	private SurfaceHolder sh = null;

	public SocketAudioVideoWriter(FileDescriptor fd, SurfaceHolder sh) {
		this.fd = fd;
		this.sh = sh;
	}

	/**
	 * Initialization of capture device
	 * 
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@Deprecated
	public void registerCapture() throws IllegalStateException, IOException {
		registerCapture(FlowPathConfig.PIC_FPS, FlowPathConfig.PIC_SIZE_WIDTH, FlowPathConfig.PIC_SIZE_HEIGHT);
	}

	public void registerCapture(int fps, int wx, int wy) throws IllegalStateException, IOException {
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setVideoFrameRate(fps);
		recorder.setVideoSize(wx, wy);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);

		// initialize socket here
		// startClient("127.0.0.1", FlowPathConfig.port);
		// to use fd from socket here
		recorder.setOutputFile(fd);

		if (sh == null) {
			Log.i("FLOWPATH", "sh is null!");
		}
		recorder.setPreviewDisplay(sh.getSurface());

		recorder.prepare();
	}

	/**
	 * Starts capture
	 */
	public void startCapture() {
		recorder.start();
	}

	/**
	 * Stops capture
	 */
	public void stopCapture() {
		recorder.stop();
		recorder.reset();
		// stopClient();
	}

	/**
	 * Release capture device
	 */
	public void unregisterCapture() {
		recorder.release();
	}

	// private Socket sckClient = null;
	// private void startClient(String hostname, int port){
	// try {
	// sckClient = new Socket(InetAddress.getByName(hostname), port);
	// sckClient.setTcpNoDelay(true);
	// } catch (UnknownHostException e2) {
	// e2.printStackTrace();
	// } catch (IOException e2) {
	// e2.printStackTrace();
	// }
	// }
	//
	// private void stopClient(){
	// try {
	// sckClient.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// private FileDescriptor getFileDescriptorFromClientSocket(){
	// return ParcelFileDescriptor.fromSocket(sckClient).getFileDescriptor();
	// }
}
