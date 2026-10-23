package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldCheckConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;
import org.firstinspires.ftc.teamcode.biobuzz.drive.FieldPaths;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck09_Square.java  -  does a Pedro square come back to its start?     ROLE: OpMode (field check)
 *
 * Place the robot on a tile seam crossing, with 3 clear tiles ahead and to its left.
 * A = Pedro drives a SQUARE_SIDE_IN (48 in) square (ahead, left, back, right), same heading.
 * PASS: back within SQUARE_MAX_ERROR_IN (1.5 in) and SQUARE_MAX_HEADING_DEG (3 deg) of the start.
 * FAIL: re-run Pedro's tuners (translational and heading), docs/TUNING.md.
 */
@TeleOp(name = "Field Check 09: Square", group = "Field Checks")
public class FieldCheck09_Square extends OpMode {

    private enum Phase { READY, DRIVING, SETTLING, DONE }

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Drive drive;
    private Phase phase = Phase.READY;
    private final StateTimer phaseTimer = new StateTimer();

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        drive = new Drive(hardwareMap);
        t.addLine("Field Check 09: Pedro drives a 48 in square. 3 clear tiles ahead and to the left.");
        t.update();
    }

    @Override
    public void start() { FieldCheckHelper.start("FieldCheck09_Square", battery.getVoltage()); }

    @Override
    public void loop() {
        boolean go = pad.isAPressed();
        switch (phase) {
            case READY:
                t.addLine("Robot on a seam crossing. A = drive the square.");
                if (go) {
                    drive.zeroPose();
                    drive.followPath(FieldPaths.square(drive.getPose(), FieldCheckConfig.SQUARE_SIDE_IN));
                    phase = Phase.DRIVING;
                    phaseTimer.reset();
                }
                break;
            case DRIVING:
                FieldCheckHelper.noteDrivingBattery(battery.getVoltage());
                if (drive.isPathDone() || phaseTimer.isTimeUp(2 * FieldCheckConfig.PATH_TIMEOUT_MS)) {
                    phase = Phase.SETTLING;
                    phaseTimer.reset();
                }
                break;
            case SETTLING:
                if (phaseTimer.isTimeUp(FieldCheckConfig.SETTLE_MS)) {
                    finish();
                }
                break;
            case DONE:
                break;
        }
        drive.update();
        drive.addDrawing();
        FieldDrawing.send();
        t.addData("Pose", "x %.2f in, y %.2f in, %.1f deg", drive.getX(), drive.getY(), drive.getHeadingDegrees());
        FieldCheckHelper.show(t, 9);
        t.update();
    }

    private void finish() {
        phase = Phase.DONE;
        double errorIn = Math.hypot(drive.getX(), drive.getY());   // the start was (0, 0)
        double headingErrorDeg = Math.abs(AngleUnit.normalizeDegrees(drive.getHeadingDegrees()));
        String grade = FieldCheckHelper.worse(
                FieldCheckHelper.grade(errorIn, FieldCheckConfig.SQUARE_MAX_ERROR_IN),
                FieldCheckHelper.grade(headingErrorDeg, FieldCheckConfig.SQUARE_MAX_HEADING_DEG));
        FieldCheckHelper.record(9, grade, String.format("end error %.2f in, %.1f deg", errorIn, headingErrorDeg));
    }

    @Override
    public void stop() {
        drive.stopDrivingItself();
        drive.update();
        FieldCheckHelper.stop();
    }
}
