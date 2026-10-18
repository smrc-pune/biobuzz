package org.firstinspires.ftc.teamcode.biobuzz.helpers;

/**
 * MatchInfo.java  -  what the drive team entered during INIT.                  ROLE: helper
 *
 * WHY IT EXISTS:
 * The match log file name and header need the match id ("Q12"), the alliance, the
 * battery label and the field name. These are picked once in Autonomous INIT with the
 * InitMenu. They are kept in "static" fields (the same idea as PoseMemory), so TeleOp's
 * INIT starts with the same values and the drive team only has to confirm them.
 *
 * MATCH ID: type + number, for example P03 (practice), Q12 (qualification), E2 (elimination).
 * Number 0 means "not entered", and the id is then "NA". Field Checks use "FIELD" and
 * test OpModes use "TEST" (they set matchIdOverride).
 *
 * TRACING A BUG: a log file named "NA_..." means nobody entered a match number in INIT.
 */
public class MatchInfo {
    // The choices shown in the InitMenu. The index into these lists is what is stored.
    public static final String[] MATCH_TYPES = {"P", "Q", "E"};
    public static final String[] BATTERY_LABELS = {"A", "B", "C", "D", "E", "F", "G", "H"};
    public static final String[] ALLIANCES = {"BLUE", "RED"};
    public static final int MAX_MATCH_NUMBER = 99;

    public static int matchType = 1;          // index into MATCH_TYPES (1 = Q)
    public static int matchNumber = 0;        // 0 = not entered
    public static int batteryLabel = 0;       // index into BATTERY_LABELS
    public static int alliance = 0;           // index into ALLIANCES (0 = BLUE)
    public static boolean allianceChosen = false;  // false until someone picks it in an INIT menu
    public static String fieldName = "home";  // set by the Field Checks
    public static String matchIdOverride = "";     // "FIELD" or "TEST", or "" for a real match

    /** @return the id for the log file name, for example "Q12", "NA", "FIELD" or "TEST". */
    public static String getMatchId() {
        if (!matchIdOverride.isEmpty()) return matchIdOverride;
        if (matchNumber == 0) return "NA";
        String number = String.valueOf(matchNumber);
        if (MATCH_TYPES[matchType].equals("P") && matchNumber < 10) number = "0" + number;  // P03
        return MATCH_TYPES[matchType] + number;
    }

    public static boolean isRed() { return alliance == 1; }

    public static String getAllianceName() { return ALLIANCES[alliance]; }

    public static String getBatteryLabel() { return BATTERY_LABELS[batteryLabel]; }
}
