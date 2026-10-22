package org.firstinspires.ftc.teamcode.biobuzz.drive;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.biobuzz.configurations.AutoConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LauncherConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;

/**
 * STUDENTS: you will change this file.
 *
 * FieldPaths.java  -  every path the robot drives by itself.                   ROLE: paths
 * PATTERN: Factory method. One method makes each kind of path, so TeleOp's assist
 * buttons and Auto always get the same path to the same place.
 *
 * Places come from configurations/FieldConfig.java, written for RED. place(...) turns
 * them for BLUE. This file and drive/Drive.java are the only ones that use Pedro.
 *
 * PEDRO WORDS USED HERE:
 *   new Pose(x, y, heading)          a place and the way the robot faces (heading in RADIANS)
 *   Paths.line(from, to)             a straight line
 *   Paths.curve(from, middle, to)    a smooth curve pulled toward the middle point
 *   Paths.through(a, b, c)           a path that passes through every point
 *   .linear(from, to)                turn smoothly from the start heading to the end heading
 * Every Pedro path MUST say how the robot turns (.linear here), or Pedro stops with an error.
 *
 * TO ADD AN ASSIST: add the place to FieldConfig, a method here, a method in Robot, and
 * one line in BioBuzzTeleOp.handleDriver().
 */
public class FieldPaths {

    private static final int CHECK_POINTS = 20;   // points checked along a path for the HIVE

    // ==================== PLACES ====================

    /** A RED place (inches, degrees). For BLUE it is turned half a turn around the field's middle. */
    public static Pose place(double xIn, double yIn, double headingDeg, boolean isRed) {
        if (isRed) return new Pose(xIn, yIn, Math.toRadians(headingDeg));
        // BLUE: flip across the middle line both ways = half a turn around the middle point.
        double centre = FieldConfig.FIELD_CENTRE_IN;
        return PoseFactory.degrees().mirrorX(centre).mirrorY(centre).of(xIn, yIn, headingDeg);
    }

    public static Pose launchSpot(boolean isRed) {
        return place(FieldConfig.LAUNCH_SPOT_X_IN, FieldConfig.LAUNCH_SPOT_Y_IN,
                FieldConfig.LAUNCH_SPOT_HEADING_DEG, isRed);
    }

    public static Pose loadingPark(boolean isRed) {
        return place(FieldConfig.LOADING_PARK_X_IN, FieldConfig.LOADING_PARK_Y_IN,
                FieldConfig.LOADING_PARK_HEADING_DEG, isRed);
    }

    public static Pose loadingApproach(boolean isRed) {
        return place(FieldConfig.LOADING_APPROACH_X_IN, FieldConfig.LOADING_APPROACH_Y_IN,
                FieldConfig.LOADING_PARK_HEADING_DEG, isRed);
    }

    public static Pose resetCorner(boolean isRed) {
        return place(FieldConfig.RESET_CORNER_X_IN, FieldConfig.RESET_CORNER_Y_IN,
                FieldConfig.RESET_CORNER_HEADING_DEG, isRed);
    }

    /** Where Autonomous starts (AutoConfig.START), turned for BLUE. */
    public static Pose startPose(AutoConfig.Start start, boolean isRed) {
        if (start == AutoConfig.Start.WALL_MIDDLE) {
            return place(FieldConfig.START_MIDDLE_X_IN, FieldConfig.START_MIDDLE_Y_IN,
                    FieldConfig.START_MIDDLE_HEADING_DEG, isRed);
        }
        return place(FieldConfig.START_NEAR_GARDEN_X_IN, FieldConfig.START_NEAR_GARDEN_Y_IN,
                FieldConfig.START_NEAR_GARDEN_HEADING_DEG, isRed);
    }

    /** The CELL that faces up now (MatchInfo knows which), turned for BLUE. Heading is not used. */
    public static Pose upCellPose(boolean isRed) {
        if (MatchInfo.isCellAUp()) return place(FieldConfig.CELL_A_X_IN, FieldConfig.CELL_A_Y_IN, 0, isRed);
        return place(FieldConfig.CELL_B_X_IN, FieldConfig.CELL_B_Y_IN, 0, isRed);
    }

    /** Inches from the robot's centre to the upward CELL (the launcher's speed depends on it). */
    public static double distanceToUpCellIn(Pose robot, boolean isRed) {
        Pose cell = upCellPose(isRed);
        return Math.hypot(cell.x() - robot.x(), cell.y() - robot.y());
    }

    /** The heading (degrees) that points the launcher at the upward CELL from this spot. */
    public static double aimHeadingDeg(Pose robot, boolean isRed) {
        Pose cell = upCellPose(isRed);
        double towardCellDeg = Math.toDegrees(Math.atan2(cell.y() - robot.y(), cell.x() - robot.x()));
        return towardCellDeg + LauncherConfig.AIM_OFFSET_DEG;
    }

