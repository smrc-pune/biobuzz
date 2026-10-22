package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LogConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * MENTORS: you do not need to change this file.
 *
 * MatchLogger.java  -  records every run to a CSV file, for the debrief.       ROLE: helper
 *
 * FILE: /sdcard/FIRST/data/biobuzz-logs/<date>_<time>_<OpMode>_<ALLIANCE>.csv
 *   # lines        about the run (OpMode, alliance, field, battery at PLAY, start time)
 *   type,t_s,event,<columns>     the column names
 *   DATA,1.25,,...               numbers, LogConfig.RATE_HZ times a second
 *   EVENT,1.31,ASSIST:launch spot  something happened
 *   # END rows=...               written by a clean STOP. NO "# END" = the OpMode crashed
 *                                or the robot lost power.
 *
 * HOW TO LOG (any file):
 *   MatchLogger.event("ASSIST", "launch spot");              // an EVENT row, at once
 *   MatchLogger.value("intake_power", power);                // one column of the next DATA row
 * Robot.update() builds each DATA row: it asks isRowDue(), then every part adds its
 * values, then endRow(). The SAME values in the SAME order every row, so columns line up.
 *
 * "static": one logger for the whole app. Robot.startMatch() opens a file at PLAY and
 * Robot.endMatch() closes it at STOP. When logging is off, every call returns at once.
 *
 * TRACING A BUG: no file? Check LogConfig.ENABLED, and look for a "LOG OFF" ALERT.
 */
public class MatchLogger {
    private static final File FOLDER = new File(AppUtil.FIRST_FOLDER, "data/biobuzz-logs");

    private static BufferedWriter file = null;          // null = not logging
    private static final ElapsedTime sinceStart = new ElapsedTime();
    private static final ElapsedTime sinceRow = new ElapsedTime();
    private static final ElapsedTime sinceFlush = new ElapsedTime();
    private static final StringBuilder row = new StringBuilder();
    private static final StringBuilder columnNames = new StringBuilder();
    private static final StringBuilder eventsBeforeColumns = new StringBuilder();
    private static boolean columnsWritten = false;
    private static int rowCount = 0;

    /** PLAY: open a new file (if LogConfig.ENABLED) and write the # lines. */
    public static void start(String opModeName, double batteryVolts) { start(opModeName, batteryVolts, ""); }

    /** The same, with one more # line, for example "routine=LAUNCH_PARK start=WALL_MIDDLE delay_s=0". */
    public static void start(String opModeName, double batteryVolts, String extraHeader) {
        close();
        if (!LogConfig.ENABLED) return;
        row.setLength(0);
        columnNames.setLength(0);
        eventsBeforeColumns.setLength(0);
        columnsWritten = false;
        rowCount = 0;
        try {
            if (!FOLDER.isDirectory() && !FOLDER.mkdirs()) throw new IOException("cannot make " + FOLDER);
            deleteOldFiles();
            file = new BufferedWriter(new FileWriter(newFile(opModeName)));
            writeLine("# opmode=" + opModeName);
            writeLine("# alliance=" + MatchInfo.alliance);
            writeLine("# field=" + MatchInfo.fieldName);
            writeLine("# battery_at_play_v=" + round(batteryVolts));
            writeLine("# started_epoch_ms=" + System.currentTimeMillis());
            if (!extraHeader.isEmpty()) writeLine("# " + clean(extraHeader));
        } catch (IOException e) {
            turnOff(e);
        }
        sinceStart.reset();
        sinceRow.reset();
        sinceFlush.reset();
        event("PLAY", opModeName);
    }

    /** @return true when it is time for the next DATA row. */
    public static boolean isRowDue() {
        return file != null && sinceRow.seconds() >= 1.0 / LogConfig.RATE_HZ;
    }

    /** One column of the DATA row being built. */
    public static void value(String column, double number) {
        if (file == null) return;
        if (row.length() == 0) row.append("DATA,").append(round(sinceStart.seconds())).append(',');
        if (!columnsWritten) columnNames.append(',').append(column);
        row.append(',').append(round(number));
    }

