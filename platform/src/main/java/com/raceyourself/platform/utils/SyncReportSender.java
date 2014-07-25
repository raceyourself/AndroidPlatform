package com.raceyourself.platform.utils;

import com.raceyourself.platform.models.CrashReport;

import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SyncReportSender implements ReportSender {
    @Override
    public void send(CrashReportData errorContent) throws ReportSenderException {
        CrashReport report = new CrashReport(errorContent);
        report.save();
        log.error("Stored crash report in db");
    }
}
