package de.uvwxy.footpath2.matching;

import java.util.LinkedList;

import android.util.Log;
import de.uvwxy.footpath2.map.GraphEdge;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.map.IndoorLocationList;
import de.uvwxy.footpath2.tools.FootPathException;

public class FirstFit extends MatchingAlgorithm {
	private NPConfig conf;

	// store each step, with its direction
	private final LinkedList<Double> dirHistory = new LinkedList<Double>();
	// If this value is passed, lookahead is started
	private final int maxFallBackSteps = 4;

	// TODO: parameterize these in the constructor?
	private double acceptanceWidth = 40.0;
	private double naiveStairsWidth = 0.25;

	@Override
	public void init() throws FootPathException {
		if (path == null) {
			throw new FootPathException("Best Fit: Path was null during init");
		}

		// TODO Auto-generated method stub
		conf = new NPConfig();
		conf.setNpCurLen(0.0);
		conf.setNpLastMatchedStep(-1);
		conf.setNpMatchedSteps(0);
		conf.setNpPointer(0);
		// /100.0f -> cm to m
		conf.setNpStepSize(initialStepLength);
		conf.setNpUnmatchedSteps(0);

		edges = new LinkedList<GraphEdge>();

		// TODO: paul: i don't see the point why could not use the path instead of the edges
		// we could fix this, minor performance issue ;)
		for (int i = 1; i < path.size(); i++) {
			IndoorLocation l0 = path.get(i - 1);
			IndoorLocation l1 = path.get(i);
			GraphEdge e = new GraphEdge();
			e.setLen(l0.distanceTo(l1));
			e.setCompDir(l0.bearingTo(l1));
			// TODO: this is not so nice, for edges going between levels!!
			e.setLevel(l0.getLevel());
			edges.add(e);
		}

	}

	// return show far we have walked on the path
	public double getNavPathWalked() {
		double len = 0.0;
		// sum all traversed edges
		for (int i = 0; i < conf.getNpPointer(); i++) {
			len += edges.get(i).getLen();
		}
		// and how far we have walked on current edge
		len += conf.getNpCurLen();
		return len;
	}

	@Override
	public void onStepUpdate(double compValue, double steplength, long timestamp, double estimatedStepLengthError,
			double estimatedBearingError) {
		// TODO Auto-generated method stub
		
		currentStep++;

		dirHistory.add(Double.valueOf(compValue));

		if (conf.getNpPointer() < edges.size()) {
			// we haven't reached a destination
			// successive matching, incoming values are/have been
			// roughly(in range of acceptanceWidth) correct, and not more than maxFallBackSteps have been unmatched
			if (isInRange(compValue, edges.get(conf.getNpPointer()).getCompDir(), acceptanceWidth)
					&& ((conf.getNpUnmatchedSteps() <= maxFallBackSteps)) // unmatched steps might be errors
					|| ((path.getTotalDistance() - getNavPathWalked()) < maxFallBackSteps * (conf.getNpStepSize() + 1))) { // near
																															// to
																															// end

				// Log.i("FOOTPATH", "Current edge matches ");

				// Updated latest matched index from walkedHistory
				conf.setNpLastMatchedStep(dirHistory.size() - 1);
				// Reset unmatched steps
				conf.setNpUnmatchedSteps(0);

				// Add one more matched step to counter
				conf.setNpMatchedSteps(conf.getNpMatchedSteps() + 1);

				// Update how far we have walked on this edge
				if (edges.get(conf.getNpPointer()).isStairs()) {
					// Don't use step length because of stairs
					if (edges.get(conf.getNpPointer()).getSteps() > 0) {
						// Calculate length from number of steps on stairs
						conf.setNpCurLen(conf.getNpCurLen() + edges.get(conf.getNpPointer()).getLen()
								/ edges.get(conf.getNpPointer()).getSteps());
					} else if (edges.get(conf.getNpPointer()).getSteps() == -1) {
						// Naive length for steps on stairs if steps undefined
						conf.setNpCurLen(conf.getNpCurLen() + naiveStairsWidth);
					} else {
						// Error in data: Edge isStairs, but not undefined/defined number of steps
						conf.setNpCurLen(conf.getNpCurLen() + conf.getNpStepSize());
					}
				} else {
					conf.setNpCurLen(conf.getNpCurLen() + conf.getNpStepSize());
				}

				if (conf.getNpCurLen() >= edges.get(conf.getNpPointer()).getLen()) {
					// Edge length was exceeded so skip to next edge
					conf.setNpPointer(conf.getNpPointer() + 1);
					// ;)
					// Log.i("FOOTPATH", "progress on path");
					// Reset amount of walked length to remainder of step length
					conf.setNpCurLen(conf.getNpCurLen() - edges.get(conf.getNpPointer() - 1).getLen());
					// Stop navigating if we have passed the last edge
					if (conf.getNpPointer() >= edges.size()) {
						// nav.setNavigating(false);
					}
				}
			} else {
				// Step did not match current assumed edge, try lookahead, if we
				// calculated with steps being larger in reality
				// If steps are smaller in reality than try to wait and resize the step size

				// Increase amount of unmatched steps
				conf.setNpUnmatchedSteps(conf.getNpUnmatchedSteps() + 1);
				// Log.i("FOOTPATH", "Unmatched steps  = " + conf.npUnmatchedSteps);

				// Do not do look ahead if the direction matches the last edge.
				// Wait for user to turn on to this edge
				if (conf.getNpPointer() >= 1
						&& conf.getNpCurLen() <= conf.getNpStepSize()
						&& isInRange(compValue, edges.get(conf.getNpPointer() - 1).getCompDir(), acceptanceWidth)) {
					return;
				}

				// Enough steps unmatched to start lookahead
				if (conf.getNpUnmatchedSteps() > maxFallBackSteps) {

					conf = findMatch(conf, true);
					// CALL FOR NEW POSITION FIRST FIND
				}

			}// -> else
		} // if

		progress = getProgress();
		currentLocation = getPositionFromProgress();
		returnedPositions.add(currentLocation);
		Log.i("FOOTPATH", "FirstFit: Progress is " + progress);

	}

