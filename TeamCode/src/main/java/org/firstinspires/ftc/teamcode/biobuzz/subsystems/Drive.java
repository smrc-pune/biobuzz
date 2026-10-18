package org.firstinspires.ftc.teamcode.biobuzz.subsystems;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.DriveConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateLog;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;
import org.firstinspires.ftc.teamcode.biobuzz.parts.DriveBase;

/**
 * Drive.java  -  BRAIN: state machine for driving.                            ROLE: brain
 *
 * Pedro Pathing does the hard math. This brain decides WHICH job the drivetrain is doing:
 *
 *                     followPath(path)                path finished / timeout
 *   DRIVER_CONTROL  ------------------>  FOLLOWING_PATH  ------------------>  HOLDING
 *        ^   ^                                 |                                 |
 *        |   +------ stick moved --------------+                                 |
 *        +---------- stick moved (or takeDriverControl()) -----------------------+
 *
 * FIELD-CENTRIC (the normal mode): pushing the stick away from the drivers moves the
 * robot away from them, whichever way it faces. ROBOT-CENTRIC: "forward" is the robot's
 * own front. It is only used when the driver presses X, or (step 4) when the heading
 * cannot be trusted. Y says "the robot faces away from the drivers now" and turns
 * field-centric back on.
 *
 * isBusy() is true ONLY while FOLLOWING_PATH: being driven by a person is not a "job".
 *
 * TRACING A BUG: look at "Drive" on telemetry (state and why it changed), then
 * "Drive mode". If field-centric points the wrong way, press Y facing away and check
 * that "Pose heading" then reads the driver heading in DriveConfig.
 * This class never reads a gamepad: stick values arrive through setDriverInput().
 */
public class Drive {

    // ==================== 1. STATES ====================
    public enum State {
        DRIVER_CONTROL,   // the driver's sticks move the robot
        FOLLOWING_PATH,   // the robot drives a path by itself
        HOLDING           // the path is done (or timed out): Pedro holds the robot still
    }

    private static final int PATH_DRAW_POINTS = 20;   // points used to draw a path on the Dashboard

    private State state = State.DRIVER_CONTROL;
    private final StateTimer timer = new StateTimer();
    private final StateLog log = new StateLog();
    private final DriveBase driveBase;   // the hardware (via Pedro)

    // The latest requests, remembered until update() uses them.
    private double forward, right, clockwise;
    private boolean slowMode = false;
    private boolean fieldCentric = true;
    private boolean isRed = false;
    private boolean headingResetRequested = false;
    private Path pathToStart = null;

    public Drive(DriveBase driveBase) {
        this.driveBase = driveBase;
    }

    // ==================== SETUP (call in init only) ====================

    /** Which alliance we are on. It picks the "away from the drivers" heading. */
    public void setAlliance(boolean isRed) { this.isRed = isRed; }

    /** Tell Pedro where the robot starts (for example AutoPaths.start, or the pose from auto). */
    public void setStartingPose(Pose pose) { driveBase.setPose(pose); }

    /** No pose from auto: assume the robot was placed facing away from the drivers. */
    public void setStartingPoseFacingAway() {
        driveBase.setPose(new Pose(0, 0, Math.toRadians(getDriverHeadingDeg())));
    }

    // ==================== 2. COMMANDS ====================

    /**
     * Give the driver's stick values (each -1 to 1). Call every loop in teleop.
     * @param forward   + = away from the driver
     * @param right     + = to the driver's right
     * @param clockwise + = turn clockwise
     */
    public void setDriverInput(double forward, double right, double clockwise) {
        this.forward = deadzone(forward);
        this.right = deadzone(right);
        this.clockwise = deadzone(clockwise);
    }

    /** Slow mode = smaller speeds for precise driving. */
    public void setSlowMode(boolean slowMode) { this.slowMode = slowMode; }

    /** "The robot faces away from the drivers now." Fixes drift; turns field-centric back on. */
    public void resetHeading() { headingResetRequested = true; }

    /** Switch between field-centric and robot-centric by hand (driver's X). */
    public void switchDriveMode() {
        if (fieldCentric) useRobotCentric("driver switched");
        else useFieldCentric("driver switched");
    }

    /** Drive robot-centric: the sticks are not turned by the heading. */
    public void useRobotCentric(String reason) {
        if (!fieldCentric) return;
        fieldCentric = false;
        log.record("FIELD-CENTRIC", "ROBOT-CENTRIC", reason);
    }

    /** Drive field-centric (the normal mode). */
    public void useFieldCentric(String reason) {
        if (fieldCentric) return;
        fieldCentric = true;
        log.record("ROBOT-CENTRIC", "FIELD-CENTRIC", reason);
    }