    // ==================== PATHS ====================

    /** A straight line from where the robot is to our launch spot. */
    public static Path toLaunchSpot(Pose from, boolean isRed) {
        Pose to = launchSpot(isRed);
        return Paths.line(from, to).linear(from, to);
    }

    /** A curve from where the robot is, toward the approach point, into our LOADING ZONE. */
    public static Path toLoadingZone(Pose from, boolean isRed) {
        Pose to = loadingPark(isRed);
        return Paths.curve(from, loadingApproach(isRed), to).linear(from, to);
    }

    /** Autonomous LEAVE: off the wall to our launch spot, turning to face the upward CELL. */
    public static Path leaveWall(Pose start, boolean isRed) {
        Pose spot = launchSpot(isRed);
        Pose to = new Pose(spot.x(), spot.y(), Math.toRadians(aimHeadingDeg(spot, isRed)));
        return Paths.line(start, to).linear(start, to);
    }

    /** Autonomous collect: to the start of our GARDEN line, then along it (the intake is on). */
    public static Path throughGarden(Pose from, boolean isRed) {
        Pose gardenStart = place(FieldConfig.GARDEN_START_X_IN, FieldConfig.GARDEN_START_Y_IN,
                FieldConfig.GARDEN_HEADING_DEG, isRed);
        Pose gardenEnd = place(FieldConfig.GARDEN_END_X_IN, FieldConfig.GARDEN_END_Y_IN,
                FieldConfig.GARDEN_HEADING_DEG, isRed);
        return Paths.through(from, gardenStart, gardenEnd).linear(from, gardenEnd);
    }

    /** Back from the GARDEN to the launch spot, facing the upward CELL (second launch). */
    public static Path gardenToLaunchSpot(Pose from, boolean isRed) {
        return leaveWall(from, isRed);
    }

    // ==================== FIELD CHECK PATHS (fieldchecks/): they start wherever the robot is ====================

    /** Field Check 08: straight ahead (the way the robot faces) for distanceIn, same heading. */
    public static Path straightAhead(Pose from, double distanceIn) {
        Pose to = new Pose(from.x() + distanceIn * Math.cos(from.heading()),
                from.y() + distanceIn * Math.sin(from.heading()), from.heading());
        return Paths.line(from, to).constant(from.heading());
    }

    /** Field Check 09: a square (ahead, left, back, right) back to the start, same heading. */
    public static Path square(Pose from, double sideIn) {
        double cos = Math.cos(from.heading());
        double sin = Math.sin(from.heading());
        Pose ahead = new Pose(from.x() + sideIn * cos, from.y() + sideIn * sin, from.heading());
        Pose aheadLeft = new Pose(ahead.x() - sideIn * sin, ahead.y() + sideIn * cos, from.heading());
        Pose left = new Pose(from.x() - sideIn * sin, from.y() + sideIn * cos, from.heading());
        return Paths.path(
                Paths.line(from, ahead).constant(from.heading()),
                Paths.line(ahead, aheadLeft).constant(from.heading()),
                Paths.line(aheadLeft, left).constant(from.heading()),
                Paths.line(left, from).constant(from.heading()));
    }

    // ==================== CHECKS ====================

    /**
     * true if the robot's centre stays on our half of the field along the whole path
     * (rule G402: in AUTO, RED stays in columns A-C, the left half; BLUE in D-F, the right half).
     */
    public static boolean isOnOwnSide(Path path, boolean isRed) {
        double middle = FieldConfig.FIELD_CENTRE_IN;
        for (int i = 0; i <= CHECK_POINTS; i++) {
            Pose point = path.get((double) i / CHECK_POINTS);
            if (isRed && point.x() > middle - FieldConfig.OWN_SIDE_MARGIN_IN) return false;
            if (!isRed && point.x() < middle + FieldConfig.OWN_SIDE_MARGIN_IN) return false;
        }
        return true;
    }

    /** true if the robot's centre stays out of the HIVE keep-out box along the whole path. */
    public static boolean avoidsHive(Path path) {
        for (int i = 0; i <= CHECK_POINTS; i++) {
            Pose point = path.get((double) i / CHECK_POINTS);
            boolean insideX = point.x() > FieldConfig.HIVE_KEEP_OUT_MIN_X_IN
                    && point.x() < FieldConfig.HIVE_KEEP_OUT_MAX_X_IN;
            boolean insideY = point.y() > FieldConfig.HIVE_KEEP_OUT_MIN_Y_IN
                    && point.y() < FieldConfig.HIVE_KEEP_OUT_MAX_Y_IN;
            if (insideX && insideY) return false;
        }
        return true;
    }
}
