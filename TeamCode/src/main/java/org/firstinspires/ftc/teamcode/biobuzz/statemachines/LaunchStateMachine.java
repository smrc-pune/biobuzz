package org.firstinspires.ftc.teamcode.biobuzz.statemachines;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LauncherConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;
import org.firstinspires.ftc.teamcode.biobuzz.drive.FieldPaths;
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
 * LaunchStateMachine.java  -  "launch N balls": spin up, feed one, recover, repeat.  ROLE: state machine
 * PATTERN: State pattern (enum + switch).
 *
 *           start(count)          up to speed             FEED_MS passed
 *   IDLE ------------> SPINNING_UP ----------> FEEDING ---------------> RECOVERING
 *    ^                    |   ^                                            |
 *    |  timeout / cancel  |   +----- more to launch, RECOVER_MS passed ----+
 *    +--------------------+                                                |
 *    +--------------------------- all launched ----------------------------+
 *
 * We can launch from ANYWHERE: the flywheel speed comes from the distance to the upward
 * CELL (Drive knows where we are, MatchInfo knows which CELL is up). Without a known pose
 * (S1) it uses LauncherConfig.DEFAULT_DISTANCE_IN. After a HIVE TIP it waits TIP_PAUSE_MS
 * before the next feed (game manual 10.5.1).
 * The robot cannot see how many balls it holds, so start(4) feeds 4 times; feeding an empty
 * slot does no harm.
 * Uses: Feeder, Launcher (commands only) and Drive (only to ask where we are).
 * Lives in: ShooterSubsystem, which builds it and calls update() every loop.
 *
 * TRACING A BUG: read the "Launch last change" line. "spin-up timeout" = the flywheel never
 * reached its speed: see Launcher's RPM vs target, and the battery.
 */
public class LaunchStateMachine implements StateMachine {

    // ==================== 1. STATES ====================
    public enum State {
        IDLE,          // feeder holds the balls back; flywheel at idle speed
        SPINNING_UP,   // flywheel speeding up for this distance; waits for "up to speed"
        FEEDING,       // feeder pushes one ball into the flywheel
        RECOVERING     // feeder pulls back; flywheel gets its speed back
    }

    private final Feeder feeder;
    private final Launcher launcher;
    private final Drive drive;
    private final StateTimer timer = new StateTimer();
    private final StateLog log = new StateLog("Launch");
    private State state = State.IDLE;
    private int toLaunch = 0;
    private int launched = 0;

    public LaunchStateMachine(Feeder feeder, Launcher launcher, Drive drive) {
        this.feeder = feeder;
        this.launcher = launcher;
        this.drive = drive;
    }

    // ==================== 2. COMMANDS ====================

    /** Launch this many balls. Ignored if already launching. */
    public void start(int count) {
        if (feeder.isFaulted() || launcher.isFaulted()) {
            log.warn("launch ignored: feeder or launcher is FAULTED");
            return;
        }
        if (isBusy()) {
            log.warn("launch ignored: already launching");
            return;
        }
        toLaunch = count;
        launched = 0;
        goTo(State.SPINNING_UP, "launch " + count);
    }

    /** Stop launching at once. */
    @Override
    public void cancel() { goTo(State.IDLE, "cancel"); }

    // ==================== 3. UPDATE (once per loop, ShooterSubsystem calls it) ====================

    @Override
    public void update() {
        switch (state) {
            case IDLE:
                // DO: nothing. (goTo() set the feeder to hold and the flywheel to idle.)
                break;

            case SPINNING_UP:
                // DO: the speed follows the distance, so it is right even if the robot moves.
                launcher.spinUpFor(getDistanceIn());
                // CHECK + GO
                boolean tipPauseOver = MatchInfo.secondsSinceTip() * 1000 >= LauncherConfig.TIP_PAUSE_MS;
                if (launcher.isUpToSpeed() && tipPauseOver) goTo(State.FEEDING, "up to speed");
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
                feeder.feedOne();
                launcher.spinUpFor(getDistanceIn());
                // CHECK + GO (this wait IS the timeout: it always ends)
                if (timer.isTimeUp(LauncherConfig.FEED_MS)) goTo(State.RECOVERING, "fed one");
                break;

            case RECOVERING:
                // DO
                feeder.hold();
                launcher.spinUpFor(getDistanceIn());
                // CHECK + GO (this wait IS the timeout: it always ends)
                if (timer.isTimeUp(LauncherConfig.RECOVER_MS)) {
                    launched++;
                    MatchLogger.event("LAUNCH", launched + " of " + toLaunch);
                    if (launched < toLaunch) goTo(State.SPINNING_UP, "next ball");
                    else goTo(State.IDLE, "all launched");
                }
                break;
        }
    }

    /** Inches to the upward CELL, or the default when we don't know where we are. */
    private double getDistanceIn() {
        if (!drive.isPoseKnown()) return LauncherConfig.DEFAULT_DISTANCE_IN;
        return FieldPaths.distanceToUpCellIn(drive.getPose(), MatchInfo.isRed());
    }

    // ==================== 4. BOOKKEEPING ====================

    /** The ONLY place the state changes. Going to the state we are already in does nothing. */
    private void goTo(State newState, String reason) {
        if (newState == state) return;
        log.record(state, newState, reason);
        state = newState;
        timer.reset();
        if (state == State.IDLE) {   // entering IDLE: balls held back, flywheel at idle
            feeder.hold();
            launcher.idle();
        }
    }

    @Override
    public boolean isBusy() { return state != State.IDLE; }

    public State getState() { return state; }

    @Override
    public void addTelemetry(Telemetry t) {
        if (!TelemetryConfig.LAUNCH) return;
        log.addTo(t, state, timer, isBusy());
        if (isBusy()) t.addData("Launched", launched + " of " + toLaunch);
    }

    @Override
    public void addLogData() { MatchLogger.value("launch_state", state.ordinal()); }
}
