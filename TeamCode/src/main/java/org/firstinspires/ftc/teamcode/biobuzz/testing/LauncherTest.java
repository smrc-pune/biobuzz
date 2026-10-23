package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
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
 * CONTROLS (gamepad 1):   D-pad up / down = test distance +/- 6 in
 *                         A = spin up for that distance    Y = idle speed    B = stop
 * WHAT TO WATCH: RPM should reach the target within about a second and stay there
 * ("READY"). Open the Dashboard's Graph view: launcher_rpm and launcher_target_rpm.
 *   Slow to reach it, or sags after each ball -> raise LauncherConfig.SPEED_F, then SPEED_P.
 *   Overshoots and wobbles -> lower SPEED_P.
 *   RPM reads about double or half of what it should -> TICKS_PER_REV is wrong.
 * To fill in the speed table, use "Test: Launch" (it feeds real balls) from each distance.
 */
@TeleOp(name = "Test: Launcher", group = "Tests")
public class LauncherTest extends OpMode {

    private static final double DISTANCE_STEP_IN = 6;

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Launcher launcher;
    private double testDistanceIn = 48;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        launcher = new Launcher(hardwareMap);   // ONLY the launcher; nothing moves
    }

    @Override
    public void start() { MatchLogger.start("LauncherTest", battery.getVoltage()); }

    @Override
    public void loop() {
        boolean farther = pad.isDpadUpPressed();
        boolean closer = pad.isDpadDownPressed();
        boolean spinUp = pad.isAPressed();
        boolean idle = pad.isYPressed();
        boolean stop = pad.isBPressed();

        if (farther) {
            testDistanceIn += DISTANCE_STEP_IN;
        }
        if (closer) {
            testDistanceIn = Math.max(0, testDistanceIn - DISTANCE_STEP_IN);
        }
        if (spinUp) {
            launcher.spinUpFor(testDistanceIn);
        }
        if (idle) {
            launcher.idle();
        }
        if (stop) {
            launcher.runStop();
        }
        launcher.runUpdate();

        t.addLine("D-pad up/down = distance   A = spin up   Y = idle   B = stop");
        t.addData("Test distance", "%.0f in -> table says %.0f RPM", testDistanceIn, Launcher.rpmForDistance(testDistanceIn));
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
