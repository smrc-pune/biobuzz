package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.biobuzz.configurations.HealthConfig;

import java.util.List;

/**
 * MENTORS: you do not need to change this file.
 *
 * BatteryMonitor.java  -  the battery's real voltage, measured by the hubs.     ROLE: helper
 *
 * The Control Hub and the Expansion Hub each measure the battery voltage. We show the
 * LOWEST of them, because a dip on either hub matters.
 *
 * Asking a hub for its voltage takes time, so we ask at most every
 * HealthConfig.BATTERY_SAMPLE_MS (100 ms) and reuse the last answer in between.
 *
 *   BatteryMonitor battery = new BatteryMonitor(hardwareMap);
 *   double volts = battery.getVoltage();      // for example 12.6
 *
 * A full battery reads about 13 V or more at rest; it drops while the motors work hard.
 * The lowest voltage since PLAY, the low-battery ALERTs and the battery_v log column are in
 * helpers/HealthMonitor.java, which uses this class.
 *
 * TRACING A BUG: "0.0 V" means no hub reported a voltage (check the hub is configured).
 */
public class BatteryMonitor {

    private final List<VoltageSensor> sensors;      // one per hub
    private final ElapsedTime sinceSample = new ElapsedTime();
    private double voltage = 0;
    private boolean sampledOnce = false;

    public BatteryMonitor(HardwareMap hardwareMap) {
        sensors = hardwareMap.getAll(VoltageSensor.class);
    }

    /** @return the lowest battery voltage seen by any hub, at most BATTERY_SAMPLE_MS old. */
    public double getVoltage() {
        if (!sampledOnce || sinceSample.milliseconds() >= HealthConfig.BATTERY_SAMPLE_MS) {
            voltage = readLowestVoltage();
            sinceSample.reset();
            sampledOnce = true;
        }
        return voltage;
    }

    private double readLowestVoltage() {
        double lowest = 0;
        for (VoltageSensor sensor : sensors) {
            double volts = sensor.getVoltage();
            if (volts <= 0) {
                continue;   // a hub that gives no reading
            }
            if (lowest == 0 || volts < lowest) {
                lowest = volts;
            }
        }
        return lowest;
    }
}
