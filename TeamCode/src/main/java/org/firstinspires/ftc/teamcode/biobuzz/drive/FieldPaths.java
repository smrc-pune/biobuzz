package org.firstinspires.ftc.teamcode.biobuzz.drive;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldConfig;

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

    // ==================== CHECKS ====================

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
