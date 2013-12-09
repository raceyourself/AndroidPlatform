package com.glassfitgames.glassfitplatform.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import au.com.bytecode.opencsv.CSVReader; 

import com.glassfitgames.glassfitplatform.gpstracker.kml.*;

import java.io.FileReader;
import java.io.FileOutputStream;

import java.util.List;
import java.util.ArrayDeque;

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

    // TODO: extract to a proper class with interface
    private boolean parsePositionLineMapMyTrack(String[] aLine, Position aPos) {
        // Parse line with lon/lat and speed
        aPos.setLngx(Float.parseFloat(aLine[2]));
        aPos.setLatx(Float.parseFloat(aLine[3]));
        aPos.setSpeed(Float.parseFloat(aLine[7])/(float)3.6); // convert to m/s  
        return true;
    }

    private boolean parsePositionLineRaceYourself(String[] aLine, Position aPos) throws java.text.ParseException{
        // For now, only track 75 is interesting
        if (Integer.parseInt(aLine[7]) != 75) {
            return false;
        }
        
        if (aLine[8].equals("") || aLine[10].equals("")) {
            return false;
        }
        // Parse line with lon/lat and speed  
        aPos.setLngx(new BigDecimal(aLine[8], MathContext.DECIMAL64).doubleValue());
        aPos.setLatx(new BigDecimal(aLine[10], MathContext.DECIMAL64).doubleValue());

        aPos.setSpeed(Float.parseFloat(aLine[12])); 
        if (!aLine[1].equals("")) {
            aPos.setBearing(Float.parseFloat(aLine[1])); 
        }
        aPos.setGpsTimestamp(Long.parseLong(aLine[14])); 
        aPos.setDeviceTimestamp(Long.parseLong(aLine[15])); 
        
        return true;
        
    }

    private void trimLine(String[] aLine) {
        for (String field : aLine) {
            field = field.trim();
        }
    }
    
    
    @Test
    //@Ignore
    public void basicRun() throws java.io.FileNotFoundException,  java.lang.Exception,
                                  java.io.IOException, java.text.ParseException {
        String testPath = "";
        CSVReader reader = new CSVReader(new FileReader(testPath + /*"track.csv"*/ "BL_tracklogs.csv"));
        List<String[]> posList = reader.readAll();
        
        GFKml kml = new GFKml();

        // TODO: parse CSV title, in the meantime just remove it
        posList.remove(0);

        
        PositionPredictor posPredictor = new PositionPredictor(); 
        int i = 0;
        for (String[] line : posList) {
            trimLine(line);
            Position p = new Position();
            // Fill position with parsed line
            if (! /*parsePositionLineMapMyTrack*/parsePositionLineRaceYourself(line, p))
                continue;


            // Plot only part of the track
            if (i > 3150 && i < 3450) {
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
                         kml.addPosition(GFKml.PathType.PREDICTION, predictedPos);    
                     }
                }
            }
            ++i;
        }
        System.out.println("Finished parsing");
        String fileName = "test.kml";
        System.out.println("Dumping KML: " + fileName); 
        FileOutputStream out = new FileOutputStream(fileName);
        kml.write(out);

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