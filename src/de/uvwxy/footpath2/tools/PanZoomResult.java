package de.uvwxy.footpath2.tools;

public class PanZoomResult {
	public float x;
	public float y;
	public float scale;
	public PanZoomType type;
	
	
	public void resetResult() {
		type= PanZoomType.NONE;
		x = 0;
		y = 0;
		scale = 0;
	}
}
