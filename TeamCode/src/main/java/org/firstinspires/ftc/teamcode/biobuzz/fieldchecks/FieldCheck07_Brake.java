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
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck07_Brake.java  -  how fast does the robot slow down on THIS field's tiles?   ROLE: OpMode (field check)
 *
 * NEEDS 4 CLEAR TILES ahead, then 4 to the robot's left. The robot drives itself at full power.
 *   A = run FORWARD: full power until BRAKE_TEST_SPEED_IN_PER_S, then coast to a stop.
 *   A = run SIDEWAYS (to the left), the same way.
 * THE METHOD is a copy of Pedro's ForwardDeceleration / StrafeDeceleration tuners (in the
 * Quickstart's pedro/procedures/ForesightTuner.java, which team code cannot call): power 1
 * until the speed, then drivetrain.stop(false) (coast), and average the slow-down until the
 * speed is under 1 in/s. So the numbers compare directly with naturalForwardDeceleration and
 * naturalStrafeDeceleration in pedro/Constants.java (filled in at drive tuning).
 * PASS: within BRAKE_TOLERANCE (15%) of the home baseline. On WARN or FAIL, the screen prints
 * the measured value: use it in pedro/Constants.java for this event, then re-run Field Check 08.
 * STOP on the Driver Station stops the robot at any time.
 */
@TeleOp(name = "Field Check 07: Brake", group = "Field Checks")
public class FieldCheck07_Brake extends OpMode {

    private enum Phase {
        READY,      // waiting for A
        SPEEDING,   // full power until the test speed
        COASTING,   // wheels coast; the slow-down is measured
        DONE
    }

    private static final double STOPPED_IN_PER_S = 1;   // the tuner's "stopped"

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Drive drive;
    private Phase phase = Phase.READY;
    private boolean sideways = false;                // false = the forward run, true = the sideways run
    private final StateTimer phaseTimer = new StateTimer();
    private final StateTimer sinceLastLoop = new StateTimer();
    private double lastSpeed = 0;
    private double decelTotal = 0;
    private int decelCount = 0;
    private double forwardDecel = 0;
    private double strafeDecel = 0;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        drive = new Drive(hardwareMap);
        t.addLine("Field Check 07: the robot drives itself at FULL POWER. 4 clear tiles ahead and to its left.");
        t.update();
    }

    @Override
    public void start() {
        FieldCheckHelper.start("FieldCheck07_Brake", battery.getVoltage());
        drive.zeroPose();
    }

    @Override
    public void loop() {
        boolean go = pad.isAPressed();
        drive.updatePoseOnly();   // we drive the motors ourselves, so Pedro must not set them
        double speed = drive.getForwardSpeedInPerS();
        if (sideways) {
            speed = drive.getLeftSpeedInPerS();
        }

        switch (phase) {
            case READY:
                if (sideways) {
                    t.addLine("A = run SIDEWAYS (to the robot's left)");
                } else {
                    t.addLine("A = run FORWARD");
                }
                if (go) {
                    phase = Phase.SPEEDING;
                    phaseTimer.reset();
                }
                break;
            case SPEEDING:
                FieldCheckHelper.noteDrivingBattery(battery.getVoltage());
                if (sideways) {
                    drive.driveDirectly(0, 1);
                } else {
                    drive.driveDirectly(1, 0);
                }
                if (Math.abs(speed) > FieldCheckConfig.BRAKE_TEST_SPEED_IN_PER_S
                        || phaseTimer.isTimeUp(FieldCheckConfig.BRAKE_MAX_DRIVE_MS)) {
                    drive.coast();
                    lastSpeed = speed;
                    decelTotal = 0;
                    decelCount = 0;
                    sinceLastLoop.reset();
                    phase = Phase.COASTING;
                }
                break;
            case COASTING:
                drive.coast();
                double dt = sinceLastLoop.seconds();
                sinceLastLoop.reset();
                if (dt > 0) {
                    decelTotal += (speed - lastSpeed) / dt;
                    decelCount++;
                }
                lastSpeed = speed;
                if (Math.abs(speed) <= STOPPED_IN_PER_S) {
                    finishRun();
                }
                break;
            case DONE:
                drive.coast();
                break;
        }
        t.addData("Speed", "%.1f in/s", speed);
        t.addData("Measured", "forward %.1f, sideways %.1f in/s/s", forwardDecel, strafeDecel);
        FieldCheckHelper.show(t, 7);
        t.update();
    }

    private void finishRun() {
        double decel = 0;
        if (decelCount > 0) {
            decel = Math.abs(decelTotal / decelCount);
        }
        if (!sideways) {
            forwardDecel = decel;
            sideways = true;
            phase = Phase.READY;
            return;
        }
        strafeDecel = decel;
        phase = Phase.DONE;
        String grade = FieldCheckHelper.worse(
                FieldCheckHelper.gradeAgainstBaseline(forwardDecel, FieldCheckConfig.BASELINE_FORWARD_DECEL, FieldCheckConfig.BRAKE_TOLERANCE),
                FieldCheckHelper.gradeAgainstBaseline(strafeDecel, FieldCheckConfig.BASELINE_STRAFE_DECEL, FieldCheckConfig.BRAKE_TOLERANCE));
        String text = String.format("forward %.1f (baseline %.1f), sideways %.1f (baseline %.1f) in/s/s",
                forwardDecel, FieldCheckConfig.BASELINE_FORWARD_DECEL, strafeDecel, FieldCheckConfig.BASELINE_STRAFE_DECEL);
        if (!grade.equals(FieldCheckHelper.PASS)) {
            text += ": use these in pedro/Constants for this event";
        }
        FieldCheckHelper.record(7, grade, text);
    }

    @Override
    public void stop() {
        drive.coast();
        FieldCheckHelper.stop();
    }
}
