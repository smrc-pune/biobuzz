package org.firstinspires.ftc.teamcode.biobuzz.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.biobuzz.configurations.LauncherConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;
import org.firstinspires.ftc.teamcode.biobuzz.drive.FieldPaths;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Feeder;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Launcher;
import org.firstinspires.ftc.teamcode.biobuzz.statemachines.LaunchStateMachine;

/**
 * STUDENTS: you will change this file.
 *
 * ShooterSubsystem.java  -  launching balls into the upward CELL.             ROLE: subsystem
 *
 *   OpMode:  robot.shooter.launch(4)     robot.shooter.cancelLaunch()    robot.shooter.spinUp()
 *            robot.shooter.isLaunching() robot.shooter.isUpToSpeed()     robot.shooter.getAimHeadingDeg()
 *            robot.shooter.holdPreloads() (Auto INIT only)               robot.shooter.stop()
 *
 * Owns: Feeder + Launcher, and LaunchStateMachine (which uses both). Because the state
 * machine and the manual commands both live HERE, this is the one file that keeps them
 * from fighting: spinUp() does nothing while a launch is running.
 * Uses Drive only to ask where we are (for the distance and the aim).
 * ADD A MENU METHOD HERE when an OpMode needs something new from the shooter.
 */
public class ShooterSubsystem extends Subsystem {

    private final Feeder feeder;
    private final Launcher launcher;
    private final LaunchStateMachine launchStateMachine;
    private final Drive drive;

    public ShooterSubsystem(HardwareMap hardwareMap, Drive drive) {
        super("shooter");
        this.drive = drive;
        feeder = new Feeder(hardwareMap);
        launcher = new Launcher(hardwareMap);
        launchStateMachine = new LaunchStateMachine(feeder, launcher, drive);   // given what it uses
        addMechanism(feeder);
        addMechanism(launcher);
        addStateMachine(launchStateMachine);
    }

    // ==================== THE MENU ====================

    /** Launch this many balls, from wherever we are. Feeding an empty slot does no harm. */
    public void launch(int count) {
        launchStateMachine.start(count);
    }

    /** Stop launching at once: balls held back, flywheel at idle (Auto's launch timeout). */
    public void cancelLaunch() {
        launchStateMachine.cancel();
    }

    /** Spin the flywheel up early, for the distance to the upward CELL from here (operator's Y). */
    public void spinUp() {
        if (launchStateMachine.isBusy()) {
            return;   // the state machine already controls the speed
        }
        launcher.spinUpFor(getDistanceToUpCellIn());
    }

    /** Auto INIT only: hold the 4 preloaded balls (the only move allowed before PLAY, G304). */
    public void holdPreloads() {
        feeder.hold();
    }

    // stop() comes from Subsystem: launch cancelled, feeder and flywheel stopped.

    // ==================== QUESTIONS ====================

    public boolean isLaunching() {
        return launchStateMachine.isBusy();
    }

    public boolean isUpToSpeed() {
        return launcher.isUpToSpeed();
    }

    public double getRpm() {
        return launcher.getRpm();
    }

    public double getTargetRpm() {
        return launcher.getTargetRpm();
    }

    /** The heading that points the launcher at the upward CELL from where we are (aim button). */
    public double getAimHeadingDeg() {
        return FieldPaths.aimHeadingDeg(drive.getPose(), MatchInfo.isRed());
    }

    /** Inches to the upward CELL, or LauncherConfig.DEFAULT_DISTANCE_IN when the pose is unknown (S1). */
    public double getDistanceToUpCellIn() {
        if (!drive.isPoseKnown()) {
            return LauncherConfig.DEFAULT_DISTANCE_IN;
        }
        return FieldPaths.distanceToUpCellIn(drive.getPose(), MatchInfo.isRed());
    }

    @Override
    public String getStatusText() {
        if (launcher.isUpToSpeed()) {
            return "launcher READY";
        }
        if (launcher.getTargetRpm() > 0) {
            return "launcher spinning";
        }
        return "launcher -";
    }
}
