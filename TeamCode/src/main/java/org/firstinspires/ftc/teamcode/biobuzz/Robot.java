package org.firstinspires.ftc.teamcode.biobuzz;

import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;
import org.firstinspires.ftc.teamcode.biobuzz.drive.FieldPaths;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
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
 * runs update() once per loop in a fixed order; shows telemetry.
 * WHAT ROBOT NEVER DOES: read gamepads (the OpMode does) or set motor powers (mechanisms do).
 *
 * OpModes drive with robot.drive directly. They reach mechanisms ONLY through Robot methods.
 *
 * TRACING A BUG: start at the OpMode's loop(), then update() below, then the part it calls.
 */
public class Robot {

    public final Drive drive;

    // ---- MECHANISMS: one field each. ADD A MECHANISM HERE (for example: private final Intake intake;)
    private final Mechanism[] mechanisms;   // every mechanism, by name, for update / stop / telemetry

    // ---- STATE MACHINES: one field each. ADD A STATE MACHINE HERE

    public Robot(HardwareMap hardwareMap) {
        drive = new Drive(hardwareMap);

        // ADD A MECHANISM HERE: build it, for example  intake = new Intake(hardwareMap);
        mechanisms = new Mechanism[] { };   // ... and add its name to this list: { intake, feeder, launcher }

        // ADD A STATE MACHINE HERE: build it with the mechanisms it uses,
        // for example  launch = new LaunchStateMachine(feeder, launcher, drive);
    }

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
        // 1. STATE MACHINES decide and give mechanisms commands.  ADD A STATE MACHINE HERE: launch.update();

        // 2. MECHANISMS: every one, once, whether or not a state machine is using it.
        for (Mechanism mechanism : mechanisms) {
            mechanism.runUpdate();
        }

        // 3. DRIVE: Pedro reads the Pinpoint and sets the drive motors.
        drive.update();

        // 4. ALWAYS-ON RULES: TODO(BioBuzz) step 4: battery, loop time, Pinpoint and heading safeguards.
    }

    // ==================== TELEMETRY ====================

    /** The first line, always shown: alliance, match, driving mode, heading and stage. */
    public void addHeader(Telemetry t) {
        String driveMode = "FIELD";
        if (!drive.isFieldCentric()) driveMode = "ROBOT";
        String stage = "S2 ready";
        if (!drive.isPoseKnown()) stage = "S1 only";
        t.addData("Match", MatchInfo.getAllianceName() + "  " + MatchInfo.getMatchId()
                + "  battery " + MatchInfo.getBatteryLabel() + "  drive " + driveMode
                + String.format("  heading %.0f deg", drive.getHeadingDegrees()) + "  " + stage);
        // TODO(BioBuzz) step 4: match time left, loop ms, battery volts, ALERT.
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
