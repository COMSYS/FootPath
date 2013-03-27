package de.uvwxy.footpath2.drawing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import de.uvwxy.footpath2.map.GraphEdge;
import de.uvwxy.footpath2.map.GraphWay;
import de.uvwxy.footpath2.map.IndoorLocation;
import de.uvwxy.footpath2.tools.GeoUtils;

@Deprecated
public class BuildingLoaderAndPainter {
	LinkedList<GraphEdge> walls_outer = new LinkedList<GraphEdge>();
	LinkedList<GraphEdge> walls_inner = new LinkedList<GraphEdge>();
	LinkedList<LinkedList<GraphEdge>> walls_outer_area = new LinkedList<LinkedList<GraphEdge>>();
	LinkedList<LinkedList<GraphEdge>> walls_inner_area = new LinkedList<LinkedList<GraphEdge>>();
	
	LinkedList<GraphEdge> stairs = new LinkedList<GraphEdge>();
	LinkedList<GraphEdge> elevators = new LinkedList<GraphEdge>();
	LinkedList<LinkedList<GraphEdge>> stairs_area = new LinkedList<LinkedList<GraphEdge>>();
	LinkedList<LinkedList<GraphEdge>> elevators_area = new LinkedList<LinkedList<GraphEdge>>();
	
	LinkedList<IndoorLocation> nodes = new LinkedList<IndoorLocation>();

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
	public synchronized boolean addToBuildingFromXMLFile(String filePath) throws ParserConfigurationException,
			FileNotFoundException, SAXException, IOException {

		if (filePath == null)
			return false;

		File od = new File(filePath);

		Log.i("FOOTPATH", "Loading building data from " + filePath);

		// store all nodes found in file
		ArrayList<IndoorLocation> allNodes = new ArrayList<IndoorLocation>();
		// store all ways found in file
		ArrayList<GraphWay> allWays = new ArrayList<GraphWay>();

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
			IndoorLocation tempGraphNode = new IndoorLocation(name, "footpath");

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
								tempGraphNode.setIndoor(tagVValue);
							} else if (tagKValue.equals("level")) {
								level = Float.parseFloat(tagVValue);
							} else if (tagKValue.equals("name")) {
								name = tagVValue;
							} else if (tagKValue.equals("merge_id")) {
								merge_id = tagVValue;
							} else if (tagKValue.equals("amenity")) {
								// Add additional attribute handling here
							}else if (tagKValue.equals("door")) {
								tempGraphNode.setDoor(tagVValue);
							}
						} // -> if (tagKValue != null && tagVValue != null)
					} // -> if (tag_attributes != null)

				} // -> for (int j = 0; j < node_children.getLength(); j++)
			} // -> if ( tagNode.getNodeName().toString().equals("tag") )

			// Create GraphNode:
			tempGraphNode.setId(id);
			tempGraphNode.setLatitude(lat);
			tempGraphNode.setLevel(level);
			tempGraphNode.setLongitude(lon);
			tempGraphNode.setMergeId(merge_id);
			tempGraphNode.setName(name);

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

			boolean isArea = false;
			String buildingpart = null;
			
			ArrayList<Integer> refs = new ArrayList<Integer>();


			NodeList way_children = way.getChildNodes();
			// for crap
			for (int j = 0; j < way_children.getLength(); j++) {
				GraphWay tempWay = new GraphWay();

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
							} else if (tagKValue.equals("buildingpart")) {
									buildingpart = tagVValue;
							}
						}
					} // -> if (tag_attributes != null)
				} // -> if (tagOrNDNode.getNodeName().toString().equals("nd OR tag"))
			} // -> for (int j = 0; j < way_children.getLength(); j++)


				GraphWay tempWay = new GraphWay();
				tempWay.setId(id);
//				tempWay.setIndoor(isIndoor);
				tempWay.setLevel(level);
				tempWay.setRefs(refs);
