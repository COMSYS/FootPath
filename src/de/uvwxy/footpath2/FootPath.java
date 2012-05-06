package de.uvwxy.footpath2;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import de.uvwxy.footpath2.map.Map;
import de.uvwxy.footpath2.types.FP_LocationProvider;
import de.uvwxy.footpath2.types.FP_MovementDetection;

/**
 * This class will be the main interface to use footpath.
 * 
 * @author paul
 * 
 */
public class FootPath {
	Context context;
	Map map;
	
	public FootPath(Context context) {
		// TODO:
		this.context = context;
		map = new Map();
	}

	private void exampleUsage() {

		// initialization
		FootPath fp = new FootPath(null);

		// movement input and location output setup
		setMovementDetection(FP_MovementDetection.MOVEMENT_DETECTION_STEPS);
		setLocationProvider(FP_LocationProvider.LOCATION_PROVIDER_FOOTPATH);

		setDestination(null);
		// set location (possibility: also later during navigation (Wifi, etc))
		setLocation(null);

		// starting, stopping, resetting of navigation
		start();
		stop();
		reset();

		return;
	}

	public void loadMapDataFromAsset(String uri) {
		// TODO:

	}

	public void loadMapDataFromXMLResource(int resID) {
		// TODO:
		try {
			map.addToGraphFromXMLResourceParser(context.getResources().getXml(resID));
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void loadMapDataFromURL(String uri) {
		// TODO:

	}

	public void loadMapDataFromXML(String uri) {
		// TODO:
		try {
			map.addToGraphFromXMLFile(uri);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void setMovementDetection(FP_MovementDetection movementDetectionSteps) {
		// TODO:
	}

	public void setLocationProvider(FP_LocationProvider provider) {
		// TODO:

	}

	public void setDestination(Location l) {
		// TODO:

	}

	public void setLocation(Location l) {
		// TODO:

	}

	public int start() {
		// TODO:

		return 0;
	}

	public int stop() {
		// TODO:

		return 0;
	}

	public int reset() {
		// TODO:

		return 0;
	}
}
