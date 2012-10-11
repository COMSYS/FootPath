package de.uvwxy.footpath2.matching.multifit;

import java.lang.reflect.Array;
import java.util.LinkedList;

import android.util.Log;

import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.matching.Score;

public class PPTNode {
	private PartialPenaltyTree ppt = null;
	private Score score = new Score();

	private int virtualLength;
	private float bearing;

	private boolean isRoot = false;
	private PPTNode parent;
	private LinkedList<PPTNode> children = new LinkedList<PPTNode>();
	private LinkedList<float[]> matrix = new LinkedList<float[]>();

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
		for (int i = 0; i < virtualLength; i++) {
			firstColumn[i] = Float.POSITIVE_INFINITY;
		}
		matrix.add(firstColumn);
	}

	public int getNumberOfNodesInTree() {
		int buf = 1;
		for (PPTNode n : children) {
			if (n != null) {
				buf += n.getNumberOfNodesInTree();
			}
		}
		return buf;
	}

	public PPTNode getBetterChild(float penalty) {
		if (children == null || children.size() == 0) {
			// leaves return themselves
			Log.i("FOOTPATH", "Returning this");
			return this;
		}

		PPTNode smallestChild = null;
		for (PPTNode n : children) {
			if (n != null) {
				// get best
				PPTNode otherSmallestChild = n.getBetterChild(penalty);
				float checkPenalty = otherSmallestChild.getMinValueFromLastColumn();
				if (checkPenalty < penalty) {
					penalty = checkPenalty;
					smallestChild = otherSmallestChild;
				}
			}
		}

		// some error avoidance, return this if the list of children was bogus
		smallestChild = smallestChild == null ? this : smallestChild;

		return smallestChild;
	}

	public float getMinValueFromLastColumn() {
		float[] column = matrix.getLast();

		float min = Float.POSITIVE_INFINITY;
		if (column == null)
			return min;

		for (float f : column)
			min = f < min ? f : min;

		return min;
	}

	public int getMinIndexFromLastColumn() {
		float[] column = matrix.getLast();

		float min = Float.POSITIVE_INFINITY;
		int index = -1;
		if (column == null)
			return index;

		for (int i = 0; i < column.length; i++) {
			if (column[i] <= min)
				index = i;
		}
		return index;
	}

	public IndoorLocation getTargetLocation() {
		return this.targetOnEdge;
	}

	/**
	 * 
	 * @param iVirtStep
	 *            (<= virtual length)
	 * @param jStep
	 *            (number of detected step)
	 * @return
	 */
	public float getPenaltyValue(int iVirtStep, int jStep) {
		if (isRoot) {
			return jStep == 0 ? 0 : Float.POSITIVE_INFINITY;
		}
		// Log.i("FOOTPATH", "matrix.get(" + jStep + ")[" + iVirtStep + "])");
		// Log.i("FOOTPATH", "matrix.size()=" + matrix.size());
		// Log.i("FOOTPATH", " matrix.get(i).length = " + matrix.get(jStep).length);
		return matrix.get(jStep)[iVirtStep];
	}

	public float getBearing(int virtualStep) {
		return (virtualStep > 0) ? bearing : parent.getBearing(parent.getVirtualLength());
	}

	public int getVirtualLength() {
		return virtualLength;
	}

	public void setTargetOnEdge(IndoorLocation l) {
		targetOnEdge = l;
	}

	private void setPenaltyValue(int iVirtStep, int jStep, float v) {
		// Log.i("FOOTPATH", "matrix.get(" + jStep + ")[" + iVirtStep + "] = " + v);
		matrix.get(jStep)[iVirtStep] = v;
	}

	public PPTNode getParent() {
		return parent;
	}

	public LinkedList<PPTNode> getChildren() {
		return children;
	}

	public void addChild(PPTNode n) {
		children.add(n);
	}

	/**
	 * Idea: carry the stepNumber -> thus use the same function for newly added expanded leaf nodes which are still
	 * empty
	 * 
	 * @param bearing
	 * @param stepNumber
	 */
	public void recursiveEvaluate(int stepNumber) {
		Log.i("FOOTPATH", "Pre Evaluation:" + printMatrix());

		// TODO: evaluate this node if it is not a root node
		if (!isRoot) {
			// check for missing columns:
			// matrix := [initColumns, step=1, step=2,...]

			int oldSize = matrix.size();
			int missingNumberOfColums = (stepNumber - matrix.size() + 1);
			// Log.i("FOOTPATH", "stepNumber - matrix.size() + 1 = " + missingNumberOfColums);
			for (int i = 0; i < missingNumberOfColums; i++) {
				float[] empty = new float[virtualLength];
				for (float f : empty)
					f = 0;
				matrix.add(empty);
				Log.i("FOOTPATH", "***Added empty column to matrix");
			}

			// 1 <= i <= ~l(e_l); 1 <= j <= |S|
			for (int jStep = oldSize; jStep <= stepNumber; jStep++) {
				for (int iVirtStep = 1; iVirtStep < virtualLength; iVirtStep++) {
					// Log.i("FOOTPATH", "" + jStep + "/" + stepNumber + "  " + iVirtStep + "/" + virtualLength);
					// if this is a node that has been added due to expansion we have to recalculate all previous steps
					// for
					// this edge.

					// D(e_l)(i,j) = min { a, b , c } WHERE
					// a := D(e_l)(i-1,j-1) + score(M(e_l)(i), S(j)) NOTE: "fixed" S(i) -> S(j)
					// b := D(e_l)(i-1,j) + score(M(e_l)(i), S(j-1))
					// c := D(e_l)(i,j-1) + score(M(e_l)(i-1), S(j))

					// below is done with getBearing(vstep).
					// D(e_l)(0,j) = D(e_(l-1))(|e_(l-1)|,j) <- TOP row accesses previous edge last row

					double a = getPenaltyValue(iVirtStep - 1, jStep - 1)
							+ score.score(getBearing(iVirtStep), ppt.getS(jStep), false);
					double b = getPenaltyValue(iVirtStep - 1, jStep)
							+ score.score(getBearing(iVirtStep), ppt.getS(jStep - 1), true);
					double c = getPenaltyValue(iVirtStep, jStep - 1)
							+ score.score(getBearing(iVirtStep - 1), ppt.getS(jStep), true);

					double d = Math.min(a, Math.min(b, c));

					setPenaltyValue(iVirtStep, jStep, (float) d);
				}
			}
		}
		
		Log.i("FOOTPATH", "Post Evaluation:" + printMatrix());

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
				newNode.setTargetOnEdge(adjn);
				// not needed as it is still calld by PPT after these recursion steps.
				// newNode.recursiveEvaluate(matrix.size());
				Log.i("FOOTPATH", "***###*** Expand");
				children.add(newNode);
			}
		}
	}

	public void recursiveAddToLeafList() {
		if (children == null || children.size() == 0) {
			// leaves add themselves
			ppt.addToLeafList(this);
			return;
		}
		for (PPTNode n : children) {
			if (n != null) {
				n.recursiveAddToLeafList();
			}
		}

	}

	public void leafTriggerRemoval() {
		parent.recursiveRemoveMeFromYourPath(this);
	}

	public void recursiveRemoveMeFromYourPath(PPTNode n) {
		if (children != null) {
			children.remove(n);
		}

		if (children.size() == 0) {
			parent.recursiveRemoveMeFromYourPath(this);
		}
	}

	public String printMatrix() {
		String buf = isRoot ? "Root:\n" : "Node:\n";
		for (int y = 0; y < virtualLength; y++) {
			for (int x = 0; x < matrix.size(); x++) {
				buf += matrix.get(x)[y] + " ";
			}
			buf += "\n";
		}
		return buf;
	}

}