	public void recalcPos() {
		// RECALCULATE POSITION FROM WHOLE ROUTE DATA
		NPConfig x = new NPConfig(conf);
		// reset route to beginning
		x.setNpLastMatchedStep(-1);
		x.setNpPointer(0);
		x.setNpCurLen(0.0);
		x.setNpUnmatchedSteps(dirHistory.size() - 1);

		x = findMatch(x, false);
		x.setNpLastMatchedStep(dirHistory.size() - 1);
		x.setNpMatchedSteps(x.getNpLastMatchedStep());

		// Log.i("FOOTPAHT", "Recalculated route");
		// Update position on path
		conf = x;
	}

	/**
	 * Calculate the best/first matching position on path, from given configuration
	 * 
	 * @param npC
	 *            the current configuration to base calculation on
	 * @param first
	 *            set to true if first match should be returned
	 * @return new configuration for position
	 */
	private NPConfig findMatch(NPConfig npC, boolean first) {

		if (dirHistory == null) {
			return npC;
		}

		if (dirHistory.size() == 0) {
			return npC;
		}

		// Move backwards through walkedHistory, and look forwards to match it to an edge.
		// This works based on the assumption that, at some point, we have correct values again.
		// Accept only if we have found at least minMetresToMatch on a single edge

		double lastDir = dirHistory.get(dirHistory.size() - 1); // last unmatched value

		// Log.i("FOOTPATH", "Searching for " + lastDir);
		// Log.i("FOOTPATH", "Current edge " + npC.npPointer);

		int maxBackLogCount = Integer.MIN_VALUE;
		int newPointer = npC.getNpPointer();
		double newCurLen = 0.0;
		int minCount = 4; // minimal 4 steps to match backwards
		// Go through all remaining edges and find first edge matching current direction
		for (int localPointer = npC.getNpPointer(); localPointer < edges.size(); localPointer++) {

			// Log.i("FOOTPATH", "Found edge (" + localPointer + ") in direction " +
			// navPathEdges.get(localPointer).getCompDir());
			if (isInRange(edges.get(localPointer).getCompDir(), lastDir, acceptanceWidth)) {
				// There is an edge matching a direction on path
				// Log.i("FOOTPATH", "Edge (" + localPointer + ") is in range");
				UglyObject o = new UglyObject();
				o.count = 0;
				int oldCount = o.count;
				o.historyPointer = dirHistory.size() - 1;
				int backLogPointer = localPointer;
				double edgeDir = edges.get(backLogPointer).getCompDir();
				double edgeLength = edges.get(backLogPointer).getLen();
				// Log.i("FOOTPATH", "Summing up path length " + lastDir);
				// Log.i("FOOTPATH", "backlogPointer = " + backLogPointer + ">" + npC.npPointer + " npLastMatchedStep");
				while (backLogPointer > npC.getNpPointer()
						&& findMatchingSteps(o, edgeDir, npC, edgeLength, npC.getNpStepSize())) {
					oldCount = o.count;
					// Log.i("FOOTPATH", "Found matching steps into edgeDir = " + edgeDir + ", oldCount = "
					// + oldCount + ", " + o.historyPointer);
					backLogPointer--;

					if (backLogPointer < 0) {
						break;
					}

					edgeDir = edges.get(backLogPointer).getCompDir();
					// remember last count. on last loop o.count is set to zero
				}
				// Log.i("FOOTPATH", "Found " + oldCount + " matching steps");
				if (oldCount >= minCount && oldCount > maxBackLogCount) {
					maxBackLogCount = oldCount;
					newPointer = localPointer;
					newCurLen = amountInSameDirection(dirHistory.size() - 1, edges.get(localPointer), npC);

					if (first) {
						break;
					}
				}
			} else {

				// Log.i("FOOTPATH", "Edge (" + localPointer + ") not in range");
			}
		}

		if (maxBackLogCount != Integer.MIN_VALUE) {
			// Log.i("FOOTPATH", "Found new position with " +
			// (double)maxBackLogCount/(double)(dirHistory.size()-1-npC.npLastMatchedStep) + " confidence");

			if (newPointer == npC.getNpPointer()) {
				// Jump along same edge
				npC.setNpCurLen(npC.getNpCurLen() + newCurLen);
				if (npC.getNpCurLen() > edges.get(npC.getNpPointer()).getLen()) {
					// Don not exceed edge!
					npC.setNpCurLen(edges.get(npC.getNpPointer()).getLen());
				}
			} else {
				// Jump along a different edge
				npC.setNpPointer(newPointer);
				npC.setNpCurLen(newCurLen);
				if (npC.getNpCurLen() > edges.get(npC.getNpPointer()).getLen()) {
					// Don not exceed edge!
					npC.setNpCurLen(edges.get(npC.getNpPointer()).getLen());
				}
			}

			npC.setNpUnmatchedSteps(0);
			npC.setNpLastMatchedStep(dirHistory.size() - 1);
		}

		return npC;
	}

