package org.firstinspires.ftc.teamcode.biobuzz.template;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TemplateConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateLog;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;
import org.firstinspires.ftc.teamcode.biobuzz.statemachines.StateMachine;

/**
 * STUDENTS: copy this file to start a new state machine.
 *
 * TemplateStateMachine.java  -  run a mechanism for a while, then stop.       ROLE: state machine
 * PATTERN: State pattern, in its simplest form: an enum of states and a switch.
 *
 *            start()             RUN_TIME_MS passed
 *    IDLE -----------> RUNNING --------------------> IDLE
 *     ^                   |
 *     +--- cancel() ------+---- TIMEOUT_MS (safety) ---+
 *
 * EVERY STATE MACHINE HAS THE SAME FOUR SECTIONS, in this order:
 *   1. STATES       the enum, one comment per state
 *   2. COMMANDS     start(), cancel(): they only call goTo(state, "reason")
 *   3. update()     a switch with one case per state, marked DO, CHECK and GO
 *   4. BOOKKEEPING  goTo(), isBusy(), getState(), addTelemetry(), addLogData()
 * Every state that waits has a timeout that goes to a safe state, with the reason logged.
 *
 * "extends StateMachine" (like "extends Mechanism"): it fills in the five methods a Subsystem
 * needs (update, cancel, isBusy, addTelemetry, addLogData), each with @Override. It gets the mechanisms it uses in its constructor:
 * the subsystem that owns them builds it with addStateMachine(...). It never calls a
 * mechanism's update() (the subsystem does), another state machine, or Robot.
 * TO ADD ONE: docs/HOW_TO_ADD_A_MECHANISM.md, second part.
 *
 * TRACING A BUG: look at the "last change" line on telemetry: the newest state and WHY.
 */
public class TemplateStateMachine extends StateMachine {

    // ==================== 1. STATES ====================
    public enum State {
        IDLE,       // doing nothing; the mechanism is stopped
        RUNNING     // the mechanism runs until RUN_TIME_MS has passed
    }

    private final TemplateMechanism mechanism;
    private final StateTimer timer = new StateTimer();
    private final StateLog log = new StateLog("Template");
    private State state = State.IDLE;

    public TemplateStateMachine(TemplateMechanism mechanism) {
        this.mechanism = mechanism;
    }

    // ==================== 2. COMMANDS ====================

    /** Start running. Pressing it again while running does nothing. */
    public void start() {
        if (mechanism.isFaulted()) {
            log.warn("start ignored: the mechanism is FAULTED");
            return;
        }
        goTo(State.RUNNING, "start");
    }

    /** Stop at once. */
    @Override
    public void cancel() { goTo(State.IDLE, "cancel"); }

    // ==================== 3. UPDATE (once per loop, its subsystem calls it) ====================

    @Override
    public void update() {
        switch (state) {
            case IDLE:
                // DO: nothing. (goTo() stopped the mechanism when we came here.)
                break;

            case RUNNING:
                // DO
                mechanism.run();
                // CHECK + GO
                if (timer.isTimeUp(TemplateConfig.RUN_TIME_MS)) {
                    goTo(State.IDLE, "done");
                }
                // TIMEOUT: a safety net for states that wait on something that may never happen
                if (timer.isTimeUp(TemplateConfig.TIMEOUT_MS)) {
                    log.warn("timeout");
                    goTo(State.IDLE, "timeout");
                }
                break;
        }
    }

    // ==================== 4. BOOKKEEPING ====================

    /** The ONLY place the state changes. Going to the state we are already in does nothing. */
    private void goTo(State newState, String reason) {
        if (newState == state) {
            return;
        }
        log.record(state, newState, reason);   // telemetry history + a match-log EVENT
        state = newState;
        timer.reset();
        if (state == State.IDLE) {
            mechanism.runStop();   // entering IDLE: make it safe
        }
    }

    @Override
    public boolean isBusy() { return state != State.IDLE; }

    public State getState() { return state; }

    @Override
    public void addTelemetry(Telemetry t) { log.addTo(t, state, timer, isBusy()); }

    @Override
    public void addLogData() { MatchLogger.value("template_state", state.ordinal()); }
}
