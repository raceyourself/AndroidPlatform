package com.raceyourself.platform.gpstracker;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

import com.raceyourself.platform.models.Position;

//! The class parses CSV-based GPS log file. Support several formats based
// on the header of CSV
public class GpsCsvReader {
	private CSVReader csvReader = null;
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
    private String[] prevLine = null;
    
    
    public GpsCsvReader(CSVReader csvReader) throws IOException {
    	this.csvReader = csvReader;
    	// Read and parse CSV header
    	String[] header = csvReader.readNext();
        trimLine(header);
        parseCsvHeader(header);
    }
  
    public Position readNextPosition() {
    	Position pos = new Position();
        try {
			String[] line = csvReader.readNext();
            // Fill position with parsed line
			if (parsePositionLineRaceYourself(line, pos)) {
			    return pos;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return null;
    }
    
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
}
