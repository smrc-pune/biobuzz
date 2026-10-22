package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldCheckConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck04_Push.java  -  do the odometry pods measure distance right?     ROLE: OpMode (field check)
 *
 * The crew PUSHES the robot by hand (the motors stay off):
 *   1. Robot's back edge on a tile seam, facing along the seam. A = start.
 *   2. Push it straight FORWARD 2 tiles (48 in) to the next-but-one seam. A = done.
 *   3. Line it up on a seam again. A = start. Push it SIDEWAYS 2 tiles. A = done.
 * PASS: distance within +/- PUSH_TOLERANCE_IN, sideways drift within +/- PUSH_DRIFT_TOLERANCE_IN.
 * FAIL: a pod, the Pinpoint mount or a cable was damaged in travel. Fix it first.
 * Measure against TILE SEAMS (every 24 in), never tape: we may not mark event fields.
 */
@TeleOp(name = "Field Check 04: Push", group = "Field Checks")
public class FieldCheck04_Push extends OpMode {

    private enum Phase {
        READY_FORWARD,     // robot on a seam: A starts the forward push
        PUSHING_FORWARD,   // crew pushes 2 tiles forward: A when there
        READY_SIDEWAYS,    // robot on a seam: A starts the sideways push
        PUSHING_SIDEWAYS,  // crew pushes 2 tiles sideways: A when there
        DONE
    }

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Drive drive;
    private Phase phase = Phase.READY_FORWARD;
    private String grade = FieldCheckHelper.PASS;
    private String text = "";

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        drive = new Drive(hardwareMap);
        t.addLine("Field Check 04: the crew pushes the robot by hand. Press PLAY.");
        t.update();
    }

    @Override
    public void start() { FieldCheckHelper.start("FieldCheck04_Push", battery.getVoltage()); }

    @Override
    public void loop() {
        boolean next = pad.isAPressed();
        drive.updatePoseOnly();   // read the Pinpoint; the motors stay off

        switch (phase) {
            case READY_FORWARD:
                t.addLine("Back edge on a seam, facing along it. A = start the FORWARD push.");
                if (next) startPush(Phase.PUSHING_FORWARD);
                break;
            case PUSHING_FORWARD:
                t.addLine("Push straight FORWARD 2 tiles (48 in) to a seam. A = there.");
                if (next) {
                    measure("forward", drive.getX(), drive.getY());
                    phase = Phase.READY_SIDEWAYS;
                }
                break;
            case READY_SIDEWAYS:
                t.addLine("Side edge on a seam. A = start the SIDEWAYS push.");
                if (next) startPush(Phase.PUSHING_SIDEWAYS);
                break;
            case PUSHING_SIDEWAYS:
                t.addLine("Push straight SIDEWAYS 2 tiles (48 in) to a seam. A = there.");
                if (next) {
                    measure("sideways", drive.getY(), drive.getX());
                    FieldCheckHelper.record(4, grade, text);
                    phase = Phase.DONE;
                }
                break;
            case DONE:
                break;
        }
        t.addData("Moved", "x %.2f in, y %.2f in", drive.getX(), drive.getY());
        FieldCheckHelper.show(t, 4);
        t.update();
    }

    private void startPush(Phase pushPhase) {
        drive.zeroPose();
        phase = pushPhase;
    }

    /** along = the distance pushed (either way), across = drift off the line. */
    private void measure(String name, double alongIn, double acrossIn) {
        double distanceError = Math.abs(alongIn) - FieldCheckConfig.PUSH_DISTANCE_IN;
        grade = FieldCheckHelper.worse(grade, FieldCheckHelper.grade(distanceError, FieldCheckConfig.PUSH_TOLERANCE_IN));
        grade = FieldCheckHelper.worse(grade, FieldCheckHelper.grade(acrossIn, FieldCheckConfig.PUSH_DRIFT_TOLERANCE_IN));
        text += String.format("%s %.2f in (drift %.2f in)  ", name, Math.abs(alongIn), Math.abs(acrossIn));
    }

    @Override
    public void stop() { FieldCheckHelper.stop(); }
}
