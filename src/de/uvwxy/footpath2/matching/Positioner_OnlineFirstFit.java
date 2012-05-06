package de.uvwxy.footpath2.matching;

import java.util.LinkedList;

import de.uvwxy.footpath.gui.Navigator;
import de.uvwxy.footpath.gui.NavigatorFootPath;
import de.uvwxy.footpath2.map.GraphEdge;

/**
 * A class calculating the position concerning First Fit
 * 
 * @author Paul Smith
 *
 */
public class Positioner_OnlineFirstFit extends Positioner{
	private NPConfig conf;
	private LinkedList<GraphEdge> navPathEdges = null;
	private int totalStepsWalked = 0;

	// store each step, with its direction
	private LinkedList<Double> dirHistory = new LinkedList<Double>();
	// If this value is passed, lookahead is started
	private int maxFallBackSteps = 4;
	
	private Navigator nav = null;
	
	
	public Positioner_OnlineFirstFit(Navigator nav, LinkedList<GraphEdge> navPathEdges, NPConfig conf){
		this.navPathEdges = navPathEdges;
		this.conf = conf;
		this.nav = nav;
	}
	
	/**
	 * This is called each time a step is detected, and thus information about
	 * the users whereabouts need to be updated
	 */
	public void addStep(double compValue) {
		
		totalStepsWalked++;
		dirHistory.add(new Double(compValue)); 		

		if (conf.npPointer < navPathEdges.size()) {
			// we haven't reached a destination
			// successive matching, incoming values are/have been
			// roughly(in range of acceptanceWidth) correct, and not more than maxFallBackSteps have been unmatched
			if (isInRange(compValue,	navPathEdges.get(conf.npPointer).getCompDir(),	nav.getAcceptanceWidth())
					&& (	(conf.npUnmatchedSteps <= maxFallBackSteps))	// unmatched steps might be errors
						||	(nav.getNavPathLenLeft()<maxFallBackSteps*(conf.npStepSize+1))	) { // near to end
				
//				Log.i("FOOTPATH", "Current edge matches ");
					
				// Updated latest matched index from walkedHistory
				conf.npLastMatchedStep = dirHistory.size()-1;
				// Reset unmatched steps
				conf.npUnmatchedSteps = 0;
				
				// Add one more matched step to counter
				conf.npMatchedSteps++;
				
				// Update how far we have walked on this edge
				if(navPathEdges.get(conf.npPointer).isStairs()){
					// Don't use step length because of stairs
					if(navPathEdges.get(conf.npPointer).getSteps()>0){
						// Calculate length from number of steps on stairs
						conf.npCurLen += navPathEdges.get(conf.npPointer).getLen()/navPathEdges.get(conf.npPointer).getSteps();
					} else if(navPathEdges.get(conf.npPointer).getSteps()==-1){
						// Naive length for steps on stairs if steps undefined
						conf.npCurLen += nav.getNaiveStairsWidth();
					} else {
						// Error in data: Edge isStairs, but not undefined/defined number of steps
						conf.npCurLen += conf.npStepSize;
					}
				} else {
					conf.npCurLen += conf.npStepSize;
				}
				
				
				if (conf.npCurLen >= navPathEdges.get(conf.npPointer).getLen()) { 
					// Edge length was exceeded so skip to next edge
					conf.npPointer++;
					// ;)
//					Log.i("FOOTPATH", "progress on path");
					// Reset amount of walked length to remainder of step length
					conf.npCurLen = conf.npCurLen - navPathEdges.get(conf.npPointer - 1).getLen();
					// Stop navigating if we have passed the last edge
					if (conf.npPointer >= navPathEdges.size()) {
						nav.setNavigating(false);
					}
				}
			} else {
				// Step did not match current assumed edge, try lookahead, if we 
				// calculated with steps being larger in reality
				// If steps are smaller in reality than try to wait and resize the step size
				
				// Increase amount of unmatched steps
				conf.npUnmatchedSteps++;
//				Log.i("FOOTPATH", "Unmatched steps  = " + conf.npUnmatchedSteps);
				
				// Do not do look ahead if the direction matches the last edge.
				// Wait for user to turn on to this edge
				if(conf.npPointer >= 1 && conf.npCurLen <= conf.npStepSize && isInRange(compValue, navPathEdges.get(conf.npPointer-1).getCompDir(),
						nav.getAcceptanceWidth())){
					return;
				}	
				
				// Enough steps unmatched to start lookahead
				if(conf.npUnmatchedSteps > maxFallBackSteps){
					
						conf = findMatch(conf, true);
						// CALL FOR NEW POSITION FIRST FIND
				}

			}// -> else 
		} // if 
	}
	
