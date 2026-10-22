package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * LauncherConfig.java  -  numbers for the flywheel and for launching.          ROLE: config
 * Used by mechanisms/Launcher.java and statemachines/LaunchStateMachine.java.
 *
 * THE SPEED TABLE: how fast the flywheel spins (RPM) for each distance (inches) from the
 * robot's centre to the upward CELL. Between two distances the speed is in between.
 * Beyond the ends, the end value is used. DISTANCE_IN must go up from left to right, and
 * both lists must have the same number of values. One table serves both POLLEN and NECTAR,
 * because the robot does not know which ball is next.
 * TODO(BioBuzz): measure every speed with "Test: Launcher" from each distance.
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

    // ---- The speed table (see above) ----
    public static double[] DISTANCE_IN = { 36, 48, 60, 72, 84 };
    public static double[] RPM = { 2800, 3100, 3400, 3700, 4000 };
    public static double DEFAULT_DISTANCE_IN = 48;    // used when the robot does not know where it is (S1)

    // ---- Launching (LaunchStateMachine) ----
    public static double SPIN_UP_TIMEOUT_MS = 2500;   // give up if the flywheel is not up to speed by then
    public static double FEED_MS = 250;               // feeder pushes for this long
    public static double RECOVER_MS = 300;            // feeder pulls back; flywheel gets its speed back
    public static double TIP_PAUSE_MS = 1500;         // after "HIVE tipped", wait this long before the next feed

    // ---- Aiming (gamepad 1 right bumper) ----
    public static double AIM_OFFSET_DEG = 0;          // 0 = the launcher shoots out of the robot's front; 180 = out the back
}
