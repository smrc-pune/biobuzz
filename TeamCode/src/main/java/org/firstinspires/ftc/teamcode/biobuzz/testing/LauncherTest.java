package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LauncherConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Launcher;

/**
 * STUDENTS: you will change this file.
 *
 * LauncherTest.java  -  test the FLYWHEEL by itself, and tune its speed.        ROLE: OpMode (test)
 *
 * CONTROLS (gamepad 1):   A = spin up to LAUNCH_RPM    Y = idle speed    B = stop
 * Changed LAUNCH_RPM in the Dashboard? Press A again to use the new speed.
 * WHAT TO WATCH: RPM should reach the target within about a second and stay there
 * ("READY"). Open the Dashboard's Graph view: launcher_rpm and launcher_target_rpm.
 *   Slow to reach it, or sags after each ball -> raise LauncherConfig.SPEED_F, then SPEED_P.
 *   Overshoots and wobbles -> lower SPEED_P.
 *   RPM reads about double or half of what it should -> TICKS_PER_REV is wrong.
 * To tune LAUNCH_RPM itself, use "Test: Launch" (it feeds real balls) from our launch spot.
 */
@TeleOp(name = "Test: Launcher", group = "Tests")
public class LauncherTest extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Launcher launcher;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        launcher = new Launcher(hardwareMap);   // ONLY the launcher; nothing moves
    }

    @Override
    public void start() {
        MatchLogger.start("LauncherTest", battery.getVoltage());
    }

    @Override
    public void loop() {
        boolean spinUp = pad.isAPressed();
        boolean idle = pad.isYPressed();
        boolean stop = pad.isBPressed();

        if (spinUp) {
            launcher.spinUp();
        }
        if (idle) {
            launcher.idle();
        }
        if (stop) {
            launcher.runStop();
        }
        launcher.runUpdate();

        t.addLine("A = spin up   Y = idle   B = stop");
        t.addData("LAUNCH_RPM", "%.0f", LauncherConfig.LAUNCH_RPM);
        t.addData("getRpm() / getTargetRpm()", "%.0f / %.0f", launcher.getRpm(), launcher.getTargetRpm());
        t.addData("isUpToSpeed()", launcher.isUpToSpeed());
        t.addData("isFaulted()", launcher.isFaulted());
        t.addData("Battery", "%.1f V", battery.getVoltage());
        launcher.addTelemetry(t);
        FieldDrawing.graph("launcher_rpm", launcher.getRpm());
        FieldDrawing.graph("launcher_target_rpm", launcher.getTargetRpm());
        FieldDrawing.send();
        t.update();
    }

    @Override
    public void stop() {
        launcher.runStop();
        MatchLogger.stop("");
    }
}
