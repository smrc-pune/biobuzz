package org.firstinspires.ftc.teamcode.biobuzz.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LauncherConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * STUDENTS: you will change this file.
 *
 * Launcher.java  -  the flywheel that throws balls into the upward CELL.       ROLE: mechanism
 * PATTERN: Adapter + Template Method.
 *
 *   spinUpFor(distanceIn)   spin at the speed for that distance (the speed table in LauncherConfig)
 *   idle()                  spin at IDLE_RPM between launches
 *   isUpToSpeed()           true when the flywheel is within SPEED_TOLERANCE_RPM of its target
 *
 * The speed is held by the motor controller itself (RUN_USING_ENCODER + setVelocity), with
 * the PIDF numbers in LauncherConfig. update() only reads the speed once per loop, and sends
 * new PIDF numbers if someone changed them in the Dashboard.
 *
 * TRACING A BUG: never "up to speed" -> compare RPM and target on telemetry. Far below: the
 * PIDF or TICKS_PER_REV is wrong, or the battery is low. Shots short or long: the speed table.
 */
public class Launcher extends Mechanism {

    private final DcMotorEx flywheel;
    private double targetRpm = 0;
    private double rpm = 0;                // measured in update(), once per loop
    private double appliedP = -1, appliedI = -1, appliedD = -1, appliedF = -1;

    public Launcher(HardwareMap hardwareMap) {
        super("launcher");
        flywheel = hardwareMap.get(DcMotorEx.class, HardwareNames.MOTOR_LAUNCHER);
        if (HardwareNames.MOTOR_LAUNCHER_REVERSED) {
            flywheel.setDirection(DcMotor.Direction.REVERSE);
        } else {
            flywheel.setDirection(DcMotor.Direction.FORWARD);
        }
        flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);   // a setting, not a movement
        flywheel.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);   // coast down, don't brake
        applySpeedControl();
        // Nothing moves here: robots must stay still during INIT (rule G304.H).
    }

    // ---- What the launcher can do ----

    /** Spin at the right speed to reach the upward CELL from this distance (inches). */
    public void spinUpFor(double distanceIn) {
        setTargetRpm(rpmForDistance(distanceIn));
    }

    /** Spin at the idle speed between launches. */
    public void idle() {
        setTargetRpm(LauncherConfig.IDLE_RPM);
    }

    // ---- What you can ask it ----
    public boolean isUpToSpeed() {
        return targetRpm > 0 && Math.abs(rpm - targetRpm) <= LauncherConfig.SPEED_TOLERANCE_RPM;
    }

    public double getRpm() {
        return rpm;
    }

    public double getTargetRpm() {
        return targetRpm;
    }

    /**
     * The speed table: straight-line in-between values, the end values beyond the ends.
     * Example: DISTANCE_IN = {36, 48}, RPM = {2800, 3100}: 42 in -> 2950 RPM.
     */
    public static double rpmForDistance(double distanceIn) {
        double[] distances = LauncherConfig.DISTANCE_IN;
        double[] speeds = LauncherConfig.RPM;
        int last = Math.min(distances.length, speeds.length) - 1;
        if (last < 0) {
            return 0;
        }
        if (distanceIn <= distances[0]) {
            return speeds[0];
        }
        for (int i = 1; i <= last; i++) {
            if (distanceIn <= distances[i]) {
                double fraction = (distanceIn - distances[i - 1]) / (distances[i] - distances[i - 1]);
                return speeds[i - 1] + fraction * (speeds[i] - speeds[i - 1]);
            }
        }
        return speeds[last];
    }

    private void setTargetRpm(double newTargetRpm) {
        if (isFaulted()) {
            return;
        }
        if (newTargetRpm == targetRpm) {
            return;
        }
        targetRpm = newTargetRpm;
        flywheel.setVelocity(targetRpm * LauncherConfig.TICKS_PER_REV / 60.0);   // ticks per second
    }

    /** Send the PIDF numbers to the motor controller, only when they changed. */
    private void applySpeedControl() {
        if (appliedP == LauncherConfig.SPEED_P && appliedI == LauncherConfig.SPEED_I
                && appliedD == LauncherConfig.SPEED_D && appliedF == LauncherConfig.SPEED_F) {
            return;
        }
        appliedP = LauncherConfig.SPEED_P;
        appliedI = LauncherConfig.SPEED_I;
        appliedD = LauncherConfig.SPEED_D;
        appliedF = LauncherConfig.SPEED_F;
        flywheel.setVelocityPIDFCoefficients(appliedP, appliedI, appliedD, appliedF);
    }

    // ---- Filled in for Mechanism ----
    @Override
    protected void update() {
        rpm = flywheel.getVelocity() * 60.0 / LauncherConfig.TICKS_PER_REV;
        applySpeedControl();
    }

    @Override
    protected void stop() {
        targetRpm = 0;
        flywheel.setPower(0);
    }

    @Override
    protected void addDetails(Telemetry t) {
        String ready = "";
        if (isUpToSpeed()) {
            ready = "  READY";
        }
        t.addData("Launcher", String.format("%.0f / %.0f RPM", rpm, targetRpm) + ready);
        if (TelemetryConfig.DETAILS) {
            PIDFCoefficients pidf = flywheel.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER);
            t.addData("Launcher PIDF", String.format("%.1f %.1f %.1f %.1f", pidf.p, pidf.i, pidf.d, pidf.f));
        }
    }

    @Override
    protected void addLogValues() {
        MatchLogger.value("launcher_rpm", rpm);
        MatchLogger.value("launcher_target_rpm", targetRpm);
    }

    @Override
    protected boolean isTelemetryOn() {
        return TelemetryConfig.LAUNCHER;
    }
}
