package org.firstinspires.ftc.teamcode.biobuzz.drive;

import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * STUDENTS: you will change this file.
 *
 * DriveToSpot.java  -  the robot drives itself to a spot on the field (S2).   ROLE: drive helper
 *
 *   OpMode:  robot.driveToSpot.launchSpot()    robot.driveToSpot.loadingZone()
 *
 * Each one checks two things before it starts, and says no (false, with a WARNING on
 * telemetry) if either fails:
 *   1. Do we know where the robot is?   (no = S1 only: "set pose here" first,
 *                                        robot.drive.setPoseAtResetCorner())
 *   2. Does the path miss the HIVE?     (FieldPaths.avoidsHive)
 * The spots come from FieldPaths (written for RED in FieldConfig). The driver takes over
 * at any moment by moving a stick (BioBuzzTeleOp.handleDriver()).
 * ADD A SPOT HERE: a method like launchSpot() with a new path from FieldPaths.
 */
public class DriveToSpot {

    private final Drive drive;

    public DriveToSpot(Drive drive) {
        this.drive = drive;
    }

    /** Drive to our favourite launch spot. @return false if refused. */
    public boolean launchSpot() {
        if (!isPoseKnown("launch spot")) {
            return false;
        }
        return start(FieldPaths.toLaunchSpot(drive.getPose(), MatchInfo.isRed()), "launch spot");
    }

    /** Drive into our LOADING ZONE (to park, or to pick up NECTAR). @return false if refused. */
    public boolean loadingZone() {
        if (!isPoseKnown("loading zone")) {
            return false;
        }
        return start(FieldPaths.toLoadingZone(drive.getPose(), MatchInfo.isRed()), "loading zone");
    }

    // ==================== the two checks ====================

    private boolean isPoseKnown(String name) {
        if (drive.isPoseKnown()) {
            return true;
        }
        drive.warn(name + " refused: position unknown (S1). Hold D-pad left in the reset corner.");
        return false;
    }

    private boolean start(Path path, String name) {
        if (!FieldPaths.avoidsHive(path)) {
            drive.warn(name + " refused: the path would hit the HIVE. Drive around it first.");
            return false;
        }
        drive.followPath(path);
        MatchLogger.event("ASSIST", name);
        return true;
    }
}
