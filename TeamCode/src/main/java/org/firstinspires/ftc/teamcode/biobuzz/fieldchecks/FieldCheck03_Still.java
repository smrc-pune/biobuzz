package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldCheckConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck03_Still.java  -  does the heading creep while the robot sits still?   ROLE: OpMode (field check)
 *
 * HANDS OFF for 10 s. PASS: the heading creeps no more than FieldCheckConfig.STILL_MAX_DEG
 * and the Pinpoint stays READY the whole time. FAIL: the Pinpoint or its mount was damaged
 * in travel: fix it before anything else.
 */
@TeleOp(name = "Field Check 03: Still", group = "Field Checks")
public class FieldCheck03_Still extends OpMode {

    private Telemetry t;
    private BatteryMonitor battery;
    private Drive drive;
    private final StateTimer timer = new StateTimer();
    private double startHeadingDeg = 0;
    private double worstCreepDeg = 0;
    private boolean alwaysReady = true;
    private boolean done = false;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        battery = new BatteryMonitor(hardwareMap);
        drive = new Drive(hardwareMap);
        t.addLine("Field Check 03: HANDS OFF the robot for 10 s after PLAY.");
        t.update();
    }

    @Override
    public void start() {
        FieldCheckHelper.start("FieldCheck03_Still", battery.getVoltage());
        drive.updatePoseOnly();
        startHeadingDeg = drive.getHeadingDegrees();
        timer.reset();
    }

    @Override
    public void loop() {
        drive.updatePoseOnly();   // read the Pinpoint; the motors stay off
        if (!done) {
            double creepDeg = Math.abs(AngleUnit.normalizeDegrees(drive.getHeadingDegrees() - startHeadingDeg));
            worstCreepDeg = Math.max(worstCreepDeg, creepDeg);
            if (drive.getPinpointStatus() != GoBildaPinpointDriver.DeviceStatus.READY) alwaysReady = false;
            t.addData("Heading creep", "%.2f deg  (%.0f s left)", creepDeg, FieldCheckConfig.STILL_S - timer.seconds());
        }
        if (!done && timer.seconds() >= FieldCheckConfig.STILL_S) {
            done = true;
            String grade = FieldCheckHelper.grade(worstCreepDeg, FieldCheckConfig.STILL_MAX_DEG);
            String text = String.format("crept %.2f deg (limit %.1f)", worstCreepDeg, FieldCheckConfig.STILL_MAX_DEG);
            if (!alwaysReady) {
                grade = FieldCheckHelper.FAIL;
                text += ", Pinpoint was NOT always READY";
            }
            FieldCheckHelper.record(3, grade, text);
        }
        FieldCheckHelper.show(t, 3);
        t.update();
    }

    @Override
    public void stop() { FieldCheckHelper.stop(); }
}
