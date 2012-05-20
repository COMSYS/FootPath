package de.uvwxy.footpath2.matching;

import java.util.LinkedList;

import de.uvwxy.footpath2.gui.old.Navigator;
import de.uvwxy.footpath2.map.GraphEdge;

/**
 * A class calculating the position concerning First Fit
 * 
 * @author Paul Smith
 * 
 */
public class Positioner_OnlineFirstFit extends Positioner {
	private NPConfig conf;
	private LinkedList<GraphEdge> navPathEdges = null;
	private int totalStepsWalked = 0;

	// store each step, with its direction
	private final LinkedList<Double> dirHistory = new LinkedList<Double>();
	// If this value is passed, lookahead is started
	private final int maxFallBackSteps = 4;

	private Navigator nav = null;

	public Positioner_OnlineFirstFit(Navigator nav, LinkedList<GraphEdge> navPathEdges, NPConfig conf) {
		this.navPathEdges = navPathEdges;
		this.conf = conf;
		this.nav = nav;
	}

	/**
	 * This is called each time a step is detected, and thus information about the users whereabouts need to be updated
	 */
	@Override
	public void addStep(double compValue) {

		totalStepsWalked++;
		dirHistory.add(Double.valueOf(compValue));

		if (conf.getNpPointer() < navPathEdges.size()) {
			// we haven't reached a destination
			// successive matching, incoming values are/have been
			// roughly(in range of acceptanceWidth) correct, and not more than maxFallBackSteps have been unmatched
			if (isInRange(compValue, navPathEdges.get(conf.getNpPointer()).getCompDir(), nav.getAcceptanceWidth())
					&& ((conf.getNpUnmatchedSteps() <= maxFallBackSteps)) // unmatched steps might be errors
					|| (nav.getNavPathLenLeft() < maxFallBackSteps * (conf.getNpStepSize() + 1))) { // near to end

				// Log.i("FOOTPATH", "Current edge matches ");

				// Updated latest matched index from walkedHistory
				conf.setNpLastMatchedStep(dirHistory.size() - 1);
				// Reset unmatched steps
				conf.setNpUnmatchedSteps(0);

				// Add one more matched step to counter
				conf.setNpMatchedSteps(conf.getNpMatchedSteps() + 1);

				// Update how far we have walked on this edge
				if (navPathEdges.get(conf.getNpPointer()).isStairs()) {
					// Don't use step length because of stairs
					if (navPathEdges.get(conf.getNpPointer()).getSteps() > 0) {
						// Calculate length from number of steps on stairs
						conf.setNpCurLen(conf.getNpCurLen() + navPathEdges.get(conf.getNpPointer()).getLen()
								/ navPathEdges.get(conf.getNpPointer()).getSteps());
					} else if (navPathEdges.get(conf.getNpPointer()).getSteps() == -1) {
						// Naive length for steps on stairs if steps undefined
						conf.setNpCurLen(conf.getNpCurLen() + nav.getNaiveStairsWidth());
					} else {
						// Error in data: Edge isStairs, but not undefined/defined number of steps
						conf.setNpCurLen(conf.getNpCurLen() + conf.getNpStepSize());
					}
				} else {
					conf.setNpCurLen(conf.getNpCurLen() + conf.getNpStepSize());
				}

				if (conf.getNpCurLen() >= navPathEdges.get(conf.getNpPointer()).getLen()) {
					// Edge length was exceeded so skip to next edge
					conf.setNpPointer(conf.getNpPointer() + 1);
					// ;)
					// Log.i("FOOTPATH", "progress on path");
					// Reset amount of walked length to remainder of step length
					conf.setNpCurLen(conf.getNpCurLen() - navPathEdges.get(conf.getNpPointer() - 1).getLen());
					// Stop navigating if we have passed the last edge
					if (conf.getNpPointer() >= navPathEdges.size()) {
						nav.setNavigating(false);
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
						&& isInRange(compValue, navPathEdges.get(conf.getNpPointer() - 1).getCompDir(),
								nav.getAcceptanceWidth())) {
					return;
				}

				// Enough steps unmatched to start lookahead
				if (conf.getNpUnmatchedSteps() > maxFallBackSteps) {

					conf = findMatch(conf, true);
					// CALL FOR NEW POSITION FIRST FIND
				}

			}// -> else
		} // if
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
		for (int localPointer = npC.getNpPointer(); localPointer < navPathEdges.size(); localPointer++) {

			// Log.i("FOOTPATH", "Found edge (" + localPointer + ") in direction " +
			// navPathEdges.get(localPointer).getCompDir());
			if (isInRange(navPathEdges.get(localPointer).getCompDir(), lastDir, nav.getAcceptanceWidth())) {
				// There is an edge matching a direction on path
				// Log.i("FOOTPATH", "Edge (" + localPointer + ") is in range");
				UglyObject o = new UglyObject();
				o.count = 0;
				int oldCount = o.count;
				o.historyPointer = dirHistory.size() - 1;
				int backLogPointer = localPointer;
				double edgeDir = navPathEdges.get(backLogPointer).getCompDir();
				double edgeLength = navPathEdges.get(backLogPointer).getLen();
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

					edgeDir = navPathEdges.get(backLogPointer).getCompDir();
					// remember last count. on last loop o.count is set to zero
				}
				// Log.i("FOOTPATH", "Found " + oldCount + " matching steps");
				if (oldCount >= minCount && oldCount > maxBackLogCount) {
					maxBackLogCount = oldCount;
					newPointer = localPointer;
					newCurLen = amountInSameDirection(dirHistory.size() - 1, navPathEdges.get(localPointer), npC);

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
				if (npC.getNpCurLen() > navPathEdges.get(npC.getNpPointer()).getLen()) {
					// Don not exceed edge!
					npC.setNpCurLen(navPathEdges.get(npC.getNpPointer()).getLen());
				}
			} else {
				// Jump along a different edge
				npC.setNpPointer(newPointer);
				npC.setNpCurLen(newCurLen);
				if (npC.getNpCurLen() > navPathEdges.get(npC.getNpPointer()).getLen()) {
					// Don not exceed edge!
					npC.setNpCurLen(navPathEdges.get(npC.getNpPointer()).getLen());
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
				&& isInRange(dirHistory.get(o.historyPointer), edgeDir, nav.getAcceptanceWidth())) {
			o.count++;
			o.historyPointer--;

			double lengthToAdd = 0.0;
			if (navPathEdges.get(npC.getNpPointer()).isStairs()) {
				// Don't use step length because of stairs
				if (navPathEdges.get(npC.getNpPointer()).getSteps() > 0) {
					// Calculate length from number of steps on stairs
					lengthToAdd = navPathEdges.get(npC.getNpPointer()).getLen()
							/ navPathEdges.get(npC.getNpPointer()).getSteps();
				} else if (navPathEdges.get(npC.getNpPointer()).getSteps() == -1) {
					// Naive length for steps on stairs if steps undefined
					lengthToAdd = nav.getNaiveStairsWidth();
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
				&& isInRange(edge.getCompDir(), dirHistory.get(historyPointer), nav.getAcceptanceWidth())) {
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
					lengthToAdd = nav.getNaiveStairsWidth();
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

	@Override
	public double getProgress() {
		double len = 0.0;
		// sum all traversed edges
		for (int i = 0; i < conf.getNpPointer(); i++) {
			len += navPathEdges.get(i).getLen();
		}
		// and how far we have walked on current edge
		len += conf.getNpCurLen();
		return len;
	}

}
