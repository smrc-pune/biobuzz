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
 * CONTROLS (gamepad 1):   A = launch      B = cancel    Y = spin up early
 *                         X = "HIVE tipped" (switches the upward CELL; launching waits TIP_PAUSE_MS)
 *                         D-pad down = stop all mechanisms
 * WHAT TO WATCH: the Launch line goes SPINNING_UP -> FEEDING, back to SPINNING_UP each time
 * the flywheel slows too much ("flywheel too slow"), then IDLE "hopper empty". The "Fed"
 * line counts up to FEED_TOTAL_MS.
 * TO TUNE FEED_TOTAL_MS: launch a full hopper (4 balls). Balls left over: raise it. The
 * windmill keeps turning long after the last ball: lower it (a little spare time is good).
 * Flywheel slows a lot with each ball: check the PIDF and MIN_FEED_RPM in LauncherConfig.
 * TO TUNE THE LAUNCH SPEED: put the robot on our launch spot (FieldConfig.LAUNCH_SPOT), facing
 * the upward CELL. Launch a full hopper (4 balls). Short: raise LauncherConfig.LAUNCH_RPM in the Dashboard (try 100
 * at a time). Long: lower it. Repeat until all 4 go in, then type the number into LauncherConfig.
 * Change MIN_FEED_RPM with it (about 100 below LAUNCH_RPM).
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
            robot.shooter.launch();
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

        t.addLine("A = launch   B = cancel   Y = spin up   X = HIVE tipped   D-pad down = stop all");
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
