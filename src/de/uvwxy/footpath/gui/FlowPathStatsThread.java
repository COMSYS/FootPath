package de.uvwxy.footpath.gui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.uvwxy.footpath.h263.DebugOut;
import de.uvwxy.footpath.h263.H263Parser;

public class FlowPathStatsThread extends Thread{
	private boolean bRunning = false;
	private H263Parser parser = null;
	private String filePath = null;
	private DebugOut log = null;

	public FlowPathStatsThread(H263Parser p ){
		this.parser = p;
	}
	
	public void setRunning(boolean run) {
		bRunning = run;
	}

	@Override
	public void run() {		
	
	}
}
