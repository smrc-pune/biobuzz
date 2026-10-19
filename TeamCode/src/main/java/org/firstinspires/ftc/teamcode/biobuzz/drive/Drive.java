package org.firstinspires.ftc.teamcode.biobuzz.drive;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.DriveConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
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
 * TeleOp only assumes "facing away from the drivers" (S1). Assist buttons need it.
 *
 * TRACING A BUG: "DRIVE NOT SET UP" at INIT means pedro/Constants.java is still the
 * Quickstart stub (filled in at drive tuning, docs/TUNING.md).
 */
public class Drive {

    private static final int PATH_DRAW_POINTS = 20;   // points used to draw a path on the Dashboard

    private final Follower follower;
    private final StateLog log = new StateLog();      // drive-mode changes and warnings, for telemetry
    private boolean fieldCentric = true;
    private boolean isRed = true;
    private boolean poseKnown = false;
    private Path currentPath = null;                  // kept so the Dashboard can draw it

    public Drive(HardwareMap hardwareMap) {
        follower = Constants.create(hardwareMap);
        if (follower == null) {
            throw new RuntimeException("DRIVE NOT SET UP: pedro/Constants.create() returned null."
                    + " Constants.java is still the Quickstart stub. Fill it in during drive tuning"
                    + " (docs/TUNING.md) before running a BioBuzz OpMode.");
        }
    }

    // ==================== SETUP (INIT and start only) ====================

    /** Which alliance we are on. It picks the "away from the drivers" heading. */
    public void setAlliance(boolean isRed) { this.isRed = isRed; }

    /** The robot is at a known place (from Auto). Assists (S2) can be used. */
    public void setStartingPose(Pose pose) {
        follower.setPose(pose);
        poseKnown = true;
    }

    /** No pose from Auto: only the heading is assumed (robot faces away from the drivers). S1 only. */
    public void setStartingPoseFacingAway() {
        follower.setPose(new Pose(0, 0, Math.toRadians(getDriverHeadingDeg())));
        poseKnown = false;
    }

    // ==================== DRIVER DRIVING (S1) ====================

    /** Drive with stick values, each -1 to 1 (deadzone already applied by GamepadInput). */
    public void driveWithSticks(double forward, double right, double clockwise) {
        DrivePowers powers = new DrivePowers(forward, -right, -clockwise);   // Pedro: +left, +counter-clockwise
        if (fieldCentric) {
            powers = ManualDrive.fieldCentric(powers, follower.pose().heading(),
                    -Math.toRadians(getDriverHeadingDeg()));
        }
        follower.manual(powers);
        currentPath = null;
    }

    /** "The robot faces away from the drivers now." Fixes drift; turns field-centric back on. */
    public void resetHeading() {
        follower.setHeading(Math.toRadians(getDriverHeadingDeg()));
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
        t.addData("Drive", mode + ", driven by " + who);
        t.addData("Pose x / y / heading", String.format("%.1f in / %.1f in / %.1f deg",
                getX(), getY(), getHeadingDegrees()));
        if (!log.getWarning().isEmpty()) t.addData("Drive WARNING", log.getWarning());
        if (TelemetryConfig.HISTORY && !log.getLastChange().isEmpty()) {
            t.addData("Drive last change", log.getLastChange());
        }
        if (TelemetryConfig.DETAILS) t.addData("Pedro mode", follower.mode().toString());
    }

    /** Draw the robot, and the path while the robot drives itself, on the Dashboard field. */
    public void addDrawing() {
        FieldDrawing.drawRobot(getX(), getY(), getHeadingDegrees());
        if (currentPath == null || !isDrivingItself()) return;
        double[] xs = new double[PATH_DRAW_POINTS + 1];
        double[] ys = new double[PATH_DRAW_POINTS + 1];
        for (int i = 0; i <= PATH_DRAW_POINTS; i++) {
            Pose point = currentPath.get((double) i / PATH_DRAW_POINTS);
            xs[i] = point.x();
            ys[i] = point.y();
        }
        FieldDrawing.drawPath(xs, ys);
    }

    private String poseText(Pose pose) {
        return String.format("(%.0f, %.0f, %.0f deg)", pose.x(), pose.y(),
                AngleUnit.normalizeDegrees(Math.toDegrees(pose.heading())));
    }
}
