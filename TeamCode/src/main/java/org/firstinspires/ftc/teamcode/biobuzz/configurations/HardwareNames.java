package org.firstinspires.ftc.teamcode.biobuzz.configurations;

/**
 * STUDENTS: you will change this file.
 *
 * HardwareNames.java  -  THE WIRING LIST.                                      ROLE: config
 *
 * WHAT IS THIS FILE?
 * Every motor, servo and sensor has a name that we typed into the Robot Controller
 * app (Configure Robot). Each part finds its devices BY THAT NAME:
 *
 *   intakeMotor = hardwareMap.get(DcMotorEx.class, HardwareNames.MOTOR_INTAKE);
 *
 * If the name here does not match the app EXACTLY (capital letters, spaces and all),
 * the OpMode stops at INIT with:
 *   Unable to find a hardware device with name "intake" and type DcMotorEx
 *
 * THE RULE:
 * This is the ONLY file that contains device name text. Parts use the constants below,
 * never "quoted names". Pedro's pedro/Constants.java also reads the drivetrain and
 * Pinpoint names from here (filled in at drive tuning).
 *
 * HUB AND PORT: the comment after each name says where the device is plugged in
 * (Control Hub or Expansion Hub, and the port). Keep it the same as the Robot
 * Controller configuration, so this file is also the wiring sheet. The code itself
 * does not care which hub a device is on: names work across both hubs.
 *
 * WHEN A MOTOR SPINS THE WRONG WAY:
 * Flip the matching "REVERSED" value in this file (true <-> false). Nothing else.
 *
 * WHEN YOU SEE "Unable to find a hardware device with name ...":
 *   1. Open the Robot Controller app -> Configure Robot -> look at the name, on the
 *      hub and port written in the comment below.
 *   2. Make the text below match it exactly (or fix the configuration in the app).
 *   3. Still missing? Check the device's cable, and for Expansion Hub devices the
 *      hub's power and RS-485 cable.
 *
 * These are "final" on purpose: names are not tuned live, so this is not a @Config file.
 */
public class HardwareNames {

    // ==================== DRIVETRAIN (used by Pedro in pedro/Constants.java) ====================
    // The drive motors go on the Control Hub: it answers faster than the Expansion Hub.
    // TODO(BioBuzz): confirm every name and port against the BioBuzz configuration.
    public static final String MOTOR_DRIVE_LEFT_FRONT = "leftFront";     // Control Hub, motor port 0
    public static final String MOTOR_DRIVE_RIGHT_FRONT = "rightFront";   // Control Hub, motor port 1
    public static final String MOTOR_DRIVE_LEFT_BACK = "leftBack";       // Control Hub, motor port 2
    public static final String MOTOR_DRIVE_RIGHT_BACK = "rightBack";     // Control Hub, motor port 3

    // TODO(BioBuzz): confirm the directions with Pedro's MecanumTuner at drive tuning.
    public static final boolean MOTOR_DRIVE_LEFT_FRONT_REVERSED = true;
    public static final boolean MOTOR_DRIVE_RIGHT_FRONT_REVERSED = false;
    public static final boolean MOTOR_DRIVE_LEFT_BACK_REVERSED = true;
    public static final boolean MOTOR_DRIVE_RIGHT_BACK_REVERSED = false;

    // ==================== ODOMETRY ====================
    // The goBILDA Pinpoint computer: tells Pedro where the robot is.
    // TODO(BioBuzz): confirm the localizer (Open items: Odometry).
    public static final String PINPOINT = "pinpoint";                    // Control Hub, I2C bus 1

    // ==================== MECHANISMS ====================
    // TODO(BioBuzz): one block per mechanism, once the mechanism list is known.
    // Most mechanism motors go on the Expansion Hub, for example:
    //   public static final String MOTOR_INTAKE = "intake";             // Expansion Hub, motor port 0
}
