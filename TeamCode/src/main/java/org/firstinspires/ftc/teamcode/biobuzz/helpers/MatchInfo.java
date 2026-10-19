package org.firstinspires.ftc.teamcode.biobuzz.helpers;

/**
 * MENTORS: you do not need to change this file.
 *
 * MatchInfo.java  -  facts about this match that every file can read.         ROLE: helper
 *
 * WHY IT EXISTS:
 * The alliance is picked once, in the INIT menu (D-pad on gamepad 1). Drive needs it to
 * know which way is "away from the drivers", and FieldPaths needs it to turn the RED
 * places into BLUE ones. The value is kept in a "static" field (the same idea as
 * PoseMemory), so TeleOp's INIT starts with what was picked in Autonomous's INIT, and the
 * drive team only has to check it.
 *
 * TRACING A BUG: the robot drives "away" toward the drivers, or an assist goes to the
 * other alliance's side? Check the Alliance row in the INIT menu.
 */
public class MatchInfo {
    // The choices shown in the InitMenu. The index into this list is what is stored.
    public static final String[] ALLIANCES = {"RED", "BLUE"};   // RED first: it is the default

    public static int alliance = 0;           // index into ALLIANCES (0 = RED)
    public static String fieldName = "home";  // which field we are on; set by the Field Checks

    public static boolean isRed() { return alliance == 0; }

    public static String getAllianceName() { return ALLIANCES[alliance]; }
}
