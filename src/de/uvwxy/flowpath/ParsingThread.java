package de.uvwxy.flowpath;

import java.io.IOException;
import java.io.InputStream;

import de.uvwxy.flowpath.h263.EOSException;
import de.uvwxy.flowpath.h263.H263Parser;


/**
 * CURRENTLY: Creates an H263Parser object and continuously tries to parse a 
 *            frame. Received MVD data is passed to PaintBoxMV object.
 * @author paul
 * 
 */
public class ParsingThread extends Thread {
	private boolean bRunning = false;
	private H263Parser parser = null;

	private FlowPathInterface flowPathInterface = FlowPathInterface.getInterface();
	
	public ParsingThread(InputStream in) {
		
		// int newPtr = boxParser.jumpIntoMDATBox();
		boolean parsePs = true; // parse picture layer
		boolean parseGOBs = true; // parse Group of Blocks layer
		boolean parseMBs = true; // parse Macro Block layer
		boolean parseBs = true; // parse Block layer
		boolean blocking = false; // blocking parsing?
		parser = new H263Parser(in, 0, parsePs, parseGOBs, parseMBs, parseBs,
				blocking);
	}

	public void setRunning(boolean run) {
		bRunning = run;
	}
	
	public boolean isRunning(){
		return bRunning;
	}

	private int frame_count = 0;
	private int periodicity = 1;

	@Override
	public void run() {
		float[][][] mvs = null;

		while (bRunning) {
			// ISOBoxParser boxParser = new ISOBoxParser(in);
			try {
				frame_count++;
				if (frame_count % periodicity == 0) {

					mvs = parser.parseH263Frame();

					if (mvs != null) {
						flowPathInterface.notifyTriggersWithMVD(System.currentTimeMillis(), mvs);
					}
					if(frame_count%60==0)
						System.gc();
				} else {
					parser.skipH263Frame();
				}
			} catch (IOException e) {
				e.printStackTrace();
				break;
			} catch (EOSException e) {
				e.printStackTrace();
			}

		}
		
		parser.closeFis();
	}

	/**
	 * We need access to parser.getStats from the GUI
	 * @return
	 */
	public H263Parser getParser() {
		H263Parser temp = null;
		while (temp == null) {
			temp = this.parser;
		}
		return temp;
	}
}
