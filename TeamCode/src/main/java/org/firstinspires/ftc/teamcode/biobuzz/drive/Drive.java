package org.firstinspires.ftc.teamcode.biobuzz.drive;

import com.pedropathing.controllers.PIDController;
import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.DriveConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateLog;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/**
 * MENTORS: you do not need to change this file.
 *
 * Drive.java  -  the drivetrain: drive with the sticks, follow a path, know where we are.  ROLE: drive
 * PATTERN: Facade. Pedro Pathing (off the shelf) does the hard math; this class gives it
 * simple names. It is the ONLY team class that calls Pedro's Constants.create().
 *
 * Drive has NO states. The OpMode decides who drives:
 *   driveWithSticks(...)          the driver drives               (S1)
 *   driveWithSticksAimedAt(...)   the driver drives, Pedro keeps it facing the upward CELL
 *   followPath(...)               the robot drives a path itself  (S2 assists, S3 Auto)
 * Moving a stick while a path runs simply calls driveWithSticks(), which takes the robot back.
 *
 * STICK SIGNS (so the OpMode never needs minus signs):
 *   forward + = away from the driver, right + = to the driver's right, clockwise + = turn clockwise.
 * Pedro uses "+ = left" and "+ = counter-clockwise", so driveWithSticks() flips two signs.
 * If strafing or turning feels backwards on the real robot, THIS is the place to fix it.
 *
 * FIELD-CENTRIC: Pedro's ManualDrive.fieldCentric() turns the stick direction by
 * -(heading + offset). Using offset = -driverHeading means: when the robot faces away from
 * the drivers, the sticks are not turned at all.
 *
 * POSE KNOWN: true after a real starting pose (from Auto) or "set pose here". False when
 * TeleOp only assumes "facing away from the drivers" (S1), or after a Pinpoint fault
 * (helpers/HealthMonitor). Assist buttons need it.
 *
 * TRACING A BUG: "DRIVE NOT SET UP" at INIT means pedro/Constants.java is still the
 * Quickstart stub (filled in at drive tuning, docs/TUNING.md).
 */
public class Drive {

    private final Follower follower;
    private final GoBildaPinpointDriver pinpoint;     // only to read its status (HealthMonitor)
    private final StateLog log = new StateLog("Drive");   // mode changes and warnings: telemetry + match log
    private boolean fieldCentric = true;
    private boolean isRed = true;
    private boolean poseKnown = false;
    private Path currentPath = null;                  // kept so the Dashboard can draw it
    private int headingSetCount = 0;                  // +1 each time WE set the heading (not a "jump")
    private double stickForward = 0, stickRight = 0, stickClockwise = 0;   // for the match log
    private final PIDController aimController = new PIDController(DriveConfig.AIM_P, 0, DriveConfig.AIM_D);

    public Drive(HardwareMap hardwareMap) {
        follower = Constants.create(hardwareMap);
        if (follower == null) {
            throw new RuntimeException("DRIVE NOT SET UP: pedro/Constants.create() returned null."
                    + " Constants.java is still the Quickstart stub. Fill it in during drive tuning"
                    + " (docs/TUNING.md) before running a BioBuzz OpMode.");
        }
        // The SAME Pinpoint object Pedro reads (the hardwareMap keeps one object per device).
        // We only read its status; Pedro keeps it updated in follower.update().
        pinpoint = hardwareMap.get(GoBildaPinpointDriver.class, HardwareNames.PINPOINT);
    }

    // ==================== SETUP (INIT and start only) ====================

    /** Which alliance we are on. It picks the "away from the drivers" heading. */
    public void setAlliance(boolean isRed) { this.isRed = isRed; }

    /** The robot is at a known place (from Auto). Assists (S2) can be used. */
    public void setStartingPose(Pose pose) {
        follower.setPose(pose);
        poseKnown = true;
        headingSetCount++;
    }

    /** No pose from Auto: only the heading is assumed (robot faces away from the drivers). S1 only. */
    public void setStartingPoseFacingAway() {
        follower.setPose(new Pose(0, 0, Math.toRadians(getDriverHeadingDeg())));
        poseKnown = false;
        headingSetCount++;
    }

