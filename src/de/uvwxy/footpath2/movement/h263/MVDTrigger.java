package de.uvwxy.footpath2.movement.h263;

public interface MVDTrigger {
	public void processMVData(long now_ms, float[][][] mvds);
}
