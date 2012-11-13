package de.uvwxy.footpath2.matching.multifit;

import java.lang.reflect.Array;
import java.util.LinkedList;

import android.util.Log;

import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.matching.Score;
import de.uvwxy.footpath2.matching.ScoreMultiFit;

public class PPTNode {
	private PartialPenaltyTree ppt = null;
	private static ScoreMultiFit score = new ScoreMultiFit();

	private int virtualLength;
	private float bearing;

	private boolean isRoot = false;
	private PPTNode parent;
	private LinkedList<PPTNode> children = new LinkedList<PPTNode>();
	private LinkedList<float[]> matrix = new LinkedList<float[]>();

	private IndoorLocation targetOnEdge;

	private float minValueOnPath = Float.POSITIVE_INFINITY;

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

	/**
	 * Returns null if all children are worse, or self is not better!
	 * 
	 * @param penalty
	 * @return
	 */
	public PPTNode getBetterChild(float penalty) {
		if (children == null || children.size() == 0) {
			if (penalty < this.getMinValueFromLastColumn())
				return null;
			else
				return this;
		}

		float checkPenalty = this.getMinValueFromLastColumn();
		// if we are better than our previous parents than we have to search for a better node with our best value
		checkPenalty = checkPenalty < penalty ? checkPenalty : penalty;

		PPTNode smallestChild = null;
		for (PPTNode n : children) {
			if (n != null) {
				// get best

				PPTNode otherSmallestChild = n.getBetterChild(checkPenalty);
				if (otherSmallestChild == null) {
					continue;
				} else {
					checkPenalty = otherSmallestChild.getMinValueFromLastColumn();
					smallestChild = otherSmallestChild;
				}
			}
		}

		// if all children are not better than us or the original penalty, return nothing from this (sub) tree
		if (smallestChild == null && penalty < this.getMinValueFromLastColumn())
			return null;

		if (smallestChild == null && penalty >= this.getMinValueFromLastColumn())
			return this;

		return smallestChild;
	}

	public float getMinValueFromLastColumn() {
		// Fix Nullpointer on startup?
		if (matrix.size() == 0) {
			return Float.POSITIVE_INFINITY;
		}

		float[] column = matrix.getLast();

		float min = Float.POSITIVE_INFINITY;
		if (column == null)
			return min;

		for (float f : column)
			min = f < min ? f : min;

		return min;
	}

	public int getMinIndexFromLastColumn() {
		if (isRoot)
			return 0;

		float[] column = matrix.getLast();

		float min = Float.POSITIVE_INFINITY;
		int index = -1;
		if (column == null)
			return index;

		for (int i = 0; i < column.length; i++) {
			if (column[i] <= min) {
				index = i;
				min = column[i];
			}
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
		// TODO: do we ask for a value from above? NO!
		if (iVirtStep >= 0)
			return matrix.get(jStep)[iVirtStep];
		else
			return getParent().getPenaltyValue(getParent().getVirtualLength() - 1, jStep);
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
		// Log.i("FOOTPATH", "Pre Evaluation:" + printMatrix());

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
				// Log.i("FOOTPATH", "***Added empty column to matrix");
			}

			// 1 <= i <= ~l(e_l); 1 <= j <= |S|
			for (int jStep = oldSize; jStep <= stepNumber; jStep++) {
				for (int iVirtStep = 0; iVirtStep < virtualLength; iVirtStep++) {
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
							+ score.score(getBearing(iVirtStep), ppt.getS(jStep), 0);
					double b = getPenaltyValue(iVirtStep - 1, jStep)
							+ score.score(getBearing(iVirtStep), ppt.getS(jStep - 1), -1);
					double c = getPenaltyValue(iVirtStep, jStep - 1)
							+ score.score(getBearing(iVirtStep - 1), ppt.getS(jStep), 1);

					double d = Math.min(a, Math.min(b, c));

					setPenaltyValue(iVirtStep, jStep, (float) d);
				}
			}
		}

		// Log.i("FOOTPATH", "Post Evaluation:" + printMatrix());

