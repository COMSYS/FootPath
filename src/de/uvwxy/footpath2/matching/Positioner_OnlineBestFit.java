package de.uvwxy.footpath2.matching;

import java.util.LinkedList;

import de.uvwxy.footpath.gui.Navigator;
import de.uvwxy.footpath.gui.NavigatorFootPath;
import de.uvwxy.footpath2.map.GraphEdge;

/**
 * A class calculating the position concerning Best Fit
 * 
 * @author Paul Smith
 *
 */
public class Positioner_OnlineBestFit extends Positioner{
	private Navigator nav = null;
	private NPConfig conf = null;
	private LinkedList<GraphEdge> edges = null;
	private double[][] c = null;
	private double all_dist = 0.0;
	private double avg_steplength = 0.0;
	private double[] from_map = null;
	private LinkedList<Double> s = null;
	private double[][] dyn = null;
	private final int INITIAL_DYN_SIZE = 2;
	private int currentStep = 0;
	private double progress = 0.0;
	
	public Positioner_OnlineBestFit(Navigator nav, LinkedList<GraphEdge> edges, NPConfig conf){
		this.nav = nav;
		this.edges = edges;
		this.conf = conf;
		
		c = new double[edges.size()][2];
		// Setup c:
		double tempLen = 0.0;
		for(int i = 0; i < edges.size(); i++){
			GraphEdge temp = edges.get(i);
			tempLen+=temp.getLen();
			c[i][0] = tempLen;
			c[i][1] = temp.getCompDir();
		}
		
		// Setup all_dist:
		all_dist = c[edges.size()-1][0];
		
		// Setup avg_steplength:
		this.avg_steplength = nav.getStepLengthInMeters();
		
		// Setup n:
		double[] n = new double[(int)(all_dist/this.avg_steplength)];
		for(int i = 0; i < n.length; i++){
			n[i] = this.avg_steplength*i;
		}
		
		// Setup from_map:
		from_map = new double[n.length];
		for(int i = 0; i < n.length; i++){
			// This code below uses directions directly from edges
			int edge_i = 0;
			while(!(c[edge_i][0] > n[i])){
				edge_i++;
			}
			from_map[i]=c[edge_i][1];
		}
		
		// s: a list to store detected step headings
		s = new LinkedList<Double>();
		
		// dyn: the last two lines from the matrix D
		dyn = new double[INITIAL_DYN_SIZE][from_map.length+1];
		
		// initialization
		for(int x = 0; x < INITIAL_DYN_SIZE; x++){
			for(int y = 0; y < dyn[0].length; y++){
				if ( x == 0 && y == 0 ){
					dyn[x][y] = 0.0;
				} else if (x == 0){
					dyn[x][y] = Double.POSITIVE_INFINITY;
				} else if (y == 0){
					dyn[x][y] = Double.POSITIVE_INFINITY;
				}
//				lens[x][y] = 0.0;
			}
		}
		
		
	}
	
	boolean firstStep = false;
	
	public void addStep(double direction){
		if(firstStep){
			dyn[0][0] = Double.POSITIVE_INFINITY;
		}
		firstStep = true;
		
		double t1,t2,t3;
		
		currentStep++;
		int x = currentStep;
		
		s.add(new Double(direction));
		
		// calculate new line of the matrix D:
		for(int y = 1; y < dyn[0].length; y++){
			// top
			t1=dyn[x % 2][y-1] + score(getFromS(x-1), getFromMap(y-2), false);
			// left
			t2=dyn[(x-1)%2][y] + score(getFromS(x-2), getFromMap(y-1), false);
			// diagonal
			t3=dyn[(x-1)%2][y-1] + score(getFromS(x-1), getFromMap(y-1), true);
			
			dyn[x%2][y] = Math.min(Math.min(t1, t2),t3);
		}
		
		int y_min = -1;
		double f_min = Double.POSITIVE_INFINITY;
		for ( int y_ = 1; y_ < dyn[0].length - 1; y_++){
			if ( f_min > dyn[x%2][y_] ){
				f_min = dyn[x%2][y_];
				y_min = y_;
			}
		}
		
		// y_min + 1 :  index i is step i + 1 ( array starting at 0)
		progress = (y_min + 1) * avg_steplength;
		
		// Update fields in NPConfig conf:
		// Find out which edge we are on and how far on that edge:
		double tempLen = edges.get(0).getLen();
		int edgeIndex = 0;
		while(tempLen < progress && edgeIndex < edges.size() ){
			edgeIndex++;
			tempLen += edges.get(edgeIndex).getLen();
		}
		
		conf.npCurLen = progress;
		for(int i = 0; i < edgeIndex; i++){
			conf.npCurLen-=edges.get(i).getLen();
		}
		
		conf.npPointer = edgeIndex;
		conf.npLastMatchedStep = y_min;
		conf.npMatchedSteps++;
		conf.npUnmatchedSteps = conf.npMatchedSteps - y_min;
	}
	
	private double getFromMap(int i){
		if ( i < 0 ) 
			return 0.0;
		else 
			return from_map[i];
	}
	
	private double getFromS(int i){
		if ( i < 0 ) 
			return 0.0;
		else
			return s.get(i).doubleValue();
	}
	private double score(double x, double y, boolean diagonal){
		double ret = 2.0; // = penalty
		
		// Sanitize:
		double t = Math.abs(x-y);
		t = (t>180.0) ? 360.0 - t : t;
		
		// And score:
		if ( t < 45.0 ){
			ret = 0.0;
		} else if ( t < 90.0 ){
			ret = 1.0;
		} else if ( t < 120.0 ){
			ret = 2.0;
		} else {
			ret = 10.0;
		}
			
		ret = !diagonal ? ret + 1.5 : ret;
		return ret;
	}
	
	public double getProgress(){
		return progress;
	}
	
}
