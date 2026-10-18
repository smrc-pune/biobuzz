package org.firstinspires.ftc.teamcode.biobuzz.configurations;

/**
 * HardwareNames.java  -  THE WIRING LIST.                                      ROLE: config
 *
 * WHAT IS THIS FILE?
 * Every motor, servo and sensor has a name that we typed into the Robot Controller
 * app (Configure Robot). The code finds each device BY THAT NAME. If the name here
 * does not match the app EXACTLY (capital letters, spaces and all), the OpMode stops
 * at INIT with "DEVICE NOT FOUND".
 *
 * THE RULE:
 * This is the ONLY file that contains device names. Parts never contain names:
 * Robot.java hands them the names from here. Pedro's pedro/Constants.java also reads
 * the drivetrain and Pinpoint names from here (filled in at drive tuning).
 *
 * WHEN A MOTOR SPINS THE WRONG WAY:
 * Flip the matching "REVERSED" value in this file (true <-> false). Nothing else.
 *
 * WHEN YOU SEE "DEVICE NOT FOUND":
 *   1. Open the Robot Controller app -> Configure Robot -> look at the name.
 *   2. Make the text below match it exactly.
 *
 * These are "final" on purpose: names are not tuned live, so this is not a @Config file.
 */
public class HardwareNames {

    // ==================== DRIVETRAIN (used by Pedro in pedro/Constants.java) ====================
    // TODO(BioBuzz): confirm every name against the BioBuzz Control Hub configuration.
    public static final String MOTOR_DRIVE_LEFT_FRONT = "leftFront";
    public static final String MOTOR_DRIVE_RIGHT_FRONT = "rightFront";
    public static final String MOTOR_DRIVE_LEFT_BACK = "leftBack";
    public static final String MOTOR_DRIVE_RIGHT_BACK = "rightBack";

    // TODO(BioBuzz): confirm the directions with Pedro's MecanumTuner at drive tuning.
    public static final boolean MOTOR_DRIVE_LEFT_FRONT_REVERSED = true;
    public static final boolean MOTOR_DRIVE_RIGHT_FRONT_REVERSED = false;
    public static final boolean MOTOR_DRIVE_LEFT_BACK_REVERSED = true;
    public static final boolean MOTOR_DRIVE_RIGHT_BACK_REVERSED = false;

    // ==================== ODOMETRY ====================
    // The goBILDA Pinpoint computer: tells Pedro where the robot is.
    // TODO(BioBuzz): confirm the localizer (Open items: Odometry).
    public static final String PINPOINT = "pinpoint";

    // ==================== MECHANISMS ====================
    // TODO(BioBuzz): one block per mechanism, once the mechanism list is known.
}
