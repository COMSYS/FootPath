package de.uvwxy.footpath2.matching.multifit;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

	private LinkedList<PPTNode> leafList = null;

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

		Log.i("FOOTPATH", "bestNode = " + bestNode);
		Log.i("FOOTPATH", "bestNode.getTargetLocation() = " + bestNode.getTargetLocation());
		// create new object using copy constructor
		currentBestLocation = new IndoorLocation(bestNode.getParent().getTargetLocation());
		// displace according to progress on edge
		currentBestLocation.moveIntoDirection(bestNode.getTargetLocation(), minIndex / lastIndex);

		int deletedNodes = pruneTree(MAX_LEAFS);

		Log.i("FOOTPATH", "Step: " + currentStep);
		Log.i("FOOTPATH", "#nodes in Tree: " + getNumberOfNodesInTree());
		Log.i("FOOTPATH", "#pruned leafs: " + deletedNodes);
	}

	private int pruneTree(int maxLeafs) {
		// setup new empty leaf list:
		leafList = leafList == null ? new LinkedList<PPTNode>() : leafList;
		leafList.clear();
		// add all leafs to list
		root.recursiveAddToLeafList();
		Collections.sort(leafList, new LeafMinValueComparator());

		LinkedList<PPTNode> deleteList = new LinkedList<PPTNode>();
		
		int del = 0;
		while (leafList.size() > maxLeafs) {
			deleteList.add(leafList.getLast());
			leafList.removeLast();
			del++;
		}

		for (PPTNode leaf : deleteList) {
			if (leaf != null) {
				leaf.leafTriggerRemoval();
			}
		}
		leafList.clear();
		return del;
	}

	public float getS(int i) {
		i--;
		// penalty matrix is looking for step 0 -> -1
		i = i < 0 ? 0 : i;

		Log.i("FOOTPATH", "get(" + i + ")");
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

	public void addToLeafList(PPTNode n) {
		leafList.add(n);
	}

	public class LeafMinValueComparator implements Comparator<PPTNode> {
		@Override
		public int compare(PPTNode lhs, PPTNode rhs) {
			return new Float(lhs.getMinValueFromLastColumn()).compareTo(rhs.getMinValueFromLastColumn());
		}
	}
}
