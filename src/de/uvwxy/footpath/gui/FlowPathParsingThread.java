package de.uvwxy.footpath.gui;

import java.io.IOException;
import java.io.InputStream;

import de.uvwxy.footpath.h263.EOSException;
import de.uvwxy.footpath.h263.H263Parser;

/**
 * 
 * @author paul
 * 
 */
public class FlowPathParsingThread extends Thread {
	private boolean bRunning = false;
	private H263Parser parser = null;
	private PaintBoxMVs pbMVs = null;

	public FlowPathParsingThread(PaintBoxMVs pbMVs,
			InputStream in) {
		this.pbMVs = pbMVs;
		
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

	int frame_count = 0;
	int periodicity = 1;

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
						// wahay we have dem vectorz
						if (pbMVs != null) {
							pbMVs.updateMVs(mvs);
						}
					}
				} else {
					parser.skipH263Frame();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (EOSException e) {
				e.printStackTrace();
			}

		}
		
		parser.closeFis();
	}

	public H263Parser getParser() {
		H263Parser temp = null;
		while (temp == null) {
			temp = this.parser;
		}
		return temp;
	}
}
