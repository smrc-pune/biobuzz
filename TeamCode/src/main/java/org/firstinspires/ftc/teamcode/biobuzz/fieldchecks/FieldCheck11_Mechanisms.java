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
 * FieldCheck11_Mechanisms.java  -  one bench check per mechanism, through the subsystems.   ROLE: OpMode (field check)
 *
 * NO BALLS in the robot; hands clear of the intake, windmill and flywheel. Press PLAY:
 *   INTAKE     roller + both side wheels run for 1 s     PASS: not FAULTED (WATCH: all three turn, inward)
 *   LAUNCHER   spins up (to LAUNCH_RPM)                  PASS: up to speed within SPIN_UP_TIMEOUT_MS
 *   LAUNCH     robot.shooter.launch() with an empty      PASS: it feeds for FEED_TOTAL_MS and ends
 *              hopper: the windmill turns                      (WATCH: the windmill turns toward the flywheel)
 *   ALL        every mechanism (cameras too)             PASS: none FAULTED
 * The servos have no sensors, so "turns the right way" is for the crew to watch.
 */
@TeleOp(name = "Field Check 11: Mechanisms", group = "Field Checks")
public class FieldCheck11_Mechanisms extends OpMode {

    private enum Phase { INTAKE, LAUNCHER, LAUNCH, DONE }

    private static final double INTAKE_RUN_MS = 1000;
    // A launch with the flywheel already at speed: FEED_TOTAL_MS of feeding, plus room for a slow-down.
    private static final double LAUNCH_SPARE_MS = 3000;

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
        t.addLine("Field Check 11: NO BALLS, hands clear. The intake, windmill and flywheel will run. Press PLAY.");
        t.update();
    }

    @Override
    public void start() {
        FieldCheckHelper.start("FieldCheck11_Mechanisms", robot.battery.getVoltage());
        robot.drive.setStartingPoseFacingAway();
        robot.intake.collect();
        phaseTimer.reset();
    }

    @Override
    public void loop() {
        switch (phase) {
            case INTAKE:
                if (phaseTimer.isTimeUp(INTAKE_RUN_MS)) {
                    robot.intake.stop();
                    text += "intake ran; ";
                    robot.shooter.spinUp();
                    phase = Phase.LAUNCHER;
                    phaseTimer.reset();
                }
                break;
            case LAUNCHER:
                if (robot.shooter.isUpToSpeed()) {
                    text += String.format("launcher up to speed in %.1f s; ", phaseTimer.seconds());
                    robot.shooter.launch();   // the whole launch, through the subsystem: the windmill turns
                    phase = Phase.LAUNCH;
                    phaseTimer.reset();
                } else if (phaseTimer.isTimeUp(LauncherConfig.SPIN_UP_TIMEOUT_MS)) {
                    grade = FieldCheckHelper.FAIL;
                    text += String.format("launcher only %.0f RPM; ", robot.shooter.getRpm());
                    finish();
                }
                break;
            case LAUNCH:
                if (!robot.shooter.isLaunching()) {
                    if (phaseTimer.milliseconds() >= LauncherConfig.FEED_TOTAL_MS) {
                        text += String.format("launch fed and ended in %.1f s; ", phaseTimer.seconds());
                    } else {
                        grade = FieldCheckHelper.FAIL;
                        text += "launch stopped early (see the ALERT); ";
                    }
                    finish();
                } else if (phaseTimer.isTimeUp(LauncherConfig.FEED_TOTAL_MS + LAUNCH_SPARE_MS)) {
                    grade = FieldCheckHelper.FAIL;
                    text += "launch never ended; ";
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
