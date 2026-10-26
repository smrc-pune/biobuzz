package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LauncherConfig;
import org.firstinspires.ftc.teamcode.biobuzz.Robot;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;

/**
 * STUDENTS: you will change this file.
 *
 * LaunchTest.java  -  test the LAUNCH state machine (feeder + flywheel together).   ROLE: OpMode (test)
 *
 * It builds the whole Robot (the shooter needs the feeder, the launcher and Drive),
 * but the robot does not drive. Put balls in the hopper and the robot where you want to test.
 * CONTROLS (gamepad 1):   A = launch 4    B = cancel    Y = spin up early
 *                         X = "HIVE tipped" (switches the upward CELL; launching waits TIP_PAUSE_MS)
 *                         D-pad down = stop all mechanisms
 * WHAT TO WATCH: the Launch line goes SPINNING_UP -> FEEDING -> RECOVERING, 4 times, then
 * IDLE "all launched".
 * TO TUNE THE LAUNCH SPEED: put the robot on our launch spot (FieldConfig.LAUNCH_SPOT), facing
 * the upward CELL. Launch 4. Short: raise LauncherConfig.LAUNCH_RPM in the Dashboard (try 100
 * at a time). Long: lower it. Repeat until all 4 go in, then type the number into LauncherConfig.
 */
@TeleOp(name = "Test: Launch", group = "Tests")
public class LaunchTest extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private Robot robot;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        robot = new Robot(hardwareMap);
    }

    @Override
    public void start() {
        robot.startMatch("LaunchTest", 0);   // 0 = no match clock; still writes a match log
        robot.drive.setStartingPoseFacingAway();
    }

    @Override
    public void loop() {
        boolean launch = pad.isAPressed();
        boolean cancel = pad.isBPressed();
        boolean spinUp = pad.isYPressed();
        boolean tipped = pad.isXPressed();
        boolean stopAll = pad.isDpadDownPressed();

        if (launch) {
            robot.shooter.launch(4);
        }
        if (cancel) {
            robot.shooter.cancelLaunch();
        }
        if (spinUp) {
            robot.shooter.spinUp();
        }
        if (tipped) {
            MatchInfo.markHiveTipped();
        }
        if (stopAll) {
            robot.stopMechanisms();
        }
        robot.update();

        t.addLine("A = launch 4   B = cancel   Y = spin up   X = HIVE tipped   D-pad down = stop all");
        t.addData("isLaunching()", robot.shooter.isLaunching());
        t.addData("LAUNCH_RPM", "%.0f", LauncherConfig.LAUNCH_RPM);
        robot.addTelemetry(t);
        t.update();
    }

    @Override
    public void stop() {
        robot.endMatch();
    }
}
