package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * TelemetryConfig.java  -  what shows on the Driver Station and Dashboard.     ROLE: config
 *
 * Tunable numbers and switches ONLY. You can change them live in FTC Dashboard
 * (http://192.168.43.1:8080/dash, "TelemetryConfig"), but live edits are LOST when
 * the app restarts. When you find a good value, type it back into this file.
 *
 * TODO(BioBuzz): step 3 adds one switch per subsystem, PARTS, DRIVE and GAMEPAD_HELP.
 */
@Config
public class TelemetryConfig {
    public static boolean HISTORY = true;              // show each subsystem's last 5 state changes
    public static double WARNING_SHOW_S = 4;           // seconds a log.warn(...) message stays on screen
    public static int DS_TRANSMIT_INTERVAL_MS = 100;   // send to the Driver Station at most this often
}
