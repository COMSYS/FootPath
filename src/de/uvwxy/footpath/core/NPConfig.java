package de.uvwxy.footpath.core;

/**
 * A class to manage a configuration of the navigation
 * @author Paul Smith
 *
 */
public class NPConfig{
	// Points to the current edge we are walking on
	public int npPointer;
	// How far we have come on this edge
	public double npCurLen;
	// How many unmatched steps we have since the last matched step
	public int npUnmatchedSteps;
	// Which step the last matched step is
	public int npLastMatchedStep;
	// How many total matched steps we have
	public int npMatchedSteps;
	// The step size
	public double npStepSize;
	
	public NPConfig(){
		this.npPointer = 0;
		this.npCurLen = 0.0;
		this.npUnmatchedSteps = 0;
		this.npLastMatchedStep = 0;
		this.npMatchedSteps = 0;
		this.npStepSize = 1.0; // 1.0m...
	}
	
	public NPConfig(NPConfig conf) {
		this.npCurLen = conf.npCurLen;
		this.npLastMatchedStep = conf.npLastMatchedStep;
		this.npMatchedSteps = conf.npMatchedSteps;
		this.npPointer = conf.npPointer;
		this.npStepSize = conf.npStepSize;
		this.npUnmatchedSteps = conf.npUnmatchedSteps;
	}
}
