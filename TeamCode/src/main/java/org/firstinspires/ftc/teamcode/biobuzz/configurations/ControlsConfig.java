package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * ControlsConfig.java  -  numbers for how the gamepads feel.                  ROLE: config
 *
 * Tunable numbers ONLY. You can change them live in FTC Dashboard
 * (http://192.168.43.1:8080/dash, "ControlsConfig"), but live edits are LOST when
 * the app restarts. When you find a good value, type it back into this file.
 *
 * WHICH BUTTON DOES WHAT lives in teleop/BioBuzzTeleOp.java (handleDriver() and
 * handleOperator()). This list is a copy for reading; keep it the same by hand:
 *
 *   GAMEPAD 1 (driver)   left stick = move (field-centric)     right stick X = turn
 *                        Y = reset heading (facing away)       X = field-centric / robot-centric
 *                        A = drive to our launch spot (S2)     B = drive to our LOADING ZONE (S2)
 *                        D-pad left (hold 1 s) = set pose here (robot in our reset corner)
 *                        Back/Share = mark a moment in the match log
 *                        D-pad left/right (INIT only) = switch alliance RED / BLUE
 *                        TODO(BioBuzz): right bumper (hold) = aim at the upward CELL
 *   GAMEPAD 2 (operator) D-pad down = STOP ALL mechanisms
 *                        Back/Share = mark a moment in the match log
 *                        TODO(BioBuzz): A = intake on/off, B (hold) = reverse intake,
 *                                       right trigger = launch 4, left bumper = HIVE tipped
 */
@Config
public class ControlsConfig {
    public static double TRIGGER_THRESHOLD = 0.5;   // 0.0-1.0: how far a trigger is pulled to count as "pressed"
    public static double SET_POSE_HOLD_MS = 1000;   // hold D-pad left this long to "set pose here"

    // RUMBLE EVENTS: one named duration per event, so the drive team learns what each buzz means.
    public static int RUMBLE_DRIVE_MODE_MS = 300;   // gamepad 1: field-centric / robot-centric switched
    public static int RUMBLE_REFUSED_MS = 150;      // gamepad 1: an assist button was refused (see telemetry)
    public static int RUMBLE_POSE_SET_MS = 600;     // gamepad 1: "set pose here" done, assists can be used
    public static int RUMBLE_ALERT_MS = 1000;       // both gamepads: an urgent ALERT (read the screen)
    public static int RUMBLE_ONE_MINUTE_MS = 400;   // both gamepads: 1:00 left
    public static int RUMBLE_PARK_MS = 800;         // gamepad 1: time to PARK

    // MATCH-TIME WARNINGS (seconds left in TeleOp)
    public static double ONE_MINUTE_WARNING_S = 60;   // NECTAR may go into the FLOWERS from here
    public static double PARK_WARNING_S = 20;         // leave enough time to drive to the LOADING ZONE
}
