package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldConfig;

/**
 * MENTORS: you do not need to change this file.
 *
 * MatchInfo.java  -  facts about this match that every file can read.         ROLE: helper
 *
 * WHY IT EXISTS:
 * The alliance is picked during INIT (D-pad left/right on gamepad 1). Drive needs it to
 * know which way is "away from the drivers", and FieldPaths needs it to turn the RED
 * places into BLUE ones. The value is kept in a "static" field (the same idea as
 * PoseMemory), so TeleOp's INIT starts with what was picked in Autonomous's INIT, and the
 * drive team only has to check it.
 *
 * THE HIVE: which of our two CELLS faces up is a match fact too. Auto's init() resets it to
 * the field setup (FieldConfig.START_CELL_A_UP); each TIP (operator's left bumper) flips it.
 * The launcher aims at the upward CELL, so a missed TIP press means missed shots.
 *
 * TRACING A BUG: the robot drives "away" toward the drivers, or an assist goes to the
 * other alliance's side? Check the Alliance line on the INIT screen.
 */
public class MatchInfo {
    public static AllianceColor alliance = AllianceColor.RED;   // RED until someone picks BLUE
    public static String fieldName = "home";                  // which field we are on; set by the Field Checks

    // ---- The HIVE: which of our two CELLS faces up, and how many TIPS we have ----
    private static boolean cellAUp = FieldConfig.START_CELL_A_UP;
    private static int tipCount = 0;
    private static boolean hasTipped = false;
    private static final ElapsedTime sinceTip = new ElapsedTime();

    public static boolean isRed() { return alliance == AllianceColor.RED; }

    /** Auto init() only: the HIVE as the field crew set it up, no TIPS yet. */
    public static void resetForNewMatch() {
        cellAUp = FieldConfig.START_CELL_A_UP;
        tipCount = 0;
        hasTipped = false;
    }

    /** Our HIVE just TIPPED (operator's button): the other CELL faces up now. */
    public static void markHiveTipped() {
        cellAUp = !cellAUp;
        tipCount++;
        hasTipped = true;
        sinceTip.reset();
        MatchLogger.event("HIVE_TIP", tipCount + " (up CELL " + getUpCellName() + ")");
    }

    public static boolean isCellAUp() { return cellAUp; }

    public static String getUpCellName() {
        if (cellAUp) return "A";
        return "B";
    }

    public static int getTipCount() { return tipCount; }

    /** The human player may enter one NECTAR per TIP of our HIVE (rule G426), and any after 1:00 left. */
    public static int getNectarAllowed() { return tipCount; }

    /** Seconds since the last TIP; a big number if there was none. The launcher waits after a TIP. */
    public static double secondsSinceTip() {
        if (!hasTipped) return 1000;
        return sinceTip.seconds();
    }

    /** RED becomes BLUE, BLUE becomes RED (used by test OpModes' B button). */
    public static void switchAlliance() {
        if (alliance == AllianceColor.RED) alliance = AllianceColor.BLUE;
        else alliance = AllianceColor.RED;
    }
}