		for (PPTNode node : children) {
			if (node != null)
				node.recursiveEvaluate(stepNumber);
		}

	}

	public void recursiveDescentExpand() {
		if (children == null || children.size() == 0) {
			// number of steps left to check during expansion depend on the distance of our endnode/(a.k.a targetnode)
			// and the best estimated location
			expandThisNode((int) (ppt.MIN_STEP_EXPANSION - this.getTargetLocation().distanceTo(
					ppt.getCurrentBestLocation())
					/ ppt.getVirtualStepLength()));
		} else {
			for (PPTNode n : children) {
				if (n != null) {
					n.recursiveDescentExpand();
				}
			}
		}
	}

	/**
	 * TODO: change expansions
	 */
	private void expandThisNode(int virtualStepsToGo) {
		if (virtualStepsToGo <= 0)
			return;
		// Log.i("FOOTPATH", "Stepping into " + targetOnEdge.getId() + " " + getVirtualLength());
		LinkedList<IndoorLocation> adjNodes = (LinkedList<IndoorLocation>) targetOnEdge
				.getAdjacentIndoorLocationsWithoutElevators();
		for (IndoorLocation adjacentNode : adjNodes) {
			// we need our location here to estimate if this node expanded at all
			// (targetOnEdge.distanceTo(my_location) / ppt.getVirtualStepLength()) <= ppt.MIN_STEP_EXPANSION
			IndoorLocation loc = ppt.getCurrentBestLocation();
			if (adjacentNode != null
					&& loc != null
					&& (loc.distanceTo(targetOnEdge) / ppt.getVirtualStepLength()) <= PartialPenaltyTree.MIN_STEP_EXPANSION) {

				// do not add doors (cleanup thingy for hoern)
				if ((adjacentNode.isDoor() && adjacentNode.getDegree() <= 1) || adjacentNode.getDegree() <= 1)
					continue;

				// e_l: edge virtual length: is this nearest integer? floor (x + 0.5) -> yes!
				int e_l = Math.round(targetOnEdge.distanceTo(adjacentNode) / ppt.getVirtualStepLength());
				PPTNode newNode = new PPTNode(this.ppt, e_l, targetOnEdge.bearingTo(adjacentNode), this);
				newNode.setTargetOnEdge(adjacentNode);
				// not needed as it is still calld by PPT after these recursion steps.
				// newNode.recursiveEvaluate(matrix.size());
				// Log.i("FOOTPATH", "***###*** Expand");

				// recursively expand down the path until we are far enough
				if (virtualStepsToGo - getVirtualLength() > 0)
					newNode.expandThisNode(virtualStepsToGo - virtualLength);

				if (!isRoot && this.parent != null && !this.parent.isRoot) {
					PPTNode me = this;
					PPTNode meP = this.parent;
					PPTNode mePP = meP != null ? meP.parent : null;
					// inhibit bouncing in graph but permit going back
					if (meP != null && mePP != null && !me.equals(mePP))
						children.add(newNode);
				} else {
				// root always expands!
				children.add(newNode);
				 }
			}
		}
	}

	public float getMinValueOnPath() {
		return minValueOnPath;
	}

	public float getLeafPathPenalty(int i) {
		int index = getPathLength() - i;

		// if requested position is further than our path length return last penalty
		if (index < 0) {
			return matrix.getLast()[virtualLength - 1];
		}

		// if the penalty we want is above us:
		if (index > virtualLength) {
			return parent.getLeafPathPenalty(i);
		}

		// the difference of the path length and requested index is from end to start
		// thus calcualte correct index
		index = virtualLength - index;
		// boundary check
		index = (index >= virtualLength - 1) ? virtualLength - 1 : index;
		if (matrix.size() == 0)
			return Float.MAX_VALUE;
		else
			return matrix.getLast()[index];
	}

	public int getPathLength() {
		if (isRoot)
			return virtualLength;
		return virtualLength + parent.getPathLength();
	}

	public void recursiveAddToLeafListAndCalcMinValueOnPath(float minv) {
		float mMinV = getMinValueFromLastColumn();
		// updates smallest value
		minv = minv < mMinV ? minv : mMinV;
		minValueOnPath = minv;

		if (children == null || children.size() == 0) {
			// leaves add themselves
			ppt.addToLeafList(this);
			return;
		}
		for (PPTNode n : children) {
			if (n != null) {
				n.recursiveAddToLeafListAndCalcMinValueOnPath(minv);
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
		if (parent != null && parent.targetOnEdge != null) {
			buf += "direction: " + parent.targetOnEdge.bearingTo(targetOnEdge) + "\n";
		}
		for (int y = 0; y < virtualLength; y++) {
			for (int x = 0; x < matrix.size(); x++) {
				buf += matrix.get(x)[y] + " ";
			}
			buf += "\n";
		}
		return buf;
	}

	public void getAllNodes(LinkedList<IndoorLocation> nodesInTree) {
		nodesInTree.add(this.getTargetLocation());
		for (PPTNode n : children)
			n.getAllNodes(nodesInTree);

	}

	public void getPath(LinkedList<PPTNode> pathlist) {
		if (pathlist != null) {
			pathlist.addFirst(this);
			if (parent != null)
				parent.getPath(pathlist);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof PPTNode) {
			if (targetOnEdge != null) {
				return targetOnEdge.equals(((PPTNode) o).getTargetLocation());
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

}
