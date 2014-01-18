package com.glassfitgames.glassfitplatform.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import au.com.bytecode.opencsv.CSVReader; 
import au.com.bytecode.opencsv.CSVWriter; 


import com.glassfitgames.glassfitplatform.gpstracker.kml.*;
//import com.glassfitgames.glassfitplatform.gpstracker.kml.GFKml.Path;
import com.glassfitgames.glassfitplatform.gpstracker.kml.GFKml.PathType;

import com.glassfitgames.glassfitplatform.gpstracker.catmullrom.*;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Map;

import java.math.BigDecimal;
import java.math.MathContext;

import junit.framework.Assert;


import com.glassfitgames.glassfitplatform.gpstracker.ConstrainedCubicSpline;
import com.glassfitgames.glassfitplatform.gpstracker.PositionPredictor;
import com.glassfitgames.glassfitplatform.gpstracker.CardinalSpline;
import com.glassfitgames.glassfitplatform.models.Position;


/**
 * Tests for {@link Foo}.
 *
 * @author user@example.com (John Doe)
 */
@RunWith(JUnit4.class)
public class BearingCalculationTest {
	
	// TODO: move to CSV reader class when it is moved out to a separate file
	private enum CsvField {
		LATX,
		LNGX,
		SPEED,
		BEARING,
		GPS_TS,
		DEVICE_TS,
		EPE,
		TRACK_ID
	};
	// Maps predefined fields into actual file indices
    private Map<CsvField, Integer> csvFieldMap = new HashMap<CsvField, Integer>();
    
    // Previously read line
    String[] prevLine = null;
  
	private void parseCsvHeader(String[] aLine) {
		int i = 0;
		CsvField csvField;
		System.out.println("HEADER: " + aLine.toString());
		for (String field : aLine) {
			System.out.println("FIELD: " + field);
			++i;
			if (field.equals("bearing"))
				csvField = CsvField.BEARING;
			else if (field.equals("latx"))
				csvField = CsvField.LATX;
			else if (field.equals("lngx"))
				csvField = CsvField.LNGX;
			else if (field.equals("speed"))
				csvField = CsvField.SPEED;
			else if (field.equals("gps_ts"))
				csvField = CsvField.GPS_TS;
			else if (field.equals("device_ts"))
				csvField = CsvField.DEVICE_TS;
			else if (field.equals("epe"))
				csvField = CsvField.EPE;
			else if (field.equals("id"))
				csvField = CsvField.TRACK_ID;
			else {
				// Skipping other fields for now
				continue;
			}
			System.out.println("FIELD: " + field + " --> " + i);

			csvFieldMap.put(csvField, i-1);
        }
 	
	}
	// TODO: extract to a proper class with interface
    private boolean parsePositionLineMapMyTrack(String[] aLine, Position aPos) {
        // Parse line with lon/lat and speed
        aPos.setLngx(Float.parseFloat(aLine[2]));
        aPos.setLatx(Float.parseFloat(aLine[3]));
        aPos.setSpeed(Float.parseFloat(aLine[7])/(float)3.6); // convert to m/s  
        return true;
    }

    private boolean isEqualPosition(String[] aLine1, String[] aLine2) {
    	return aLine1[csvFieldMap.get(CsvField.LATX)].equals(aLine2[csvFieldMap.get(CsvField.LATX)])
    			&& aLine1[csvFieldMap.get(CsvField.LNGX)].equals(aLine2[csvFieldMap.get(CsvField.LNGX)]);
    }
    
