package org.firstinspires.ftc.teamcode.biobuzz.parts;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/**
 * DriveBase.java  -  PART: the drivetrain, seen through Pedro Pathing.          ROLE: part
 *
 * This is the ONLY team class that calls Pedro's follower factory, Constants.create().
 * Everything else asks DriveBase in plain English:
 *
 *   driveManual(...)    drive with stick values
 *   followPath(...)     drive a path by itself (autonomous)
 *   getX / getY / getHeadingDegrees     where are we?
 *
 * A part makes NO decisions: the Drive subsystem decides which of these to call.
 *
 * STICK CONVENTION USED BY THIS CLASS (so the OpMode never needs minus signs):
 *   forward   + = away from the driver        (stick pushed UP)
 *   right     + = to the driver's right       (stick pushed RIGHT)
 *   clockwise + = turn clockwise              (stick pushed RIGHT)
 * Pedro itself uses "+ = left" and "+ = counter-clockwise", so we flip the signs
 * inside driveManual(). If strafing or turning feels backwards on the real robot,
 * THIS is the one place to fix it.
 *
 * TRACING A BUG: "Constants.create() returned null" at INIT means pedro/Constants.java
 * is still the Quickstart's empty stub. It is filled in during drive tuning (docs/TUNING.md).
 */
public class DriveBase {
    private final Follower follower;
    private Path lastPath = null;   // remembered so the Dashboard can draw it

    public DriveBase(HardwareMap hardwareMap) {
        follower = Constants.create(hardwareMap);
        if (follower == null) {
            throw new RuntimeException("DRIVE NOT SET UP: pedro/Constants.create() returned null."
                    + " Constants.java is still the Quickstart stub. Fill it in during drive tuning"
                    + " (docs/TUNING.md) before running a BioBuzz OpMode.");
        }
    }

    /**
     * Drive using stick values (each -1 to 1).
     * @param fieldCentric     true = "forward" means away from the drivers, however the robot faces.
     * @param driverHeadingDeg the robot's heading when it faces away from the drivers.
     *
     * Pedro's ManualDrive.fieldCentric() turns the stick direction by -(heading + offset).
     * Using offset = -driverHeading means: when the robot faces away from the drivers,
     * the sticks are not turned at all, and "forward" is simply the robot's forward.
     */
    public void driveManual(double forward, double right, double clockwise,
                            boolean fieldCentric, double driverHeadingDeg) {
        DrivePowers powers = new DrivePowers(forward, -right, -clockwise);   // Pedro: +left, +counter-clockwise
        if (fieldCentric) {
            powers = ManualDrive.fieldCentric(powers, follower.pose().heading(),
                    -Math.toRadians(driverHeadingDeg));
        }
        follower.manual(powers);
    }

    /** Start following a path. Pedro holds the end position when it finishes. */
    public void followPath(Path path) {
        lastPath = path;
        follower.follow(path);
    }

    /** @return true while a path is still being followed. */
    public boolean isFollowingPath() { return follower.following(); }

    /** Stop where we are and hold this spot (used when a path times out). */
    public void holdCurrentPose() { follower.hold(follower.pose()); }

    /** Tell Pedro where the robot really is (start of auto, pose from auto, a correction). */
    public void setPose(Pose pose) { follower.setPose(pose); }

    /** Tell Pedro which way the robot faces, keeping x and y. */
    public void setHeadingDegrees(double headingDeg) { follower.setHeading(Math.toRadians(headingDeg)); }

    public Pose getPose() { return follower.pose(); }
    public double getX() { return follower.pose().x(); }
    public double getY() { return follower.pose().y(); }

    /** @return heading from -180 to 180, so "away" reads near 0 instead of jumping between 0 and 360. */
    public double getHeadingDegrees() {
        return AngleUnit.normalizeDegrees(Math.toDegrees(follower.pose().heading()));
    }

    /** @return the point at fraction t (0 = start, 1 = end) of the last path, or null if none. */
    public Pose getPathPoint(double t) {
        if (lastPath == null) return null;
        return lastPath.get(t);
    }

    /** @return what Pedro is doing: FOLLOW, HOLD, MANUAL or IDLE. */
    public String getFollowerMode() { return follower.mode().toString(); }

    /** Pedro must be updated EVERY loop: it reads the sensors and sets the motor powers. */
    public void update() { follower.update(); }

    public void addTelemetry(Telemetry t) {
        t.addData("Pose x / y (in)", String.format("%.1f / %.1f", getX(), getY()));
        t.addData("Pose heading (deg)", String.format("%.1f", getHeadingDegrees()));
        t.addData("Follower mode", getFollowerMode());
    }
}
