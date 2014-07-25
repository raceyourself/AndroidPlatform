package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.roscopeco.ormdroid.Entity;

import org.acra.collector.CrashReportData;
import org.acra.util.JSONReportBuilder;

public class CrashReport extends Entity {
    @JsonIgnore
    public int id;

    public String json;

    public CrashReport() {}

    public CrashReport(CrashReportData data) {
        try {
            this.json = data.toJSON().toString();
        } catch (JSONReportBuilder.JSONReportException e) {
            this.json = String.format("{\"super_duper_error\" : \"%s\"}", data.toString());
        }
    }

    @JsonValue
    @JsonRawValue
    public String toJson() {
        return json;
    }
}
