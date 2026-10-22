package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * FieldCheckConfig.java  -  the Field Checks' home baseline and tolerances.     ROLE: config
 * Used by fieldchecks/FieldCheck01_Hardware ... FieldCheck13_Summary.
 *
 * BEFORE THE CHECKS: set FIELD_NAME ("home", "event-practice-1", ...). It goes in every log.
 * THE BASELINE: run all checks on our home field, then copy the values Field Check 13 prints
 * into the BASELINE_ values below, with the date. Re-record after any re-tune or repair.
 * A BASELINE_ value of 0 means "not recorded yet": that check can only WARN.
 * GRADES: PASS within tolerance, WARN between 1x and 2x, FAIL beyond 2x (or a hard limit broken).
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class FieldCheckConfig {
    public static String FIELD_NAME = "home";
    public static String BASELINE_DATE = "not recorded yet";   // TODO(BioBuzz): record the home baseline

    // ---- 01 Hardware ----
    public static int EXPECTED_HUBS = 2;                 // Control Hub + Expansion Hub

    // ---- 02 Battery ----
    public static double BATTERY_MIN_V = 12.5;           // resting voltage to judge a field
    public static double BATTERY_S = 2;
    public static double BASELINE_BATTERY_V = 0;
    public static double BATTERY_DIFFERS_V = 0.5;        // further than this from the baseline: 07-09 are only fair as WARN

    // ---- 03 Still ----
    public static double STILL_S = 10;
    public static double STILL_MAX_DEG = 0.5;

    // ---- 04 Push and 05 Spin ----
    public static double PUSH_DISTANCE_IN = 48;          // 2 tiles
    public static double PUSH_TOLERANCE_IN = 0.75;
    public static double PUSH_DRIFT_TOLERANCE_IN = 0.5;
    public static double SPIN_TURNS = 4;
    public static double SPIN_TOLERANCE_DEG = 2;

    // ---- 07 Brake (same method as Pedro's deceleration tuner) ----
    public static double BRAKE_TEST_SPEED_IN_PER_S = 30; // full power until this speed, then coast
    public static double BRAKE_MAX_DRIVE_MS = 2000;      // safety: stop pushing after this long anyway
    public static double BASELINE_FORWARD_DECEL = 0;     // in/s/s; compare with naturalForwardDeceleration in pedro/Constants
    public static double BASELINE_STRAFE_DECEL = 0;      // in/s/s; compare with naturalStrafeDeceleration
    public static double BRAKE_TOLERANCE = 0.15;         // 0.15 = within 15%

    // ---- 08 Line and 09 Square (Pedro paths from wherever the robot is placed) ----
    public static double LINE_DISTANCE_IN = 48;
    public static double BASELINE_LINE_ERROR_IN = 0;
    public static double LINE_TOLERANCE_IN = 0.75;
    public static double SQUARE_SIDE_IN = 48;
    public static double SQUARE_MAX_ERROR_IN = 1.5;
    public static double SQUARE_MAX_HEADING_DEG = 3;
    public static double SETTLE_MS = 1000;               // after the path ends, wait this long before measuring
    public static double PATH_TIMEOUT_MS = 10000;

    // ---- 10 Wall ----
    public static double WALL_MAX_DEG = 3;

    // ---- 12 Camera: robot on a seam crossing, a known AprilTag in view ----
    // TODO(BioBuzz): pick a seam crossing and a tag; measure the tag's centre (field frame, RED side).
    public static double CAMERA_CHECK_X_IN = 48;         // where the robot's centre is placed
    public static double CAMERA_CHECK_Y_IN = 72;
    public static double CAMERA_CHECK_HEADING_DEG = 0;
    public static int CAMERA_CHECK_TAG_ID = 20;
    public static double CAMERA_CHECK_TAG_X_IN = 72;
    public static double CAMERA_CHECK_TAG_Y_IN = 72;
    public static double CAMERA_CHECK_TAG_Z_IN = 6;      // the tag's centre height above the floor
    public static double CAMERA_TOLERANCE_IN = 2;
}
