package org.firstinspires.ftc.teamcode.biobuzz.mechanisms;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * MENTORS: you do not need to change this file.
 *
 * Mechanism.java  -  what every mechanism has in common.                       ROLE: base class
 * PATTERN: Template Method. Your mechanism fills in up to three methods; this class
 * does the safety around them.
 *
 *   public class Intake extends Mechanism {
 *       public Intake(HardwareMap hardwareMap) { super("intake"); ... }
 *       @Override protected void update() { ... }               // every loop (optional)
 *       @Override protected void stop() { ... }                 // safe state: motors off
 *       @Override protected void addDetails(Telemetry t) { ... } // your telemetry lines
 *   }
 *
 * Robot calls runUpdate(), runStop() and addTelemetry() on every mechanism. If your
 * mechanism throws an error (for example a cable came out), it is marked FAULTED: it stops,
 * telemetry says why, and the rest of the robot keeps running.
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

    public Mechanism(String name) { this.name = name; }

    // ==================== HOOKS: your mechanism fills these in ====================

    /** Every loop: control loops and sensor reads. Never wait or sleep here. */
    protected void update() { }

    /** Put the devices in their safe state: motors off, servos where they hold nothing. */
    protected void stop() { }

    /** Your own telemetry lines (powers, speeds, sensor values). */
    protected void addDetails(Telemetry t) { }

    // ==================== FIXED: Robot and test OpModes call these ====================

    public final void runUpdate() {
        if (faulted) return;
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
        if (faulted) t.addData(name, "FAULTED: " + faultReason);
        else addDetails(t);
    }

    public final boolean isFaulted() { return faulted; }

    public final String getName() { return name; }

    /** A mechanism may call this itself, for example when it sees a motor stall. */
    protected final void markFaulted(String reason) {
        if (faulted) return;
        faulted = true;
        faultReason = reason;
        runStop();
        // TODO(BioBuzz) step 4: write a FAULT event to the match log and raise an ALERT.
    }
}
