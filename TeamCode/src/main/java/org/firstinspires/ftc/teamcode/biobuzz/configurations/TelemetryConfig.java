package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
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
    // ---- One switch per subsystem ----
    public static boolean DRIVE = true;                // Drive state, drive mode and pose
    // TODO(BioBuzz): one switch per mechanism subsystem.

    // ---- Extra detail ----
    public static boolean HISTORY = true;              // each subsystem's last 5 state changes and WHY
    public static boolean PARTS = false;               // low-level numbers: motor powers, follower mode
    public static boolean GAMEPAD_HELP = true;         // the controls cheat sheet (the two lines below)

    // The controls cheat sheet. Keep it the same as BioBuzzTeleOp's header and docs/CONTROLS.md.
    public static String GAMEPAD1_HELP = "GP1: sticks=drive  Y=face-away reset  X=field/robot  Back=mark";
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
