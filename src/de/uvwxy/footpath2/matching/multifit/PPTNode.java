package de.uvwxy.footpath2.matching.multifit;

import java.util.LinkedList;

import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.matching.Score;

public class PPTNode {
	private PartialPenaltyTree ppt = null;
	private Score score = new Score();

	private int virtualLength;
	private float bearing;

	private boolean isRoot = false;
	private PPTNode parent;
	private LinkedList<PPTNode> children;
	private LinkedList<float[]> matrix;

	private IndoorLocation targetOnEdge;

	public PPTNode(PartialPenaltyTree ppt) {
		isRoot = true;
		virtualLength = 1;
		parent = null;
		this.ppt = ppt;
	}

	public PPTNode(PartialPenaltyTree ppt, int virtualLength, float bearing, PPTNode parent) {
		isRoot = false;
		this.virtualLength = virtualLength;
		this.bearing = bearing;
		this.parent = parent;
		this.ppt = ppt;
		// create and fill first column with infty.
		float[] firstColumn = new float[virtualLength];
		for (float f : firstColumn)
			f = Float.POSITIVE_INFINITY;
	}

	public void setTargetOnEdge(IndoorLocation l) {
		targetOnEdge = l;
	}

	/**
	 * 
	 * @param i
	 *            (<= virtual length)
	 * @param j
	 *            (number of detected step)
	 * @return
	 */
	public float getPenaltyValue(int i, int j) {
		if (isRoot) {
			return j == 0 ? 0 : Float.POSITIVE_INFINITY;
		}
		return matrix.get(j)[i];
	}

	public float getBearing(int virtualStep) {
		return (virtualStep > 0) ? bearing : parent.getBearing(parent.getVirtualLength());
	}

	public int getVirtualLength() {
		return virtualLength;
	}

	public void addChild(PPTNode n) {
		children.add(n);
	}

	public PPTNode getParent() {
		return parent;
	}

	public LinkedList<PPTNode> getChildren() {
		return children;
	}

	/**
	 * Idea: carry the stepNumber -> thus use the same function for newly added expanded leaf nodes which are still
	 * empty
	 * 
	 * @param bearing
	 * @param stepNumber
	 */
	public void recursiveEvaluate(int stepNumber) {
		// TODO: evaluate this node if it is not a root node
		if (!isRoot) {
			// 1 <= i <= ~l(e_l); 1 <= j <= |S|
			for (int j = matrix.size(); j <= stepNumber; j++) {
				for (int i = 1; i < virtualLength; i++) {
					// if this is a node that has been added due to expansion we have to recalculate all previous steps
					// for
					// this edge.

					// D(e_l)(i,j) = min { a, b , c } WHERE
					// a := D(e_l)(i-1,j-1) + score(M(e_l)(i), S(j)) NOTE: "fixed" S(i) -> S(j)
					// b := D(e_l)(i-1,j) + score(M(e_l)(i), S(j-1))
					// c := D(e_l)(i,j-1) + score(M(e_l)(i-1), S(j))

					// below is done with getBearing(vstep).
					// D(e_l)(0,j) = D(e_(l-1))(|e_(l-1)|,j) <- TOP row accesses previous edge last row

					double a = getPenaltyValue(i - 1, j - 1) + score.score(getBearing(i), ppt.getS(j), false);
					double b = getPenaltyValue(i - 1, j) + score.score(getBearing(i), ppt.getS(j - 1), true);
					double c = getPenaltyValue(i, j - 1) + score.score(getBearing(i - 1), ppt.getS(j), true);

					double d = Math.min(a, Math.min(b, c));

				}
			}
		}
		for (PPTNode node : children) {
			if (node != null)
				node.recursiveEvaluate(stepNumber);
		}
	}

	public void recursiveDescentExpand() {
		if (children == null || children.size() == 0) {
			expandThisNode();
		} else {
			for (PPTNode n : children) {
				if (n != null) {
					n.recursiveDescentExpand();
				}
			}
		}
	}

	private void expandThisNode() {
		LinkedList<IndoorLocation> adjNodes = (LinkedList<IndoorLocation>) targetOnEdge.getAdjacentIndoorLocations();
		for (IndoorLocation adjn : adjNodes) {
			// we need our location here to estimate if this node expanded at all
			// (targetOnEdge.distanceTo(my_location) / ppt.getVirtualStepLength()) <= ppt.MIN_STEP_EXPANSION
			IndoorLocation loc = ppt.getCurrentBestLocation();
			if (adjn != null && loc != null
					&& (loc.distanceTo(targetOnEdge) / ppt.getVirtualStepLength()) <= ppt.MIN_STEP_EXPANSION) {
				// is this nearest integer? floor (x + 0.5) -> yes!
				int e_l = Math.round(targetOnEdge.distanceTo(adjn) / ppt.getVirtualStepLength());
				PPTNode newNode = new PPTNode(this.ppt, e_l, targetOnEdge.bearingTo(adjn), this);

				// not needed as it is still calld by PPT after these recursion steps.
				// newNode.recursiveEvaluate(matrix.size());

				children.add(newNode);
			}
		}
	}
	
	public int getNumberOfNodesInTree(){
		int buf = 1;
		for (PPTNode n : children){
			if (n!=null){
				buf += n.getNumberOfNodesInTree();
			}
		}
		return buf;
	}
	
}
