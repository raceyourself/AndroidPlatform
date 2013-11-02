package com.glassfitgames.glassfitplatform.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import au.com.bytecode.opencsv.CSVReader; 
import com.javadocmd.simplelatlng.*;

import java.io.FileReader;
import java.util.List;
import java.util.ArrayDeque;


import com.glassfitgames.glassfitplatform.gpstracker.BearingCalculationAlgorithm;
import com.glassfitgames.glassfitplatform.models.Position;


/**
 * Tests for {@link Foo}.
 *
 * @author user@example.com (John Doe)
 */
@RunWith(JUnit4.class)
public class BearingCalculationTest {

    @Test
    public void basicRun() throws java.io.FileNotFoundException, java.io.IOException {
        String testPath = "/home/raginsky/gfg/";
        CSVReader reader = new CSVReader(new FileReader(testPath + "track.csv"));
        List<String[]> posList = reader.readAll();
        // TODO: parse CSV title, in the meantime just remove it
        posList.remove(0);
        Position prevPos = null;
        
        ArrayDeque<Position> positions = new ArrayDeque<Position>();
        BearingCalculationAlgorithm bearingAlg = new BearingCalculationAlgorithm(positions); 
        
        for (String[] line : posList) {
            Position p = new Position();
            // Parse line with lon/lat and speed
            p.setLngx(Float.parseFloat(line[2]));
            p.setLatx(Float.parseFloat(line[3]));
            p.setSpeed(Float.parseFloat(line[7])/(float)3.6); // convert to m/s
            // No bearing in CSV thus update previous bearing to point directly to the next position
            if (prevPos != null) {
                prevPos.setBearing(calcBearing(prevPos, p));
            } else {
                p.setBearing((float)0.0);
            }
            prevPos = p;
            // Store latest position
            positions.push(p);
            // Run bearing calc algorithm
            bearingAlg.calculateCurrentBearingSpline();
            
        }
        System.out.println("Finished parsing");     
    }
    
    private float calcBearing(Position from, Position to) {
        LatLng fromL = new LatLng(from.getLatx(), from.getLngx());
        LatLng toL = new LatLng(to.getLatx(), to.getLngx());
        return (float)LatLngTool.initialBearing(fromL, toL);
    }

    @Test
    @Ignore
    public void thisIsIgnored() {
    }
}