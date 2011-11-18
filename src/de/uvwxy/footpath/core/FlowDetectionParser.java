package de.uvwxy.footpath.core;

import android.util.Log;

public class FlowDetectionParser extends Thread{
	private FlowDetection fd = null;

	public FlowDetectionParser(FlowDetection fd) {
		this.fd = fd;
	}

	@Override
	public void run() {
		int i = 3;
		while (i > 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			fd.parse();
			i--;
		}
		Log.i("FOOTPATH", ".undloadFlowDetection()");
		fd.undloadFlowDetection();
		Log.i("FOOTPATH", "FlowDetection is Done");
	}
}
