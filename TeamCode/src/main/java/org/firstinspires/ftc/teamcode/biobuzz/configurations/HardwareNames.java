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
 * WHERE THE NAMES COME FROM: our robot is the goBILDA StarterBot with mecanum wheels,
 * so we use goBILDA's names (docs/DESIGN-REVIEW.md, section 14, H4). Then goBILDA's own
 * example TeleOp runs on our configuration too: a quick hardware check. Only
 * "pinpoint" and "hiveCamera" are ours (the kit has no odometry and no camera).
 *
 * CONTINUOUS SERVOS: the windmill and the two intake side wheels are goBILDA dual-mode
 * servos set to CONTINUOUS mode (with the goBILDA servo programmer). In the Robot
 * Controller app, configure them as "Continuous Rotation Servo", not "Servo".
 *
 * WHEN A MOTOR OR SERVO SPINS THE WRONG WAY:
 * Flip the matching "REVERSED" value in this file (true <-> false). Nothing else.
 *
 * WHEN YOU SEE "Unable to find a hardware device with name ...":
 *   1. Open the Robot Controller app -> Configure Robot -> look at the name, on the
 *      hub and port written in the comment below.
 *   2. Make the text below match it exactly (or fix the configuration in the app).
 *   3. Still missing? Check the device's cable, and for Expansion Hub devices the
 *      hub's power and RS-485 cable.
 *
 * THE LIMITS (game rules R503, R701): at most 8 motors, 8 servos, one Control Hub and
 * one Expansion Hub. Count after every change:
 *   motors: 4 drive + intake + launcher = 6 of 8
 *   servos: windmill + 2 intake side wheels = 3 of 8
 * 6 motors need the Expansion Hub: the Control Hub has only 4 motor ports.
 *
 * These are "final" on purpose: names are not tuned live, so this is not a @Config file.
 */
public class HardwareNames {

    // ==================== DRIVETRAIN (used by Pedro in pedro/Constants.java) ====================
    // The drive motors go on the Control Hub: it answers faster than the Expansion Hub.
    // TODO(BioBuzz): confirm every name and port against the BioBuzz configuration.
    public static final String MOTOR_DRIVE_LEFT_FRONT = "left_front_drive";     // Control Hub, motor port 0
    public static final String MOTOR_DRIVE_RIGHT_FRONT = "right_front_drive";   // Control Hub, motor port 1
    public static final String MOTOR_DRIVE_LEFT_BACK = "left_back_drive";       // Control Hub, motor port 2
    public static final String MOTOR_DRIVE_RIGHT_BACK = "right_back_drive";     // Control Hub, motor port 3

    // Left side reversed, as in goBILDA's example.
    // TODO(BioBuzz): confirm the directions with Pedro's MecanumTuner at drive tuning.
    public static final boolean MOTOR_DRIVE_LEFT_FRONT_REVERSED = true;
    public static final boolean MOTOR_DRIVE_RIGHT_FRONT_REVERSED = false;
    public static final boolean MOTOR_DRIVE_LEFT_BACK_REVERSED = true;
    public static final boolean MOTOR_DRIVE_RIGHT_BACK_REVERSED = false;

    // ==================== ODOMETRY ====================
    // The goBILDA Pinpoint computer with 2 odometry pods: tells Pedro where the robot is.
    // Not in the StarterBot kit: added by the team (DESIGN-REVIEW section 14, H3).
    // TODO(BioBuzz): mount, pod offsets and directions at drive tuning.
    public static final String PINPOINT = "pinpoint";                    // Control Hub, I2C bus 1

    // ==================== MECHANISMS ====================
    // Most mechanism motors go on the Expansion Hub.
    // TODO(BioBuzz): ports and directions are first guesses until checked on the built robot.

    // Intake: the roller that picks up the balls (7 grip wheels on one shaft)
    public static final String MOTOR_INTAKE = "intake";                // Expansion Hub, motor port 0
    public static final boolean MOTOR_INTAKE_REVERSED = false;

    // Intake side wheels: two continuous servos at the front corners that pull balls to
    // the middle. They turn opposite ways, so one is reversed (as in goBILDA's example).
    public static final String SERVO_INTAKE_LEFT = "left_intake_servo";    // Expansion Hub, servo port 1
    public static final boolean SERVO_INTAKE_LEFT_REVERSED = false;
    public static final String SERVO_INTAKE_RIGHT = "right_intake_servo";  // Expansion Hub, servo port 2
    public static final boolean SERVO_INTAKE_RIGHT_REVERSED = true;

    // Launcher: the flywheel (a motor with an encoder, so we can control its speed)
    public static final String MOTOR_LAUNCHER = "launcher";            // Expansion Hub, motor port 1
    public static final boolean MOTOR_LAUNCHER_REVERSED = false;

    // Feeder: the 4-paddle windmill that pushes balls from the hopper into the flywheel.
    // A continuous servo (goBILDA's example reverses it).
    public static final String SERVO_FEEDER = "windmillServo";         // Expansion Hub, servo port 0
    public static final boolean SERVO_FEEDER_REVERSED = true;

    // Cameras
    public static final String HUSKYLENS_HIVE = "hiveCamera";          // Control Hub, I2C bus 2 (looks UP at the HIVE)

    // ==================== TEMPLATE (template/ only, never on the robot) ====================
    public static final String MOTOR_TEMPLATE = "templateMotor";     // rename when you copy the template

    // ==================== EVERY DEVICE ====================
    // Test: Parts, Robot Check and Field Check 01 go through this list. When you add a
    // device above, ADD ITS NAME HERE TOO. Motors and servos are moved by Test: Parts.
    public static final String[] ALL_DEVICES = {
            MOTOR_DRIVE_LEFT_FRONT,
            MOTOR_DRIVE_RIGHT_FRONT,
            MOTOR_DRIVE_LEFT_BACK,
            MOTOR_DRIVE_RIGHT_BACK,
            PINPOINT,
            MOTOR_INTAKE,
            SERVO_INTAKE_LEFT,
            SERVO_INTAKE_RIGHT,
            MOTOR_LAUNCHER,
            SERVO_FEEDER,
            HUSKYLENS_HIVE,
    };

    // The four drive motors, for Robot Check (it spins each one, one at a time).
    public static final String[] DRIVE_MOTORS = {
            MOTOR_DRIVE_LEFT_FRONT,
            MOTOR_DRIVE_RIGHT_FRONT,
            MOTOR_DRIVE_LEFT_BACK,
            MOTOR_DRIVE_RIGHT_BACK,
    };
}