    // ==================== DRIVER DRIVING (S1) ====================

    /** Drive with stick values, each -1 to 1 (deadzone already applied by GamepadInput). */
    public void driveWithSticks(double forward, double right, double clockwise) {
        boolean sticksMoved = forward != 0 || right != 0 || clockwise != 0;
        if (sticksMoved && follower.following()) MatchLogger.event("ASSIST_CANCELLED", "stick");
        stickForward = forward;
        stickRight = right;
        stickClockwise = clockwise;
        DrivePowers powers = new DrivePowers(forward, -right, -clockwise);   // Pedro: +left, +counter-clockwise
        if (fieldCentric) {
            powers = ManualDrive.fieldCentric(powers, follower.pose().heading(),
                    -Math.toRadians(getDriverHeadingDeg()));
        }
        follower.manual(powers);
        currentPath = null;
    }

    /**
     * Drive with the left stick while Pedro turns the robot to face targetHeadingDeg (the aim
     * button). Pedro's ManualDrive.headingLock() replaces the turn power. In robot-centric mode
     * the heading can't be trusted, so the robot just drives without turning.
     */
    public void driveWithSticksAimedAt(double forward, double right, double targetHeadingDeg) {
        if (!fieldCentric) {
            driveWithSticks(forward, right, 0);
            return;
        }
        if (follower.following()) MatchLogger.event("ASSIST_CANCELLED", "aim");
        stickForward = forward;
        stickRight = right;
        stickClockwise = 0;
        aimController.kP = DriveConfig.AIM_P;   // so Dashboard edits count at once
        aimController.kD = DriveConfig.AIM_D;
        DrivePowers powers = ManualDrive.fieldCentric(new DrivePowers(forward, -right, 0),
                follower.pose().heading(), -Math.toRadians(getDriverHeadingDeg()));
        powers = ManualDrive.headingLock(follower, aimController, powers, Math.toRadians(targetHeadingDeg));
        follower.manual(powers);
        currentPath = null;
    }

    /** "The robot faces away from the drivers now." Fixes drift; turns field-centric back on. */
    public void resetHeading() {
        follower.setHeading(Math.toRadians(getDriverHeadingDeg()));
        headingSetCount++;
        log.record("heading", String.format("%.0f deg", getDriverHeadingDeg()), "driver pressed Y");
        useFieldCentric("heading reset");
    }

    /** Switch between field-centric and robot-centric by hand (driver's X). */
    public void switchDriveMode() {
        if (fieldCentric) useRobotCentric("driver switched");
        else useFieldCentric("driver switched");
    }

    /** Robot-centric: "forward" is the robot's own front. */
    public void useRobotCentric(String reason) {
        if (!fieldCentric) return;
        fieldCentric = false;
        log.record("FIELD-CENTRIC", "ROBOT-CENTRIC", reason);
    }

    /** Field-centric (the normal mode): "forward" is away from the drivers. */
    public void useFieldCentric(String reason) {
        if (fieldCentric) return;
        fieldCentric = true;
        log.record("ROBOT-CENTRIC", "FIELD-CENTRIC", reason);
    }

    public boolean isFieldCentric() { return fieldCentric; }

    // ==================== PATH DRIVING (S2 assists, S3 Auto) ====================

    /** Drive this path by itself. Pedro holds the robot still at the end. */
    public void followPath(Path path) {
        currentPath = path;
        follower.follow(path);
    }

    /** true while Pedro is driving a path or holding the robot still at the end of one. */
    public boolean isDrivingItself() { return follower.following() || follower.holding(); }

    /** true once the path has reached its end (or there is no path). */
    public boolean isPathDone() { return !follower.following(); }

    /** Stop where we are and hold this spot (an Auto step that ran out of time). */
    public void holdHere() { follower.hold(follower.pose()); }

    /** Stop driving by itself and wait for the sticks. */
    public void stopDrivingItself() { driveWithSticks(0, 0, 0); }

    // ==================== WHERE ARE WE? ====================

