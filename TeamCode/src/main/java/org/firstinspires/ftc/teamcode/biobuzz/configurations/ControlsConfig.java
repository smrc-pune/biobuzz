package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * ControlsConfig.java  -  numbers for how the gamepads feel.                  ROLE: config
 *
 * Tunable numbers ONLY. You can change them live in FTC Dashboard
 * (http://192.168.43.1:8080/dash, "ControlsConfig"), but live edits are LOST when
 * the app restarts. When you find a good value, type it back into this file.
 *
 * TODO(BioBuzz): step 3 adds the mechanism stick deadband, the rumble durations
 * (one named constant per rumble event) and the button map as comments.
 */
@Config
public class ControlsConfig {
    public static double TRIGGER_THRESHOLD = 0.5;   // 0.0-1.0: how far a trigger is pulled to count as "pressed"
}
