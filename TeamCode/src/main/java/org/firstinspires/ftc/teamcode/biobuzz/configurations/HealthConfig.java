package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * MENTORS: you do not need to change this file.
 *
 * HealthConfig.java  -  numbers for watching the robot's health (helpers/HealthMonitor.java).  ROLE: config
 *
 * These are starting values. Set them from our own match logs (about 10 runs).
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class HealthConfig {
    // ---- Battery ----
    public static double BATTERY_SAMPLE_MS = 100;     // read the battery voltage at most this often
    public static double INIT_LOW_BATTERY_V = 12.0;   // below this in INIT: "LOW - swap the battery"
    public static double PLAY_LOW_BATTERY_V = 9.0;    // below this during play ...
    public static double PLAY_LOW_BATTERY_MS = 500;   // ... for this long: ALERT and rumble

    // ---- Loop time ----
    public static double SLOW_LOOP_AVERAGE_MS = 25;   // ALERT if the average over 1 second is above this
    public static double SLOW_LOOP_WORST_MS = 100;    // ... or if any single loop takes longer than this

    // ---- Heading jump (field-centric safeguard) ----
    public static double MAX_TURN_DEG_PER_S = 720;    // faster than any FTC robot really turns
    public static double HEADING_JUMP_MARGIN_DEG = 10;

    // ---- Robot Check (testing/RobotCheck.java), before every match ----
    public static double CHECK_BATTERY_MIN_V = 12.5;      // resting voltage to start a match
    public static double CHECK_BATTERY_S = 2;             // measure the resting voltage this long
    public static double CHECK_PINPOINT_S = 2;            // watch the Pinpoint's update rate this long
    public static double CHECK_PINPOINT_HZ = 0;           // the rate noted on the first run (0 = not noted yet)
    public static double CHECK_PINPOINT_HZ_TOLERANCE = 0.2;   // 0.2 = within 20% of the noted rate
    public static double CHECK_STILL_S = 10;              // gyro still-test length, hands off the robot
    public static double CHECK_STILL_MAX_DEG = 1;         // the heading may creep at most this much
    public static double CHECK_MIN_FREE_MB = 50;          // free storage for the match logs
}
