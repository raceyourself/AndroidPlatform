package com.glassfitgames.glassfitplatform.gpstracker.kml;

import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;

import com.ekito.simpleKML.model.*;
import com.ekito.simpleKML.Serializer;

import com.glassfitgames.glassfitplatform.models.Position;

public class GFKml {
    private Kml kml = new Kml();
    // High-level KML document object
    private Document document = new Document();
    
    // Enum defines types of paths
    public enum PathType { 
        GPS ("ffff0000", "Blue", "GPS", (float)1.0), 
        PREDICTION("c0c0c0ff", "Grey", "Prediction", (float)0.3),
        EXTRAPOLATED("b0b0b0ff", "DarkGrey", "Extrapolated", (float)0.3);

        private final String color;
        private final String colorName;
        private final String pathName;     
        private float scale;

        PathType(String color, String colorName, String pathName, float scale) {
            this.color = color;
            this.colorName = colorName;
            this.pathName = pathName;
            this.scale = scale;
        }
        public String color() { return color; } 
        public String colorName() { return colorName; }
        public String pathName() { return pathName; }
        public float scale() { return scale; }
    };

    
    private Map<PathType, Path> pathMap = new HashMap<PathType, Path>();
    
    public GFKml() { 
        document.setFeatureList( new Vector<Feature>());
        document.setStyleSelector(new ArrayList<StyleSelector>());

        kml.setFeature(document);
    }
            
    // Add position to KML as a placemark
    public boolean addPosition(PathType pathType, Position pos) {
        if (pathMap.get(pathType) == null) {
            startPath(pathType);
        }
        
        // Position mark holds all data about a position 
        PositionMark pm = new PositionMark(pos);
        // Add placemark to the current path        
        pathMap.get(pathType).addPlacemark(pm.getPlacemark());
        return true;

    }  
    
    public void write(java.io.OutputStream out) throws java.lang.Exception {
        Serializer serializer = new Serializer();
        serializer.write(kml, out);
    }
    
    // Start new position's path. Sequential calls to addPosition will add positions
    // to this path
    // TODO: get as a parameter path type: GPS, PREDICTION etc.
    private void startPath(PathType pathType) {
        Path path = new Path(document, pathType);
        pathMap.put(pathType, path);
        path.initStyles(document.getStyleSelector());
        // TODO: choose style
    }
             
    
    // The class represents positions path 
    private class Path {
        private Folder folder;
        private String styleId;
        
        private String color;
        private String colorName;
        private String style;
        
        int positionId = 0;
        PathType pathType;
        
        
        public Path(Document doc, PathType pathType) {
            folder = new Folder();
            folder.setName(pathType.pathName());
            folder.setFeatureList(new Vector<Feature>());
            doc.getFeatureList().add(folder);
            // Init styles
            this.pathType = pathType;

        }
        
        public void initStyles(List<StyleSelector> styleSelector) {
            Pair normStylePair = initStyle(pathType.color(), "normalPositionStyle" + pathType.colorName(), false);
            Pair hiliStylePair = initStyle(pathType.color(), "hiliPositionStyle" + pathType.colorName(), true);
            
            StyleMap styleMap = new StyleMap();
            styleId = "generalPositionStyle" + pathType.colorName();
            styleMap.setId(style);
            // TODO: bug in XML writing of StyleMap, using normal style instead
            style = "normalPositionStyle"  + pathType.colorName();
            List<Pair> lp = new ArrayList<Pair>();
            lp.add(normStylePair);
            lp.add(hiliStylePair);
            styleMap.setPairList(lp);
            
            styleSelector.add(normStylePair.getStyle());
            styleSelector.add(hiliStylePair.getStyle());
            styleSelector.add(styleMap);
            
            // FIXME: workaround to avoid duplicated styles
            normStylePair.setStyle(null);
            hiliStylePair.setStyle(null);            
        }
        
        private Pair initStyle(String color, String id, boolean isHiLi) {
            String href;
            float scale;
            String pairKey;
            if (isHiLi) {
                href = "http://maps.google.com/mapfiles/kml/shapes/arrow.png";
                scale = (float)0.0;
                pairKey = "highlight";
                
            } else {
                href = "http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png";
                scale = pathType.scale();
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
            st.setId(id);
            st.setIconStyle(is);
            st.setLabelStyle(ls);
            st.setLineStyle(lis);
            
            Pair stylePair = new Pair();
            stylePair.setKey(pairKey);
            stylePair.setStyleUrl(id);
            stylePair.setStyle(st);
            return stylePair;
            
        }

        public String getStyleId() {
            return styleId;
        }
        
        public void addPlacemark(Placemark pm) {
            // Set style & name
            pm.setStyleUrl("#" + style);
            pm.setName("Point " + Integer.toString(++positionId));
            // Add placemark to the current path        
            folder.getFeatureList().add(pm);

        }
    }
    
    // The class represents single placemark initialized from position
    private class PositionMark {
        private Placemark pm = new Placemark();
        private Position pos;
        
        PositionMark(Position pos) {
            this.pos = pos;
            // Fill placemark with position's details
            addTime();
            addGeometry(); 
            addDisplayData();
        }
        
        public Placemark getPlacemark() { return pm; }
        
        private void addTime() {
            // Add timestamp. TODO: choose between Gps and Device timestamp according to path type
            Date date = new Date();
            date.setTime(pos.getDeviceTimestamp());
            TimeStamp ts = new TimeStamp();
            ts.setWhen(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(date));
            pm.setTimePrimitive(ts);
        }
        
        private void addGeometry() {
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
            LineString heading = addHeading();
            if (heading != null) {
                mg.getGeometryList().add(heading);
            }
        }
        
        // Draw line from current to predicted position
        private LineString addHeading() {
            Position predictedPos = Position.predictPosition(pos, 300); // milliseconds
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

        private void addDisplayData() {
            List<Data> ld = new ArrayList<Data>();
            
            Data d = new Data();

            d.setDisplayName("DeviceTs");
            d.setValue(formatTimeStamp(pos.getDeviceTimestamp()));
            ld.add(d);
            
            d = new Data();
            d.setDisplayName("Speed");
            d.setValue(Float.toString(pos.getSpeed()));
            ld.add(d);
            
            if (pos.getBearing() != null) {
            	d = new Data();
                d.setDisplayName("Bearing");
                d.setValue(Float.toString(pos.getBearing()));
                ld.add(d);	
            }
             
            ExtendedData ed = new ExtendedData();
            ed.setDataList(ld);
            pm.setExtendedData(ed);
            
        }
        
        private String formatTimeStamp(long ts) {
        	return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(ts);
        }
        
        private Coordinate positionToCoordinate(Position pos) {
            return new Coordinate(pos.getLngx(), pos.getLatx(), pos.getAltitude());
        }
    
        private String positionToString(Position pos) {
            return positionToCoordinate(pos).toString();
        }

        

    }
    
}