	public void recalcPos(){
		// RECALCULATE POSITION FROM WHOLE ROUTE DATA
		NPConfig x = new NPConfig(conf);
		// reset route to beginning
		x.npLastMatchedStep = -1;
		x.npPointer = 0;
		x.npCurLen = 0.0;
		x.npUnmatchedSteps = dirHistory.size() - 1;
		
		x = findMatch(x, false);
		x.npLastMatchedStep = dirHistory.size() - 1;
		x.npMatchedSteps = x.npLastMatchedStep;
		
//		Log.i("FOOTPAHT", "Recalculated route");
		// Update position on path
		conf = x;
	}
	
	/**
	 * Calculate the best/first matching position on path, from given configuration
	 * @param npC the current configuration to base calculation on
	 * @param first set to true if first match should be returned
	 * @return new configuration for position
	 */
	private NPConfig findMatch(NPConfig npC, boolean first){
		
		if(dirHistory == null){
			return npC;
		}
		
		if(dirHistory.size() == 0){
			return npC;
		}
		
		// Move backwards through walkedHistory, and look forwards to match it to an edge.
		// This works based on the assumption that, at some point, we have correct values again.
		// Accept only if we have found at least minMetresToMatch on a single edge
		
		double lastDir = dirHistory.get(dirHistory.size()-1); // last unmatched value
		
//		Log.i("FOOTPATH", "Searching for " + lastDir);
//		Log.i("FOOTPATH", "Current edge " + npC.npPointer);
		
		int maxBackLogCount = Integer.MIN_VALUE;
		int newPointer = npC.npPointer;
		double newCurLen = 0.0;
		int minCount = 4; // minimal 4 steps to match backwards
		// Go through all remaining edges and find first edge matching current direction
		for(int localPointer = npC.npPointer; localPointer < navPathEdges.size(); localPointer++){

//			Log.i("FOOTPATH", "Found edge (" + localPointer + ") in direction " + navPathEdges.get(localPointer).getCompDir());
			if(isInRange(navPathEdges.get(localPointer).getCompDir(),lastDir,nav.getAcceptanceWidth())){
				// There is an edge matching a direction on path
//				Log.i("FOOTPATH", "Edge (" + localPointer + ") is in range");
				UglyObject o = new UglyObject();
				o.count = 0;
				int oldCount = o.count;
				o.historyPointer = dirHistory.size()-1;
				int backLogPointer = localPointer;
				double edgeDir = navPathEdges.get(backLogPointer).getCompDir();
				double edgeLength = navPathEdges.get(backLogPointer).getLen();
//				Log.i("FOOTPATH", "Summing up path length " + lastDir);
//				Log.i("FOOTPATH", "backlogPointer = " + backLogPointer + ">" + npC.npPointer + " npLastMatchedStep");
				while(backLogPointer > npC.npPointer && findMatchingSteps(o, edgeDir, npC, edgeLength, npC.npStepSize)){
					oldCount = o.count;
//					Log.i("FOOTPATH", "Found matching steps into edgeDir = " + edgeDir + ", oldCount = " 
//							+ oldCount + ", " + o.historyPointer);
					backLogPointer--;
					
					if(backLogPointer<0){
						break;
					}
					
					edgeDir = navPathEdges.get(backLogPointer).getCompDir();
					// remember last count. on last loop o.count is set to zero
				}
//				Log.i("FOOTPATH", "Found " + oldCount + " matching steps");
				if(oldCount >= minCount && oldCount>maxBackLogCount){
					maxBackLogCount = oldCount;
					newPointer = localPointer;
					newCurLen = amountInSameDirection(dirHistory.size()-1,navPathEdges.get(localPointer), npC);
					
					if(first){
						break;
					}
				}
			} else {

//				Log.i("FOOTPATH", "Edge (" + localPointer + ") not in range");
			}
		}
		
		if(maxBackLogCount != Integer.MIN_VALUE){
//			Log.i("FOOTPATH", "Found new position with " + (double)maxBackLogCount/(double)(dirHistory.size()-1-npC.npLastMatchedStep) + " confidence");
			
			if(newPointer == npC.npPointer){
				// Jump along same edge
				npC.npCurLen += newCurLen;
				if(npC.npCurLen > navPathEdges.get(npC.npPointer).getLen()){
					// Don not exceed edge!
					npC.npCurLen = navPathEdges.get(npC.npPointer).getLen();
				}
			} else {					
				// Jump along a different edge
				npC.npPointer = newPointer;
				npC.npCurLen = newCurLen;
				if(npC.npCurLen > navPathEdges.get(npC.npPointer).getLen()){
					// Don not exceed edge!
					npC.npCurLen = navPathEdges.get(npC.npPointer).getLen();
				}
			}
			
			npC.npUnmatchedSteps = 0;
			npC.npLastMatchedStep = dirHistory.size() - 1;
		}

		return npC;
	}
	