    /** Drive this path by itself. Allowed from any state; a new path replaces the old one. */
    public void followPath(Path path) {
        pathToStart = path;
        goTo(State.FOLLOWING_PATH, "followPath() command");
    }

    /** Give the sticks control back. Allowed from any state. */
    public void takeDriverControl() { goTo(State.DRIVER_CONTROL, "takeDriverControl() command"); }

    // ==================== 3. THE STATE MACHINE ====================

    public void update() {
        if (headingResetRequested) {                                   // a request from any state
            driveBase.setHeadingDegrees(getDriverHeadingDeg());
            headingResetRequested = false;
            useFieldCentric("heading reset");
        }

        switch (state) {
            case DRIVER_CONTROL:
                double moveScale = DriveConfig.SPEED_NORMAL;                                   // DO
                double turnScale = DriveConfig.TURN_SPEED_NORMAL;
                if (slowMode) {
                    moveScale = DriveConfig.SPEED_SLOW;
                    turnScale = DriveConfig.TURN_SPEED_SLOW;
                }
                driveBase.driveManual(forward * moveScale, right * moveScale, clockwise * turnScale,
                        fieldCentric, getDriverHeadingDeg());
                break;                                                     // (commands leave this state)

            case FOLLOWING_PATH:
                if (pathToStart != null) {                                                     // DO (once)
                    driveBase.followPath(pathToStart);
                    pathToStart = null;
                }
                if (isStickMoved()) {                                                          // CHECK
                    goTo(State.DRIVER_CONTROL, "stick moved");                                // GO
                } else if (!driveBase.isFollowingPath()) {
                    goTo(State.HOLDING, "path finished");
                } else if (timer.isTimeUp(DriveConfig.PATH_TIMEOUT_MS)) {
                    driveBase.holdCurrentPose();
                    log.warn("path timeout: holding where it stopped");
                    goTo(State.HOLDING, "path timeout");
                }
                break;

            case HOLDING:
                // Nothing to DO: Pedro holds the position by itself.
                if (isStickMoved()) {                                                          // CHECK
                    goTo(State.DRIVER_CONTROL, "stick moved");                                // GO
                }
                break;
        }

        // Pedro must be updated EVERY loop, whatever the state.
        driveBase.update();
    }

    /** The heading (deg) the robot has when it faces away from this alliance's drivers. */
    public double getDriverHeadingDeg() {
        if (isRed) return DriveConfig.RED_DRIVER_HEADING_DEG;
        return DriveConfig.BLUE_DRIVER_HEADING_DEG;
    }

    /** true when any stick is past the deadzone (the driver wants control). */
    private boolean isStickMoved() { return forward != 0 || right != 0 || clockwise != 0; }

    /** Stick values smaller than the deadzone count as 0, so the robot doesn't creep. */
    private double deadzone(double value) {
        if (Math.abs(value) < DriveConfig.STICK_DEADZONE) return 0;
        return value;
    }

    // ==================== 4. BOOKKEEPING (same in every brain) ====================

    private void goTo(State next, String reason) {
        if (next == state) return;
        log.record(state, next, reason);
        state = next;
        timer.reset();
    }

    /** "Busy" = following a path. (A person driving is not a job.) */
    public boolean isBusy() { return state == State.FOLLOWING_PATH; }

    public State getState() { return state; }

    public boolean isFieldCentric() { return fieldCentric; }

    public double getX() { return driveBase.getX(); }
    public double getY() { return driveBase.getY(); }
    public double getHeadingDegrees() { return driveBase.getHeadingDegrees(); }
    public Pose getPose() { return driveBase.getPose(); }

    public void addTelemetry(Telemetry t) {
        log.addTo(t, "Drive", state, timer, isBusy());
        String mode = "FIELD-centric";
        if (!fieldCentric) mode = "ROBOT-centric";
        if (slowMode) mode = mode + ", SLOW";
        t.addData("Drive mode", mode);
        t.addData("Pose x / y / heading", String.format("%.1f in / %.1f in / %.1f deg",
                getX(), getY(), getHeadingDegrees()));
        if (TelemetryConfig.PARTS) driveBase.addTelemetry(t);
    }

    /** Draw the robot (and the path while following one) on the Dashboard field. */
    public void addDrawing() {
        FieldDrawing.drawRobot(getX(), getY(), getHeadingDegrees());
        if (state != State.FOLLOWING_PATH) return;
        double[] xs = new double[PATH_DRAW_POINTS + 1];
        double[] ys = new double[PATH_DRAW_POINTS + 1];
        for (int i = 0; i <= PATH_DRAW_POINTS; i++) {
            Pose point = driveBase.getPathPoint((double) i / PATH_DRAW_POINTS);
            if (point == null) return;
            xs[i] = point.x();
            ys[i] = point.y();
        }
        FieldDrawing.drawPath(xs, ys);
    }
}
