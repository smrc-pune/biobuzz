package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * LauncherConfig.java  -  numbers for the flywheel and for launching.          ROLE: config
 * Used by mechanisms/Launcher.java and statemachines/LaunchStateMachine.java.
 *
 * THE LAUNCH SPEED: ONE speed (LAUNCH_RPM) for every launch. Tune it with the robot on our
 * LAUNCH SPOT (FieldConfig.LAUNCH_SPOT), because that is where Auto and the driver's A button
 * launch from. From other spots the balls land short or long. One speed serves both POLLEN
 * and NECTAR, because the robot does not know which ball is next.
 * A speed that changes with the distance to the CELL is a future improvement (docs/LLD.md, section 14).
 * TODO(BioBuzz): tune LAUNCH_RPM with "Test: Launch" from the launch spot.
 *
 * STARTING NUMBERS: from goBILDA's example code for the StarterBot (1:1 bare motor, 96 mm
 * wheel): 1250 ticks/s = about 2680 RPM, feed above 1200 ticks/s = about 2570 RPM,
 * PIDF 40 / 0 / 0 / 12.5 (docs/DESIGN-REVIEW.md, section 14, H6).
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class LauncherConfig {
    // ---- The flywheel motor ----
    public static double TICKS_PER_REV = 28;      // encoder ticks per flywheel turn (goBILDA bare motor: 28)
    public static double SPEED_P = 40;            // velocity control (PIDF) inside the motor controller
    public static double SPEED_I = 0;
    public static double SPEED_D = 0;
    public static double SPEED_F = 12.5;
    public static double IDLE_RPM = 0;            // speed between launches (0 = stopped)
    public static double SPEED_TOLERANCE_RPM = 100;   // "up to speed" when this close to the target

    // ---- The launch speed (see above) ----
    public static double LAUNCH_RPM = 2680;           // the ONE launch speed, tuned from our launch spot
    public static double MIN_FEED_RPM = 2570;         // the windmill turns only while the flywheel is at least this fast.
                                                      // Change it with LAUNCH_RPM: about 100 below it

    // ---- Launching (LaunchStateMachine) ----
    public static double SPIN_UP_TIMEOUT_MS = 2500;   // give up if the flywheel is not up to speed by then
                                                      // (at the start, and each time it slows while feeding)
    public static double FEED_TOTAL_MS = 2000;        // the windmill turns this long in total: enough to empty
                                                      // a full hopper (4 balls). TODO(BioBuzz): time it on the robot
    public static double TIP_PAUSE_MS = 1500;         // after "HIVE tipped", wait this long before the next feed

    // ---- Aiming (gamepad 1 right bumper) ----
    public static double AIM_OFFSET_DEG = 0;          // 0 = the launcher shoots out of the robot's front; 180 = out the back
}
