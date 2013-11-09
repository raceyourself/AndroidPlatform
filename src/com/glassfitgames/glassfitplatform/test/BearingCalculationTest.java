package com.glassfitgames.glassfitplatform.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import au.com.bytecode.opencsv.CSVReader; 


import java.io.FileReader;
import java.util.List;
import java.util.ArrayDeque;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.math.MathContext;


import com.glassfitgames.glassfitplatform.gpstracker.BearingCalculationAlgorithm;
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
    public void basicRun() throws java.io.FileNotFoundException, java.io.IOException, java.text.ParseException {
        String testPath = "/home/raginsky/gfg/";
        CSVReader reader = new CSVReader(new FileReader(testPath + /*"track.csv"*/ "BL_tracklogs.csv"));
        List<String[]> posList = reader.readAll();
        // TODO: parse CSV title, in the meantime just remove it
        posList.remove(0);
        Position prevPos = null;
        
        ArrayDeque<Position> positions = new ArrayDeque<Position>();
        BearingCalculationAlgorithm bearingAlg = new BearingCalculationAlgorithm(); 
        
        for (String[] line : posList) {
            trimLine(line);
            Position p = new Position();
            // Fill position with parsed line
            if (! /*parsePositionLineMapMyTrack*/parsePositionLineRaceYourself(line, p))
                continue;
            // If no bearing in CSV thus update previous bearing to point directly to the next position
            if (p.getBearing() == null) {
                if (prevPos != null) {
                    prevPos.setBearing(BearingCalculationAlgorithm.calcBearing(prevPos, p));
                } else {
                    p.setBearing((float)0.0);
                }
            }
             // Store latest position
            positions.push(p);
            // Run bearing calc algorithm
            Position nextPos = null;
            if (prevPos != null)
                nextPos = bearingAlg.interpolatePositionsSpline(prevPos);
            if (nextPos != null) {
                System.out.printf("GPS: %.15f,%.15f str: %s %s \n" , p.getLngx(), p.getLatx(), line[8], line[10]);
                System.out.printf("PREDICTED: %.15f,,%.15f\n", nextPos.getLngx(), nextPos.getLatx());
            }
            prevPos = p;

        }
        System.out.println("Finished parsing");     
    }
    

    @Test
    @Ignore
    public void thisIsIgnored() {
    }
}