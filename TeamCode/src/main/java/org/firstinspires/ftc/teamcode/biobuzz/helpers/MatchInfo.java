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
    public static AllianceColor alliance = AllianceColor.RED;   // RED until someone picks BLUE
    public static String fieldName = "home";                  // which field we are on; set by the Field Checks

    public static boolean isRed() { return alliance == AllianceColor.RED; }

    /** RED becomes BLUE, BLUE becomes RED (used by test OpModes' B button). */
    public static void switchAlliance() {
        if (alliance == AllianceColor.RED) alliance = AllianceColor.BLUE;
        else alliance = AllianceColor.RED;
    }
}
