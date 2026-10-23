package org.firstinspires.ftc.teamcode.biobuzz.statemachines;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * MENTORS: you do not need to change this file.
 *
 * StateMachine.java  -  what every state machine has in common.              ROLE: base class
 * PATTERN: the same idea as Mechanism: your state machine "extends StateMachine" and fills
 * in these five methods with @Override. Then a Subsystem can update, cancel and show ANY
 * state machine the same way, without knowing which one it is.
 *
 *   public class GatePulseStateMachine extends StateMachine {
 *       @Override public void update() { ... }
 *       @Override public void cancel() { goTo(State.IDLE, "cancel"); }
 *       ...
 *   }
 *
 * FILL IN ALL FIVE. The empty versions below do nothing, so a state machine that forgets
 * cancel() would keep running after stop-all. Copy template/TemplateStateMachine.java: it
 * has all five.
 * The commands that START a job (start(count), open(), ...) are not here: each state
 * machine has its own, and its subsystem calls them.
 */
public class StateMachine {

    /** One small step. Its subsystem calls it once per loop, BEFORE its mechanisms update. */
    public void update() {
        // empty here: your state machine fills it in with @Override
    }

    /** Stop the job at once and go to the safe state (stop-all, or a timeout in Auto). */
    public void cancel() {
        // empty here: your state machine fills it in with @Override
    }

    /** @return true while the job is running (not IDLE). */
    public boolean isBusy() {
        return false;
    }

    /** The state, the "last change" line, and anything else worth seeing. */
    public void addTelemetry(Telemetry t) {
        // empty here: your state machine fills it in with @Override
    }

    /** Its match-log columns, for example MatchLogger.value("launch_state", ...). */
    public void addLogData() {
        // empty here: your state machine fills it in with @Override
    }
}
