package de.uvwxy.flowpath;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import android.util.Log;


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
	
	private FlowPathInterface(){
		// Set Constructor to private so no further object instantiation
		// -> FlowPathInterface has to be singleton
	}
	
	public static FlowPathInterface getInterface(){
		return singleton;
	}
	
	SocketAudioVideoWriter avwCapture;
	
	private ParsingThread parsingThread = null;

	// server socket + functions:
	private ServerSocket sckSrvListen = null;
	private Socket sckSrvCon = null;

	private ServerThread st = new ServerThread();

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
	
	public boolean startFlowpath() {
		startServer(++FlowPathConfig.port);

		// create audio writer + start it
		avwCapture = new SocketAudioVideoWriter();
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

		while (sckSrvCon == null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		InputStream sckIn = null;
		try {
			sckIn = sckSrvCon.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		parsingThread = new ParsingThread(sckIn);
		parsingThread.setRunning(true);
		parsingThread.start();

		return true;
	}

	public void stopFlowPath() {
		parsingThread.setRunning(false);

		avwCapture.stopCapture();
		avwCapture.unregisterCapture();

		try {
			sckSrvCon.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getStats() {
		return parsingThread.getParser().getStats();
	}
	
	// SOCKET STUFF:
	
	private class ServerThread extends Thread {
		public void run() {
			accept();
		}
	}

	private void accept() {
		try {
			sckSrvCon = sckSrvListen.accept();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startServer(int port) {
		try {
			sckSrvListen = new ServerSocket(port);
			st = new ServerThread();
			st.start();
		} catch (IOException e3) {
			e3.printStackTrace();
		}
	}
}
