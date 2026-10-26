package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * FieldConfig.java  -  every place on the field we drive to.                  ROLE: config
 *
 * EVERY PLACE IS WRITTEN FOR THE RED ALLIANCE (the default). For BLUE, drive/FieldPaths
 * turns it half a turn around the middle of the field (72, 72): the BIOBUZZ field looks
 * the same from both sides after a half turn. You never write BLUE numbers.
 *
 * THE FIELD (seen from the audience, like field-biobuzz.png):
 *
 *   y = 144 +--------------------------------------+
 *           | RESET                                |
 *           | CORNER                               |
 *           |LOADING                               |   RED drivers stand along
 *           | ZONE        +----------+             |   the LEFT wall (x = 0)
 *           |             |   HIVE   |             |
 *           |   LAUNCH    |          |             |   BLUE drivers along the
 *           |   SPOT      +----------+             |   RIGHT wall (x = 144)
 *           |                                      |
 *           |RED GARDEN                            |
 *   y = 0   +--------------------------------------+
 *          x = 0                                 x = 144      (inches; heading 0 = toward +x)
 *
 * Each place is the robot's CENTRE and the way it faces. Positions are in inches,
 * headings in degrees (0 = facing the BLUE wall, 90 = facing the far wall).
 *
 * TODO(BioBuzz): every number below is a first guess from the field drawing. Measure them
 * on a real field against the tile seams (every 24 in), then type them in here.
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class FieldConfig {

    // The middle of the field. RED places are turned around this point for BLUE.
    public static double FIELD_CENTRE_IN = 72;

    // ---- Start positions for Autonomous (AutoConfig.START picks one). Rule G304: touching
    //      the wall, on our side, not in the LOADING ZONE, touching the 4 preloaded POLLEN. ----
    public static double START_NEAR_GARDEN_X_IN = 9;      // back on the left wall
    public static double START_NEAR_GARDEN_Y_IN = 36;
    public static double START_NEAR_GARDEN_HEADING_DEG = 0;
    public static double START_MIDDLE_X_IN = 9;
    public static double START_MIDDLE_Y_IN = 84;
    public static double START_MIDDLE_HEADING_DEG = 0;

    // ---- Our side of the field for Autonomous (rule G402: columns A-C are RED). The robot's
    //      CENTRE must stay this far from the middle line, so no part of it crosses. ----
    public static double OWN_SIDE_MARGIN_IN = 9;

    // ---- Our launch spot (driver's A button, and Auto). LauncherConfig.LAUNCH_RPM is tuned
    //      from HERE, so launches land best from this spot. Move it? Retune LAUNCH_RPM. ----
    public static double LAUNCH_SPOT_X_IN = 30;
    public static double LAUNCH_SPOT_Y_IN = 72;
    public static double LAUNCH_SPOT_HEADING_DEG = 0;

    // ---- Our LOADING ZONE (driver's B button): drive toward the approach point, then park ----
    public static double LOADING_APPROACH_X_IN = 36;
    public static double LOADING_APPROACH_Y_IN = 108;
    public static double LOADING_PARK_X_IN = 12;
    public static double LOADING_PARK_Y_IN = 108;
    public static double LOADING_PARK_HEADING_DEG = 0;

    // ---- "Set pose here" (driver holds D-pad left): the robot's centre when its back
    //      touches the left wall and its side touches the far wall (top-left corner) ----
    public static double RESET_CORNER_X_IN = 9;
    public static double RESET_CORNER_Y_IN = 135;
    public static double RESET_CORNER_HEADING_DEG = 0;

    // ---- Our two CELLS on the HIVE (the middle of each, where the launcher aims). The HIVE
    //      tips over, so one CELL faces up at a time; MatchInfo knows which (operator's TIP button).
    //      Measured from field-biobuzz.png: CELL A is the one nearer the audience (lower y).
    //      For BLUE (turned half a turn) CELL A is the blue CELL OPPOSITE the audience.
    //      The AprilTag numbers under each CELL are in CameraConfig. ----
    public static double CELL_A_X_IN = 59;
    public static double CELL_A_Y_IN = 61;
    public static double CELL_B_X_IN = 59;
    public static double CELL_B_Y_IN = 88;
    public static boolean START_CELL_A_UP = true;   // which CELL faces up when the match starts (field setup)

    // ---- The HIVE in the middle, made bigger by half a robot, so the robot's CENTRE must stay
    //      outside this box (rules G409, G417). It is the same for both alliances. ----
    public static double HIVE_KEEP_OUT_MIN_X_IN = 38;
    public static double HIVE_KEEP_OUT_MAX_X_IN = 106;
    public static double HIVE_KEEP_OUT_MIN_Y_IN = 43;
    public static double HIVE_KEEP_OUT_MAX_Y_IN = 101;
}
