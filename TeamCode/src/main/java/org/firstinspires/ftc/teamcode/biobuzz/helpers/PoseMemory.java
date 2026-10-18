package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * PoseMemory.java  -  carries the robot's pose from Autonomous to TeleOp.       ROLE: helper
 *
 * WHY IT EXISTS:
 * Field-centric driving needs to know which way the robot faces. At the end of
 * Autonomous the robot knows exactly where it is, so BioBuzzAuto saves the pose here
 * in stop(), and BioBuzzTeleOp starts from it in init().
 *
 * HOW IT WORKS: "static" fields belong to the class, not to one OpMode, so they
 * survive from one OpMode to the next. They are lost when the Robot Controller app
 * restarts (for example after a power loss). Then hasPose() is false, and TeleOp
 * shows "No auto pose: press Y facing away from drivers".
 *
 * TRACING A BUG: if TeleOp's field-centric is rotated after a good Auto, check the
 * "Pose from auto" telemetry line, and how old the saved pose is.
 */
public class PoseMemory {
    private static Pose savedPose = null;
    private static final ElapsedTime sinceSaved = new ElapsedTime();

    /** Remember this pose for the next OpMode. Called by BioBuzzAuto in stop(). */
    public static void save(Pose pose) {
        savedPose = pose;
        sinceSaved.reset();
    }

    /** @return true if an Autonomous saved a pose since the app started. */
    public static boolean hasPose() { return savedPose != null; }

    /** @return the saved pose, or null if there is none (check hasPose() first). */
    public static Pose getPose() { return savedPose; }

    /** @return seconds since the pose was saved, so TeleOp can tell an old pose from a fresh one. */
    public static double secondsSinceSaved() { return sinceSaved.seconds(); }

    /** Forget the saved pose, for example after TeleOp has used it. */
    public static void clear() { savedPose = null; }
}
