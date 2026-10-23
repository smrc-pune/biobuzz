package org.firstinspires.ftc.teamcode.biobuzz.mechanisms;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.Alerts;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * MENTORS: you do not need to change this file.
 *
 * Mechanism.java  -  what every mechanism has in common.                       ROLE: base class
 * PATTERN: Template Method. Your mechanism fills in up to five methods; this class
 * does the safety around them.
 *
 *   public class Intake extends Mechanism {
 *       public Intake(HardwareMap hardwareMap) { super("intake"); ... }
 *       @Override protected void update() { ... }               // every loop (optional)
 *       @Override protected void stop() { ... }                 // safe state: motors off
 *       @Override protected void addDetails(Telemetry t) { ... } // your telemetry lines
 *       @Override protected void addLogValues() { ... }          // match-log columns (optional)
 *       @Override protected boolean isTelemetryOn() { return TelemetryConfig.INTAKE; }  // its switch
 *   }
 *
 * Its subsystem calls runUpdate(), runStop(), addTelemetry() and addLogData() on every
 * mechanism it owns (see subsystems/Subsystem.java).
 * If your mechanism throws an error (for example a cable came out), it is marked FAULTED:
 * it stops, an urgent ALERT says why, and the rest of the robot keeps running.
 *
 * RULES FOR EVERY MECHANISM: get devices in the constructor with hardwareMap.get(...) and
 * HardwareNames; never move anything in the constructor (rule G304.H: robots stay still
 * during INIT); no states and no timers (those go in a state machine); every number in a
 * ...Config file.
 */
public class Mechanism {

    private final String name;
    private boolean faulted = false;
    private String faultReason = "";

    public Mechanism(String name) {
        this.name = name;
    }

    // ==================== HOOKS: your mechanism fills these in ====================

    /** Every loop: control loops and sensor reads. Never wait or sleep here. */
    protected void update() {
        // empty here: your mechanism fills it in with @Override
    }

    /** Put the devices in their safe state: motors off, servos where they hold nothing. */
    protected void stop() {
        // empty here: your mechanism fills it in with @Override
    }

    /** Your own telemetry lines (powers, speeds, sensor values). */
    protected void addDetails(Telemetry t) {
        // empty here: your mechanism fills it in with @Override
    }

    /**
     * Your match-log columns, the same ones in the same order every time, for example
     * MatchLogger.value("intake_power", power). Log numbers you already keep in fields:
     * do NOT read hardware here (this also runs while FAULTED).
     */
    protected void addLogValues() {
        // empty here: your mechanism fills it in with @Override
    }

    /** Your switch in TelemetryConfig, so a student can hide these lines. A FAULTED line always shows. */
    protected boolean isTelemetryOn() {
        return true;
    }

    // ==================== FIXED: its subsystem and test OpModes call these ====================

    public final void runUpdate() {
        if (faulted) {
            return;
        }
        try {
            update();
        } catch (RuntimeException e) {
            markFaulted("error: " + e.getMessage());
        }
    }

    public final void runStop() {
        try {
            stop();
        } catch (RuntimeException e) {
            markFaulted("stop error: " + e.getMessage());
        }
    }

    public final void addTelemetry(Telemetry t) {
        if (faulted) {
            t.addData(name, "FAULTED: " + faultReason);
        } else if (isTelemetryOn()) {
            addDetails(t);
        }
    }

    /** Its subsystem calls this for each match-log DATA row. */
    public final void addLogData() {
        MatchLogger.value(name + "_faulted", faulted);
        addLogValues();
    }

    public final boolean isFaulted() {
        return faulted;
    }

    public final String getName() {
        return name;
    }

    /** A mechanism may call this itself, for example when it sees a motor stall. */
    protected final void markFaulted(String reason) {
        if (faulted) {
            return;
        }
        faulted = true;
        faultReason = reason;
        runStop();
        Alerts.raise(name + " FAULTED: " + reason, true);   // urgent: also written to the match log
    }
}
