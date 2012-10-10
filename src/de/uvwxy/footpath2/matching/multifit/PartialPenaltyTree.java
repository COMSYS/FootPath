package de.uvwxy.footpath2.matching.multifit;

import java.util.LinkedList;

import android.util.Log;

import de.uvwxy.footpath2.map.IndoorLocation;

public class PartialPenaltyTree {
	/*
	 * Set screw (8/5/15)
	 * 
	 * self.MIN_STEP_EXPANSION = 8 # Holds the number of steps the path have to be expanded previously
	 * self.EXPANSION_FREQUENZY = 5 # Holds information how often the expansion step is repeated (in steps)
	 * self.MAX_LEAFS = 15 # Holds the number of leafs the tree should be pruned to
	 */

	// if a leaf is in range of MIN_STEP_EXPANSION: expand it
	public static final int MIN_STEP_EXPANSION = 8;
	// repeat expansion check every EXPANSION_FREQUENZY number of steps (and on first step!)
	public static final int EXPANSION_FREQUENZY = 5; // we'll keep the typo for now. FEEEEEEELIX
	// maximum number of leafs to keep in the tree
	public static final int MAX_LEAFS = 15;

	private float virtualStepLength;
	private PPTNode root;

	private int currentStep;

	private LinkedList<Float> bearings = new LinkedList<Float>();

	private IndoorLocation currentBestLocation = null;

	public PartialPenaltyTree() {
		root = new PPTNode(this);
	}

	public void _a_setStartLocation(IndoorLocation start) {
		root.setTargetOnEdge(start);
		currentBestLocation = start;
	}

	public void _b_setVirtualStepLength(float stepLength) {
		this.virtualStepLength = stepLength;
	}

	public float getVirtualStepLength() {
		return this.virtualStepLength;
	}

	public void onStepUpdate(float bearing, double steplength, long timestamp, double estimatedStepLengthError,
			double estimatedBearingError) {
		currentStep++;
		bearings.add(new Float(bearing));

		if ((currentStep - 1) % EXPANSION_FREQUENZY == 0) {
			root.recursiveDescentExpand();
		}

		root.recursiveEvaluate(currentStep);

		// TODO: determine best location
		PPTNode bestNode = root.getBetterChild(Float.POSITIVE_INFINITY);
		double minIndex = bestNode.getMinIndexFromLastColumn();
		double lastIndex = bestNode.getVirtualLength();
		
		// create new object using copy constructor
		currentBestLocation = new IndoorLocation(bestNode.getParent().getTargetLocation());
		// displace according to progress on edge
		currentBestLocation.moveIntoDirection(bestNode.getTargetLocation(), minIndex / lastIndex);

		Log.i("MULTIFIT", "Step: " + currentStep);
		Log.i("MULTIFIT", "#nodes in Tree: " + getNumberOfNodesInTree());
	}

	public float getS(int i) {
		return bearings.get(i).floatValue();
	}

	public IndoorLocation getCurrentBestLocation() {
		return this.currentBestLocation;
	}

	private void ideaPruneAroundPosition(IndoorLocation l) {
		// have multiple trees in memory pruning around different locations?
	}

	public int getNumberOfNodesInTree() {
		return root == null ? 0 : root.getNumberOfNodesInTree();
	}
}