	private class UglyObject {
		int count;
		int historyPointer;
	}

	private boolean findMatchingSteps(UglyObject o, double edgeDir, NPConfig npC, double edgeLength, double stepLength) {
		int oldCount = o.count;
		// Log.i("FOOTPATH", "findMatchingSteps: " + oldCount);
		while (o.historyPointer >= 0 && o.historyPointer > npC.getNpLastMatchedStep()
				&& isInRange(dirHistory.get(o.historyPointer), edgeDir, acceptanceWidth)) {
			o.count++;
			o.historyPointer--;

			double lengthToAdd = 0.0;
			if (edges.get(npC.getNpPointer()).isStairs()) {
				// Don't use step length because of stairs
				if (edges.get(npC.getNpPointer()).getSteps() > 0) {
					// Calculate length from number of steps on stairs
					lengthToAdd = edges.get(npC.getNpPointer()).getLen()
							/ edges.get(npC.getNpPointer()).getSteps();
				} else if (edges.get(npC.getNpPointer()).getSteps() == -1) {
					// Naive length for steps on stairs if steps undefined
					lengthToAdd = naiveStairsWidth;
				} else {
					// Error in data: Edge isStairs, but not undefined/defined number of steps
					lengthToAdd = npC.getNpStepSize();
				}
			} else {
				lengthToAdd = npC.getNpStepSize();
			}

			if (edgeLength <= (o.count - oldCount) * lengthToAdd) {
				// Log.i("FOOTPATH", "findMatchingSteps/ edgeLength (" + edgeLength + ") <= "
				// + (o.count-oldCount)*lengthToAdd);
				// return true if whole edge has been traveled along
				return true;
			}
		}
		// Log.i("FOOTPATH", "found: " + oldCount);
		if (oldCount != o.count) {
			return true;
		} else {
			return false;
		}
	}

	private double amountInSameDirection(int historyPointer, GraphEdge edge, NPConfig npC) {
		double retLength = 0.0;

		while (historyPointer >= 0 && historyPointer >= npC.getNpLastMatchedStep()
				&& isInRange(edge.getCompDir(), dirHistory.get(historyPointer), acceptanceWidth)) {
			// Log.i("FOOTPATH", "Adding amount into direction " + edge.getCompDir());
			historyPointer--;
			double lengthToAdd = 0.0;
			if (edge.isStairs()) {
				// Don't use step length because of stairs
				if (edge.getSteps() > 0) {
					// Calculate length from number of steps on stairs
					lengthToAdd = edge.getLen() / edge.getSteps();
				} else if (edge.getSteps() == -1) {
					// Naive length for steps on stairs if steps undefined
					lengthToAdd = naiveStairsWidth;
				} else {
					// Error in data: Edge isStairs, but not undefined/defined number of steps
					lengthToAdd = npC.getNpStepSize();
				}
			} else {
				lengthToAdd = npC.getNpStepSize();
			}
			retLength += lengthToAdd;
		}
		return retLength;
	}

	private double getProgress() {
		double len = 0.0;
		// sum all traversed edges
		for (int i = 0; i < conf.getNpPointer(); i++) {
			len += edges.get(i).getLen();
		}
		// and how far we have walked on current edge
		len += conf.getNpCurLen();
		return len;
	}

	/**
	 * Check if the difference of the given angles in degrees is less than the given alowed difference
	 * 
	 * @param v
	 *            the first angle
	 * @param t
	 *            the second angle
	 * @param diff
	 *            the allowed difference
	 * @return true if v <= diff away from t
	 */
	public static boolean isInRange(double v, double t, double diff) {
		if (Math.abs(v - t) <= diff) {
			return true;
		}
		if (Math.abs((v + diff) % 360 - (t + diff) % 360) <= diff) {
			return true;
		}
		return false;
	}
}
