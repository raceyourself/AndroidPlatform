package com.glassfitgames.glassfitplatform.gpstracker.kml;

import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;

import com.ekito.simpleKML.model.*;
import com.ekito.simpleKML.Serializer;

import com.glassfitgames.glassfitplatform.models.Position;

public class GFKml {
    private Kml kml = new Kml();
    // High-level KML document object
    private Document document = new Document();
    
    // Represents curretly active path. Can be null if no active path
    private Folder path;
    // Currently active style
    private String style;
    // Current position id
    private int positionId;
       
    public GFKml() { 
        document.setFeatureList( new Vector<Feature>());
        kml.setFeature(document);
        // Init styles
        initStyles();
    }
    
    // Start new position's path. Sequential calls to addPosition will add positions
    // to this path
    // TODO: get as a parameter path type: GPS, PREDICTION etc.
    public void startPath() {
        path = new Folder();
        path.setFeatureList(new Vector<Feature>());
        document.getFeatureList().add(path);
        positionId = 0;
        // TODO: choose style
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
        // Placemark holds all data about a position
        Placemark pm = new Placemark();
        // Set style & name
        pm.setStyleUrl("#" + style);
        pm.setName("Point " + Integer.toString(++positionId));
        // Add timestamp. TODO: choose between Gps and Device timestamp according to path type
        Date date = new Date();
        date.setTime(pos.getDeviceTimestamp());
        TimeStamp ts = new TimeStamp();
        ts.setWhen(DateFormat.getDateTimeInstance().format(date));
        pm.setTimePrimitive(ts);
        // Geometry list of the placemark
        List<Geometry> lg = new Vector<Geometry>();
        pm.setGeometryList(lg);
        // Multigeometry will hold point and heading line
        MultiGeometry mg = new MultiGeometry();
        mg.setGeometryList(new Vector<Geometry>());
        lg.add(mg);
        // Add point
        Point pt = new Point();
        pt.setCoordinates(positionToCoordinate(pos));
        mg.getGeometryList().add(pt);
        // Add heading as a line from given to predicted position
        LineString heading = addHeading(pos);
        if (heading != null) {
            mg.getGeometryList().add(heading);
        }
        // Add placemark to the current path        
        path.getFeatureList().add(pm);
        return true;

    }
    
    public void write(java.io.OutputStream out) throws java.lang.Exception {
        Serializer serializer = new Serializer();
        serializer.write(kml, out);
    }
    
    private Coordinate positionToCoordinate(Position pos) {
        return new Coordinate(pos.getLngx(), pos.getLatx(), pos.getAltitude());
    }
    
    private String positionToString(Position pos) {
        return positionToCoordinate(pos).toString();
    }
    
    // Draw line from current to predicted position
    private LineString addHeading(Position pos) {
        Position predictedPos = Position.predictPosition(pos, 10); // milliseconds
        if (predictedPos == null) {
            return null;
        }
        ArrayList<Coordinate> coordList = new ArrayList<Coordinate>();
        coordList.add(positionToCoordinate(pos));
        coordList.add(positionToCoordinate(predictedPos));

        Coordinates coords = new Coordinates(positionToString(pos));
        coords.setList(coordList);
        
        LineString ls = new LineString();        
        ls.setCoordinates(coords);
        return ls;
    }
    
    private void initStyles() {
        String color = "ffff0000";        
        Pair normStylePair = initStyle(color, false);
        Pair hiliStylePair = initStyle(color, true);
        normStylePair.getStyle().setId("normalPositionStyleBlue");
        hiliStylePair.getStyle().setId("hiliPositionStyleBlue");
        
        StyleMap styleMap = new StyleMap();
        //style = "generalPositionStyleBlue";
        style = "normalPositionStyleBlue";
        styleMap.setId(style);
        List<Pair> lp = new ArrayList<Pair>();
        lp.add(normStylePair);
        lp.add(hiliStylePair);
        styleMap.setPairList(lp);
        
        List<StyleSelector> styleSelector = new ArrayList<StyleSelector>();
        styleSelector.add(normStylePair.getStyle());
        styleSelector.add(hiliStylePair.getStyle());
        styleSelector.add(styleMap);
        
        document.setStyleSelector(styleSelector);
    }
    
    private Pair initStyle(String color, boolean isHiLi) {
        String href;
        float scale;
        String pairKey;
        if (isHiLi) {
           href = "http://maps.google.com/mapfiles/kml/shapes/arrow.png";
           scale = (float)0.0;
           pairKey = "highlight";
           
        } else {
           href = "http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png";
           scale = (float)1.0;
           pairKey = "normal";
        }

        // Icon style
        IconStyle is = new IconStyle();
        is.setColor(color);
        Icon ic = new Icon();        
        ic.setHref(href);
        is.setIcon(ic);
        // Label style
        LabelStyle ls = new LabelStyle();
        ls.setScale(scale);
        // Line style
        LineStyle lis = new LineStyle();
        is.setColor(color);
        // Construct style itself
        Style st = new Style();
        st.setIconStyle(is);
        st.setLabelStyle(ls);
        st.setLineStyle(lis);

        Pair stylePair = new Pair();
        stylePair.setKey(pairKey);
        stylePair.setStyle(st);
        return stylePair;
        
    }
    
}