package de.uvwxy.footpath2.map;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;
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

	@Deprecated
	private IndoorLocation[] array_nodes_by_id;
	@Deprecated
	private IndoorLocation[] array_nodes_by_name;

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

		File od = new File(filePath);

		// store all nodes found in file
		LinkedList<IndoorLocation> allNodes = new LinkedList<IndoorLocation>();
		// store all ways found in file
		LinkedList<GraphWay> allWays = new LinkedList<GraphWay>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document dom = builder.parse(new FileInputStream(od));

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

			// Interesting attributes:
			// id='-2910'
			int id = Integer.parseInt(way_attributes.getNamedItem("id").getNodeValue());
			boolean isIndoor = false;
			float level = 0;
			short wheelchair = -1;

			// Just to remember our lazy brains:
			// >0 := number correct steps given
			// 0 := no steps
			// -1 := undefined number of steps
			// -2 := elevator
			int numSteps = 0;

			LinkedList<Integer> refs = new LinkedList<Integer>();

			NodeList way_children = way.getChildNodes();
			// for crap
			for (int j = 0; j < way_children.getLength(); j++) {

				// Possible tags:
				// <nd ref='-466' />
				// <nd ref='-464' />
				// <tag k='indoor' v='yes' />
				// <tag k='level' v='0' />
				// <tag k='wheelchair' v='no' />
				// <tag k='highway' v='no' />
				Node tagOrNDNode = way_children.item(j);

				if (tagOrNDNode.getNodeName().toString().equals("nd")) {
					// collect referenced nodes

					NamedNodeMap tag_attributes = tagOrNDNode.getAttributes();
					if (tag_attributes != null) {
						String refValue = tag_attributes.getNamedItem("ref").getNodeValue();
						if (refValue != null) {
							refs.add(new Integer(refValue));
						}
					} // -> if (tag_attributes != null)
				} else if (tagOrNDNode.getNodeName().toString().equals("tag")) {
					// collect way attributes
					NamedNodeMap tag_attributes = tagOrNDNode.getAttributes();
					if (tag_attributes != null) {
						String tagKValue = tag_attributes.getNamedItem("k").getNodeValue();
						String tagVValue = tag_attributes.getNamedItem("v").getNodeValue();

						// we need values for k= and v= otherwise bogus xml
						if (tagKValue != null && tagVValue != null) {
							if (tagKValue.equals("indoor")) {
								if (tagVValue.equals("yes")) {
									isIndoor = true;
								} else {
									isIndoor = false;
								}
							} else if (tagKValue.equals("level")) {
								level = Float.parseFloat(tagVValue);
							} else if (tagKValue.equals("wheelchair")) {
								if (tagVValue.equals("yes")) {
									wheelchair = 1;
								} else if (tagVValue.equals("no")) {
									wheelchair = -1;
								} else {
									wheelchair = 0;
								}
							} else if (tagKValue.equals("highway")) {
								if (tagVValue.equals("steps")) {
									wheelchair = -1;
									if (numSteps == 0) {
										numSteps = -1;
									}
								} else if (tagVValue.equals("elevator")) {
									numSteps = -2;
									wheelchair = 1;
								}
							} else if (tagKValue.equals("step_count")) {
								numSteps = Integer.parseInt(tagVValue);
							}
						}
					} // -> if (tag_attributes != null)
				} // -> if (tagOrNDNode.getNodeName().toString().equals("nd OR tag"))
			} // -> for (int j = 0; j < way_children.getLength(); j++)

			GraphWay tempWay = new GraphWay();
			tempWay.setId(id);
			tempWay.setIndoor(isIndoor);
			tempWay.setLevel(level);
			tempWay.setRefs(refs);
			tempWay.setWheelchair(wheelchair);
			tempWay.setSteps(numSteps);

			allWays.add(tempWay);
		}

		LinkedList<GraphWay> remainingWays = new LinkedList<GraphWay>();

		for (GraphWay way : allWays) { // find ways which are indoors at some point
			LinkedList<Integer> refs = way.getRefs();
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
			short wheelchair = way.getWheelchair();
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

	public synchronized boolean addToGraphFromXMLResourceParser(XmlResourceParser xrp) throws XmlPullParserException,
			IOException {
		boolean ret = false; // return value
		if (xrp == null) {
			return ret;
		}

		boolean isOsmData = false; // flag to wait for osm data

		IndoorLocation tempNode = new IndoorLocation("", ""); // temporary node to be added to all nodes in file
		IndoorLocation NULL_NODE = new IndoorLocation("nullnode", ""); // 'NULL' node to point to, for dereferencing
		GraphWay tempWay = new GraphWay(); // temporary way to be added to all nodes in file
		GraphWay NULL_WAY = new GraphWay(); // 'NULL' node to point to, for dereferencing

		LinkedList<IndoorLocation> allNodes = new LinkedList<IndoorLocation>(); // store all nodes found in file
		LinkedList<GraphWay> allWays = new LinkedList<GraphWay>(); // store all ways found in file

		xrp.next();
		int eventType = xrp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_DOCUMENT:
				break;
			case XmlPullParser.START_TAG:
				if (!isOsmData) {
					if (xrp.getName().equals("osm")) {
						isOsmData = true; // osm
						// TODO: Test for correct version? (v0.6)
					}
				} else {
					int attributeCount = xrp.getAttributeCount();
					if (xrp.getName().equals("node")) { // node
						tempNode = new IndoorLocation("", "");
						for (int i = 0; i < attributeCount; i++) {
							if (xrp.getAttributeName(i).equals("id")) {
								tempNode.setId(xrp.getAttributeIntValue(i, 0)); // node.id
							}
							if (xrp.getAttributeName(i).equals("lat")) {
								tempNode.setLatitude(Double.parseDouble(xrp.getAttributeValue(i))); // node.lat
							}
							if (xrp.getAttributeName(i).equals("lon")) {
								tempNode.setLongitude(Double.parseDouble(xrp.getAttributeValue(i))); // node.lon
							}
						}
					} else if (xrp.getName().equals("tag")) { // tag
						if (tempNode != NULL_NODE) { // node.tag
							for (int i = 0; i < attributeCount; i++) {
								if (xrp.getAttributeName(i).equals("k") && xrp.getAttributeValue(i).equals("indoor")) { // node.tag.indoor
									String v = xrp.getAttributeValue(i + 1);
									tempNode.setIndoors(v.equals("yes"));
									if (v.equals("door")) {
										tempNode.setIndoors(true);
										tempNode.setDoor(true); // this is a
																// door (which
																// is always
																// inDOORS) ;)
									}
								} else if (xrp.getAttributeName(i).equals("k")
										&& xrp.getAttributeValue(i).equals("name")) { // node.tag.name
									String v = xrp.getAttributeValue(i + 1);
									tempNode.setName(v);
								} else if (xrp.getAttributeName(i).equals("k")
										&& xrp.getAttributeValue(i).equals("merge_id")) { // node.tag.merge_id
									String v = xrp.getAttributeValue(i + 1);
									tempNode.setMergeId(v);
								} else if (xrp.getAttributeName(i).equals("k")
										&& xrp.getAttributeValue(i).equals("step_count")) { // node.tag.step_count
									int v = xrp.getAttributeIntValue(i + 1, Integer.MAX_VALUE);
									tempNode.setSteps(v);
								} else if (xrp.getAttributeName(i).equals("k")
										&& xrp.getAttributeValue(i).equals("level")) { // node.tag.level
									String v = xrp.getAttributeValue(i + 1);
									float f = Float.parseFloat(v);
									tempNode.setLevel(f);
								}

							}
						} else { // way.tag
							for (int i = 0; i < attributeCount; i++) {
								if (xrp.getAttributeName(i).equals("k")
										&& xrp.getAttributeValue(i).equals("wheelchair")) { // way.tag.wheelchair
									String v = xrp.getAttributeValue(i + 1);
									short wheelchair = (short) (v.equals("yes") ? 1 : v.equals("limited") ? 0 : -1);
									tempWay.setWheelchair(wheelchair);
								} else if (xrp.getAttributeName(i).equals("k")
										&& xrp.getAttributeValue(i).equals("step_count")) { // way.tag.step_count
									int v = xrp.getAttributeIntValue(i + 1, Integer.MAX_VALUE);
									tempWay.setSteps(v);
								} else if (xrp.getAttributeName(i).equals("k")
										&& xrp.getAttributeValue(i).equals("level")) { // way.tag.level
									String v = xrp.getAttributeValue(i + 1);
									float f = Float.parseFloat(v);
									tempWay.setLevel(f);
								} else if (xrp.getAttributeName(i).equals("k")
										&& xrp.getAttributeValue(i).equals("indoor")) { // way.tag.indoor
									String v = xrp.getAttributeValue(i + 1);
									tempWay.setIndoor(v.equals("yes"));
								} else if (xrp.getAttributeName(i).equals("k")
										&& xrp.getAttributeValue(i).equals("highway")) { // way.tag.highway
									String v = xrp.getAttributeValue(i + 1);
									if (v.equals("steps")) {
										tempWay.setWheelchair((short) -1);
										if (tempWay.getSteps() == 0) { // no steps configured before
											tempWay.setSteps(-1); // so set to undefined (but present), otherwise might
																	// be set later
										}
									}
									if (v.equals("elevator")) {
										tempWay.setWheelchair((short) 1);
										tempWay.setSteps(-2);
									}
								}
							}
						}

					} else if (xrp.getName().equals("way")) { // way
						tempWay = new GraphWay();
						for (int i = 0; i < attributeCount; i++) {
							if (xrp.getAttributeName(i).equals("id")) {
								tempWay.setId(xrp.getAttributeIntValue(i, 0)); // way.id
							}
						}
					} else if (xrp.getName().equals("nd")) { // way.nd
						for (int i = 0; i < attributeCount; i++) {
							if (xrp.getAttributeName(i).equals("ref")) { // way.nd.ref
								String v = xrp.getAttributeValue(i);
								int ref = Integer.parseInt(v);
								tempWay.addRef(ref);
							}
						}
					}
				}
				break;
			case XmlPullParser.END_TAG:
				if (isOsmData) {
					if (xrp.getName().equals("osm")) {
						ret = true;
					} else if (xrp.getName().equals("node")) { // node
						allNodes.add(tempNode);
						tempNode = NULL_NODE;
					} else if (xrp.getName().equals("tag")) { // tag

					} else if (xrp.getName().equals("way")) { // way
						allWays.add(tempWay);
						tempWay = NULL_WAY;
					} else if (xrp.getName().equals("nd")) { // way.nd

					}
				}
				break;
			default:
			}
			eventType = xrp.next();
		}

		List<GraphWay> remainingWays = new LinkedList<GraphWay>();

		for (GraphWay way : allWays) { // find ways which are indoors at some
										// point
			List<Integer> refs = way.getRefs();
			if (way.isIndoor()) { // whole path is indoors -> keep
				remainingWays.add(way);
			} else { // check for path with indoor node
				boolean stop = false;
				for (Integer ref : refs) { // check if there is a node on path
											// which is indoors
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
			short wheelchair = way.getWheelchair();
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
					tempEdge.setStairs(true); // make edge a staircase if steps_count was set to -1
												// (undefined steps)
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
		return ret;
	}

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

	// Returns a stack of nodes, with the destination at the bottom using
	// Dykstra's algorithm
	public synchronized Stack<IndoorLocation> getShortestPath(IndoorLocation from, IndoorLocation to,
			boolean staircase, boolean elevator, boolean outside) {
		if (from == null || to == null) {
			return null;
		}

		Log.i("FOOTPATH", "Looking up path from " + from.getName() + " to " + to.getName());

		int remaining_nodes = nodes.size();// array_nodes_by_id.length;
		IndoorLocation[] previous = new IndoorLocation[remaining_nodes];
		double[] dist = new double[remaining_nodes];
		boolean[] visited = new boolean[remaining_nodes];

		// Set initial values
		for (int i = 0; i < remaining_nodes; i++) {
			dist[i] = Double.POSITIVE_INFINITY;
			previous[i] = null;
			visited[i] = false;
		}
		dist[getNodePosInIdArray(from)] = 0;
		while (remaining_nodes > 0) {
			// Vertex u in q with smallest dist[]
			IndoorLocation u;
			double minDist = Double.POSITIVE_INFINITY;
			int u_i = -1;
			for (int i = 0; i < remaining_nodes; i++) {
				if (!visited[i] && dist[i] < minDist) {
					u_i = i;
					minDist = dist[i];
				}
			}

			if (u_i == -1) {
				// No nodes left
				break;
			}
			// u was found
			u = nodes.get(u_i);// array_nodes_by_id[u_i];
			visited[u_i] = true;
			if (dist[u_i] == Double.POSITIVE_INFINITY) {
				// All remaining nodes are unreachable from source
				break;
			}
			// Get neighbors of u in q
			List<IndoorLocation> nOuIq = getNeighbours(visited, u, staircase, elevator, outside);
			if (u.equals(to)) {
				// u = to -> found path to destination Build stack of nodes, destination at the
				// bottom
				Stack<IndoorLocation> s = new Stack<IndoorLocation>();
				while (previous[u_i] != null) {
					s.push(u);
					u_i = getNodePosInIdArray(u);
					u = previous[u_i];
				}
				return s;
			} else {
				remaining_nodes--;
			}
			for (IndoorLocation v : nOuIq) {
				double dist_alt = dist[u_i] + u.distanceTo(v);
				int v_i = getNodePosInIdArray(v);
				if (dist_alt < dist[v_i]) {
					dist[v_i] = dist_alt;
					previous[v_i] = u;
				}
			}
		}

		Log.i("FOOTPATH", "Looking up path from " + from.getName() + " to " + to.getName() + " failed!");
		return null;
	}

	// Returns all neighbors of given node from a given subset (list) of nodes in this graph
	private LinkedList<IndoorLocation> getNeighbours(boolean[] visited, IndoorLocation node, boolean staircase,
			boolean elevator, boolean outside) {

		LinkedList<IndoorLocation> ret = new LinkedList<IndoorLocation>();
		for (GraphEdge edge : node.getEdges()) { // check all edges if they contain node
			if (edge.isStairs() && !staircase) { // edge has steps, but not
													// allowed -> skip
				continue;
			}
			if (edge.isElevator() && !elevator) { // edge is elevator, but not
													// allowed -> skip
				continue;
			}
			if (!edge.isIndoor() && !outside) { // edge is outdoors, but not
												// allowed -> skip
				continue;
			}

			IndoorLocation buf = null;
			if (edge.getNode0().equals(node)) { // node0 is node
				buf = edge.getNode1(); // add node1
			} else if (edge.getNode1().equals(node)) { // node 1 is node
				buf = edge.getNode0(); // add node0
			}
			if (outside) {
				if (buf != null) { // if outside, all nodes are allowed
					if (!ret.contains(buf) && !visited[getNodePosInIdArray(buf)]) {
						ret.add(buf); // add buf only once, iff not visited
					}
				}
			} else { // if !outside, only indoor nodes are allowed
				if (buf != null && buf.isIndoors()) {
					if (!ret.contains(buf) && !visited[getNodePosInIdArray(buf)]) {
						ret.add(buf); // add buf only once, iff not visited
					}
				}
			}
		}
		return ret;
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

	/**
	 * return node pos via binary search<br>
	 * TODO rename method.<br>
	 * <i>NOTE: Collections.binarySearch() runs in O(n) on unsorted lists</i>
	 * 
	 * @param node
	 * @return
	 */
	private int getNodePosInIdArray(IndoorLocation node) {
		return Collections.binarySearch(nodes, node, new IndoorLocationComparator());
		// return nodes.indexOf(node); // O(n)

		/*
		 * if (array_nodes_by_id == null) { initNodes(); } int u = 0; int o = array_nodes_by_id.length - 1; int m = 0;
		 * 
		 * while (!(o < u)) { m = (u + o) / 2; if (node.getId() == array_nodes_by_id[m].getId()) { return m; } if
		 * (node.getId() < array_nodes_by_id[m].getId()) { o = m - 1; } else { u = m + 1; } } return -1;
		 */
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
	 * TODO remove.
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

	// creates a linked list form a stack, top to bottom
	public synchronized List<GraphEdge> getPathEdges(Stack<IndoorLocation> navPath) {
		List<GraphEdge> pathEdges = new LinkedList<GraphEdge>();
		IndoorLocation a = navPath.pop();
		while (!navPath.isEmpty()) {
			IndoorLocation b = navPath.pop();
			GraphEdge e = this.getEdge(a, b);
			if (e != null) {
				pathEdges.add(e);
			} else {
				return null;
			}
			a = b;
		}
		return pathEdges;
	}

	/**
	 * returns the edge containing nodes a and b<br>
	 * TODO more efficient solution
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public synchronized GraphEdge getEdge(IndoorLocation a, IndoorLocation b) {
		GraphEdge ret = null;
		for (GraphEdge edge : a.getEdges()) { // return edge if
			if (edge.getNode0().equals(a) && edge.getNode1().equals(b)) { // node0=a, node1=b
				ret = edge; // or
			} else if (edge.getNode1().equals(a) && edge.getNode0().equals(b)) { // node0=b, node1=a
				ret = edge;
			} // else null
		}
		return ret;
	}

	// returns the node with the given name, binary search
	public synchronized IndoorLocation getNodeFromName(String name) {
		if (map_nodes_by_name == null) {
			initNodes();
		}
		if (map_nodes_by_name.containsKey(name)) {
			return map_nodes_by_name.get(name);
		}

		Log.i("FOOTPATH", "Room " + name + " not found!");
		return null;

		// should use mappings now
		/*
		 * if (array_nodes_by_name == null) { initNodes(); } int u = 0; int o = array_nodes_by_name.length - 1; int m =
		 * 0;
		 * 
		 * while (!(o < u)) { m = (u + o) / 2; if (name.equals(array_nodes_by_name[m].getName())) { Log.i("FOOTPATH",
		 * "Room " + array_nodes_by_name[m].getName() + " found!"); return array_nodes_by_name[m]; } if
		 * (name.compareTo(array_nodes_by_name[m].getName()) < 0) { o = m - 1; } else { u = m + 1; } }
		 * 
		 * Log.i("FOOTPATH", "Room " + name + " not found!"); return null;
		 */
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
	 * creates an array, containing only nodes _with_ a name, sorted ascending
	 * 
	 * @param nodes
	 * @return
	 */
	@Deprecated
	private IndoorLocation[] sortNodesByName(List<IndoorLocation> nodes) {
		IndoorLocation[] node_array;
		IndoorLocation temp = null;
		int num_nulls = 0;
		int c = 0;
		boolean not_sorted = true;
		// count number of nodes without a name (null)
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i) != null && nodes.get(i).getName() == null) {
				num_nulls++;
			}
		}
		// create an array for nodes with a name
		node_array = new IndoorLocation[nodes.size() - num_nulls];
		for (IndoorLocation node : nodes) {
			if (node != null && node.getName() != null) {
				// insert node with name into array
				node_array[c] = node;
				c++;
			}
		}
		// sort by name (bubble sort)
		while (not_sorted) {
			not_sorted = false;
			for (int i = 0; i < node_array.length - 1; i++) {
				if (node_array[i].getName().compareTo(node_array[i + 1].getName()) > 0) {
					temp = node_array[i];
					node_array[i] = node_array[i + 1];
					node_array[i + 1] = temp;
					not_sorted = true;
				}
			}
		}
		// return array which had no nulls
		return node_array;
	}

	/**
	 * creates an array,sorted by id ascending
	 * 
	 * @param nodes
	 * @return
	 */
	@Deprecated
	private IndoorLocation[] sortNodesById(List<IndoorLocation> nodes) {
		IndoorLocation[] node_array;
		IndoorLocation temp = null;
		int c = 0;
		boolean not_sorted = true;
		// create an array for all nodes
		node_array = new IndoorLocation[nodes.size()];
		for (IndoorLocation node : nodes) {
			if (node != null) {
				// insert node
				node_array[c] = node;
				c++;
			}
		}
		// sort by id (bubble sort)
		while (not_sorted) {
			not_sorted = false;
			for (int i = 0; i < node_array.length - 1; i++) {
				if (node_array[i].getId() > node_array[i + 1].getId()) {
					temp = node_array[i];
					node_array[i] = node_array[i + 1];
					node_array[i + 1] = temp;
					not_sorted = true;
				}
			}
		}
		return node_array;
	}
}
