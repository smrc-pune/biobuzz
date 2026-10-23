package org.firstinspires.ftc.teamcode.biobuzz.template;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * STUDENTS: copy this file to make a test for a new mechanism.
 *
 * TemplateMechanismTest.java  -  test ONE mechanism by itself.               ROLE: OpMode (test)
 *
 * Copy it to testing/RollerTest.java, change @TeleOp's name to "Test: Roller", REMOVE the
 * @Disabled line (it hides this template from the Driver Station), and use your mechanism.
 * It builds only that mechanism (new TemplateMechanism(hardwareMap)), not the whole Robot.
 *
 * CONTROLS (gamepad 1):   A = run    B = stop
 * Show every question's answer on telemetry, so you can see what the mechanism thinks.
 */
@Disabled
@TeleOp(name = "Test: Template", group = "Tests")
public class TemplateMechanismTest extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private TemplateMechanism mechanism;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        mechanism = new TemplateMechanism(hardwareMap);   // ONLY this mechanism; nothing moves
    }

    @Override
    public void start() {
        MatchLogger.start("TemplateTest", battery.getVoltage());   // tests write match logs too
    }

    @Override
    public void loop() {
        boolean run = pad.isAPressed();
        boolean stop = pad.isBPressed();

        if (run) mechanism.run();
        if (stop) mechanism.runStop();

        mechanism.runUpdate();   // what a subsystem's update() does for every mechanism it owns

        t.addLine("A = run   B = stop");
        t.addData("isRunning()", mechanism.isRunning());
        t.addData("isFaulted()", mechanism.isFaulted());
        mechanism.addTelemetry(t);
        t.update();
    }

    @Override
    public void stop() {
        mechanism.runStop();
        MatchLogger.stop("");
    }
}
