package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
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
 *                        Y = reset heading (facing away)
 *                        X = switch field-centric / robot-centric
 *                        Back/Share = mark a moment in the match log
 *                        D-pad (INIT only) = INIT menu
 *                        TODO(BioBuzz) step 4: right stick button = snap to 90,
 *                                              hold left stick button 1 s = recalibrate gyro
 *   GAMEPAD 2 (operator) D-pad down = STOP ALL mechanisms
 *                        Back/Share = mark a moment in the match log
 *                        TODO(BioBuzz): mechanism buttons, once the mechanisms are designed
 */
@Config
public class ControlsConfig {
    public static double TRIGGER_THRESHOLD = 0.5;   // 0.0-1.0: how far a trigger is pulled to count as "pressed"

    // RUMBLE EVENTS: one named duration per event, so the drive team learns what each buzz means.
    public static int RUMBLE_DRIVE_MODE_MS = 300;   // gamepad 1: field-centric / robot-centric switched
}
