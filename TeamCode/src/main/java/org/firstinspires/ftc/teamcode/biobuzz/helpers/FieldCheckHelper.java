package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldCheckConfig;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheckHelper.java  -  what every Field Check shares.                      ROLE: helper
 *
 *   FieldCheckHelper.start("FieldCheck04_Push", volts);        at PLAY: field name + match log
 *   String grade = FieldCheckHelper.grade(errorIn, 0.75);      "PASS", "WARN" or "FAIL"
 *   FieldCheckHelper.record(4, grade, "48.3 in, drift 0.2 in"); kept for the Summary + logged
 *   FieldCheckHelper.show(t, 4);                               the result line on telemetry
 *
 * GRADES: PASS within the tolerance, WARN between 1x and 2x the tolerance, FAIL beyond 2x.
 * Results are kept in "static" arrays (they survive from one OpMode to the next, like
 * PoseMemory) for Field Check 13, AND written to the match log as FIELDCHECK_<nn> events,
 * so nothing is lost if the app restarts.
 * Battery: test 02 notes the resting voltage; tests 07-09 note the lowest while driving.
 * If the battery differs from the baseline, a FAIL in 07-09 is shown as WARN ("battery differs").
 */
public class FieldCheckHelper {
    public static final int CHECK_COUNT = 13;
    public static final String PASS = "PASS";
    public static final String WARN = "WARN";
    public static final String FAIL = "FAIL";

    private static final String[] grades = new String[CHECK_COUNT + 1];   // [1] .. [13]
    private static final String[] texts = new String[CHECK_COUNT + 1];
    private static double restingBatteryV = 0;
    private static double lowestDrivingBatteryV = 0;
    private static boolean batteryDiffers = false;

    /** PLAY: the field name goes into MatchInfo (and the log header), then the match log starts. */
    public static void start(String opModeName, double batteryVolts) {
        MatchInfo.fieldName = FieldCheckConfig.FIELD_NAME;
        MatchLogger.start(opModeName, batteryVolts);
    }

    /** STOP: close the match log. */
    public static void stop() {
        MatchLogger.stop("field=" + FieldCheckConfig.FIELD_NAME);
    }

    /** Grade an error against a tolerance: PASS, WARN (up to 2x) or FAIL. */
    public static String grade(double error, double tolerance) {
        double size = Math.abs(error);
        if (size <= tolerance) {
            return PASS;
        }
        if (size <= 2 * tolerance) {
            return WARN;
        }
        return FAIL;
    }

    /** Grade a measurement against the home baseline (tolerance is a fraction, 0.15 = 15%). */
    public static String gradeAgainstBaseline(double measured, double baseline, double toleranceFraction) {
        if (baseline <= 0) {
            return WARN;   // no baseline yet: look at it, and record one at home
        }
        return grade((measured - baseline) / baseline, toleranceFraction);
    }

    /** The worse of two grades. */
    public static String worse(String a, String b) {
        if (a.equals(FAIL) || b.equals(FAIL)) {
            return FAIL;
        }
        if (a.equals(WARN) || b.equals(WARN)) {
            return WARN;
        }
        return PASS;
    }

    /** Keep a result for the Summary and write it to the match log. */
    public static void record(int number, String grade, String text) {
        if ((number == 7 || number == 8 || number == 9) && batteryDiffers) {
            text += " (battery differs from baseline)";
            if (grade.equals(FAIL)) {
                grade = WARN;
            }
        }
        grades[number] = grade;
        texts[number] = text;
        MatchLogger.event(String.format("FIELDCHECK_%02d", number), grade + " " + text);
    }

    public static String getGrade(int number) {
        if (grades[number] == null) {
            return "not run";
        }
        return grades[number];
    }

    public static String getText(int number) {
        if (texts[number] == null) {
            return "";
        }
        return texts[number];
    }

    /** One telemetry line: "04: PASS 48.3 in, drift 0.2 in". */
    public static void show(Telemetry t, int number) {
        t.addData(String.format("%02d", number), getGrade(number) + "  " + getText(number));
    }

    // ---- battery (test 02, then 07-09) ----
    public static void setRestingBattery(double volts) {
        restingBatteryV = volts;
        batteryDiffers = FieldCheckConfig.BASELINE_BATTERY_V > 0
                && Math.abs(volts - FieldCheckConfig.BASELINE_BATTERY_V) > FieldCheckConfig.BATTERY_DIFFERS_V;
    }

    public static void noteDrivingBattery(double volts) {
        if (volts <= 0) {
            return;
        }
        if (lowestDrivingBatteryV == 0 || volts < lowestDrivingBatteryV) {
            lowestDrivingBatteryV = volts;
        }
    }

    public static double getRestingBatteryV() {
        return restingBatteryV;
    }
    public static double getLowestDrivingBatteryV() {
        return lowestDrivingBatteryV;
    }
    public static boolean doesBatteryDiffer() {
        return batteryDiffers;
    }
}
