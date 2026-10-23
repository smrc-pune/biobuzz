package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;

/**
 * MENTORS: you do not need to change this file.
 *
 * Alerts.java  -  tells the drive team that something is wrong.                 ROLE: helper
 *
 * Any file can raise an ALERT:
 *   Alerts.raise("LOW BATTERY 8.7 V", true);    // true = urgent: the OpMode rumbles the gamepads
 *
 * An ALERT shows on the telemetry line under the header for TelemetryConfig.ALERT_SHOW_S
 * seconds, and is written to the match log as an EVENT row. Only the newest ALERT is shown.
 * Only OpModes touch gamepads, so for an urgent ALERT the OpMode asks
 * robot.hasNewUrgentAlert() and rumbles.
 *
 * "static" means there is one set of alerts for the whole app (the same idea as PoseMemory).
 * Robot.startMatch() clears them.
 */
public class Alerts {
    private static String message = "";
    private static boolean urgentWaiting = false;
    private static final ElapsedTime sinceRaised = new ElapsedTime();

    /** Show an ALERT and write it to the match log. urgent = the OpMode should rumble. */
    public static void raise(String text, boolean urgent) {
        message = text;
        sinceRaised.reset();
        if (urgent) {
            urgentWaiting = true;
        }
        MatchLogger.event("ALERT", text);
    }

    /** @return the newest ALERT, or "" once it is older than TelemetryConfig.ALERT_SHOW_S. */
    public static String getMessage() {
        if (sinceRaised.seconds() > TelemetryConfig.ALERT_SHOW_S) {
            return "";
        }
        return message;
    }

    /** @return true ONCE for each new urgent ALERT (so the OpMode rumbles once). */
    public static boolean takeUrgent() {
        boolean wasWaiting = urgentWaiting;
        urgentWaiting = false;
        return wasWaiting;
    }

    /** Forget old ALERTs (a new match starts). */
    public static void clear() {
        message = "";
        urgentWaiting = false;
    }
}
