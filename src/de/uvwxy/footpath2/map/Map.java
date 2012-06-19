package de.uvwxy.footpath2.map;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.Environment;
import android.util.Log;

/**
 * This class is used to create a graph from XML files stored in the directory res/xml. Data from multiple files/layers
 * can be joined into a single map/graph with the function mergeNodes(). After graph creation use functions implemented
 * in this class to find routes, nodes, etc.
 * 
 * @author Paul Smith
 * 
 */
public class Map {
	private final List<IndoorLocation> nodes;
	private final List<GraphEdge> edges;

	// sorted maps for fast access
	private final SortedMap<Integer, IndoorLocation> map_nodes_by_id;
	private final SortedMap<String, IndoorLocation> map_nodes_by_name;

	public Map() {
		Log.i("FOOTPATH", "Creating empty map");
		nodes = new LinkedList<IndoorLocation>();
		edges = new LinkedList<GraphEdge>();

		map_nodes_by_id = new TreeMap<Integer, IndoorLocation>();
		map_nodes_by_name = new TreeMap<String, IndoorLocation>();
	}

	public synchronized boolean writeGraphToXMLFile(String filePath) throws IOException {

		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but all we need to know
			// is we can neither
			// read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		if (mExternalStorageAvailable && mExternalStorageWriteable) {
			String mFirstLine = "<?xml version='1.0' encoding='UTF-8'?>";
			String mSecondLine = "\n<osm version='0.6' generator='JOSM'>";
			String mLastLine = "\n</osm>";

			BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
			out.write(mFirstLine);
			out.write(mSecondLine);

			for (IndoorLocation n : nodes) {
				out.write(n.toXML());
			}

			for (GraphEdge e : edges) {
				out.write(e.toXML());
			}

			out.write(mLastLine);
			out.close();

			return true;
		}

