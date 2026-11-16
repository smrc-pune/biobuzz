package org.firstinspires.ftc.teamcode.biobuzz.mechanisms;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.IntakeConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * STUDENTS: you will change this file.
 *
 * Intake.java  -  the roller and side wheels that pick up the balls.          ROLE: mechanism
 * PATTERN: Adapter (game words for three devices) + Template Method (fills in Mechanism's hooks).
 *
 * THE HARDWARE (goBILDA StarterBot): a roller motor turning 7 grip wheels on one shaft,
 * and two continuous servos at the front corners, each turning a flat side wheel that
 * pulls balls toward the middle. All three always run together: collect, reverse, stop.
 *
 * It only does what it is told: collect, reverse or stop. It never decides anything.
 * The operator turns it on and off (gamepad 2 A) and spits balls out (hold B), through
 * IntakeSubsystem.toggle() and IntakeSubsystem.reverse().
 * The hopper holds at most 4 balls (rule G407); the robot cannot count them (no sensors).
 *
 * TRACING A BUG: the roller spins the wrong way -> flip MOTOR_INTAKE_REVERSED in HardwareNames.
 * A side wheel pushes balls away -> flip SERVO_INTAKE_LEFT_REVERSED or _RIGHT_REVERSED.
 * Too weak or too fast -> IntakeConfig.
 */
public class Intake extends Mechanism {

    private final DcMotorEx roller;
    private final CRServo leftWheel;    // continuous servo, front left corner
    private final CRServo rightWheel;   // continuous servo, front right corner
    private double power = 0;           // roller power we asked for; kept so the log never reads hardware
    private double sidePower = 0;       // side wheel power we asked for

    public Intake(HardwareMap hardwareMap) {
        super("intake");
        roller = hardwareMap.get(DcMotorEx.class, HardwareNames.MOTOR_INTAKE);
        if (HardwareNames.MOTOR_INTAKE_REVERSED) {
            roller.setDirection(DcMotor.Direction.REVERSE);
        } else {
            roller.setDirection(DcMotor.Direction.FORWARD);
        }
        leftWheel = hardwareMap.get(CRServo.class, HardwareNames.SERVO_INTAKE_LEFT);
        if (HardwareNames.SERVO_INTAKE_LEFT_REVERSED) {
            leftWheel.setDirection(DcMotorSimple.Direction.REVERSE);
        } else {
            leftWheel.setDirection(DcMotorSimple.Direction.FORWARD);
        }
        rightWheel = hardwareMap.get(CRServo.class, HardwareNames.SERVO_INTAKE_RIGHT);
        if (HardwareNames.SERVO_INTAKE_RIGHT_REVERSED) {
            rightWheel.setDirection(DcMotorSimple.Direction.REVERSE);
        } else {
            rightWheel.setDirection(DcMotorSimple.Direction.FORWARD);
        }
        // Nothing moves here: robots must stay still during INIT (rule G304.H).
    }

    // ---- What the intake can do ----
    public void collect() {
        setPowers(IntakeConfig.COLLECT_POWER, IntakeConfig.SIDE_COLLECT_POWER);
    }

    public void reverse() {
        setPowers(IntakeConfig.REVERSE_POWER, IntakeConfig.SIDE_REVERSE_POWER);
    }

    // ---- What you can ask it ----
    public boolean isCollecting() {
        return power > 0;
    }

    public boolean isReversing() {
        return power < 0;
    }

    public double getPower() {
        return power;
    }

    /** The roller and both side wheels together. */
    private void setPowers(double newPower, double newSidePower) {
        if (isFaulted()) {
            return;
        }
        if (newPower != power || newSidePower != sidePower) {
            MatchLogger.event("INTAKE", String.format("power %.2f, side wheels %.2f", newPower, newSidePower));
        }
        power = newPower;
        sidePower = newSidePower;
        roller.setPower(power);
        leftWheel.setPower(sidePower);
        rightWheel.setPower(sidePower);
    }

    // ---- Filled in for Mechanism ----
    @Override
    protected void stop() {
        if (power != 0) {
            MatchLogger.event("INTAKE", "off");
        }
        power = 0;
        sidePower = 0;
        roller.setPower(0);
        leftWheel.setPower(0);
        rightWheel.setPower(0);
    }

    @Override
    protected void addDetails(Telemetry t) {
        String what = "OFF";
        if (isCollecting()) {
            what = "COLLECTING";
        }
        if (isReversing()) {
            what = "SPITTING OUT";
        }
        t.addData("Intake", what + String.format("  (power %.2f, side wheels %.2f)", power, sidePower));
    }

    @Override
    protected void addLogValues() {
        MatchLogger.value("intake_power", power);
        MatchLogger.value("intake_side_power", sidePower);
    }

    @Override
    protected boolean isTelemetryOn() {
        return TelemetryConfig.INTAKE;
    }
}
