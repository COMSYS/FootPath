package de.uvwxy.footpath2.matching;

import java.util.LinkedList;
import java.util.List;

import android.util.Log;
import de.uvwxy.footpath2.map.GraphEdge;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.map.IndoorLocationList;
import de.uvwxy.footpath2.tools.FootPathException;

/**
 * README:
 * 
 * paul: i had to recreate the member edges during the initialization as i would like to have the matching algorithms
 * use a location history as a navigation path
 * 
 * @author paul
 * 
 */
public class BestFit extends MatchingAlgorithm {
	// private List<GraphEdge> edges = null;
	private double[][] c = null;
	private double all_dist = 0.0;
	private double avg_steplength = 0.0;
	private double[] from_map = null;
	private LinkedList<Double> s = null;
	private double[][] dyn = null;
	private final int INITIAL_DYN_SIZE = 2;

	boolean firstStep = false;


	@Override
	public void onStepUpdate(double bearing, double steplength, long timestamp, double estimatedStepLengthError,
			double estimatedBearingError) {
		// TODO Auto-generated method stub
		if (firstStep) {
			dyn[0][0] = Double.POSITIVE_INFINITY;
		}
		firstStep = true;

		Log.i("BESTFIT", "Walking into " + bearing);
		double t1, t2, t3;

		currentStep++;
		int x = currentStep;

		s.add(new Double(bearing));

		// calculate new line of the matrix D:
		for (int y = 1; y < dyn[0].length; y++) {
			// top
			t1 = dyn[x % 2][y - 1] + score(getFromS(x - 1), getFromMap(y - 2), false);
			// left
			t2 = dyn[(x - 1) % 2][y] + score(getFromS(x - 2), getFromMap(y - 1), false);
			// diagonal
			t3 = dyn[(x - 1) % 2][y - 1] + score(getFromS(x - 1), getFromMap(y - 1), true);

			dyn[x % 2][y] = Math.min(Math.min(t1, t2), t3);
		}

		int y_min = -1;
		double f_min = Double.POSITIVE_INFINITY;
		for (int y_ = 1; y_ < dyn[0].length - 1; y_++) {
			if (f_min > dyn[x % 2][y_]) {
				f_min = dyn[x % 2][y_];
				y_min = y_;
			}
		}

		// y_min + 1 : index i is step i + 1 ( array starting at 0)
		progress = (y_min + 1) * avg_steplength;

		// Update fields in NPConfig conf:
		// Find out which edge we are on and how far on that edge:
		double tempLen = edges.get(0).getLen();
		int edgeIndex = 0;
		while (tempLen < progress && edgeIndex < edges.size()) {
			edgeIndex++;
			tempLen += edges.get(edgeIndex).getLen();
		}

//		double tmp = progress;
//		for (int i = 0; i < edgeIndex; i++) {
//			tmp -= edges.get(i).getLen();
//		}

		currentLocation = getPositionFromProgress();
		returnedPositions.add(currentLocation);
		Log.i("FOOTPATH", "BestFit: Progress is " + progress);
		// conf.setNpCurLen(tmp);
		//
		// conf.setNpPointer(edgeIndex);
		// conf.setNpLastMatchedStep(y_min);
		// conf.setNpMatchedSteps(conf.getNpMatchedSteps() + 1);
		// conf.setNpUnmatchedSteps(conf.getNpMatchedSteps() - y_min);
	}

	@Override
	public void init() throws FootPathException {
		if (path == null) {
			throw new FootPathException("Best Fit: Path was null during init");
		}

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


		c = new double[edges.size()][2];
		// Setup c:
		double tempLen = 0.0;
		for (int i = 0; i < edges.size(); i++) {
			GraphEdge temp = edges.get(i);
			tempLen += temp.getLen();
			c[i][0] = tempLen;
			c[i][1] = temp.getCompDir();
		}

		// Setup all_dist:
		all_dist = c[edges.size() - 1][0];

		// Setup avg_steplength:
		this.avg_steplength = initialStepLength;

		// Setup n:
		double[] n = new double[(int) (all_dist / this.avg_steplength)];
		for (int i = 0; i < n.length; i++) {
			n[i] = this.avg_steplength * i;
		}

		// Setup from_map:
		from_map = new double[n.length];
		for (int i = 0; i < n.length; i++) {
			// This code below uses directions directly from edges
			int edge_i = 0;
			while (!(c[edge_i][0] > n[i])) {
				edge_i++;
			}
			from_map[i] = c[edge_i][1];
		}

		// s: a list to store detected step headings
		s = new LinkedList<Double>();

		// dyn: the last two lines from the matrix D
		dyn = new double[INITIAL_DYN_SIZE][from_map.length + 1];

		// initialization
		for (int x = 0; x < INITIAL_DYN_SIZE; x++) {
			for (int y = 0; y < dyn[0].length; y++) {
				if (x == 0 && y == 0) {
					dyn[x][y] = 0.0;
				} else if (x == 0) {
					dyn[x][y] = Double.POSITIVE_INFINITY;
				} else if (y == 0) {
					dyn[x][y] = Double.POSITIVE_INFINITY;
				}
				// lens[x][y] = 0.0;
			}
		}
	}

	private double getFromMap(int i) {
		if (i < 0)
			return 0.0;
		else
			return from_map[i];
	}

	private double getFromS(int i) {
		if (i < 0)
			return 0.0;
		else
			return s.get(i).doubleValue();
	}

	private double score(double x, double y, boolean diagonal) {
		double ret = 2.0; // = penalty

		// Sanitize:
		double t = Math.abs(x - y);
		t = (t > 180.0) ? 360.0 - t : t;

		// And score:
		if (t < 45.0) {
			ret = 0.0;
		} else if (t < 90.0) {
			ret = 1.0;
		} else if (t < 120.0) {
			ret = 2.0;
		} else {
			ret = 10.0;
		}

		ret = !diagonal ? ret + 1.5 : ret;
		return ret;
	}


}
