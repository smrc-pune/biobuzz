package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.ControlsConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HealthConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LogConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;

/**
 * MENTORS: you do not need to change this file.
 *
 * RobotCheck.java  -  the one-minute check in the pits, before EVERY match.   ROLE: OpMode (test)
 * PATTERN: a step list (enum Step + switch), like Autonomous.
 *
 *   BATTERY -> PINPOINT -> GYRO_STILL -> DRIVE_MOTORS -> LOGGER -> DONE: "READY FOR MATCH"
 *
 * Each step measures, then shows PASS or FAIL.  A = next step,  B = repeat this step.
 * DRIVE_MOTORS spins each wheel for a moment, one at a time: A = it turned FORWARD,
 * X = it turned the WRONG way, B = spin it again.
 * The Pedro follower is not built: the Pinpoint and the drive motors come straight from
 * hardwareMap.tryGet(...), so a missing device is a FAIL, not a crash.
 * Every limit is in HealthConfig (CHECK_...). Things code cannot see (cables seated,
 * battery strapped down) are on the paper checklist in docs/MATCH_DAY.md.
 */
@TeleOp(name = "Robot Check", group = "Tests")
public class RobotCheck extends OpMode {

    private enum Step {
        BATTERY,        // resting voltage over CHECK_BATTERY_S
        PINPOINT,       // status READY, update rate steady
        GYRO_STILL,     // hands off: the heading must not creep
        DRIVE_MOTORS,   // each wheel spins forward, one at a time; the crew confirms
        LOGGER,         // match log on, enough free storage
        DONE            // READY FOR MATCH, or the list of failed steps
    }

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private GoBildaPinpointDriver pinpoint;               // null if missing
    private final DcMotorEx[] driveMotors = new DcMotorEx[HardwareNames.DRIVE_MOTORS.length];

    private Step step = Step.BATTERY;
    private final StateTimer timer = new StateTimer();
    private final boolean[] passed = new boolean[Step.values().length];
    private String result = "";            // the current step's PASS/FAIL text ("" = still measuring)