		return false;
	}

	/**
	 * This function loads a OSM map from a XML/OSM file.
	 * 
	 * @param filePath
	 *            - the path to the XML/OSM file
	 * @return true if succeeded
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws FileNotFoundException
	 */
	public synchronized boolean addToGraphFromXMLFile(String filePath) throws ParserConfigurationException,
			FileNotFoundException, SAXException, IOException {

		if (filePath == null)
			return false;

		File file = new File(filePath);

		return addToGraphFromFileInputStream(new FileInputStream(file));

	}

	/**
	 * 
	 * @param fis
	 *            the file input stream to read from
	 * @return true if succeeded
	 * @throws ParserConfigurationException
	 * @throws FileNotFoundException
	 * @throws SAXException
	 * @throws IOException
	 */
	public synchronized boolean addToGraphFromFileInputStream(FileInputStream fis) throws ParserConfigurationException,
			FileNotFoundException, SAXException, IOException {

		// store all nodes found in file
		List<IndoorLocation> allNodes = new LinkedList<IndoorLocation>();
		// store all ways found in file
		List<GraphWay> allWays = new LinkedList<GraphWay>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document dom = builder.parse(fis);

		NodeList domNodes = dom.getDocumentElement().getElementsByTagName("node");
		NodeList domWays = dom.getDocumentElement().getElementsByTagName("way");

		// Collect GraphNodes:
		for (int i = 0; i < domNodes.getLength(); i++) {
			Node node = domNodes.item(i);
			NamedNodeMap node_attributes = node.getAttributes();

			// Interesting attributes:
			// id='-734'
			// lat='50.77832601390677'
			// lon='6.0785245124026615'

			int id = Integer.parseInt(node_attributes.getNamedItem("id").getNodeValue());
			double lat = Double.parseDouble(node_attributes.getNamedItem("lat").getNodeValue());
			double lon = Double.parseDouble(node_attributes.getNamedItem("lon").getNodeValue());
			boolean isIndoor = false;
			boolean isDoor = false;
			String name = null;
			String merge_id = null;
			float level = 0.0f;

			// Collect GraphNode Data:
			NodeList node_children = node.getChildNodes();
			for (int j = 0; j < node_children.getLength(); j++) {

				// Possible tags:
				// <tag k='building' v='entrance' /> NOT USED by FOOTPATH yet
				// <tag k='indoor' v='yes|no' />
				// <tag k='level' v='0' />
				// <tag k='name' v='C-Caffè' />
				// <tag k='merge_id' v='RE_0' />
				// <tag k='amenity' v='toilets' /> NOT USED by FOOTPATH yet

				Node tagNode = node_children.item(j);

				if (tagNode.getNodeName().toString().equals("tag")) {
					NamedNodeMap tag_attributes = tagNode.getAttributes();
					if (tag_attributes != null) {
						String tagKValue = tag_attributes.getNamedItem("k").getNodeValue();
						String tagVValue = tag_attributes.getNamedItem("v").getNodeValue();

						// we need values for k= and v= otherwise bogus xml
						if (tagKValue != null && tagVValue != null) {

							if (tagKValue.equals("building")) {
								// Add additional attribute handling here
							} else if (tagKValue.equals("indoor")) {
								if (tagVValue.equals("yes")) {
									isIndoor = true;
								} else if (tagVValue.equals("no")) {
									isIndoor = false;
								}
							} else if (tagKValue.equals("level")) {
								level = Float.parseFloat(tagVValue);
							} else if (tagKValue.equals("name")) {
								name = tagVValue;
							} else if (tagKValue.equals("merge_id")) {
								merge_id = tagVValue;
							} else if (tagKValue.equals("amenity")) {
								// Add additional attribute handling here
							} else if (tagKValue.equals("highway")) {
								if (tagVValue.equals("door")) {
									isIndoor = true;
									isDoor = true;
								}
							}
						} // -> if (tagKValue != null && tagVValue != null)
					} // -> if (tag_attributes != null)

				} // -> for (int j = 0; j < node_children.getLength(); j++)
			} // -> if ( tagNode.getNodeName().toString().equals("tag") )

			// Create GraphNode:
			IndoorLocation tempGraphNode = new IndoorLocation(name, "");
			tempGraphNode.setDoor(isDoor);
			tempGraphNode.setId(id);
			tempGraphNode.setIndoors(isIndoor);
			tempGraphNode.setLatitude(lat);
			tempGraphNode.setLevel(level);
			tempGraphNode.setLongitude(lon);
			tempGraphNode.setMergeId(merge_id);
			// tempGraphNode.setName(name);

			allNodes.add(tempGraphNode);

		}

		// Collect GraphWays:
		for (int i = 0; i < domWays.getLength(); i++) {
			Node way = domWays.item(i);
			NamedNodeMap way_attributes = way.getAttributes();
			GraphWay tempWay = new GraphWay(Integer.parseInt(way_attributes.getNamedItem("id").getNodeValue()));
			LinkedList<Integer> refs = new LinkedList<Integer>();
			NodeList way_children = way.getChildNodes();
			for (int j = 0; j < way_children.getLength(); j++) {
				Node tagOrNDNode = way_children.item(j);
				if (tagOrNDNode.getNodeName().toString().equals("nd")) {
					// collect referenced nodes
					NamedNodeMap tag_attributes = tagOrNDNode.getAttributes();
					if (tag_attributes != null) {
						String refValue = tag_attributes.getNamedItem("ref").getNodeValue();
						if (refValue != null) {
							refs.add(new Integer(refValue));
						}
					}
					tempWay.setRefs(refs);
				} else if (tagOrNDNode.getNodeName().toString().equals("tag")) {
					// collect way attributes
					NamedNodeMap tag_attributes = tagOrNDNode.getAttributes();
					if (tag_attributes != null) {
						String tagKValue = tag_attributes.getNamedItem("k").getNodeValue();
						String tagVValue = tag_attributes.getNamedItem("v").getNodeValue();
						// we need values for k= and v= otherwise bogus xml
						if (tagKValue != null && tagVValue != null) {
							if (tagKValue.equals("indoor")) {
								tempWay.setIndoor(tagVValue);
							} else if (tagKValue.equals("level")) {
								tempWay.setLevel(Float.parseFloat(tagVValue));
							} else if (tagKValue.equals("wheelchair")) {
								tempWay.setWheelchair(tagVValue);
							} else if (tagKValue.equals("highway")) {
								tempWay.setHighway(tagVValue);
								// do some reading between the lines, if some tags have been omitted:s
								// if we have steps -> no wheeling about
								if (tagVValue.equals("steps")) {
									tempWay.setWheelchair("no");
									if (tempWay.getStepCount() == 0) {
										// if the step count has not been set, set it to undefined (-1) first
										tempWay.setStepCount(-1);
									}
								} else if (tagVValue.equals("elevator")) {
									tempWay.setStepCount(-2);
									if (tempWay.getWheelchair() != null)
										tempWay.setWheelchair("yes");
								}
							} else if (tagKValue.equals("step_count")) {
								tempWay.setStepCount(Integer.parseInt(tagVValue));
								// usually this tag is missing, or only set if step count is known
							}
						}
					}
				}
			}

			allWays.add(tempWay);
		}

		List<GraphWay> remainingWays = new LinkedList<GraphWay>();

		for (GraphWay way : allWays) { // find ways which are indoors at some point
			List<Integer> refs = way.getRefs();
			if (way.isIndoor()) { // whole path is indoors -> keep
				remainingWays.add(way);
			} else { // check for path with indoor node
				boolean stop = false;
				for (Integer ref : refs) { // check if there is a node on path which is indoors
					for (IndoorLocation node : allNodes) {
						if (node.getId() == ref.intValue()) {
							remainingWays.add(way);
							stop = true; // found indoor node on path to be added to graph thus stop both for loops and
											// continue with next way
						}
						if (stop)
							break;
					}
					if (stop)
						break;
				}
			}
		}

		if (remainingWays.size() == 0) // return false, nothing to be added to graph
			return false;

		for (GraphWay way : remainingWays) {
			String wheelchair = way.getWheelchair();
			float level = way.getLevel();
			boolean indoor = way.isIndoor();
			IndoorLocation firstNode = getNode(allNodes, way.getRefs().get(0).intValue());
			for (int i = 1; i <= way.getRefs().size() - 1; i++) {
				IndoorLocation nextNode = getNode(allNodes, way.getRefs().get(i).intValue());
				double len = firstNode.distanceTo(nextNode);
				double compDegree = firstNode.bearingTo(nextNode);
				GraphEdge tempEdge = new GraphEdge(firstNode, nextNode, len, compDegree, wheelchair, level, indoor);
				if (way.getSteps() > 0) { // make edge a staircase if steps_count was set correctly
					tempEdge.setStairs(true);
					tempEdge.setElevator(false);
					tempEdge.setSteps(way.getSteps());
				} else if (way.getSteps() == -1) {
					tempEdge.setStairs(true); // make edge a staircase if steps_count was set to -1 (undefined steps)
					tempEdge.setElevator(false);
					tempEdge.setSteps(-1);
				} else if (way.getSteps() == -2) {
					tempEdge.setStairs(false); // make edge an elevator if steps_count was set to -2
					tempEdge.setElevator(true);
					tempEdge.setSteps(-2);
				} else if (way.getSteps() == 0) {
					tempEdge.setStairs(false);
					tempEdge.setElevator(false);
					tempEdge.setSteps(0);
				}
				edges.add(tempEdge); // add edge to graph
				if (!nodes.contains(firstNode)) {
					nodes.add(firstNode); // add node to graph if not present
				}
				firstNode = nextNode;
			}

			if (!nodes.contains(firstNode)) {
				nodes.add(firstNode); // add last node to graph if not present
			}
		}
		initNodes();
		return true;
	} // -> addToGraphFromXMLFile(String filePath) { ... }

	// use this to add edges for stairs to flags, this should be called once
	public synchronized void mergeNodes() {
		// Edges are note inserted anymore.
		// Nodes are "Merged". Currently.
		LinkedList<IndoorLocation> nodesWithMergeId = new LinkedList<IndoorLocation>();
		// Collect all relevant nodes to merge
		for (IndoorLocation node : nodes) {
			if (node.getMergeId() != null) {
				nodesWithMergeId.add(node);
			}
		}
		for (IndoorLocation node : nodesWithMergeId) {
			for (IndoorLocation otherNode : nodesWithMergeId) {
				// Only merge if same id, but not same node!
				if (node.getMergeId() != null && node.getMergeId().equals(otherNode.getMergeId())
						&& !node.equals(otherNode)) {
					// Update all references pointing to otherNode to node
					for (GraphEdge edge : edges) {
						if (edge.getNode0().equals(otherNode)) {
							edge.setNode0(node);
						}
						if (edge.getNode1().equals(otherNode)) {
							edge.setNode1(node);
						}
					}
					// otherNode was merged/removed, do not check
					otherNode.setMergeId(null);
				}
			}
		}
		initNodes();
	}

	/**
	 * setup the faster access to nodes:
	 * <ul>
	 * <li>mapping id->node</li>
	 * <li>mapping name->node</li>
	 * <li>sort the node-list by id</li>
	 * <li>add edged to nodes</li>
	 * </ul>
	 */
	private void initNodes() {
		// Create arrays for binary search
		// array_nodes_by_id = sortNodesById(nodes);
		// array_nodes_by_name = sortNodesByName(nodes);

		// setup mappings for fast access
		for (int i = 0; i < nodes.size(); i++) {
			IndoorLocation n = nodes.get(i);
			map_nodes_by_id.put(n.getId(), n);

			if (n.getName() != null) {
				map_nodes_by_name.put(n.getName(), n);
			}
		}

		// Add edges to node, faster look up for neighbors
		for (GraphEdge edge : edges) {
			IndoorLocation n0 = edge.getNode0();
			IndoorLocation n1 = edge.getNode1();
			if (!n0.getEdges().contains(edge)) {
				n0.getEdges().add(edge);
			}
			if (!n1.getEdges().contains(edge)) {
				n1.getEdges().add(edge);
			}
		}

		// sort the node list
		Collections.sort(nodes, new IndoorLocationComparator());
	}

	public synchronized Stack<IndoorLocation> getShortestPath(String from, String to, boolean staircase,
			boolean elevator, boolean outside) {
		IndoorLocation gnFrom = getNodeFromName(from);
		IndoorLocation gnTo = getNodeFromName(to);
		return getShortestPath(gnFrom, gnTo, staircase, elevator, outside);
	}

	public synchronized Stack<IndoorLocation> getShortestPath(int from, String to, boolean staircase, boolean elevator,
			boolean outside) {
		IndoorLocation gnFrom = getNode(from);
		IndoorLocation gnTo = getNodeFromName(to);
		return getShortestPath(gnFrom, gnTo, staircase, elevator, outside);
	}

	/**
	 * Returns a stack of nodes, with the destination at the bottom using<br>
	 * Dykstra's algorithm<br>
	 * 
	 * @param from
	 * @param to
	 * @param staircase
	 * @param elevator
	 * @param outside
	 * @return
	 */
	public synchronized Stack<IndoorLocation> getShortestPath(IndoorLocation from, IndoorLocation to,
			boolean staircase, boolean elevator, boolean outside) {
		if (from == null || to == null) {
			return null;
		}

		// create dykstra instance
		Dykstra d = new Dykstra(from, to, staircase, elevator, outside);
		// compute path
		Stack<IndoorLocation> path = d.getShortestPathTo();

		if (path == null)
			Log.i("FOOTPATH", "Looking up path from " + from.getName() + " to " + to.getName() + " failed!");

		return path;
	}

	

	/**
	 * return a node for a given id<br>
	 * <i>this is the fast implementation after having parsed the data</i>
	 * 
	 * @param id
	 * @return
	 */
	public synchronized IndoorLocation getNode(int id) {
		if (map_nodes_by_id.containsKey(Integer.valueOf(id))) {
			return map_nodes_by_id.get(id);
		}
		return null;
		// use mappings instead.
		/*
		 * if (array_nodes_by_id == null) { initNodes(); } int u = 0; int o = array_nodes_by_id.length - 1; int m = 0;
		 * 
		 * while (!(o < u)) { m = (u + o) / 2; if (id == array_nodes_by_id[m].getId()) { return array_nodes_by_id[m]; }
		 * if (id < array_nodes_by_id[m].getId()) { o = m - 1; } else { u = m + 1; } } return null;
		 */
	}

	/**
	 * This is the slower version which is used during parsing<br>
	 * TODO remove
	 * 
	 * @param list
	 * @param id
	 * @return
	 */
	@Deprecated
	private synchronized IndoorLocation getNode(List<IndoorLocation> list, int id) {
		for (IndoorLocation node : list) {
			if (node.getId() == id)
				return node;
		}
		return null;
	}

	// return all names of nodes != null in a String array
	public synchronized String[] getRoomList() {
		if (map_nodes_by_name == null) {
			initNodes();
		}

		String[] retVal = new String[map_nodes_by_name.size()];
		retVal = map_nodes_by_name.keySet().toArray(retVal);

		return retVal;
		// use mappings instead.
		/*
		 * String[] retArray = new String[array_nodes_by_name.length]; for (int i = 0; i < retArray.length; i++) {
		 * retArray[i] = array_nodes_by_name[i].getName(); } return retArray;
		 */
	}

	/**
	 * creates a list form a stack, top to bottom
	 * 
	 * @param navPath
	 * @return
	 */
	public synchronized List<GraphEdge> getPathEdges(Stack<IndoorLocation> navPath) {
		// ArrayList should be more efficient here?!
		List<GraphEdge> pathEdges = new ArrayList<GraphEdge>(navPath.size());

		// we dont have a path with any edges
		if (navPath.size() < 2)
			return pathEdges;

		// get the edges
		Iterator<IndoorLocation> it = navPath.iterator();
		IndoorLocation current = it.next();
		IndoorLocation next;
		while (it.hasNext()) {
			next = it.next();
			for (GraphEdge e : current.getEdges()) {
				// this edge contains the next node
				if (e.getNode0().equals(next) || e.getNode1().equals(next))
					pathEdges.add(e);
				// update current node
				current = next;
			}
		}

		return pathEdges;

		/*
		 * IndoorLocation a = navPath.pop(); while (!navPath.isEmpty()) { IndoorLocation b = navPath.pop(); GraphEdge e
		 * = this.getEdge(a, b); if (e != null) { pathEdges.add(e); } else { return null; } a = b; }
		 */
	}

	/**
	 * returns the node with the given name, binary search
	 * 
	 * @param name
	 * @return
	 */
	public synchronized IndoorLocation getNodeFromName(String name) {
		if (map_nodes_by_name == null) {
			initNodes();
		}
		if (map_nodes_by_name.containsKey(name)) {
			return map_nodes_by_name.get(name);
		}

		Log.i("FOOTPATH", "Room " + name + " not found!");
		return null;
	}

	/**
	 * Returns the closest node to a position at the given level
	 * 
	 * @param pos
	 *            the position
	 * @param level
	 *            the level
	 * @param indoor
	 *            set to true if indoor nodes should be included
	 * @param maxMeters
	 *            limit of distance to a node
	 * @return the closest GraphNode
	 */
	public synchronized IndoorLocation getClosestNodeToLatLonPos(IndoorLocation pos, float level, boolean indoor,
			int maxMeters) {
		double minDistance = Double.MAX_VALUE;
		double tempDistance = Double.MAX_VALUE;
		IndoorLocation minDistNode = null;

		for (IndoorLocation node : nodes) {
			// First: node has to be at the same level Second: if indoor = true, then take all nodes
			// Third: if indoor = false check if node is not indoors!
			if (node.getLevel() == level && (indoor || (node.isIndoors() == indoor))) {
				tempDistance = pos.distanceTo(node);
				if (tempDistance < minDistance) {
					minDistance = tempDistance;
					minDistNode = node;
				}
			}
		}
		if (minDistance < maxMeters) {
			return minDistNode;
		} else {
			return null;
		}
	}

	/**
	 * @param pos
	 * @param level
	 * @param indoor
	 * @return
	 */
	public synchronized double getClosestDistanceToNode(IndoorLocation pos, float level, boolean indoor) {
		double minDistance = Double.MAX_VALUE;
		double tempDistance = Double.MAX_VALUE;

		for (IndoorLocation node : nodes) {
			// First: node has to be at the same level Second: if indoor = true, then take all nodes
			// Third: if indoor = false check if node is not indoors!
			if (node.getLevel() == level && (indoor || (node.isIndoors() != indoor))) {
				tempDistance = pos.distanceTo(node);
				if (tempDistance < minDistance) {
					minDistance = tempDistance;
				}
			}
		}
		return minDistance;
	}

	/**
	 * simple dykstra implementation using a priorityqueue comparing min dists towards nodes neighbors
	 * 
	 * @author helge
	 * 
	 */
	public class Dykstra {
		private final java.util.Map<IndoorLocation, Double> dists = new HashMap<IndoorLocation, Double>();
		private final java.util.Map<IndoorLocation, IndoorLocation> previous = new HashMap<IndoorLocation, IndoorLocation>();
		private final IndoorLocation from;
		private final IndoorLocation to;
		private boolean computed = false;
	
		private final boolean staircase;
		private final boolean elevator;
		private final boolean outside;
	
		/**
		 * @param from
		 * @param to
		 * @param staircase
		 * @param elevator
		 * @param outside
		 */
		public Dykstra(IndoorLocation from, IndoorLocation to, boolean staircase, boolean elevator, boolean outside) {
			this.from = from;
			this.to = to;
	
			this.staircase = staircase;
			this.elevator = elevator;
			this.outside = outside;
		}
	
		/**
		 * compute shortest path
		 */
		private void computePaths() {
			// setup starting distance
			dists.put(from, 0d);
	
			// the priorityqueue will hold all neighbornodes to check next - it will ensure the min dist one to be the
			// first
			PriorityQueue<IndoorLocation> queue = new PriorityQueue<IndoorLocation>(20,
					new Comparator<IndoorLocation>() {
	
						@Override
						public int compare(IndoorLocation lhs, IndoorLocation rhs) {
							return Double.compare(dists.get(lhs), dists.get(rhs));
						}
					});
			queue.add(from);
	
			while (!queue.isEmpty()) {
				IndoorLocation u = queue.poll();
	
				// Visit each edge exiting u
				for (GraphEdge e : u.getEdges()) {
					// check wether this is an allowed node
					if (e.isStairs() && !staircase) { // edge has steps, but not allowed -> skip
						continue;
					}
					if (e.isElevator() && !elevator) { // edge is elevator, but not allowed -> skip
						continue;
					}
					if (!e.isIndoor() && !outside) { // edge is outdoors, but not allowed -> skip
						continue;
					}
	
					// get the neigbor-node
					IndoorLocation v = (e.getNode0().equals(u)) ? e.getNode1() : e.getNode0();
	
					double dist = e.getLen();
					double distOverU = dists.get(u) + dist;
	
					// the map may not yet contain an entry for v
					double distOfV = (dists.containsKey(v)) ? dists.get(v) : Double.POSITIVE_INFINITY;
	
					// relax edge?
					if (distOverU < distOfV) {
						// remove from queue as it wont be up to date on the dist-change
						queue.remove(v);
	
						// update new dist
						dists.put(v, distOverU);
	
						// keep track of path
						previous.put(v, u);
	
						// stop computing as soon as target is reached
						if (v.equals(to)) {
							break;
						}
	
						// re-add this node with updated dists
						queue.add(v);
					}
				}
			}
	
			this.computed = true;
		}
	
		/**
		 * return the shortest path from source to target<br>
		 * it will compute the needed paths if necessary
		 * 
		 * @return
		 */
		public Stack<IndoorLocation> getShortestPathTo() {
			if (computed == false) {
				computePaths();
			}
	
			// did we find a path?
			if (!previous.containsKey(to)) {
				return null;
			}
	
			// yes.
			Stack<IndoorLocation> path = new Stack<IndoorLocation>();
			for (IndoorLocation vertex = to; vertex != null; vertex = previous.get(vertex)) {
				path.add(vertex);
			}
	
			return path;
		}
	}

	/**
	 * TODO maybe move directly into IndoorLocation
	 * 
	 * @author helge
	 */
	public class IndoorLocationComparator implements Comparator<IndoorLocation> {
		@Override
		public int compare(IndoorLocation lhs, IndoorLocation rhs) {
			return Integer.valueOf(lhs.getId()).compareTo(rhs.getId());
		}
	}
}