//				tempWay.setWheelchair(wheelchair);
				tempWay.setSteps(numSteps);
				tempWay.setBuildingpart(buildingpart);
				// if (tempWay.getWheelchair() == )
				allWays.add(tempWay);
			
		}

		if (allWays.size() == 0) // return false, nothing to be added to graph
			return false;

		for (GraphWay way : allWays) {
			String wheelchair = way.getWheelchair();
			float level = way.getLevel();
			IndoorLocation firstNode = getNode(allNodes, way.getRefs().get(0).intValue());
			for (int i = 1; i <= way.getRefs().size() - 1; i++) {
				IndoorLocation nextNode = getNode(allNodes, way.getRefs().get(i).intValue());
				double len = firstNode.distanceTo(nextNode);
				double compDegree = firstNode.bearingTo(nextNode);
				GraphEdge tempEdge = new GraphEdge(firstNode, nextNode, len, compDegree, wheelchair, level, way.getIndoor());
				tempEdge.setHighway(way.getHighway());
				tempEdge.setSteps(way.getSteps());

				if (tempEdge.isElevator()) {
					elevators.add(tempEdge);
				} else if (tempEdge.isStairs()) {
					stairs.add(tempEdge);
				} else {
					walls_inner.add(tempEdge); // add edge to graph
				}
				if (!nodes.contains(firstNode)) {
					nodes.add(firstNode); // add node to graph if not present
				}
				firstNode = nextNode;
			}

			if (!nodes.contains(firstNode)) {
				nodes.add(firstNode); // add last node to graph if not present
			}
		}

		Log.i("FOOTPATH", "Add map data: " + elevators.size() + " elevator edges");
		Log.i("FOOTPATH", "Add map data: " + stairs.size() + " stairs edges");
		Log.i("FOOTPATH", "Add map data: " + walls_inner.size() + " walls (inner) edges");
		Log.i("FOOTPATH", "Add map data: " + walls_outer.size() + " walls (outer) edges");

		return true;
	} // -> addToGraphFromXMLFile(String filePath) { ... }+

	public synchronized void drawToCanvas(Canvas canvas, IndoorLocation center, Rect boundingBox,
			double pixelsPerMeterOrMaxValue, Paint pLine, Paint pDots) {
		int w = boundingBox.width() / 2 + boundingBox.left;
		int h = boundingBox.height() / 2 + boundingBox.top;

		if (canvas == null || center == null || pLine == null || pDots == null) {
			return;
		}

		pLine.setColor(Color.WHITE);
		for (int i = 0; i < walls_inner.size() - 1; i++) {
			int[] apix = GeoUtils.convertToPixelLocation(walls_inner.get(i).getNode0(), center, pixelsPerMeterOrMaxValue);
			int[] bpix = GeoUtils.convertToPixelLocation(walls_inner.get(i).getNode1(), center, pixelsPerMeterOrMaxValue);
			canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pLine);
		}
		pLine.setColor(Color.RED);
		for (int i = 0; i < stairs.size() - 1; i++) {
			int[] apix = GeoUtils.convertToPixelLocation(stairs.get(i).getNode0(), center, pixelsPerMeterOrMaxValue);
			int[] bpix = GeoUtils.convertToPixelLocation(stairs.get(i).getNode1(), center, pixelsPerMeterOrMaxValue);
			canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pLine);
		}

		pLine.setColor(Color.GREEN);
		for (int i = 0; i < elevators.size() - 1; i++) {
			int[] apix = GeoUtils.convertToPixelLocation(elevators.get(i).getNode0(), center, pixelsPerMeterOrMaxValue);
			int[] bpix = GeoUtils.convertToPixelLocation(elevators.get(i).getNode1(), center, pixelsPerMeterOrMaxValue);
			canvas.drawLine(w + apix[0], h + apix[1], w + bpix[0], h + bpix[1], pLine);
		}

	}

	// This is the slower version which is used during parsing (how about a has map?)
	private synchronized IndoorLocation getNode(ArrayList<IndoorLocation> list, int id) {
		for (IndoorLocation node : list) {
			if (node.getId() == id)
				return node;
		}
		return null;
	}

}