    // measurements of the current step
    private double voltsTotal = 0;
    private int voltsCount = 0;
    private double lowestHz = 0, highestHz = 0;
    private double startHeadingDeg = 0, worstDriftDeg = 0;
    private int motorNumber = 0;           // which drive motor DRIVE_MOTORS is on
    private boolean motorsAllForward = true;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        pinpoint = hardwareMap.tryGet(GoBildaPinpointDriver.class, HardwareNames.PINPOINT);
        boolean[] reversed = {
                HardwareNames.MOTOR_DRIVE_LEFT_FRONT_REVERSED, HardwareNames.MOTOR_DRIVE_RIGHT_FRONT_REVERSED,
                HardwareNames.MOTOR_DRIVE_LEFT_BACK_REVERSED, HardwareNames.MOTOR_DRIVE_RIGHT_BACK_REVERSED };
        for (int i = 0; i < driveMotors.length; i++) {
            driveMotors[i] = hardwareMap.tryGet(DcMotorEx.class, HardwareNames.DRIVE_MOTORS[i]);
            if (driveMotors[i] == null) continue;
            if (reversed[i]) driveMotors[i].setDirection(DcMotor.Direction.REVERSE);
            else driveMotors[i].setDirection(DcMotor.Direction.FORWARD);
        }
    }

    @Override
    public void init_loop() {
        t.addLine("ROBOT CHECK: about one minute. Robot on the floor or on blocks, clear around it.");
        t.addLine("Press PLAY to start. A = next step, B = repeat a step.");
        t.update();
    }

    @Override
    public void start() {
        MatchLogger.start("RobotCheck", battery.getVoltage());
        goTo(Step.BATTERY);
    }

    @Override
    public void loop() {
        boolean next = pad.isAPressed();
        boolean repeat = pad.isBPressed();
        boolean wrongWay = pad.isXPressed();
        if (pinpoint != null) pinpoint.update();   // no follower here, so we read the Pinpoint ourselves

        switch (step) {
            case BATTERY:
                voltsTotal += battery.getVoltage();
                voltsCount++;
                if (result.isEmpty() && timer.seconds() >= HealthConfig.CHECK_BATTERY_S) {
                    double volts = voltsTotal / voltsCount;
                    finish(volts >= HealthConfig.CHECK_BATTERY_MIN_V, String.format("%.2f V resting (need %.1f V)",
                            volts, HealthConfig.CHECK_BATTERY_MIN_V));
                }
                break;
            case PINPOINT:
                checkPinpoint();
                break;
            case GYRO_STILL:
                checkStill();
                break;
            case DRIVE_MOTORS:
                checkDriveMotors(next, repeat, wrongWay);
                next = false;      // A and B are used inside this step
                repeat = false;
                break;
            case LOGGER:
                double freeMb = MatchLogger.getFreeStorageMb();
                String logging = "OFF (turn on LogConfig.ENABLED)";
                if (LogConfig.ENABLED) logging = "ON";
                finish(LogConfig.ENABLED && freeMb > HealthConfig.CHECK_MIN_FREE_MB,
                        String.format("logging %s, %.0f MB free (need %.0f)", logging, freeMb,
                                HealthConfig.CHECK_MIN_FREE_MB));
                break;
            case DONE:
                showSummary();
                break;
        }

        if (repeat) goTo(step);
        if (next && !result.isEmpty() && step != Step.DONE) goTo(Step.values()[step.ordinal() + 1]);

        if (step != Step.DONE) {
            t.addData("STEP " + (step.ordinal() + 1) + " of 5", step);
            if (result.isEmpty()) t.addLine(String.format("measuring... %.1f s", timer.seconds()));
            else t.addLine(result + "      A = next step, B = repeat");
        }
        t.addData("Battery", String.format("%.1f V", battery.getVoltage()));
        t.update();
    }

    @Override
    public void stop() {
        stopMotors();
        MatchLogger.stop("ready=" + isReady());
    }

    // ==================== the steps ====================

    private void checkPinpoint() {
        if (pinpoint == null) {
            finish(false, "Pinpoint MISSING: check HardwareNames.PINPOINT and its I2C cable");
            return;
        }
        double hz = pinpoint.getFrequency();
        if (lowestHz == 0 || hz < lowestHz) lowestHz = hz;
        highestHz = Math.max(highestHz, hz);
        if (!result.isEmpty() || timer.seconds() < HealthConfig.CHECK_PINPOINT_S) return;
        GoBildaPinpointDriver.DeviceStatus status = pinpoint.getDeviceStatus();
        boolean steady = true;
        String rateText = String.format("%.0f-%.0f Hz", lowestHz, highestHz);
        if (HealthConfig.CHECK_PINPOINT_HZ > 0) {
            double allowed = HealthConfig.CHECK_PINPOINT_HZ * HealthConfig.CHECK_PINPOINT_HZ_TOLERANCE;
            steady = Math.abs(lowestHz - HealthConfig.CHECK_PINPOINT_HZ) <= allowed
                    && Math.abs(highestHz - HealthConfig.CHECK_PINPOINT_HZ) <= allowed;
            rateText += String.format(" (noted %.0f Hz)", HealthConfig.CHECK_PINPOINT_HZ);
        } else {
            rateText += " - type this into HealthConfig.CHECK_PINPOINT_HZ";
        }
        finish(status == GoBildaPinpointDriver.DeviceStatus.READY && steady, status + ", " + rateText);
    }

    private void checkStill() {
        if (pinpoint == null) {
            finish(false, "Pinpoint MISSING");
            return;
        }
        double headingDeg = pinpoint.getHeading(AngleUnit.DEGREES);
        if (timer.milliseconds() < 100) startHeadingDeg = headingDeg;   // first readings: the start
        double driftDeg = Math.abs(AngleUnit.normalizeDegrees(headingDeg - startHeadingDeg));
        worstDriftDeg = Math.max(worstDriftDeg, driftDeg);
        if (result.isEmpty()) {
            t.addLine("HANDS OFF the robot.");
            t.addData("Heading change", String.format("%.2f deg", driftDeg));
        }
        if (result.isEmpty() && timer.seconds() >= HealthConfig.CHECK_STILL_S) {
            finish(worstDriftDeg <= HealthConfig.CHECK_STILL_MAX_DEG, String.format("heading crept %.2f deg (limit %.1f)",
                    worstDriftDeg, HealthConfig.CHECK_STILL_MAX_DEG));
        }
    }

    private void checkDriveMotors(boolean forward, boolean again, boolean wrongWay) {
        if (motorNumber >= driveMotors.length) {   // every motor done: A moves on
            if (result.isEmpty() && motorsAllForward) finish(true, "all 4 wheels forward");
            if (result.isEmpty()) finish(false, "a wheel turned the WRONG way or is missing: fix its REVERSED in HardwareNames");
            if (forward) goTo(Step.LOGGER);
            if (again) goTo(Step.DRIVE_MOTORS);
            return;
        }
        String name = HardwareNames.DRIVE_MOTORS[motorNumber];
        DcMotorEx motor = driveMotors[motorNumber];
        if (motor == null) {
            motorsAllForward = false;
            MatchLogger.event("ROBOTCHECK", name + " MISSING");
            motorNumber++;
            timer.reset();
            return;
        }
        if (timer.milliseconds() < ControlsConfig.ROBOT_CHECK_MOTOR_MS) motor.setPower(ControlsConfig.ROBOT_CHECK_MOTOR_POWER);
        else motor.setPower(0);
        t.addData("Motor", name + "  (" + (motorNumber + 1) + " of 4)");
        t.addLine("Did the wheel turn FORWARD?  A = yes,  X = wrong way,  B = spin again");
        if (again) timer.reset();
        if (forward || wrongWay) {
            motor.setPower(0);
            if (wrongWay) {
                motorsAllForward = false;
                MatchLogger.event("ROBOTCHECK", name + " WRONG WAY");
            } else {
                MatchLogger.event("ROBOTCHECK", name + " forward");
            }
            motorNumber++;
            timer.reset();
        }
    }

    private void showSummary() {
        if (isReady()) {
            t.addLine("*** READY FOR MATCH ***");
        } else {
            t.addLine("NOT READY. Failed:");
            for (Step each : Step.values()) {
                if (each != Step.DONE && !passed[each.ordinal()]) t.addLine("  " + each);
            }
            t.addLine("B = run the whole check again");
        }
    }

    // ==================== bookkeeping ====================

    private void goTo(Step newStep) {
        stopMotors();
        if (newStep == Step.DONE && step == Step.DONE) {   // B on the summary: start again
            newStep = Step.BATTERY;
        }
        step = newStep;
        timer.reset();
        result = "";
        voltsTotal = 0;
        voltsCount = 0;
        lowestHz = 0;
        highestHz = 0;
        worstDriftDeg = 0;
        motorNumber = 0;
        motorsAllForward = true;
        if (step == Step.DONE && isReady()) MatchLogger.event("ROBOTCHECK", "READY FOR MATCH");
        if (step == Step.DONE && !isReady()) MatchLogger.event("ROBOTCHECK", "NOT READY");
    }

    private void finish(boolean pass, String text) {
        passed[step.ordinal()] = pass;
        if (pass) result = "PASS: " + text;
        else result = "FAIL: " + text;
        MatchLogger.event("ROBOTCHECK", step + " " + result);
    }

    private boolean isReady() {
        for (Step each : Step.values()) {
            if (each != Step.DONE && !passed[each.ordinal()]) return false;
        }
        return true;
    }

    private void stopMotors() {
        for (DcMotorEx motor : driveMotors) {
            if (motor != null) motor.setPower(0);
        }
    }
}
