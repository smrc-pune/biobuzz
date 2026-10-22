package org.firstinspires.ftc.teamcode.biobuzz;

import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.ControlsConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.LauncherConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;
import org.firstinspires.ftc.teamcode.biobuzz.drive.FieldPaths;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.Alerts;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.HealthMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchClock;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Feeder;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.FrontCamera;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.HiveCamera;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Intake;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Launcher;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Mechanism;
import org.firstinspires.ftc.teamcode.biobuzz.statemachines.LaunchStateMachine;

/**
 * STUDENTS: you will change the marked "ADD ... HERE" spots in this file.
 *
 * Robot.java  -  the whole robot in one object. TeleOp AND Autonomous both use it.   ROLE: robot
 * PATTERN: Facade (one "menu" of everything the robot can do) and Mediator (it stops two
 * state machines from fighting over one mechanism).
 *
 *   OpMode (buttons or Auto steps)
 *        |  robot.driveToLaunchSpot(), robot.stopMechanisms(), robot.drive...
 *        v
 *      Robot  ----> state machines (tasks that take time)        LaunchStateMachine
 *        |    ----> mechanisms                                  Intake, Feeder, Launcher, HiveCamera, FrontCamera
 *        |    ----> drive (Drive + FieldPaths)  ----> Pedro Pathing (off the shelf)
 *
 * WHAT ROBOT DOES: builds everything; has one method for each thing the robot can do;
 * runs update() once per loop in a fixed order; watches the robot's health; writes the
 * match log; shows telemetry.
 * WHAT ROBOT NEVER DOES: read gamepads (the OpMode does) or set motor powers (mechanisms do).
 *
 * OpModes drive with robot.drive directly. They reach mechanisms ONLY through Robot methods.
 *
 * TRACING A BUG: start at the OpMode's loop(), then update() below, then the part it calls.
 */
public class Robot {

    public final Drive drive;
    public final BatteryMonitor battery;    // the battery's real voltage, read from the hubs
    public final MatchClock clock = new MatchClock();   // match time left, started by startMatch()
    private final HealthMonitor health;     // loop time, battery, Pinpoint, heading jumps -> ALERTs

    // ---- MECHANISMS: one field each. ADD A MECHANISM HERE
    private final Intake intake;            // the roller that picks up balls
    private final Feeder feeder;            // pushes one ball into the launcher
    private final Launcher launcher;        // the flywheel
    private final HiveCamera hiveCamera;    // HuskyLens, looks up at the CELL tags
    private final FrontCamera frontCamera;  // webcam: field tags, POLLEN, NECTAR
    private final Mechanism[] mechanisms;   // every mechanism, by name, for update / stop / telemetry

    // ---- STATE MACHINES: one field each. ADD A STATE MACHINE HERE
    private final LaunchStateMachine launch;

    public Robot(HardwareMap hardwareMap) {
        drive = new Drive(hardwareMap);
        battery = new BatteryMonitor(hardwareMap);
        health = new HealthMonitor(drive, battery);

        // Each mechanism finds its own devices by name. Nothing moves here (rule G304.H).
        intake = new Intake(hardwareMap);
        feeder = new Feeder(hardwareMap);
        launcher = new Launcher(hardwareMap);
        hiveCamera = new HiveCamera(hardwareMap);
        frontCamera = new FrontCamera(hardwareMap);
        // ADD A MECHANISM HERE: build it above, and add its name to this list.
        mechanisms = new Mechanism[] { intake, feeder, launcher, hiveCamera, frontCamera };

        // State machines get the mechanisms they use. ADD A STATE MACHINE HERE
        launch = new LaunchStateMachine(feeder, launcher, drive);
    }

    // ==================== START AND END OF A RUN ====================

    /** PLAY: start the match clock, the health checks and the match log. */
    public void startMatch(String opModeName, double matchLengthS) {
        Alerts.clear();
        clock.start(matchLengthS);
        health.start();
        MatchLogger.start(opModeName, battery.getVoltage());
    }

    /** STOP: every mechanism to its safe state, then close the match log. */
    public void endMatch() {
        stopMechanisms();
        update();                // push the "off" state to the motors
        MatchLogger.stop(health.getSummary());
    }

