package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * DriveConfig.java  -  tunable numbers for driving.                            ROLE: config
 *
 * HOW THE DASHBOARD WORKS (same for every ...Config file):
 *  - Open http://192.168.43.1:8080/dash -> "Configuration" tab -> DriveConfig.
 *  - Edit a number; the robot uses it on its very next loop. No re-deploy!
 *  - Live edits are LOST when the app restarts. When you find a good value,
 *    type it in HERE so it becomes the new default.
 *  - Values MUST stay "public static" and NOT "final", or the Dashboard can't edit them.
 *
 * Speed limits for paths are Pedro's, in pedro/Constants.java (set at drive tuning).
 * The sticks always drive at full power.
 */
@Config
public class DriveConfig {
    public static double STICK_DEADZONE = 0.05;     // stick values smaller than this count as 0 (stops creeping)

    // FIELD-CENTRIC: the robot's heading (degrees) when it faces straight AWAY from that
    // alliance's drivers. Pressing Y sets the heading to this value.
    // RED drivers stand along the left wall, so "away" is toward +x (0 deg); BLUE is the opposite.
    public static double RED_DRIVER_HEADING_DEG = 0;
    public static double BLUE_DRIVER_HEADING_DEG = 180;
}
