package de.uvwxy.footpath.core;

/**
 * 
 * @author Paul Smith
 *
 */
public abstract class Positioner {
	public abstract void addStep(double direction);
	public abstract double getProgress();
	
	/**
	 * Check if the difference of the given angles in degrees is less than the given alowed difference
	 * @param v the first angle
	 * @param t the second angle
	 * @param diff the allowed difference
	 * @return true if v <= diff away from t
	 */
	public static boolean isInRange(double v, double t, double diff) {
		if(Math.abs(v-t)<=diff){
			return true;
		}
		if(Math.abs((v+diff)%360-(t+diff)%360)<=diff){
			return true;
		}
		return false;
	}

}
