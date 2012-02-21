package de.uvwxy.flowpath;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceHolder;


/**
 * This class should provide an interface to easily obtain movement readings
 * from the camera. This should facilitate a simple means of integrating
 * flowpath into another project/navigator gui instance.
 * 
 * @author Paul Smith
 * 
 */
public class FlowPathInterface {
	private static final FlowPathInterface singleton = new FlowPathInterface();
	
	private ParcelFileDescriptor[] fds = null;
	
	private FlowPathInterface(){
		// Set Constructor to private so no further object instantiation
		// -> FlowPathInterface has to be singleton
	}
	
	public static FlowPathInterface getInterface(){
		return singleton;
	}
	
	SocketAudioVideoWriter avwCapture;
	
	private ParsingThread parsingThread = null;




	LinkedList<MVDTrigger> mvdTriggers = new LinkedList<MVDTrigger>();
	
		
	public void addMVDTrigger(MVDTrigger t){
		mvdTriggers.add(t);
	}

	protected void notifyTriggersWithMVD(long now_ms, float[][][] mvds){
		if (mvds == null)
			return;
		
		for (MVDTrigger t : mvdTriggers){
			if (t != null){
				t.processMVData(now_ms, mvds);
			}
		}
	}
	
	public boolean startFlowpath(SurfaceHolder sh) {
		
		try {
			// The first ParcelFileDescriptor in the returned array is the read 
			// side; the second is the write side.
			fds = ParcelFileDescriptor.createPipe();
		} catch (IOException e1) {
			// no pipes created
			e1.printStackTrace();
			return false;
		}
		
		
		
//		startServer(++FlowPathConfig.port);

		// create audio writer + start it
		avwCapture = new SocketAudioVideoWriter(fds[1].getFileDescriptor(), sh);
		try {
			avwCapture.registerCapture();
		} catch (IllegalStateException e) {
			// failed
			Log.i("FLOWPATH", "Failed to register capture device (ISE).");
			return false;
		} catch (IOException e) {
			// failed
			Log.i("FLOWPATH", "Failed to register capture device (IOE).");
			return false;
		}

		avwCapture.startCapture();

//		while (sckSrvCon == null) {
//			try {
//				Thread.sleep(50);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}

		
		FileInputStream x = new FileInputStream(fds[0].getFileDescriptor());
		
//		InputStream sckIn = null;
//		try {
//			sckIn = sckSrvCon.getInputStream();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		parsingThread = new ParsingThread(x);
		parsingThread.setRunning(true);
		parsingThread.start();

		return true;
	}

	public void stopFlowPath() {
		if(parsingThread.isRunning())
			parsingThread.setRunning(false);

		avwCapture.stopCapture();
		avwCapture.unregisterCapture();

//		try {
//			sckSrvCon.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	public String getStats() {
		return parsingThread.getParser().getStats();
	}
	
	// SOCKET STUFF:
//	private ServerThread st = new ServerThread();
//	// server socket + functions:
//	private ServerSocket sckSrvListen = null;
//	private Socket sckSrvCon = null;

//	private class ServerThread extends Thread {
//		public void run() {
//			accept();
//		}
//	}
//
//	private void accept() {
//		try {
//			sckSrvCon = sckSrvListen.accept();
//			sckSrvCon.setTcpNoDelay(true);
////			sckSrvCon.setReceiveBufferSize(128);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void startServer(int port) {
//		try {
//			sckSrvListen = new ServerSocket(port);
//			st = new ServerThread();
//			st.start();
//		} catch (IOException e3) {
//			e3.printStackTrace();
//		}
//	}
}