    /** One column that is true or false: written as 1 or 0. */
    public static void value(String column, boolean flag) {
        value(column, flag ? 1 : 0);
    }

    /** The DATA row is complete: write it. */
    public static void endRow() {
        if (file == null || row.length() == 0) return;
        sinceRow.reset();
        writeColumnNamesOnce();
        writeLine(row.toString());
        row.setLength(0);
        rowCount++;
    }

    /** An EVENT row, for example event("ASSIST", "launch spot"). */
    public static void event(String type, String text) {
        if (file == null) return;
        String line = "EVENT," + round(sinceStart.seconds()) + "," + clean(type + ":" + text);
        if (columnsWritten) writeLine(line);
        else eventsBeforeColumns.append(line).append('\n');   // kept until the column names are written
    }

    /** STOP: write the END line and close the file. endText: a summary, for example "min_battery_v=11.8". */
    public static void stop(String endText) {
        if (file == null) return;
        event("STOP", "");
        writeColumnNamesOnce();
        writeLine("# END rows=" + rowCount + " " + endText);
        close();
    }

    /** The folder the log files go in (LogSummary reads them from here). */
    public static File getFolder() { return FOLDER; }

    /** @return free storage on the Control Hub, in MB (Robot Check wants more than 50). */
    public static double getFreeStorageMb() {
        File place = FOLDER;
        if (!place.isDirectory()) place = AppUtil.FIRST_FOLDER;
        return place.getUsableSpace() / (1024.0 * 1024.0);
    }

    // ==================== inside the logger ====================

    private static void writeColumnNamesOnce() {
        if (columnsWritten) return;
        columnsWritten = true;
        writeLine("type,t_s,event" + columnNames);
        if (eventsBeforeColumns.length() > 0) writeText(eventsBeforeColumns.toString());
        eventsBeforeColumns.setLength(0);
    }

    private static void writeLine(String line) {
        writeText(line + "\n");
    }

    private static void writeText(String text) {
        if (file == null) return;
        try {
            file.write(text);
            if (sinceFlush.seconds() >= LogConfig.FLUSH_S) {
                file.flush();
                sinceFlush.reset();
            }
        } catch (IOException e) {
            turnOff(e);
        }
    }

    /** A file error never stops the robot: logging turns off with one ALERT. */
    private static void turnOff(IOException e) {
        close();
        Alerts.raise("LOG OFF: " + e.getMessage(), false);
    }

    private static void close() {
        if (file == null) return;
        try {
            file.close();   // close() also saves what is still waiting in memory
        } catch (IOException e) {
            // nothing more we can do; the file keeps what was saved before
        }
        file = null;
    }

    /** <yyyy-MM-dd_HH-mm-ss>_<OpMode>_<ALLIANCE>.csv, with _2, _3 if that name is taken. */
    private static File newFile(String opModeName) {
        String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        String name = time + "_" + opModeName + "_" + MatchInfo.alliance;
        File newFile = new File(FOLDER, name + ".csv");
        int number = 2;
        while (newFile.exists()) {
            newFile = new File(FOLDER, name + "_" + number + ".csv");
            number++;
        }
        return newFile;
    }

    /** Keep the newest LogConfig.KEEP_FILES - 1 files, so there is room for the new one. */
    private static void deleteOldFiles() {
        File[] files = FOLDER.listFiles();
        if (files == null) return;
        Arrays.sort(files);   // names start with the time, so oldest first
        int toDelete = files.length - (LogConfig.KEEP_FILES - 1);
        for (int i = 0; i < toDelete; i++) {
            files[i].delete();
        }
    }

    /** Keep 3 decimals: smaller files, still exact enough. */
    private static double round(double number) {
        return Math.round(number * 1000) / 1000.0;
    }

    /** A comma or line break inside the text would break the CSV columns. */
    private static String clean(String text) {
        return text.replace(',', ';').replace('\n', ' ').replace('"', '\'');
    }
}
