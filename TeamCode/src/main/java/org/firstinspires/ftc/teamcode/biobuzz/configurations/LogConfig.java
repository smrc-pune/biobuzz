package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * MENTORS: you do not need to change this file.
 *
 * LogConfig.java  -  switches for the match log (helpers/MatchLogger.java).     ROLE: config
 *
 * The log files go to /sdcard/FIRST/data/biobuzz-logs/ on the Control Hub.
 * ENABLED is read once, when PLAY is pressed, so a file is never half-written.
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class LogConfig {
    public static boolean ENABLED = true;   // true at competition: every match is recorded
    public static double RATE_HZ = 20;      // DATA rows per second
    public static double FLUSH_S = 1;       // save to storage at least this often (and at STOP)
    public static int KEEP_FILES = 100;     // only the newest files are kept; older ones are deleted at PLAY
}
