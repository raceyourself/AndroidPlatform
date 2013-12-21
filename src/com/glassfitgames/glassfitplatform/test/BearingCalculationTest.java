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
		TRACK_ID
	};
	// Maps predefined fields into actual file indices
    private Map<CsvField, Integer> csvFieldMap = new HashMap<CsvField, Integer>();
  
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

    private boolean parsePositionLineRaceYourself(String[] aLine, Position aPos) throws java.text.ParseException{
        
        if (aLine[csvFieldMap.get(CsvField.LATX)].equals("") || aLine[csvFieldMap.get(CsvField.LNGX)].equals("")) {
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
        aPos.setGpsTimestamp(Long.parseLong(aLine[csvFieldMap.get(CsvField.GPS_TS)])); 
        aPos.setDeviceTimestamp(Long.parseLong(aLine[csvFieldMap.get(CsvField.DEVICE_TS)])); 
        
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
        CSVReader reader = new CSVReader(new FileReader(aInputFile));
        List<String[]> posList = reader.readAll();
        
        GFKml kml = new GFKml();

        // TODO: parse CSV title, in the meantime just remove it
        String[] header = posList.remove(0);
        trimLine(header);
        parseCsvHeader(header);
        
        PositionPredictor posPredictor = new PositionPredictor(); 
        int i = 0;
        
        File outCsv = new File("track_cut.csv");
        CSVWriter csvWriter = new CSVWriter(new FileWriter(outCsv));
        csvWriter.writeNext(header);
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
                     if (predictedPos != null) {
                         //System.out.printf("PREDICTED: %.15f,,%.15f, bearing: %f\n", predictedPos.getLngx(), predictedPos.getLatx(), predictedPos.getBearing());
                         //kml.addPosition(GFKml.PathType.PREDICTION, predictedPos);    
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

    }
    
    @Test
    public void bicycleTest() {
   		//
    	try {
			basicTest("BL_track.csv", 0, 900000, "bicycle.kml");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    @Test
    public void walkingTest() {
    	try {
			basicTest("AM_track.csv", 0, 9000000, "walking.kml");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
        posArray[0].setLngx(10.1);
        posArray[0].setLatx(10.1);
        
        posArray[1].setLngx(10.5);
        posArray[1].setLatx(10.5);
        
        posArray[2].setLngx(11.0);
        posArray[2].setLatx(10.7);

        ArrayDeque<Position> posResult = CardinalSpline.create(posArray);
        
        GFKml kml = new GFKml();
        for (Position p: posArray) {
            kml.addPosition(GFKml.PathType.GPS, p);
        }

        for (Position p: posResult) {
            kml.addPosition(GFKml.PathType.PREDICTION, p);
        }
        String fileName = "spline.kml";
        System.out.println("Dumping KML: " + fileName); 
        FileOutputStream out = new FileOutputStream(fileName);
        kml.write(out);
        
    }

    

    @Test
    @Ignore
    public void thisIsIgnored() {
    }
}