    private boolean parsePositionLineRaceYourself(String[] aLine, Position aPos) throws java.text.ParseException{
        
        if (aLine[csvFieldMap.get(CsvField.LATX)].equals("") || aLine[csvFieldMap.get(CsvField.LNGX)].equals("")) {
            return false;
        }
        // Skip the same positions (indoor CSV contains not only GPS points)
        if (prevLine != null && isEqualPosition(aLine, prevLine)) {
        	return false;
        }
        // Parse line with lon/lat and speed  
        aPos.setLngx(new BigDecimal(aLine[csvFieldMap.get(CsvField.LNGX)], MathContext.DECIMAL64).doubleValue());
        aPos.setLatx(new BigDecimal(aLine[csvFieldMap.get(CsvField.LATX)], MathContext.DECIMAL64).doubleValue());

        if (!aLine[csvFieldMap.get(CsvField.SPEED)].equals("null")) {
        	//System.out.println("SPEED: " + aLine[csvFieldMap.get(CsvField.SPEED)]);
        	aPos.setSpeed(Float.parseFloat(aLine[csvFieldMap.get(CsvField.SPEED)])); 
        }
        
        if (!aLine[csvFieldMap.get(CsvField.BEARING)].equals("")) {
            aPos.setBearing(Float.parseFloat(aLine[csvFieldMap.get(CsvField.BEARING)])); 
        }
        if (!aLine[csvFieldMap.get(CsvField.GPS_TS)].equals("")) {
        	aPos.setGpsTimestamp(Long.parseLong(aLine[csvFieldMap.get(CsvField.GPS_TS)]));
        }
        if (!aLine[csvFieldMap.get(CsvField.DEVICE_TS)].equals("")) {
        	aPos.setDeviceTimestamp(Long.parseLong(aLine[csvFieldMap.get(CsvField.DEVICE_TS)]));
        }
        if (!aLine[csvFieldMap.get(CsvField.EPE)].equals("")) {
        	aPos.setEpe(Float.parseFloat(aLine[csvFieldMap.get(CsvField.EPE)]));
        }
        prevLine = aLine;
        return true;
        
    }

    private void trimLine(String[] aLine) {
        for (String field : aLine) {
            field = field.trim();
        }
    }
    
    
    public void basicTest(String aInputFile, int aStartIndex, int aEndIndex, String aOutFile) 
    							throws 
    							java.io.FileNotFoundException,  java.lang.Exception,
                                  java.io.IOException, java.text.ParseException {
        CSVReader reader = new CSVReader(new FileReader(aInputFile + ".csv"));
        List<String[]> posList = reader.readAll();
        
        GFKml kml = new GFKml();

        // TODO: parse CSV title, in the meantime just remove it
        String[] header = posList.remove(0);
        trimLine(header);
        parseCsvHeader(header);
        
        PositionPredictor posPredictor = new PositionPredictor(); 
        int i = 0;
        
        File outCsv = new File(aInputFile + "_cut.csv");
        CSVWriter csvWriter = new CSVWriter(new FileWriter(outCsv));
        csvWriter.writeNext(header);

        File outPredictCsv = new File(aInputFile + "_predict.csv");
        CSVWriter csvPredictWriter = new CSVWriter(new FileWriter(outPredictCsv));
        String[] predictHeaderList = {"lngx", "latx", "bearing"};
        csvPredictWriter.writeNext(predictHeaderList);

        for (String[] line : posList) {
            trimLine(line);
            Position p = new Position();
            // Fill position with parsed line
            if (! /*parsePositionLineMapMyTrack*/parsePositionLineRaceYourself(line, p))
                continue;


            // Plot only part of the track
            //if (i > 3150 && i < 3450) {
            if (i >= aStartIndex && i <= aEndIndex) {
            	csvWriter.writeNext(line);
            	csvPredictWriter.writeNext(line);

                kml.addPosition(GFKml.PathType.GPS, p);
                // Run bearing calc algorithm
                Position nextPos = posPredictor.updatePosition(p);
                System.out.printf("GPS: %.15f,%.15f, ts: %d, bearing: %f\n" , p.getLngx(), p.getLatx(), p.getDeviceTimestamp(), p.getBearing());
                if (nextPos != null) {
                    System.out.printf("EXTRAP: %.15f,,%.15f, ts: %d, bearing: %f\n", nextPos.getLngx(), nextPos.getLatx(), nextPos.getDeviceTimestamp(), nextPos.getBearing());
                	kml.addPosition(GFKml.PathType.EXTRAPOLATED, nextPos);
                }

                for (long timeStampOffset = 0; timeStampOffset < 1000; timeStampOffset += 100) {
                     Position predictedPos = posPredictor.predictPosition(p.getDeviceTimestamp() + timeStampOffset);
                     Float bearing = posPredictor.predictBearing(p.getDeviceTimestamp() + timeStampOffset);
                     if (predictedPos != null) {
                    	 predictedPos.setBearing(bearing);
                         System.out.printf("PREDICTED: %.15f,,%.15f, bearing: %f\n", predictedPos.getLngx(), predictedPos.getLatx(), predictedPos.getBearing());
                         kml.addPosition(GFKml.PathType.PREDICTION, predictedPos);
                    	String[] predictLine = { Double.toString(predictedPos.getLngx()),
                    			Double.toString(predictedPos.getLatx()),
                    			Float.toString(predictedPos.getBearing() != null ? 
                    					predictedPos.getBearing() : -999) } ;
                     	csvPredictWriter.writeNext(predictLine);
                     }
                }
            }
            ++i;
        }
        reader.close();
        System.out.println("Finished parsing");
        System.out.println("Dumping KML: " + aOutFile); 
        FileOutputStream out = new FileOutputStream(aOutFile);
        kml.write(out);
        csvWriter.close();
        csvPredictWriter.close();

    }
    
