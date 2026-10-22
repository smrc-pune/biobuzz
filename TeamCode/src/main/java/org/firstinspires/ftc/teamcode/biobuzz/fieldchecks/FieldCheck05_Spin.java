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
 * FieldCheck05_Spin.java  -  does the gyro count full turns right?            ROLE: OpMode (field check)
 *
 * 1. Robot edge on a tile seam. A = start.
 * 2. The crew turns the robot by hand, SPIN_TURNS (4) full turns, back to the same seam. A = done.
 * PASS: the heading is back within +/- SPIN_TOLERANCE_DEG (2 deg) of where it started.
 * FAIL: the Pinpoint is loose or its yaw scale is off: fix the mount, then re-tune.
 */
@TeleOp(name = "Field Check 05: Spin", group = "Field Checks")
public class FieldCheck05_Spin extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Drive drive;
    private boolean spinning = false;
    private boolean done = false;
    private double lastHeadingDeg = 0;
    private double turnedDeg = 0;   // adds up every bit of turning, so 4 turns = 1440

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        drive = new Drive(hardwareMap);
        t.addLine("Field Check 05: the crew turns the robot by hand. Press PLAY.");
        t.update();
    }

    @Override
    public void start() { FieldCheckHelper.start("FieldCheck05_Spin", battery.getVoltage()); }

    @Override
    public void loop() {
        boolean next = pad.isAPressed();
        drive.updatePoseOnly();
        double headingDeg = drive.getHeadingDegrees();

        if (spinning) {
            double change = headingDeg - lastHeadingDeg;   // the short way round, -180 to 180
            if (change > 180) change -= 360;
            if (change < -180) change += 360;
            turnedDeg += change;
        }
        lastHeadingDeg = headingDeg;

        if (!spinning && !done) {
            t.addLine("Robot edge on a seam. A = start.");
            if (next) {
                drive.zeroPose();
                drive.updatePoseOnly();
                lastHeadingDeg = drive.getHeadingDegrees();
                turnedDeg = 0;
                spinning = true;
            }
        } else if (spinning) {
            t.addLine(String.format("Turn it %.0f full turns, back to the seam. A = done.", FieldCheckConfig.SPIN_TURNS));
            t.addData("Turned", "%.1f turns", turnedDeg / 360);
            if (next) {
                spinning = false;
                done = true;
                double errorDeg = Math.abs(turnedDeg) - FieldCheckConfig.SPIN_TURNS * 360;
                FieldCheckHelper.record(5, FieldCheckHelper.grade(errorDeg, FieldCheckConfig.SPIN_TOLERANCE_DEG),
                        String.format("%.1f deg off after %.0f turns", errorDeg, FieldCheckConfig.SPIN_TURNS));
            }
        }
        FieldCheckHelper.show(t, 5);
        t.update();
    }

    @Override
    public void stop() { FieldCheckHelper.stop(); }
}
