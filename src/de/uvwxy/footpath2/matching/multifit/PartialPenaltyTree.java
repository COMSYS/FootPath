package de.uvwxy.footpath2.matching.multifit;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import de.uvwxy.footpath2.drawing.DrawToCanvas;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.tools.GeoUtils;

public class PartialPenaltyTree implements DrawToCanvas {
	/*
	 * Set screw (8/5/15)
	 * 
	 * self.MIN_STEP_EXPANSION = 8 # Holds the number of steps the path have to be expanded previously
	 * self.EXPANSION_FREQUENZY = 5 # Holds information how often the expansion step is repeated (in steps)
	 * self.MAX_LEAFS = 15 # Holds the number of leafs the tree should be pruned to
	 */

	// if a leaf is in range of MIN_STEP_EXPANSION: expand it
	public static final int MIN_STEP_EXPANSION = 16; // originally 8
	// repeat expansion check every EXPANSION_FREQUENZY number of steps (and on first step!)
	public static final int EXPANSION_FREQUENZY = 8; // we'll keep the typo for now. FEEEEEEELIX
	// maximum number of leafs to keep in the tree
	public static final int MAX_LEAFS = 20;

	private float virtualStepLength;
	private PPTNode root;

	private int currentStep;

	private LinkedList<Float> bearings = new LinkedList<Float>();

	private IndoorLocation currentBestLocation = null;

	private LinkedList<PPTNode> leafList = null;

	private IndoorLocation destination = null;

	private LinkedList<IndoorLocation> nodesInTree = new LinkedList<IndoorLocation>();

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

	public void _c_setEndLocation(IndoorLocation end) {
		destination = end;
	}

	public IndoorLocation getDestination() {
		return destination;
	}

	public float getVirtualStepLength() {
		return this.virtualStepLength;
	}

	public void onStepUpdate(float bearing, double steplength, long timestamp, double estimatedStepLengthError,
			double estimatedBearingError) {
		currentStep++;
		bearings.add(new Float(bearing));
		int deletedNodes = 0;

		if ((currentStep - 1) % EXPANSION_FREQUENZY == 0) {
			root.recursiveDescentExpand();
			root.recursiveEvaluate(currentStep);

			deletedNodes = pruneTree(MAX_LEAFS);
			nodesInTree.clear();

			// call this to setup the list of expanded nodes to draw
			root.getAllNodes(nodesInTree);
		}

		root.recursiveEvaluate(currentStep);

		// TODO: determine best location
		PPTNode bestNode = root.getBetterChild(Float.POSITIVE_INFINITY);
		// Log.i("FOOTPATH", "bestNode: " + bestNode.getTargetLocation() + "\n" + bestNode.printMatrix());

		Log.i("FOOTPATH", "lastNode: lastPenalty: " + bestNode.getLeafPathPenalty(bestNode.getPathLength()));

		double minIndex = bestNode.getMinIndexFromLastColumn();
		double lastIndex = bestNode.getVirtualLength();
		double factor = minIndex / lastIndex;
		// Log.i("FOOTPATH", "factor = " + minIndex + "/" + lastIndex);
		// LinkedList<PPTNode> path = new LinkedList<PPTNode>();
		// bestNode.getPath(path);
		//
		// for (PPTNode n : path) {
		// Log.i("FOOTPATH", "Path: " + n.printMatrix());
		// }

		// Log.i("FOOTPATH", "bestNode = " + bestNode);
		// Log.i("FOOTPATH", "bestNode.getTargetLocation() = " + bestNode.getTargetLocation());
		// Log.i("FOOTPATH", "factor = " + factor);
		// create new object using copy constructor
		currentBestLocation = new IndoorLocation(bestNode.getParent().getTargetLocation());
		// Log.i("FOOTPATH", "currentBestLocation = " + currentBestLocation);

		// displace according to progress on edge
		currentBestLocation.moveIntoDirection(bestNode.getTargetLocation(), factor);
		// Log.i("FOOTPATH", "currentBestLocation = " + currentBestLocation);

		// Log.i("FOOTPATH", "Level: " + currentBestLocation.getLevel() + " Step: " + currentStep);
		// Log.i("FOOTPATH", "#nodes in Tree: " + getNumberOfNodesInTree());
		// Log.i("FOOTPATH", "#pruned leafs: " + deletedNodes);
	}

	private int pruneTree(int maxLeafs) {
		// setup new empty leaf list:
		leafList = leafList == null ? new LinkedList<PPTNode>() : leafList;
		leafList.clear();
		// add all leafs to list
		root.recursiveAddToLeafListAndCalcMinValueOnPath(Float.POSITIVE_INFINITY);
		Collections.sort(leafList, new LeafMinValueOnPathComparator());

		// returns an ascending list of scores:
		// for (PPTNode n : leafList) {
		// Log.i("FOOTPATH", "[[[" + n.getMinValueOnPath() + "]]]");
		// }

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

		// Log.i("FOOTPATH", "get(" + i + ")");
		return bearings.get(i).floatValue();
	}

	public IndoorLocation getCurrentBestLocation() {
		return this.currentBestLocation;
	}

	public LinkedList<IndoorLocation> getAllNodesInTree() {
		return nodesInTree;
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

	public class LeafMinValueOnPathComparator implements Comparator<PPTNode> {
		@Override
		public int compare(PPTNode lhs, PPTNode rhs) {
			int stepOne = new Float(lhs.getMinValueOnPath()).compareTo(rhs.getMinValueOnPath());

			if (stepOne == 0) {
				int ll = lhs.getPathLength();
				int lr = rhs.getPathLength();
				double llv, llr;
				if (ll > lr){
					llv = lhs.getLeafPathPenalty(lr);
					llr = lhs.getLeafPathPenalty(lr);
				} else {
					llv = lhs.getLeafPathPenalty(ll);
					llr = lhs.getLeafPathPenalty(ll);
				}
				
				int stepTwo = new Float(lhs.getMinValueOnPath()).compareTo(rhs.getMinValueOnPath());;
				return stepTwo;
			} else {
				return stepOne;
			}
		}
	}

	@Override
	public void drawToCanvas(Canvas canvas, IndoorLocation center, Rect boundingBox, double pixelsPerMeterOrMaxValue,
			Paint pLine, Paint pDots) {
		int w = boundingBox.width() / 2 + boundingBox.left;
		int h = boundingBox.height() / 2 + boundingBox.top;

		if (canvas == null || center == null || pLine == null || pDots == null) {
			return;
		}

		for (int i = 0; i < nodesInTree.size() - 1; i++) {
			// draw line between nodes
			IndoorLocation a = nodesInTree.get(i);
			int[] apix = GeoUtils.convertToPixelLocation(a, center, pixelsPerMeterOrMaxValue);
			canvas.drawCircle(w + apix[0], h + apix[1], 3.0f, pDots);
		}
	}
}
