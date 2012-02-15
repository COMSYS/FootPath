package de.uvwxy.flowpath;

public interface MVDTrigger {
	public void processMVData(long now_ms, float[][][] mvds);
}
