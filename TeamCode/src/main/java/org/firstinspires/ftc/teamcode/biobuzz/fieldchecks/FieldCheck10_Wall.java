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
 * FieldCheck10_Wall.java  -  are the walls square with each other, as the gyro sees them?   ROLE: OpMode (field check)
 *
 * 1. Push the robot flat against one wall. A = "this is 0 deg".
 * 2. Push it flat against ANOTHER wall (the next one round, or the same wall facing the other way). A = measure.
 * PASS: the heading is within WALL_MAX_DEG (3 deg) of the nearest 90 deg.
 * A WARN here on an event field often means the walls are not square: drivers should not
 * trust "flat on the wall" for a heading reset there. (Snap-to-90 is a future improvement.)
 */
@TeleOp(name = "Field Check 10: Wall", group = "Field Checks")
public class FieldCheck10_Wall extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Drive drive;
    private boolean zeroed = false;
    private boolean done = false;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        drive = new Drive(hardwareMap);
        t.addLine("Field Check 10: the crew moves the robot from wall to wall. Press PLAY.");
        t.update();
    }

    @Override
    public void start() { FieldCheckHelper.start("FieldCheck10_Wall", battery.getVoltage()); }

    @Override
    public void loop() {
        boolean next = pad.isAPressed();
        drive.updatePoseOnly();
        double headingDeg = drive.getHeadingDegrees();
        double nearest90 = Math.round(headingDeg / 90) * 90;
        double offDeg = headingDeg - nearest90;

        if (!zeroed) {
            t.addLine("Robot flat against a wall. A = this is 0 deg.");
            if (next) {
                drive.zeroPose();
                zeroed = true;
            }
        } else if (!done) {
            t.addLine("Now flat against ANOTHER wall. A = measure.");
            if (next) {
                done = true;
                FieldCheckHelper.record(10, FieldCheckHelper.grade(offDeg, FieldCheckConfig.WALL_MAX_DEG),
                        String.format("%.1f deg, %.1f deg from %.0f", headingDeg, offDeg, nearest90));
            }
        }
        t.addData("Heading", "%.1f deg (%.1f deg from %.0f)", headingDeg, offDeg, nearest90);
        FieldCheckHelper.show(t, 10);
        t.update();
    }

    @Override
    public void stop() { FieldCheckHelper.stop(); }
}