    /** Tell Pedro where the robot really is ("set pose here", a correction). Assists can be used. */
    public void setPose(Pose pose, String reason) {
        log.record(poseText(follower.pose()), poseText(pose), reason);
        follower.setPose(pose);
        poseKnown = true;
        headingSetCount++;
    }

    /** We can no longer trust x and y (for example a Pinpoint fault). Assists are refused. */
    public void forgetPose(String reason) {
        if (!poseKnown) return;
        poseKnown = false;
        log.record("pose known", "pose UNKNOWN", reason);
    }

    public boolean isPoseKnown() { return poseKnown; }

    public Pose getPose() { return follower.pose(); }
    public double getX() { return follower.pose().x(); }
    public double getY() { return follower.pose().y(); }

    /** @return heading from -180 to 180, so "away" reads near 0 instead of jumping between 0 and 360. */
    public double getHeadingDegrees() {
        return AngleUnit.normalizeDegrees(Math.toDegrees(follower.pose().heading()));
    }

    /** The heading (deg) the robot has when it faces away from this alliance's drivers. */
    public double getDriverHeadingDeg() {
        if (isRed) return DriveConfig.RED_DRIVER_HEADING_DEG;
        return DriveConfig.BLUE_DRIVER_HEADING_DEG;
    }

    /** READY when all is well. Pedro reads the Pinpoint in update(); this just looks at the answer. */
    public GoBildaPinpointDriver.DeviceStatus getPinpointStatus() { return pinpoint.getDeviceStatus(); }

    /** Goes up by one every time our code sets the heading, so HealthMonitor knows it was not a jump. */
    public int getHeadingSetCount() { return headingSetCount; }

    /** Show a short message on telemetry, for example "assist refused: position unknown". */
    public void warn(String message) { log.warn(message); }

    // ==================== EVERY LOOP ====================

    /** Pedro must be updated EVERY loop: it reads the Pinpoint and sets the motor powers. */
    public void update() { follower.update(); }

    public void addTelemetry(Telemetry t) {
        String mode = "FIELD-centric";
        if (!fieldCentric) mode = "ROBOT-centric";
        String who = "driver";
        if (isDrivingItself()) who = "robot (path)";
        t.addData("Drive", mode + ", driven by " + who + ", Pinpoint " + getPinpointStatus());
        t.addData("Pose x / y / heading", String.format("%.1f in / %.1f in / %.1f deg",
                getX(), getY(), getHeadingDegrees()));
        if (!log.getWarning().isEmpty()) t.addData("Drive WARNING", log.getWarning());
        if (TelemetryConfig.HISTORY && !log.getLastChange().isEmpty()) {
            t.addData("Drive last change", log.getLastChange());
        }
        if (TelemetryConfig.DETAILS) t.addData("Pedro mode", follower.mode().toString());
    }

    /** Drive columns of a match-log DATA row. */
    public void addLogData() {
        MatchLogger.value("x_in", getX());
        MatchLogger.value("y_in", getY());
        MatchLogger.value("heading_deg", getHeadingDegrees());
        MatchLogger.value("heading_vel_dps", Math.toDegrees(follower.velocity().omega));
        MatchLogger.value("pinpoint_status", getPinpointStatus().ordinal());   // 1 = READY
        MatchLogger.value("follower_mode", follower.mode().ordinal());         // 0 FOLLOW 1 HOLD 2 MANUAL 3 IDLE
        MatchLogger.value("field_centric", fieldCentric);
        MatchLogger.value("pose_known", poseKnown);
        MatchLogger.value("stick_forward", stickForward);
        MatchLogger.value("stick_right", stickRight);
        MatchLogger.value("stick_turn", stickClockwise);
    }

    /** Draw the robot, and the path while the robot drives itself, on the Dashboard field. */
    public void addDrawing() {
        FieldDrawing.drawRobot(getX(), getY(), getHeadingDegrees());
        if (currentPath != null && isDrivingItself()) FieldDrawing.drawPath(currentPath);
    }

    private String poseText(Pose pose) {
        return String.format("(%.0f, %.0f, %.0f deg)", pose.x(), pose.y(),
                AngleUnit.normalizeDegrees(Math.toDegrees(pose.heading())));
    }
}
