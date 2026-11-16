package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Feeder;

/**
 * STUDENTS: you will change this file.
 *
 * FeederTest.java  -  test the FEEDER by itself.                               ROLE: OpMode (test)
 *
 * CONTROLS (gamepad 1):   A (hold) = feed    B (hold) = unjam (backwards)    let go = stop
 * WHAT TO WATCH: the windmill turns while A is held, and each paddle pushes a ball toward
 * the flywheel. If it turns the other way, flip SERVO_FEEDER_REVERSED in HardwareNames.
 * Let go: it stops and keeps the balls back. Try FeederConfig.FEED_POWER and UNJAM_POWER
 * live in the Dashboard, then type good values into FeederConfig.java.
 * The flywheel is NOT running here: to watch balls fly, use "Test: Launch".
 */
@TeleOp(name = "Test: Feeder", group = "Tests")
public class FeederTest extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Feeder feeder;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        feeder = new Feeder(hardwareMap);   // ONLY the feeder; nothing moves
    }

    @Override
    public void start() {
        MatchLogger.start("FeederTest", battery.getVoltage());
    }

    @Override
    public void loop() {
        boolean feed = pad.isAHeld();
        boolean unjam = pad.isBHeld();

        if (feed) {
            feeder.feed();
        } else if (unjam) {
            feeder.unjam();
        } else {
            feeder.hold();
        }
        feeder.runUpdate();

        t.addLine("A (hold) = feed   B (hold) = unjam   let go = stop");
        t.addData("isFeeding()", feeder.isFeeding());
        t.addData("getPower()", feeder.getPower());
        t.addData("isFaulted()", feeder.isFaulted());
        feeder.addTelemetry(t);
        t.update();
    }

    @Override
    public void stop() {
        feeder.runStop();
        MatchLogger.stop("");
    }
}
