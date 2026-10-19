package org.firstinspires.ftc.teamcode.biobuzz;

import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
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
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Mechanism;

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
 *      Robot  ----> state machines (tasks that take time)        TODO(BioBuzz): Launch
 *        |    ----> mechanisms (Intake, Feeder, Launcher, ...)   TODO(BioBuzz)
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

    // ---- MECHANISMS: one field each. ADD A MECHANISM HERE (for example: private final Intake intake;)
    private final Mechanism[] mechanisms;   // every mechanism, by name, for update / stop / telemetry

    // ---- STATE MACHINES: one field each. ADD A STATE MACHINE HERE

    public Robot(HardwareMap hardwareMap) {
        drive = new Drive(hardwareMap);
        battery = new BatteryMonitor(hardwareMap);
        health = new HealthMonitor(drive, battery);

        // ADD A MECHANISM HERE: build it, for example  intake = new Intake(hardwareMap);
        mechanisms = new Mechanism[] { };   // ... and add its name to this list: { intake, feeder, launcher }

        // ADD A STATE MACHINE HERE: build it with the mechanisms it uses,
        // for example  launch = new LaunchStateMachine(feeder, launcher, drive);
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
    // ADD A ROBOT METHOD HERE for each mechanism action, for example:
    //   public void reverseIntake() { intake.reverse(); }

    /** Put every mechanism in its safe state. OpModes call this in stop(); gamepad 2 D-pad down calls it. */
    public void stopMechanisms() {
        // ADD A STATE MACHINE HERE: its cancel(), for example  launch.cancel();
        for (Mechanism mechanism : mechanisms) {
            mechanism.runStop();
        }
    }

    // ==================== EVERY LOOP ====================

    /** Run one step of everything. Call once per loop, and ONLY once per loop. */
    public void update() {
        clock.update();

        // 1. STATE MACHINES decide and give mechanisms commands.  ADD A STATE MACHINE HERE: launch.update();

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
            // ADD A STATE MACHINE HERE: its state, for example  MatchLogger.value("launch_state", launch.getState().ordinal());
            MatchLogger.endRow();
        }
    }

    // ==================== TELEMETRY ====================

    /**
     * The first line, always shown, for example
     *   RED | 1:42 left | 18 ms | 12.6 V | FIELD 3 deg | S2 ready
     * and under it the newest ALERT, if there is one.
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
        String alert = Alerts.getMessage();
        if (!alert.isEmpty()) t.addData("ALERT", alert);
    }

    /** Header, then each part that is switched on in TelemetryConfig, then the field drawing. */
    public void addTelemetry(Telemetry t) {
        addHeader(t);
        if (TelemetryConfig.DRIVE) drive.addTelemetry(t);
        for (Mechanism mechanism : mechanisms) {
            mechanism.addTelemetry(t);
        }
        drive.addDrawing();
        FieldDrawing.send();
    }
}