    /** @return true ONCE for each new urgent ALERT. The OpMode rumbles the gamepads. */
    public boolean hasNewUrgentAlert() { return Alerts.takeUrgent(); }

    // ==================== DRIVING ASSISTS (S2) ====================
    // Each assist drives one short path by itself. The driver takes over by moving a stick.
    // @return false (with a warning on telemetry) if the assist is refused.

    /** Drive to our favourite launch spot. */
    public boolean driveToLaunchSpot() {
        if (!isPoseKnownForAssist("launch spot")) return false;
        return startAssist(FieldPaths.toLaunchSpot(drive.getPose(), MatchInfo.isRed()), "launch spot");
    }

    /** Drive into our LOADING ZONE (to park, or to pick up NECTAR). */
    public boolean driveToLoadingZone() {
        if (!isPoseKnownForAssist("loading zone")) return false;
        return startAssist(FieldPaths.toLoadingZone(drive.getPose(), MatchInfo.isRed()), "loading zone");
    }

    /** "Set pose here": the robot is sitting in our reset corner (FieldConfig.RESET_CORNER). */
    public void setPoseAtResetCorner() {
        drive.setPose(FieldPaths.resetCorner(MatchInfo.isRed()), "set pose here");
    }

    private boolean isPoseKnownForAssist(String name) {
        if (drive.isPoseKnown()) return true;
        drive.warn(name + " refused: position unknown (S1). Hold D-pad left in the reset corner.");
        return false;
    }

    private boolean startAssist(Path path, String name) {
        if (!FieldPaths.avoidsHive(path)) {
            drive.warn(name + " refused: the path would hit the HIVE. Drive around it first.");
            return false;
        }
        drive.followPath(path);
        MatchLogger.event("ASSIST", name);
        return true;
    }

    // ==================== MECHANISMS ====================
    // One method for each thing the operator (or Auto) can ask for. ADD A ROBOT METHOD HERE.
    // If a state machine also uses that mechanism, cancel it first (Robot is the "Mediator").

    /** Intake on if it was off, off if it was on (operator's A). */
    public void toggleIntake() {
        if (intake.isCollecting()) intake.runStop();
        else intake.collect();
    }

    public void startIntake() { intake.collect(); }

    public void stopIntake() { intake.runStop(); }

    /** Spit balls out while the operator holds B (also how opponent NECTAR goes back out, G408). */
    public void reverseIntake() { intake.reverse(); }

    /** Launch this many balls, from wherever we are. Feeding an empty slot does no harm. */
    public void startLaunching(int count) {
        // No other state machine uses the Feeder or Launcher yet. When one does, cancel it here.
        launch.start(count);
    }

    public void stopLaunching() { launch.cancel(); }

    /** Spin the flywheel up early, for the distance to the upward CELL from here (operator's Y). */
    public void spinUpLauncher() {
        if (launch.isBusy()) return;   // the state machine already controls the speed
        launcher.spinUpFor(getDistanceToUpCellIn());
    }

    /** Our HIVE just TIPPED (operator's left bumper): the other CELL faces up now. */
    public void markHiveTipped() { MatchInfo.markHiveTipped(); }

    /** Auto INIT only: hold the 4 preloaded balls (the only move allowed before PLAY, G304). */
    public void holdPreloads() { feeder.hold(); }

    /** Put every mechanism in its safe state. OpModes call this in stop(); gamepad 2 D-pad down calls it. */
    public void stopMechanisms() {
        launch.cancel();   // ADD A STATE MACHINE HERE: its cancel()
        for (Mechanism mechanism : mechanisms) {
            mechanism.runStop();
        }
    }

    // ---- Questions for OpModes ----
    public boolean isIntakeOn() { return intake.isCollecting(); }

    public boolean isLaunching() { return launch.isBusy(); }

    /** The heading that points the launcher at the upward CELL from where we are (aim button). */
    public double getAimHeadingDeg() { return FieldPaths.aimHeadingDeg(drive.getPose(), MatchInfo.isRed()); }

    /** Inches to the upward CELL, or LauncherConfig.DEFAULT_DISTANCE_IN when the pose is unknown (S1). */
    public double getDistanceToUpCellIn() {
        if (!drive.isPoseKnown()) return LauncherConfig.DEFAULT_DISTANCE_IN;
        return FieldPaths.distanceToUpCellIn(drive.getPose(), MatchInfo.isRed());
    }

