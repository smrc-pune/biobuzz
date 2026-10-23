package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * MENTORS: you do not need to change this file.
 *
 * LogSummary.java  -  reads ONE match-log file and sums it up, for Log Viewer.   ROLE: helper
 *
 *   LogSummary[] runs = LogSummary.newest(10);      // the 10 newest files, newest first
 *   runs[0].getShortText()                          // "14:32 BioBuzzTeleOp RED 121 s, 2 alerts, END ok"
 *
 * It reads what helpers/MatchLogger wrote: the "#" header lines, the column names, the
 * DATA rows (duration, worst loop time, lowest battery) and the EVENT rows (ALERTs,
 * TIMEOUTs), and whether the file ends with "# END" (a clean stop).
 * Reading files can fail, so the try/catch lives here, in helpers/.
 */
public class LogSummary {

    private final String fileName;
    private String opModeName = "?";
    private String alliance = "?";
    private double batteryAtPlayV = 0;
    private double durationS = 0;
    private double worstLoopMs = 0;
    private double lowestBatteryV = 0;
    private int alertCount = 0;
    private int timeoutCount = 0;
    private int rowCount = 0;
    private boolean endedCleanly = false;
    private String lastAlert = "";
    private String readError = "";

    /** @return the newest "count" log files, newest first (an empty list if there are none). */
    public static LogSummary[] newest(int count) {
        File[] files = MatchLogger.getFolder().listFiles();
        if (files == null) {
            return new LogSummary[0];
        }
        Arrays.sort(files);   // names start with the date and time, so oldest first
        int howMany = Math.min(count, files.length);
        LogSummary[] summaries = new LogSummary[howMany];
        for (int i = 0; i < howMany; i++) {
            summaries[i] = new LogSummary(files[files.length - 1 - i]);
        }
        return summaries;
    }

    public LogSummary(File file) {
        fileName = file.getName();
        int worstLoopColumn = -1;
        int batteryColumn = -1;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("# END")) {
                    endedCleanly = true;
                } else if (line.startsWith("# opmode=")) {
                    opModeName = line.substring("# opmode=".length());
                } else if (line.startsWith("# alliance=")) {
                    alliance = line.substring("# alliance=".length());
                } else if (line.startsWith("# battery_at_play_v=")) {
                    batteryAtPlayV = number(line.substring("# battery_at_play_v=".length()));
                } else if (line.startsWith("type,")) {
                    String[] names = line.split(",", -1);
                    worstLoopColumn = Arrays.asList(names).indexOf("worst_loop_ms");
                    batteryColumn = Arrays.asList(names).indexOf("battery_v");
                } else if (line.startsWith("DATA,")) {
                    String[] cells = line.split(",", -1);
                    rowCount++;
                    durationS = Math.max(durationS, number(cells[1]));
                    if (worstLoopColumn > 0 && worstLoopColumn < cells.length) {
                        worstLoopMs = Math.max(worstLoopMs, number(cells[worstLoopColumn]));
                    }
                    if (batteryColumn > 0 && batteryColumn < cells.length) {
                        double volts = number(cells[batteryColumn]);
                        if (volts > 0 && (lowestBatteryV == 0 || volts < lowestBatteryV)) {
                            lowestBatteryV = volts;
                        }
                    }
                } else if (line.startsWith("EVENT,")) {
                    String[] cells = line.split(",", 3);
                    if (cells.length == 3) {
                        durationS = Math.max(durationS, number(cells[1]));
                        if (cells[2].startsWith("ALERT:")) {
                            alertCount++;
                            lastAlert = cells[2].substring("ALERT:".length());
                        }
                        if (cells[2].startsWith("TIMEOUT")) {
                            timeoutCount++;
                        }
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            readError = e.getMessage();
        }
    }

    private static double number(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== answers ====================

    public String getFileName() {
        return fileName;
    }
    public String getOpModeName() {
        return opModeName;
    }
    public String getAlliance() {
        return alliance;
    }
    public double getDurationS() {
        return durationS;
    }
    public double getWorstLoopMs() {
        return worstLoopMs;
    }
    public double getLowestBatteryV() {
        return lowestBatteryV;
    }
    public double getBatteryAtPlayV() {
        return batteryAtPlayV;
    }
    public int getAlertCount() {
        return alertCount;
    }
    public int getTimeoutCount() {
        return timeoutCount;
    }
    public int getRowCount() {
        return rowCount;
    }
    public boolean hasEndLine() {
        return endedCleanly;
    }
    public String getLastAlert() {
        return lastAlert;
    }

    /** "2026-10-18 14:32" from a name like 2026-10-18_14-32-05_BioBuzzTeleOp_RED.csv */
    public String getStartText() {
        if (fileName.length() < 16) {
            return fileName;
        }
        return fileName.substring(0, 10) + " " + fileName.substring(11, 16).replace('-', ':');
    }

    /** One line for the list: "14:32 BioBuzzTeleOp RED 121 s, 2 alerts, END ok". */
    public String getShortText() {
        if (!readError.isEmpty()) {
            return fileName + ": cannot read (" + readError + ")";
        }
        String end = "END ok";
        if (!endedCleanly) {
            end = "NO END (crash or power loss)";
        }
        return String.format("%s %s %s %.0f s, %d alerts, %s", getStartText().substring(Math.min(11,
                getStartText().length())), opModeName, alliance, durationS, alertCount, end);
    }
}
