package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.Robot;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LauncherConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck11_Mechanisms.java  -  one bench check per mechanism, through Robot.   ROLE: OpMode (field check)
 *
 * NO BALLS in the robot; hands clear of the intake and flywheel. Press PLAY:
 *   INTAKE     runs for 1 s                              PASS: not FAULTED
 *   LAUNCHER   spins up (default distance)               PASS: up to speed within SPIN_UP_TIMEOUT_MS
 *   ALL        every mechanism (feeder, cameras too)     PASS: none FAULTED
 * TODO(BioBuzz): add a check for each new mechanism, once the robot is built.
 */
@TeleOp(name = "Field Check 11: Mechanisms", group = "Field Checks")
public class FieldCheck11_Mechanisms extends OpMode {

    private enum Phase { INTAKE, LAUNCHER, DONE }

    private static final double INTAKE_RUN_MS = 1000;

    private Telemetry t;
    private Robot robot;
    private Phase phase = Phase.INTAKE;
    private final StateTimer phaseTimer = new StateTimer();
    private String grade = FieldCheckHelper.PASS;
    private String text = "";

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        robot = new Robot(hardwareMap);
        t.addLine("Field Check 11: NO BALLS, hands clear. The intake and flywheel will run. Press PLAY.");
        t.update();
    }

    @Override
    public void start() {
        FieldCheckHelper.start("FieldCheck11_Mechanisms", robot.battery.getVoltage());
        robot.drive.setStartingPoseFacingAway();
        robot.startIntake();
        phaseTimer.reset();
    }

    @Override
    public void loop() {
        switch (phase) {
            case INTAKE:
                if (phaseTimer.isTimeUp(INTAKE_RUN_MS)) {
                    robot.stopIntake();
                    text += "intake ran; ";
                    robot.spinUpLauncher();
                    phase = Phase.LAUNCHER;
                    phaseTimer.reset();
                }
                break;
            case LAUNCHER:
                if (robot.isLauncherUpToSpeed()) {
                    text += String.format("launcher up to speed in %.1f s; ", phaseTimer.seconds());
                    finish();
                } else if (phaseTimer.isTimeUp(LauncherConfig.SPIN_UP_TIMEOUT_MS)) {
                    grade = FieldCheckHelper.FAIL;
                    text += String.format("launcher only %.0f RPM; ", robot.getLauncherRpm());
                    finish();
                }
                break;
            case DONE:
                break;
        }
        robot.update();
        t.addData("Checking", phase);
        FieldCheckHelper.show(t, 11);
        robot.addTelemetry(t);
        t.update();
    }

    private void finish() {
        robot.stopMechanisms();
        String faulted = robot.getFaultedMechanisms();
        if (faulted.isEmpty()) {
            text += "none FAULTED";
        } else {
            grade = FieldCheckHelper.FAIL;
            text += "FAULTED: " + faulted;
        }
        FieldCheckHelper.record(11, grade, text);
        phase = Phase.DONE;
    }

    @Override
    public void stop() {
        robot.endMatch();
    }
}