    // ==================== EVERY LOOP ====================

    /** Run one step of everything. Call once per loop, and ONLY once per loop. */
    public void update() {
        clock.update();

        // 1. STATE MACHINES decide and give mechanisms commands.
        launch.update();   // ADD A STATE MACHINE HERE

        // 2. MECHANISMS: every one, once, whether or not a state machine is using it.
        for (Mechanism mechanism : mechanisms) {
            mechanism.runUpdate();
        }

        // 3. DRIVE: Pedro reads the Pinpoint and sets the drive motors.
        drive.update();

        // 4. HEALTH: loop time, battery, Pinpoint, heading jumps. Raises ALERTs; never stops the robot.
        health.update();

        // 5. MATCH LOG: one DATA row, LogConfig.RATE_HZ times a second.
        if (MatchLogger.isRowDue()) {
            MatchLogger.value("match_s_left", clock.secondsLeft());
            health.addLogData();
            drive.addLogData();
            for (Mechanism mechanism : mechanisms) {
                mechanism.addLogData();
            }
            launch.addLogData();   // ADD A STATE MACHINE HERE
            MatchLogger.value("up_cell_a", MatchInfo.isCellAUp());
            MatchLogger.value("tip_count", MatchInfo.getTipCount());
            MatchLogger.endRow();
        }
    }

    // ==================== TELEMETRY ====================

    /**
     * The first two lines, always shown, for example
     *   RED | 1:42 left | 18 ms | 12.6 V | FIELD 3 deg | S2 ready
     *   intake ON | launcher READY | up CELL A | tips 2 | NECTAR +2
     * and under them the newest ALERT, if there is one.
     */
    public void addHeader(Telemetry t) {
        String driveMode = "FIELD";
        if (!drive.isFieldCentric()) driveMode = "ROBOT";
        String stage = "S2 ready";
        if (!drive.isPoseKnown()) stage = "S1 only";
        String timeLeft = "";
        if (clock.hasStarted()) timeLeft = " | " + clock.getTimeLeftText() + " left";
        t.addData("Match", MatchInfo.alliance + timeLeft
                + String.format(" | %.0f ms | %.1f V | %s %.0f deg | ", health.getLoopMs(),
                        battery.getVoltage(), driveMode, drive.getHeadingDegrees()) + stage);
        String intakeText = "intake off";
        if (intake.isCollecting()) intakeText = "intake ON";
        String launcherText = "launcher -";
        if (launcher.isUpToSpeed()) launcherText = "launcher READY";
        else if (launcher.getTargetRpm() > 0) launcherText = "launcher spinning";
        // NECTAR: the human player may enter one per TIP, and any with 1:00 left (G426).
        String nectarText = "NECTAR +" + MatchInfo.getNectarAllowed();
        if (clock.isTeleOp() && clock.secondsLeft() <= ControlsConfig.ONE_MINUTE_WARNING_S) nectarText = "NECTAR open";
        t.addData("Game", intakeText + " | " + launcherText + " | up CELL " + MatchInfo.getUpCellName()
                + " | tips " + MatchInfo.getTipCount() + " | " + nectarText);
        String alert = Alerts.getMessage();
        if (!alert.isEmpty()) t.addData("ALERT", alert);
    }

    /** Header, then each part that is switched on in TelemetryConfig, then the field drawing. */
    public void addTelemetry(Telemetry t) {
        addHeader(t);
        if (TelemetryConfig.DRIVE) drive.addTelemetry(t);
        if (TelemetryConfig.LAUNCH) launch.addTelemetry(t);   // ADD A STATE MACHINE HERE (with its switch)
        for (Mechanism mechanism : mechanisms) {
            mechanism.addTelemetry(t);   // each one checks its own TelemetryConfig switch
        }
        drive.addDrawing();
        FieldDrawing.drawCell(FieldPaths.upCellPose(MatchInfo.isRed()));
        FieldDrawing.graph("launcher_rpm", launcher.getRpm());
        FieldDrawing.graph("launcher_target_rpm", launcher.getTargetRpm());
        FieldDrawing.graph("loop_ms", health.getLoopMs());
        FieldDrawing.graph("battery_v", battery.getVoltage());
        FieldDrawing.send();
    }
}
