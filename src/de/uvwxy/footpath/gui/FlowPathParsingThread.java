package de.uvwxy.footpath.gui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Log;
import de.uvwxy.footpath.h263.DebugOut;
import de.uvwxy.footpath.h263.EOSException;
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
	private PaintBoxMVs pbMVs = null;
	
	
	public FlowPathParsingThread(String filePath, PaintBoxMVs pbMVs){
		this.filePath = filePath;
		this.pbMVs = pbMVs;
		
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
		boolean parseGOBs = true; // parse Group of Blocks layer
		boolean parseMBs = true; // parse Macro Block layer
		boolean parseBs = true; // parse Block layer
		boolean blocking = false; // blocking parsing?
		parser = new H263Parser(in, 0, parsePs, parseGOBs, parseMBs, parseBs, blocking);
	}
	
	public void setRunning(boolean run) {
		bRunning = run;
	}

	int frame_count = 0;
	int periodicity = 1;
	long timeMillis = 0;
	@Override
	public void run() {
		double[][][] mvs = null;
		timeMillis = System.currentTimeMillis();
		
		log.debug_v("parsing started");
		while(bRunning){
		//		ISOBoxParser boxParser = new ISOBoxParser(in);
			try {
				log.debug_v("parsing next frame");
				frame_count++;
				if (frame_count % periodicity == 0){
					Log.i("FLOWPATH", "parsing frame after (" + (System.currentTimeMillis()-timeMillis) + "ms)");
					timeMillis = System.currentTimeMillis();
					mvs = parser.parseH263Frame();
					
					if (mvs != null){
						// wahay we have dem vectorz
						log.debug_v("read " + mvs.length*mvs[0].length + " vectors");
						if (pbMVs!=null){
							pbMVs.updateMVs(mvs);
						}
					}
				
				} else {
					Log.i("FLOWPATH", "skipping frame");
					parser.skipH263Frame();
				}
			} catch (IOException e) {
				log.debug_v("error parsing H263 "
						+ e.getLocalizedMessage());
				e.printStackTrace();
			} catch (EOSException e) {
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
