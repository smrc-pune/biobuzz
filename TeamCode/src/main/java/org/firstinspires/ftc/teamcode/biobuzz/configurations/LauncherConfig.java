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
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class LauncherConfig {
    // ---- The flywheel motor ----
    public static double TICKS_PER_REV = 28;      // encoder ticks per flywheel turn (goBILDA bare motor: 28)
    public static double SPEED_P = 10;            // velocity control (PIDF) inside the motor controller
    public static double SPEED_I = 0;
    public static double SPEED_D = 0;
    public static double SPEED_F = 12;
    public static double IDLE_RPM = 0;            // speed between launches (0 = stopped)
    public static double SPEED_TOLERANCE_RPM = 100;   // "up to speed" when this close to the target

    // ---- The launch speed (see above) ----
    public static double LAUNCH_RPM = 3100;           // the ONE launch speed, tuned from our launch spot

    // ---- Launching (LaunchStateMachine) ----
    public static double SPIN_UP_TIMEOUT_MS = 2500;   // give up if the flywheel is not up to speed by then
    public static double FEED_MS = 250;               // feeder pushes for this long
    public static double RECOVER_MS = 300;            // feeder pulls back; flywheel gets its speed back
    public static double TIP_PAUSE_MS = 1500;         // after "HIVE tipped", wait this long before the next feed

    // ---- Aiming (gamepad 1 right bumper) ----
    public static double AIM_OFFSET_DEG = 0;          // 0 = the launcher shoots out of the robot's front; 180 = out the back
}
