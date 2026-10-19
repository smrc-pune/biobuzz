package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * TelemetryConfig.java  -  what shows on the Driver Station and Dashboard.     ROLE: config
 *
 * Too much text is hard to read during a match and slows the loop. Turn off what you
 * are not working on. The match header line (alliance, match, drive mode) is always shown.
 *
 * You can change these live in FTC Dashboard (http://192.168.43.1:8080/dash,
 * "TelemetryConfig"), but live edits are LOST when the app restarts. When you find a
 * good value, type it back into this file.
 */
@Config
public class TelemetryConfig {
    // ---- One switch per part ----
    public static boolean DRIVE = true;                // drive mode, who is driving, pose, warnings

    // ---- Extra detail ----
    public static boolean HISTORY = true;              // the last change (and WHY) of each part and state machine
    public static boolean DETAILS = false;             // low-level numbers: motor powers, Pedro's mode
    public static boolean GAMEPAD_HELP = true;         // the controls cheat sheet (the two lines below)

    // The controls cheat sheet. Keep it the same as BioBuzzTeleOp's header and docs/CONTROLS.md.
    public static String GAMEPAD1_HELP = "GP1: sticks=drive  Y=face-away  X=field/robot  A=launch spot  B=loading zone  Dpad-left(hold)=set pose";
    public static String GAMEPAD2_HELP = "GP2: Dpad-down=STOP ALL  Back=mark  (mechanisms: TODO)";

    // ---- Timing ----
    public static double WARNING_SHOW_S = 4;           // seconds a log.warn(...) message stays on screen
    public static int DS_TRANSMIT_INTERVAL_MS = 100;   // send to the Driver Station at most this often

    // ---- Dashboard field view ----
    public static boolean FIELD_DRAWING = true;        // draw the robot and its path on the Dashboard field
    public static boolean FIELD_IMAGE = false;         // TODO(BioBuzz): true once the BioBuzz field image exists
    // The Dashboard serves pictures from TeamCode/src/main/assets/images/ at the web path /images/...
    public static String FIELD_IMAGE_PATH = "/images/field-biobuzz.png";
}
