package de.uvwxy.footpath.gui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.uvwxy.footpath.h263.DebugOut;
import de.uvwxy.footpath.h263.H263Parser;

/**
 * 
 * @author paul
 *
 */
public class FlowPathParsingThread extends Thread{
	private boolean bRunning = false;
	private H263Parser parser = null;
	private String filePath = null;
	private DebugOut log = null;
	
	public FlowPathParsingThread(String filePath){
		this.filePath = filePath;
		log = new DebugOut(false,false,false);
		
		FileInputStream in;
		try {
			in = new FileInputStream(filePath);
		} catch (FileNotFoundException e1) {
			log.debug_vv("File " + filePath + " not found!");
			e1.printStackTrace();
			return;
		}
		
//		int newPtr = boxParser.jumpIntoMDATBox();
		boolean parsePs = true; // parse picture layer
		boolean parseGOBs = false; // parse Group of Blocks layer
		boolean parseMBs = false; // parse Macro Block layer
		boolean parseBs = false; // parse Block layer
		boolean blocking = false; // blocking parsing?
		parser = new H263Parser(in, 0, parsePs, parseGOBs, parseMBs, parseBs, blocking);
	}
	
	public void setRunning(boolean run) {
		bRunning = run;
	}

	@Override
	public void run() {		
		log.debug_v("parsing started");
		while(bRunning){
		//		ISOBoxParser boxParser = new ISOBoxParser(in);
			try {
				log.debug_v("parsing next frame");
				parser.parseH263Frame();
			} catch (IOException e) {
				log.debug_v("error parsing H263 "
						+ e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		log.debug_v("parsing stopped");
	}
	
	public H263Parser getParser(){
		H263Parser temp = null;
		while (temp == null) {
			temp = this.parser;
		}
		return temp;
	}
}
