package de.uvwxy.footpath2.matching;

/**
 * A class to manage a configuration of the navigation
 * 
 * @author Paul Smith
 * 
 */
public class NPConfig {
	// Points to the current edge we are walking on
	private int npPointer;
	// How far we have come on this edge
	private double npCurLen;
	// How many unmatched steps we have since the last matched step
	private int npUnmatchedSteps;
	// Which step the last matched step is
	private int npLastMatchedStep;
	// How many total matched steps we have
	private int npMatchedSteps;
	// The step size
	private double npStepSize;

	public NPConfig() {
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

	/**
	 * @return the npPointer
	 */
	public int getNpPointer() {
		return npPointer;
	}

	/**
	 * @param npPointer
	 *            the npPointer to set
	 */
	public void setNpPointer(int npPointer) {
		this.npPointer = npPointer;
	}

	/**
	 * @return the npCurLen
	 */
	public double getNpCurLen() {
		return npCurLen;
	}

	/**
	 * @param npCurLen
	 *            the npCurLen to set
	 */
	public void setNpCurLen(double npCurLen) {
		this.npCurLen = npCurLen;
	}

	/**
	 * @return the npUnmatchedSteps
	 */
	public int getNpUnmatchedSteps() {
		return npUnmatchedSteps;
	}

	/**
	 * @param npUnmatchedSteps
	 *            the npUnmatchedSteps to set
	 */
	public void setNpUnmatchedSteps(int npUnmatchedSteps) {
		this.npUnmatchedSteps = npUnmatchedSteps;
	}

	/**
	 * @return the npLastMatchedStep
	 */
	public int getNpLastMatchedStep() {
		return npLastMatchedStep;
	}

	/**
	 * @param npLastMatchedStep
	 *            the npLastMatchedStep to set
	 */
	public void setNpLastMatchedStep(int npLastMatchedStep) {
		this.npLastMatchedStep = npLastMatchedStep;
	}

	/**
	 * @return the npMatchedSteps
	 */
	public int getNpMatchedSteps() {
		return npMatchedSteps;
	}

	/**
	 * @param npMatchedSteps
	 *            the npMatchedSteps to set
	 */
	public void setNpMatchedSteps(int npMatchedSteps) {
		this.npMatchedSteps = npMatchedSteps;
	}

	/**
	 * @return the npStepSize
	 */
	public double getNpStepSize() {
		return npStepSize;
	}

	/**
	 * @param npStepSize
	 *            the npStepSize to set
	 */
	public void setNpStepSize(double npStepSize) {
		this.npStepSize = npStepSize;
	}

}
