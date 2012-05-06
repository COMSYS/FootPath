package de.uvwxy.footpath2.movement;

public class SensorTriple {
	public float[] values;
	long ts;
	int type;
	
	public SensorTriple(float[] values, long ts, int type) {
		this.values = values.clone();
		this.ts = ts;
		this.type = type;
	}
	
	public SensorTriple clone(){
		return new SensorTriple(values,ts,type);
	}
}