	private class UglyObject{
		int count;
		int historyPointer;
	}
	
	private boolean findMatchingSteps(UglyObject o, double edgeDir, NPConfig npC, double edgeLength, double stepLength){
		int oldCount = o.count;
//		Log.i("FOOTPATH", "findMatchingSteps: " + oldCount);
		while(o.historyPointer >= 0
				&& o.historyPointer > npC.npLastMatchedStep 
				&& isInRange(dirHistory.get(o.historyPointer), edgeDir, nav.getAcceptanceWidth())){
			o.count++;
			o.historyPointer--;
			
			double lengthToAdd = 0.0;
			if(navPathEdges.get(npC.npPointer).isStairs()){
				// Don't use step length because of stairs
				if(navPathEdges.get(npC.npPointer).getSteps()>0){
					// Calculate length from number of steps on stairs
					lengthToAdd = navPathEdges.get(npC.npPointer).getLen()/navPathEdges.get(npC.npPointer).getSteps();
				} else if(navPathEdges.get(npC.npPointer).getSteps()==-1){
					// Naive length for steps on stairs if steps undefined
					lengthToAdd= nav.getNaiveStairsWidth();
				} else {
					// Error in data: Edge isStairs, but not undefined/defined number of steps
					lengthToAdd = npC.npStepSize;
				}
			} else {
				lengthToAdd = npC.npStepSize;
			}
			
			if(edgeLength <= (o.count-oldCount)*lengthToAdd){
//				Log.i("FOOTPATH", "findMatchingSteps/ edgeLength (" + edgeLength + ") <= "
//						+ (o.count-oldCount)*lengthToAdd);
				// return true if whole edge has been traveled along
				return true;
			}
		}
//		Log.i("FOOTPATH", "found: " + oldCount);
		if(oldCount != o.count){
			return true;
		} else {
			return false;
		}
	}

	
	private double amountInSameDirection(int historyPointer, GraphEdge edge, NPConfig npC){
		double retLength = 0.0;
		
		while(historyPointer >= 0
				&& historyPointer >= npC.npLastMatchedStep  
				&& isInRange(edge.getCompDir(), dirHistory.get(historyPointer), nav.getAcceptanceWidth())){
//			Log.i("FOOTPATH", "Adding amount into direction " + edge.getCompDir());
			historyPointer--;
			double lengthToAdd = 0.0;
			if(edge.isStairs()){
				// Don't use step length because of stairs
				if(edge.getSteps()>0){
					// Calculate length from number of steps on stairs
					lengthToAdd = edge.getLen()/edge.getSteps();
				} else if(edge.getSteps()==-1){
					// Naive length for steps on stairs if steps undefined
					lengthToAdd= nav.getNaiveStairsWidth();
				} else {
					// Error in data: Edge isStairs, but not undefined/defined number of steps
					lengthToAdd = npC.npStepSize;
				}
			} else {
				lengthToAdd = npC.npStepSize;
			}
			retLength += lengthToAdd;
		}
		return retLength;
	}

	@Override
	public double getProgress() {
		double len = 0.0;
		// sum all traversed edges
		for(int i = 0; i < conf.npPointer; i++){
			len += navPathEdges.get(i).getLen();
		}
		// and how far we have walked on current edge
		len += conf.npCurLen;
		return len;
	}
	
	
}
