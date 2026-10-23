package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
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
 * FieldCheck08_Line.java  -  does a Pedro straight line end where it should?    ROLE: OpMode (field check)
 *
 * Place the robot's back edge on a tile seam, facing along it, with 3 clear tiles ahead.
 * A = Pedro drives LINE_DISTANCE_IN (48 in) straight ahead and stops, ideally on a seam.
 * It measures the end error (after SETTLE_MS), the overshoot and the settle time.
 * PASS: end error within BASELINE_LINE_ERROR_IN + LINE_TOLERANCE_IN.
 * FAIL: adjust Auto's end poses for this field, or re-run Pedro's tuners for this direction.
 */
@TeleOp(name = "Field Check 08: Line", group = "Field Checks")
public class FieldCheck08_Line extends OpMode {

    private enum Phase { READY, DRIVING, SETTLING, DONE }

    private static final double STOPPED_IN_PER_S = 1;

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Drive drive;
    private Phase phase = Phase.READY;
    private final StateTimer phaseTimer = new StateTimer();
    private Pose target;
    private double farthestIn = 0;
    private double settleS = -1;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        drive = new Drive(hardwareMap);
        t.addLine("Field Check 08: Pedro drives 48 in straight ahead. 3 clear tiles ahead.");
        t.update();
    }

    @Override
    public void start() { FieldCheckHelper.start("FieldCheck08_Line", battery.getVoltage()); }

    @Override
    public void loop() {
        boolean go = pad.isAPressed();
        switch (phase) {
            case READY:
                t.addLine("Back edge on a seam, facing along it. A = drive.");
                if (go) {
                    drive.zeroPose();
                    target = FieldPaths.straightAhead(drive.getPose(), FieldCheckConfig.LINE_DISTANCE_IN).endPose();
                    drive.followPath(FieldPaths.straightAhead(drive.getPose(), FieldCheckConfig.LINE_DISTANCE_IN));
                    farthestIn = 0;
                    settleS = -1;
                    phase = Phase.DRIVING;
                    phaseTimer.reset();
                }
                break;
            case DRIVING:
                FieldCheckHelper.noteDrivingBattery(battery.getVoltage());
                farthestIn = Math.max(farthestIn, drive.getX());
                if (drive.isPathDone() || phaseTimer.isTimeUp(FieldCheckConfig.PATH_TIMEOUT_MS)) {
                    phase = Phase.SETTLING;
                    phaseTimer.reset();
                }
                break;
            case SETTLING:
                farthestIn = Math.max(farthestIn, drive.getX());
                if (settleS < 0 && drive.getSpeedInPerS() < STOPPED_IN_PER_S) {
                    settleS = phaseTimer.seconds();
                }
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
        FieldCheckHelper.show(t, 8);
        t.update();
    }

    private void finish() {
        phase = Phase.DONE;
        double errorIn = Math.hypot(drive.getX() - target.x(), drive.getY() - target.y());
        double overshootIn = Math.max(0, farthestIn - FieldCheckConfig.LINE_DISTANCE_IN);
        String grade = FieldCheckHelper.grade(Math.max(0, errorIn - FieldCheckConfig.BASELINE_LINE_ERROR_IN),
                FieldCheckConfig.LINE_TOLERANCE_IN);
        if (FieldCheckConfig.BASELINE_LINE_ERROR_IN <= 0 && grade.equals(FieldCheckHelper.PASS)) {
            grade = FieldCheckHelper.WARN;
        }
        FieldCheckHelper.record(8, grade, String.format("end error %.2f in (baseline %.2f), overshoot %.2f in, settle %.2f s",
                errorIn, FieldCheckConfig.BASELINE_LINE_ERROR_IN, overshootIn, settleS));
    }

    @Override
    public void stop() {
        drive.stopDrivingItself();
        drive.update();
        FieldCheckHelper.stop();
    }
}
