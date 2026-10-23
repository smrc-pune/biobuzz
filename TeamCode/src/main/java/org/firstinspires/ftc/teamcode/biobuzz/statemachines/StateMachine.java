package org.firstinspires.ftc.teamcode.biobuzz.statemachines;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * MENTORS: you do not need to change this file.
 *
 * StateMachine.java  -  the five methods every state machine has.             ROLE: interface
 * PATTERN: Interface ("a promise"). A class that says "implements StateMachine" promises
 * to have these five methods. Then a Subsystem can update, cancel and show ANY state
 * machine the same way, without knowing which one it is.
 *
 *   public class GatePulseStateMachine implements StateMachine { ... }
 *
 * The commands that START a job (start(count), open(), ...) are not here: each state
 * machine has its own, and its subsystem calls them.
 * The one interface in the team code (Requirements v2.8, R3): if a state machine forgets
 * one of these methods (cancel() is the easy one to forget), the build fails and says so.
 */
public interface StateMachine {

    /** One small step. Its subsystem calls it once per loop, BEFORE its mechanisms update. */
    void update();

    /** Stop the job at once and go to the safe state (stop-all, or a timeout in Auto). */
    void cancel();

    /** @return true while the job is running (not IDLE). */
    boolean isBusy();

    /** The state, the "last change" line, and anything else worth seeing. */
    void addTelemetry(Telemetry t);

    /** Its match-log columns, for example MatchLogger.value("launch_state", ...). */
    void addLogData();
}
