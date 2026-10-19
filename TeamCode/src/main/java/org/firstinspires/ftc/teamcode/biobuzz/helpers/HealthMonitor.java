package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HealthConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;

/**
 * MENTORS: you do not need to change this file.
 *
 * HealthMonitor.java  -  watches the robot every loop and raises ALERTs.         ROLE: helper
 *
 * Robot owns one and calls update() once per loop, after drive.update(). It checks:
 *   SLOW LOOP     average over 1 s above SLOW_LOOP_AVERAGE_MS, or one loop above SLOW_LOOP_WORST_MS
 *   LOW BATTERY   below PLAY_LOW_BATTERY_V for PLAY_LOW_BATTERY_MS (urgent, once per match)
 *   PINPOINT      status is not READY: robot-centric and "position unknown" (urgent)
 *   HEADING JUMP  the heading changed faster than any robot can turn: robot-centric (urgent)
 * It never stops the robot. The driver recovers with Y (facing away) and, for assists,
 * "set pose here". All numbers are in HealthConfig.
 *
 * TRACING A BUG: every ALERT is an EVENT row in the match log, next to the DATA rows
 * (battery_v, loop_ms, pinpoint_status, heading_deg) that show what led up to it.
 */
public class HealthMonitor {

    private final Drive drive;
    private final BatteryMonitor battery;

    // loop time
    private final ElapsedTime sinceLoop = new ElapsedTime();
    private final ElapsedTime sinceWindow = new ElapsedTime();
    private double loopMs = 0;
    private double worstLoopSinceRowMs = 0;
    private double worstLoopMs = 0;
    private double windowTotalMs = 0;
    private double windowWorstMs = 0;
    private int windowLoops = 0;

    // battery
    private final ElapsedTime batteryLowFor = new ElapsedTime();
    private boolean lowBatteryAlerted = false;
    private double lowestVolts = 0;

    // Pinpoint and heading
    private GoBildaPinpointDriver.DeviceStatus lastStatus = GoBildaPinpointDriver.DeviceStatus.READY;
    private double lastHeadingDeg = 0;
    private int lastHeadingSetCount = 0;

    public HealthMonitor(Drive drive, BatteryMonitor battery) {
        this.drive = drive;
        this.battery = battery;
    }

    /** PLAY: start counting from now. */
    public void start() {
        sinceLoop.reset();
        sinceWindow.reset();
        worstLoopMs = 0;
        worstLoopSinceRowMs = 0;
        windowTotalMs = 0;
        windowWorstMs = 0;
        windowLoops = 0;
        batteryLowFor.reset();
        lowBatteryAlerted = false;
        lowestVolts = battery.getVoltage();
        lastStatus = GoBildaPinpointDriver.DeviceStatus.READY;   // so a bad status at PLAY is caught
        lastHeadingDeg = drive.getHeadingDegrees();
        lastHeadingSetCount = drive.getHeadingSetCount();
    }

    /** Once per loop, after drive.update(). */
    public void update() {
        checkLoopTime();
        checkBattery();
        checkPinpoint();
        checkHeadingJump();
    }

    private void checkLoopTime() {
        loopMs = sinceLoop.milliseconds();
        sinceLoop.reset();
        worstLoopMs = Math.max(worstLoopMs, loopMs);
        worstLoopSinceRowMs = Math.max(worstLoopSinceRowMs, loopMs);
        windowTotalMs += loopMs;
        windowWorstMs = Math.max(windowWorstMs, loopMs);
        windowLoops++;
        if (sinceWindow.seconds() < 1) return;   // judge once per second
        double averageMs = windowTotalMs / windowLoops;
        if (averageMs > HealthConfig.SLOW_LOOP_AVERAGE_MS || windowWorstMs > HealthConfig.SLOW_LOOP_WORST_MS) {
            Alerts.raise(String.format("SLOW LOOP: average %.0f ms, worst %.0f ms", averageMs, windowWorstMs), false);
        }
        sinceWindow.reset();
        windowTotalMs = 0;
        windowWorstMs = 0;
        windowLoops = 0;
    }

    private void checkBattery() {
        double volts = battery.getVoltage();
        if (volts <= 0) return;   // no reading
        if (lowestVolts <= 0 || volts < lowestVolts) lowestVolts = volts;
        if (volts >= HealthConfig.PLAY_LOW_BATTERY_V) {
            batteryLowFor.reset();
            return;
        }
        if (!lowBatteryAlerted && batteryLowFor.milliseconds() > HealthConfig.PLAY_LOW_BATTERY_MS) {
            lowBatteryAlerted = true;
            Alerts.raise(String.format("LOW BATTERY %.1f V", volts), true);
        }
    }

    private void checkPinpoint() {
        GoBildaPinpointDriver.DeviceStatus status = drive.getPinpointStatus();
        if (status == lastStatus) return;   // only act when the status changes
        lastStatus = status;
        MatchLogger.event("PINPOINT", status.toString());
        if (status == GoBildaPinpointDriver.DeviceStatus.READY) return;   // Y turns field-centric back on
        drive.useRobotCentric("Pinpoint " + status);
        drive.forgetPose("Pinpoint " + status);
        Alerts.raise("PINPOINT " + status + ": robot-centric. Y when READY", true);
    }

    private void checkHeadingJump() {
        double headingDeg = drive.getHeadingDegrees();
        int setCount = drive.getHeadingSetCount();
        // A heading WE set (Y, set pose here) is not a jump: skip that loop.
        if (setCount == lastHeadingSetCount) {
            double changeDeg = Math.abs(AngleUnit.normalizeDegrees(headingDeg - lastHeadingDeg));
            double allowedDeg = HealthConfig.MAX_TURN_DEG_PER_S * loopMs / 1000
                    + HealthConfig.HEADING_JUMP_MARGIN_DEG;
            if (changeDeg > allowedDeg) {
                drive.useRobotCentric(String.format("heading jump %.0f deg", changeDeg));
                Alerts.raise(String.format("HEADING JUMP %.0f deg: robot-centric. Y facing away", changeDeg), true);
            }
        }
        lastHeadingDeg = headingDeg;
        lastHeadingSetCount = setCount;
    }

    // ==================== for telemetry and the match log ====================

    public double getLoopMs() { return loopMs; }

    /** Loop time and battery columns of a DATA row. */
    public void addLogData() {
        MatchLogger.value("loop_ms", loopMs);
        MatchLogger.value("worst_loop_ms", worstLoopSinceRowMs);   // so spikes between rows are not lost
        MatchLogger.value("battery_v", battery.getVoltage());
        worstLoopSinceRowMs = 0;
    }

    /** For the log's END line. */
    public String getSummary() {
        return String.format("min_battery_v=%.2f worst_loop_ms=%.0f", lowestVolts, worstLoopMs);
    }
}
