package com.glassfitgames.glassfitplatform.gpstracker.kml;

import java.util.Vector;
import java.util.List;

import com.ekito.simpleKML.model.*;
import com.ekito.simpleKML.Serializer;

import com.glassfitgames.glassfitplatform.models.Position;

public class GFKml {
    private Kml kml = new Kml();
    // High-level KML document object
    private Document document = new Document();
    
    // Represents curretly active path. Can be null if no active path
    private Folder path;
        
    public GFKml() { 
        document.setFeatureList( new Vector<Feature>());
        kml.setFeature(document);
        // TODO: init styles
    }
    
    // Start new position's path. Sequential calls to addPosition will add positions
    // to this path
    // TODO: get as a parameter path type: GPS, PREDICTION etc.
    public void startPath() {
        path = new Folder();
        path.setFeatureList(new Vector<Feature>());
        document.getFeatureList().add(path);
    }
    
    // End position path
    public void endPath() {
        path = null;
    }
    
    // Add position to KML as a placemark
    public boolean addPosition(Position pos) {
        if (path == null) {
            return false;
        }
        String coordsString = Double.toString(pos.getLngx()) + "," + Double.toString(pos.getLatx());
        Placemark pm = new Placemark();
        Point pt = new Point();

        pt.setCoordinates(new Coordinate(pos.getLngx(), pos.getLatx(), pos.getAltitude()));
        List<Geometry> lg = new Vector<Geometry>();
        lg.add(pt);
        pm.setGeometryList(lg);
        
        path.getFeatureList().add(pm);
        return true;

    }
    
    public void write(java.io.OutputStream out) throws java.lang.Exception {
        Serializer serializer = new Serializer();
        serializer.write(kml, out);
    }
    
}