    @Test
    public void bicycleTest() {
   		//
    	try {
			basicTest("BL_track", 3000, 3450, "bicycle.kml");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    @Test
    public void walkingTest() {
    	try {
			basicTest("AM_track", 0, 200, "walking.kml");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
        
    @Test
    public void indoorTest() {
    	try {
			basicTest("PositionData_2013-12-27", 0, 9000000, "indoor.kml");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    @Test
    public void wakingTest_Jan10_2014() {
    	try {
			basicTest("BL_tracks_112", 0, 9000000, "waking_Jan10_2014_112.kml");
			basicTest("BL_tracks_114", 0, 9000000, "waking_Jan10_2014_114.kml");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }


    private void runCardinalSpline(Position[] posArray, GFKml kml) {
        for (Position p: posArray) {
            kml.addPosition(GFKml.PathType.GPS, p);
        }
    	
    	ArrayDeque<Position> posResult = CardinalSpline.create(posArray);
        
        for (Position p: posResult) {
            kml.addPosition(GFKml.PathType.PREDICTION, p);
        }

    }
    
    @Test
    public void cardinalSpline() throws java.io.FileNotFoundException,  java.lang.Exception,
                                  java.io.IOException, java.text.ParseException {
        Position[] posArray = new Position[3];
        for (int i = 0; i < posArray.length; ++i) {
            posArray[i] = new Position();
            posArray[i].setSpeed(1000);
        }
        posArray[0].setLngx(3.29);
        posArray[0].setLatx(13.29);
        
        posArray[1].setLngx(4.83);
        posArray[1].setLatx(11.65);
        
        posArray[2].setLngx(5.32);
        posArray[2].setLatx(11.13);

        GFKml kml = new GFKml();
        runCardinalSpline(posArray, kml);
        posArray[0].setLngx(2.32);
        posArray[0].setLatx( -1.92);
        
        posArray[1].setLngx(2.52);
        posArray[1].setLatx(-2.13);
        
        posArray[2].setLngx(3.67);
        posArray[2].setLatx(-3.29);
        runCardinalSpline(posArray, kml);

        posArray[0].setLngx(0.262);
        posArray[0].setLatx(10.262);
        
        posArray[1].setLngx(2.11);
        posArray[1].setLatx(8.30);
        
        posArray[2].setLngx(2.21);
        posArray[2].setLatx(8.19);
        runCardinalSpline(posArray, kml);
        
        
        String fileName = "spline.kml";
        System.out.println("Dumping KML: " + fileName); 
        FileOutputStream out = new FileOutputStream(fileName);
        kml.write(out);
        
    }

    private void runCatmullRomSpline(Point2D[] posArray, GFKml kml) {
        for (Point2D p: posArray) {
            kml.addPosition(GFKml.PathType.GPS, p.toPosition());
        }
    	
    	Point2D[] posResult = CatmullRomSplineUtils.subdividePoints(posArray, 30);
        
        for (Point2D p: posResult) {
            kml.addPosition(GFKml.PathType.PREDICTION, p.toPosition());
        }

    }

    
    @Test
    public void catmullRomSpline() throws java.io.FileNotFoundException,  java.lang.Exception,
                                  java.io.IOException, java.text.ParseException {
        Point2D[] posArray = new Point2D[3];
        for (int i = 0; i < posArray.length; ++i) {
            posArray[i] = new Point2D();
        }
        posArray[0].setX(3.29f);
        posArray[0].setY(13.29f);
        
        posArray[1].setX(4.83f);
        posArray[1].setY(11.65f);
        
        posArray[2].setX(5.32f);
        posArray[2].setY(11.13f);

        GFKml kml = new GFKml();
        runCatmullRomSpline(posArray, kml);
        posArray[0].setX(2.32f);
        posArray[0].setY( -1.92f);
        
        posArray[1].setX(2.52f);
        posArray[1].setY(-2.13f);
        
        posArray[2].setX(3.67f);
        posArray[2].setY(-3.29f);
        runCatmullRomSpline(posArray, kml);

        posArray[0].setX(0.262f);
        posArray[0].setY(0.262f);
        
        posArray[1].setX(2.11f);
        posArray[1].setY(-1.70f);
        
        posArray[2].setX(2.21f);
        posArray[2].setY(-1.81f);
        runCatmullRomSpline(posArray, kml);
        
        
        String fileName = "catmullrom.kml";
        System.out.println("Dumping KML: " + fileName); 
        FileOutputStream out = new FileOutputStream(fileName);
        kml.write(out);
        
    }

    private void runConstrainedSpline(Position[] posArray, GFKml kml) {
        for (Position p: posArray) {
            kml.addPosition(GFKml.PathType.GPS, p);
        }
    	
    	Position[] posResult = ConstrainedCubicSpline.create(posArray);
        
        for (Position p: posResult) {
            kml.addPosition(GFKml.PathType.PREDICTION, p);
        }

    }

    
    @Test
    public void constrainedSpline() throws java.io.FileNotFoundException,  java.lang.Exception,
                                  java.io.IOException, java.text.ParseException {
        Position[] posArray = new Position[3];
        for (int i = 0; i < posArray.length; ++i) {
            posArray[i] = new Position();
            posArray[i].setSpeed(1000);
        }
        posArray[0].setLngx(0);
        posArray[0].setLatx(30);
        
        posArray[1].setLngx(10);
        posArray[1].setLatx(130);
        
        posArray[2].setLngx(30);
        posArray[2].setLatx(150);

        GFKml kml = new GFKml();
        runConstrainedSpline(posArray, kml);


        Assert.assertEquals(0.0, ConstrainedCubicSpline.fDer2Xi1(1));
        Assert.assertEquals(-2.45, Math.round(ConstrainedCubicSpline.fDer2Xi(1)* 100.0) / 100.0);

        Assert.assertEquals(30.0, ConstrainedCubicSpline.Ai(1));
        Assert.assertEquals(14.09, Math.round(ConstrainedCubicSpline.Bi(1)* 100.0) / 100.0);
        Assert.assertEquals(0.0, ConstrainedCubicSpline.Ci(1));
        Assert.assertEquals(-0.04,Math.round(ConstrainedCubicSpline.Di(1)* 100.0) / 100.0);
        
        posArray[0].setLngx(0.262);
        posArray[0].setLatx(10.262);
        
        posArray[1].setLngx(2.11);
        posArray[1].setLatx(8.30);
        
        posArray[2].setLngx(2.21);
        posArray[2].setLatx(8.19);
        runConstrainedSpline(posArray, kml);
        
        
        String fileName = "constr_spline.kml";
        System.out.println("Dumping KML: " + fileName); 
        FileOutputStream out = new FileOutputStream(fileName);
        kml.write(out);
        
    }

    

    @Test
    @Ignore
    public void thisIsIgnored() {
    }
}