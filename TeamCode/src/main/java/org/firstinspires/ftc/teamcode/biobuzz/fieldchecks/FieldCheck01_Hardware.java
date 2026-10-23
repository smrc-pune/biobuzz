package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.HardwareDevice;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldCheckConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck01_Hardware.java  -  is every device there and answering?         ROLE: OpMode (field check)
 *
 * The robot does NOTHING. PASS when: every name in HardwareNames.ALL_DEVICES is found
 * (each missing one is listed), both hubs answer, the Pinpoint is READY and the HuskyLens
 * answers. It uses hardwareMap.tryGet(...), so a missing device is listed, not a crash.
 * Press PLAY and wait 2 seconds.
 */
@TeleOp(name = "Field Check 01: Hardware", group = "Field Checks")
public class FieldCheck01_Hardware extends OpMode {

    private static final double WAIT_S = 2;   // give the Pinpoint time to report READY

    private Telemetry t;
    private BatteryMonitor battery;
    private GoBildaPinpointDriver pinpoint;
    private final StateTimer timer = new StateTimer();
    private boolean done = false;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        battery = new BatteryMonitor(hardwareMap);
        pinpoint = hardwareMap.tryGet(GoBildaPinpointDriver.class, HardwareNames.PINPOINT);
        t.addLine("Field Check 01: the robot does nothing. Press PLAY.");
        t.update();
    }

    @Override
    public void start() {
        FieldCheckHelper.start("FieldCheck01_Hardware", battery.getVoltage());
        timer.reset();
    }

    @Override
    public void loop() {
        if (pinpoint != null) {
            pinpoint.update();
        }
        if (!done && timer.seconds() >= WAIT_S) {
            done = true;
            check();
        }
        if (!done) {
            t.addLine("checking...");
        }
        FieldCheckHelper.show(t, 1);
        t.update();
    }

    private void check() {
        String missing = "";
        for (String name : HardwareNames.ALL_DEVICES) {
            if (hardwareMap.tryGet(HardwareDevice.class, name) == null) {
                missing += name + " ";
            }
        }
        int hubsAnswering = 0;
        for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
            if (!hub.isNotResponding() && hub.getInputVoltage(VoltageUnit.VOLTS) > 0) {
                hubsAnswering++;
            }
        }
        String pinpointStatus = "MISSING";
        if (pinpoint != null) {
            pinpointStatus = pinpoint.getDeviceStatus().toString();
        }
        HuskyLens huskyLens = hardwareMap.tryGet(HuskyLens.class, HardwareNames.HUSKYLENS_HIVE);
        boolean huskyLensAnswers = huskyLens != null && huskyLens.knock();

        boolean pass = missing.isEmpty() && hubsAnswering >= FieldCheckConfig.EXPECTED_HUBS
                && pinpointStatus.equals("READY") && huskyLensAnswers;
        String huskyLensText = "NO ANSWER";
        if (huskyLensAnswers) {
            huskyLensText = "answers";
        }
        String text = "hubs " + hubsAnswering + " of " + FieldCheckConfig.EXPECTED_HUBS
                + ", Pinpoint " + pinpointStatus + ", HuskyLens " + huskyLensText;
        if (!missing.isEmpty()) {
            text = "MISSING: " + missing + "; " + text;
        }
        if (pass) {
            FieldCheckHelper.record(1, FieldCheckHelper.PASS, text);
        } else {
            FieldCheckHelper.record(1, FieldCheckHelper.FAIL, text);
        }
    }

    @Override
    public void stop() {
        FieldCheckHelper.stop();
    }
}
