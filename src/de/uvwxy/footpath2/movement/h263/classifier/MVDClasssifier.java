package de.uvwxy.footpath2.movement.h263.classifier;

public interface MVDClasssifier {
	public int classify(long now_ms, float[][][] mvds);
}
