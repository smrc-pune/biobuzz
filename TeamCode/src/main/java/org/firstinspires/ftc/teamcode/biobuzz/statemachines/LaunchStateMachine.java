package org.firstinspires.ftc.teamcode.biobuzz.statemachines;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LauncherConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.Alerts;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateLog;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Feeder;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Launcher;

/**
 * STUDENTS: you will change this file.
 *
 * LaunchStateMachine.java  -  "launch": spin up, then feed while the flywheel is fast enough.  ROLE: state machine
 * PATTERN: State pattern (enum + switch).
 *
 *           start()               up to speed
 *   IDLE ------------> SPINNING_UP ----------------> FEEDING
 *    ^                  |      ^                      |    |
 *    |  timeout/cancel  |      +-- flywheel too slow -+    |
 *    +------------------+          (or a HIVE TIP)         |
 *    +--------------- fed FEED_TOTAL_MS in total ----------+
 *
 * The feeder is a windmill (goBILDA StarterBot). It turns nonstop while the flywheel is at
 * least LauncherConfig.MIN_FEED_RPM. Each ball slows the flywheel a little; if it drops
 * below MIN_FEED_RPM, the windmill stops (SPINNING_UP) until the flywheel is back up to
 * speed. When the windmill has turned for FEED_TOTAL_MS in total, the hopper is empty.
 * The robot cannot count balls, so it does not try: it feeds long enough for 4.
 *
 * The flywheel always spins at ONE speed, LauncherConfig.LAUNCH_RPM, tuned from our launch
 * spot. So launches land best from that spot (where Auto and the driver's A button go).
 * After a HIVE TIP it does not feed for TIP_PAUSE_MS (game manual 10.5.1).
 * Uses: Feeder and Launcher (commands only). It never runs the intake (DESIGN-REVIEW 14, H2).
 * Lives in: ShooterSubsystem, which builds it and calls update() every loop.
 *
 * TRACING A BUG: turn on TelemetryConfig.HISTORY and read the "Launch last change" line
 * (it is also in the match log). "spin-up timeout" = the flywheel never
 * reached its speed: see Launcher's RPM vs target, and the battery. Balls left in the
 * hopper: raise FEED_TOTAL_MS.
 */
public class LaunchStateMachine extends StateMachine {

    // ==================== 1. STATES ====================
    public enum State {
        IDLE,          // windmill stopped (balls held back); flywheel at idle speed
        SPINNING_UP,   // windmill stopped; flywheel speeding up to LAUNCH_RPM
        FEEDING        // windmill turning: balls go into the flywheel
    }

    private final Feeder feeder;
    private final Launcher launcher;
    private final StateTimer timer = new StateTimer();
    private final StateLog log = new StateLog("Launch");
    private State state = State.IDLE;
    private double fedMs = 0;   // how long the windmill has turned in this launch, before this FEEDING

    public LaunchStateMachine(Feeder feeder, Launcher launcher) {
        this.feeder = feeder;
        this.launcher = launcher;
    }

    // ==================== 2. COMMANDS ====================

    /** Launch everything in the hopper. Ignored if already launching. */
    public void start() {
        if (feeder.isFaulted() || launcher.isFaulted()) {
            log.warn("launch ignored: feeder or launcher is FAULTED");
            return;
        }
        if (isBusy()) {
            log.warn("launch ignored: already launching");
            return;
        }
        fedMs = 0;
        goTo(State.SPINNING_UP, "launch");
    }

    /** Stop launching at once. */
    @Override
    public void cancel() {
        goTo(State.IDLE, "cancel");
    }

    // ==================== 3. UPDATE (once per loop, ShooterSubsystem calls it) ====================

    @Override
    public void update() {
        boolean tipPauseOver = MatchInfo.secondsSinceTip() * 1000 >= LauncherConfig.TIP_PAUSE_MS;

        switch (state) {
            case IDLE:
                // DO: nothing. (goTo() stopped the windmill and set the flywheel to idle.)
                break;

            case SPINNING_UP:
                // DO: asked every loop, so a new LAUNCH_RPM typed in the Dashboard counts at once.
                feeder.hold();
                launcher.spinUp();
                // CHECK + GO
                if (launcher.isUpToSpeed() && tipPauseOver) {
                    goTo(State.FEEDING, "up to speed");
                }
                // TIMEOUT
                if (timer.isTimeUp(LauncherConfig.SPIN_UP_TIMEOUT_MS)) {
                    log.warn("spin-up timeout");
                    Alerts.raise(String.format("LAUNCHER TIMEOUT: %.0f of %.0f RPM", launcher.getRpm(),
                            launcher.getTargetRpm()), true);
                    goTo(State.IDLE, "spin-up timeout");
                }
                break;

            case FEEDING:
                // DO
                feeder.feed();
                launcher.spinUp();
                // CHECK + GO (FEED_TOTAL_MS IS the timeout: feeding always ends)
                if (fedMs + timer.milliseconds() >= LauncherConfig.FEED_TOTAL_MS) {
                    goTo(State.IDLE, "hopper empty");
                } else if (!launcher.isFastEnoughToFeed()) {
                    goTo(State.SPINNING_UP, "flywheel too slow");
                } else if (!tipPauseOver) {
                    goTo(State.SPINNING_UP, "HIVE tipped");
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
        if (state == State.FEEDING) {   // leaving FEEDING: add up how long the windmill turned
            fedMs += timer.milliseconds();
            MatchLogger.event("LAUNCH", String.format("fed %.0f of %.0f ms", fedMs, LauncherConfig.FEED_TOTAL_MS));
        }
        log.record(state, newState, reason);
        state = newState;
        timer.reset();
        if (state == State.IDLE) {   // entering IDLE: windmill stopped, flywheel at idle
            feeder.hold();
            launcher.idle();
        }
    }

    @Override
    public boolean isBusy() {
        return state != State.IDLE;
    }

    public State getState() {
        return state;
    }

    /** How long the windmill has turned in this launch, in milliseconds. */
    public double getFedMs() {
        if (state == State.FEEDING) {
            return fedMs + timer.milliseconds();
        }
        return fedMs;
    }

    @Override
    public void addTelemetry(Telemetry t) {
        if (!TelemetryConfig.LAUNCH) {
            return;
        }
        log.addTo(t, state, timer, isBusy());
        if (isBusy()) {
            t.addData("Fed", String.format("%.1f of %.1f s", getFedMs() / 1000, LauncherConfig.FEED_TOTAL_MS / 1000));
        }
    }

    @Override
    public void addLogData() {
        MatchLogger.value("launch_state", state.ordinal());
    }
}
