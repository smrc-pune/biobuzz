package org.firstinspires.ftc.teamcode.biobuzz.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
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
 * Intake.java  -  the roller that picks up POLLEN and NECTAR.                   ROLE: mechanism
 * PATTERN: Adapter (game words for one motor) + Template Method (fills in Mechanism's hooks).
 *
 * It only does what it is told: collect, reverse or stop. It never decides anything.
 * The operator turns it on and off (gamepad 2 A) and spits balls out (hold B), through
 * Robot.toggleIntake() and Robot.reverseIntake().
 * The hopper holds at most 4 balls (rule G407); the robot cannot count them (no sensors).
 *
 * TRACING A BUG: the roller spins the wrong way -> flip MOTOR_INTAKE_REVERSED in HardwareNames.
 * Too weak or too fast -> IntakeConfig.
 */
public class Intake extends Mechanism {

    private final DcMotorEx roller;
    private double power = 0;     // what we asked for; kept so the log never reads hardware

    public Intake(HardwareMap hardwareMap) {
        super("intake");
        roller = hardwareMap.get(DcMotorEx.class, HardwareNames.MOTOR_INTAKE);
        if (HardwareNames.MOTOR_INTAKE_REVERSED) roller.setDirection(DcMotor.Direction.REVERSE);
        else roller.setDirection(DcMotor.Direction.FORWARD);
        // Nothing moves here: robots must stay still during INIT (rule G304.H).
    }

    // ---- What the intake can do ----
    public void collect() { setPower(IntakeConfig.COLLECT_POWER); }

    public void reverse() { setPower(IntakeConfig.REVERSE_POWER); }

    // ---- What you can ask it ----
    public boolean isCollecting() { return power > 0; }

    public boolean isReversing() { return power < 0; }

    public double getPower() { return power; }

    private void setPower(double newPower) {
        if (isFaulted()) return;
        if (newPower != power) MatchLogger.event("INTAKE", String.format("power %.2f", newPower));
        power = newPower;
        roller.setPower(power);
    }

    // ---- Filled in for Mechanism ----
    @Override
    protected void stop() {
        if (power != 0) MatchLogger.event("INTAKE", "off");
        power = 0;
        roller.setPower(0);
    }

    @Override
    protected void addDetails(Telemetry t) {
        String what = "OFF";
        if (isCollecting()) what = "COLLECTING";
        if (isReversing()) what = "SPITTING OUT";
        t.addData("Intake", what + String.format("  (power %.2f)", power));
    }

    @Override
    protected void addLogValues() { MatchLogger.value("intake_power", power); }

    @Override
    protected boolean isTelemetryOn() { return TelemetryConfig.INTAKE; }
}
