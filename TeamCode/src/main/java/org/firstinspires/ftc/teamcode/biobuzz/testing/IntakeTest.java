package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Intake;

/**
 * STUDENTS: you will change this file.
 *
 * IntakeTest.java  -  test the INTAKE by itself.                               ROLE: OpMode (test)
 *
 * CONTROLS (gamepad 1):   A = collect    B (hold) = reverse    X = stop
 * WHAT TO WATCH: A pulls a ball in. If it pushes it out, flip MOTOR_INTAKE_REVERSED in
 * HardwareNames. Powers are in IntakeConfig (change them live in the Dashboard).
 */
@TeleOp(name = "Test: Intake", group = "Tests")
public class IntakeTest extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Intake intake;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        intake = new Intake(hardwareMap);   // ONLY the intake; nothing moves
    }

    @Override
    public void start() { MatchLogger.start("IntakeTest", battery.getVoltage()); }

    @Override
    public void loop() {
        boolean collect = pad.isAPressed();
        boolean reverse = pad.isBHeld();
        boolean reverseReleased = pad.wasBReleased();
        boolean stop = pad.isXPressed();

        if (collect) {
            intake.collect();
        }
        if (reverse) {
            intake.reverse();
        }
        if (reverseReleased || stop) {
            intake.runStop();
        }
        intake.runUpdate();

        t.addLine("A = collect   B (hold) = reverse   X = stop");
        t.addData("isCollecting()", intake.isCollecting());
        t.addData("isReversing()", intake.isReversing());
        t.addData("getPower()", intake.getPower());
        t.addData("isFaulted()", intake.isFaulted());
        intake.addTelemetry(t);
        t.update();
    }

    @Override
    public void stop() {
        intake.runStop();
        MatchLogger.stop("");
    }
}
