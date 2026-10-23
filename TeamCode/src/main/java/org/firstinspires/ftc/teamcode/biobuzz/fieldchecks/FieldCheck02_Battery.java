package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldCheckConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck02_Battery.java  -  is the battery good enough to judge a field?   ROLE: OpMode (field check)
 *
 * The robot does nothing for 2 s. PASS: resting voltage at least FieldCheckConfig.BATTERY_MIN_V
 * (WARN just below, FAIL more than 0.3 V below). If it is more than BATTERY_DIFFERS_V away
 * from the baseline battery, checks 07-09 are marked "battery differs" (at most WARN).
 * FAIL: charge or swap the battery and start again.
 */
@TeleOp(name = "Field Check 02: Battery", group = "Field Checks")
public class FieldCheck02_Battery extends OpMode {

    private static final double WARN_BELOW_V = 0.3;

    private Telemetry t;
    private BatteryMonitor battery;
    private final StateTimer timer = new StateTimer();
    private double voltsTotal = 0;
    private int samples = 0;
    private boolean done = false;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        battery = new BatteryMonitor(hardwareMap);
        t.addLine("Field Check 02: the robot does nothing for 2 s. Press PLAY.");
        t.update();
    }

    @Override
    public void start() {
        FieldCheckHelper.start("FieldCheck02_Battery", battery.getVoltage());
        timer.reset();
    }

    @Override
    public void loop() {
        if (!done) {
            voltsTotal += battery.getVoltage();
            samples++;
        }
        if (!done && timer.seconds() >= FieldCheckConfig.BATTERY_S) {
            done = true;
            double volts = voltsTotal / samples;
            FieldCheckHelper.setRestingBattery(volts);
            String grade = FieldCheckHelper.PASS;
            if (volts < FieldCheckConfig.BATTERY_MIN_V) {
                grade = FieldCheckHelper.WARN;
            }
            if (volts < FieldCheckConfig.BATTERY_MIN_V - WARN_BELOW_V) {
                grade = FieldCheckHelper.FAIL;
            }
            String text = String.format("%.2f V resting (need %.1f V)", volts, FieldCheckConfig.BATTERY_MIN_V);
            if (FieldCheckHelper.doesBatteryDiffer()) {
                text += String.format(", baseline %.2f V: 07-09 battery differs", FieldCheckConfig.BASELINE_BATTERY_V);
            }
            FieldCheckHelper.record(2, grade, text);
        }
        if (!done) {
            t.addData("Measuring", "%.1f V", battery.getVoltage());
        }
        FieldCheckHelper.show(t, 2);
        t.update();
    }

    @Override
    public void stop() {
        FieldCheckHelper.stop();
    }
}
