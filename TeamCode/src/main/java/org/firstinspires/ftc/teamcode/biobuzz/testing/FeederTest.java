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
 * CONTROLS (gamepad 1):   A = feed one    B = hold
 * WHAT TO WATCH: "hold" keeps a ball back; "feed" pushes it all the way into the flywheel.
 * Find the right positions by changing FeederConfig live in the Dashboard while you press
 * A and B, then type them into FeederConfig.java.
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
    public void start() { MatchLogger.start("FeederTest", battery.getVoltage()); }

    @Override
    public void loop() {
        boolean feed = pad.isAPressed();
        boolean hold = pad.isBPressed();

        if (feed) feeder.feedOne();
        if (hold) feeder.hold();
        feeder.runUpdate();

        t.addLine("A = feed one   B = hold");
        t.addData("isFeeding()", feeder.isFeeding());
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
