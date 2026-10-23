package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;

/**
 * MENTORS: you do not need to change this file.
 *
 * StateLog.java  -  the "black box recorder" for a state machine.          ROLE: helper
 *
 * WHY IT EXISTS:
 * When a mechanism does something unexpected, the first question is always
 * "what state is it in, and how did it get there?". This class remembers the
 * last few state changes AND the reason for each one, and shows them on
 * telemetry, so you can debug without reading any code.
 *
 * HOW A STATE MACHINE USES IT:
 *   private final StateLog log = new StateLog("Launch"); // the name shown on telemetry and in the log
 *   log.record(oldState, newState, "at speed");          // inside goTo()
 *   log.warn("launch ignored: launcher is IDLE");        // when a command can't be obeyed
 *   log.addTo(telemetry, state, timer, busy);            // inside addTelemetry()
 * Every record() and warn() is also written to the match log as an EVENT row.
 *
 * TRACING A BUG: the "last change" line tells you the newest state and why it
 * changed. The "earlier" lines are the 4 changes before it, newest first.
 * Read this file once; you will not need to change it.
 */
public class StateLog {
    private static final int HISTORY_SIZE = 5;   // the last change + the 4 before it

    private final String name;
    private final String[] history = new String[HISTORY_SIZE];  // newest first
    private String warning = "";
    private final ElapsedTime sinceWarning = new ElapsedTime();
    private final ElapsedTime sinceChange = new ElapsedTime();

    public StateLog(String name) { this.name = name; }

    /** Remember that we moved from one state to another, and why. */
    public void record(Object from, Object to, String reason) {
        for (int i = HISTORY_SIZE - 1; i > 0; i--) {
            history[i] = history[i - 1];              // shift older entries down
        }
        history[0] = from + " -> " + to + "  (" + reason + ")";
        sinceChange.reset();
        MatchLogger.event("CHANGE", name + ": " + history[0]);
    }

    /** Remember that a command was ignored, so telemetry can tell the student. */
    public void warn(String message) {
        warning = message;
        sinceWarning.reset();
        MatchLogger.event("WARN", name + ": " + message);
    }

    /** @return the newest change, for example "IDLE -> INTAKING  (intake on)", or "" if none yet. */
    public String getLastChange() {
        if (history[0] == null) {
            return "";
        }
        return history[0];
    }

    /** @return the newest warning, or "" once it is older than TelemetryConfig.WARNING_SHOW_S. */
    public String getWarning() {
        if (sinceWarning.seconds() > TelemetryConfig.WARNING_SHOW_S) {
            return "";
        }
        return warning;
    }

    /** Add this machine's status lines to telemetry. */
    public void addTo(Telemetry telemetry, Object state, StateTimer timer, boolean busy) {
        telemetry.addData(name, state + "  (" + String.format("%.1f", timer.seconds()) + " s)"
                + (busy ? "  busy" : ""));
        if (!getWarning().isEmpty()) {
            telemetry.addData(name + " WARNING", warning);
        }
        if (TelemetryConfig.HISTORY && history[0] != null) {
            telemetry.addData(name + " last change", history[0] + ", "
                    + String.format("%.1f", sinceChange.seconds()) + " s ago");
            for (int i = 1; i < HISTORY_SIZE; i++) {   // entry 0 is the "last change" shown above
                if (history[i] != null) {
                    telemetry.addData(name + " earlier", history[i]);
                }
            }
        }
    }
}
