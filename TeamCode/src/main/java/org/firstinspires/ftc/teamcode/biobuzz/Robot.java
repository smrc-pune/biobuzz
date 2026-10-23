package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.ControlsConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.DriveToSpot;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;
import org.firstinspires.ftc.teamcode.biobuzz.drive.FieldPaths;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.Alerts;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.HealthMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchClock;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.biobuzz.subsystems.ShooterSubsystem;
import org.firstinspires.ftc.teamcode.biobuzz.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.biobuzz.subsystems.VisionSubsystem;

/**
 * STUDENTS: you will change the marked "ADD A SUBSYSTEM HERE" spot in this file.
 *
 * Robot.java  -  the whole robot in one object. TeleOp AND Autonomous both use it.   ROLE: robot
 * PATTERN: Composition (a Robot HAS a drive and some subsystems).
 *
 *   OpMode (buttons or Auto steps)
 *        |  robot.intake.toggle()   robot.shooter.launch(4)   robot.driveToSpot.launchSpot()
 *        v
 *      Robot ---> drive         Drive (sticks, paths, pose)  ---> Pedro Pathing (off the shelf)
 *            ---> driveToSpot   DriveToSpot (S2: the robot drives itself to a spot)
 *            ---> intake        IntakeSubsystem    (Intake)
 *            ---> shooter       ShooterSubsystem   (Feeder, Launcher, LaunchStateMachine)
 *            ---> vision        VisionSubsystem    (HiveCamera, FrontCamera)
 *
 * WHAT ROBOT DOES: builds everything; runs update() once per loop in a fixed order; watches
 * the robot's health; writes the match log; shows the telemetry header.
 * WHAT ROBOT NEVER DOES: read gamepads (the OpMode does), or know what is inside a subsystem.
 * To find what a button does, open the subsystem: robot.shooter.launch(4) is in
 * subsystems/ShooterSubsystem.java.
 *
 * TRACING A BUG: start at the OpMode's loop(), then update() below, then the subsystem.
 */
public class Robot {

    // ---- What OpModes use ----
    public final Drive drive;
    public final DriveToSpot driveToSpot;
    public final IntakeSubsystem intake;
    public final ShooterSubsystem shooter;
    public final VisionSubsystem vision;
    public final BatteryMonitor battery;                 // the battery's real voltage, read from the hubs
    public final MatchClock clock = new MatchClock();    // match time left, started by startMatch()

    // ---- Robot's own ----
    private final HealthMonitor health;     // loop time, battery, Pinpoint, heading jumps -> ALERTs
    private final Subsystem[] subsystems;   // every subsystem, in update order

    public Robot(HardwareMap hardwareMap) {
        drive = new Drive(hardwareMap);
        driveToSpot = new DriveToSpot(drive);
        battery = new BatteryMonitor(hardwareMap);
        health = new HealthMonitor(drive, battery);

        // Each subsystem builds its own mechanisms. Nothing moves here (rule G304.H).
        intake = new IntakeSubsystem(hardwareMap);
        shooter = new ShooterSubsystem(hardwareMap, drive);
        vision = new VisionSubsystem(hardwareMap);
        // ADD A SUBSYSTEM HERE: a field above, build it here, and add it to this list.
        subsystems = new Subsystem[] { intake, shooter, vision };
    }

    // ==================== START AND END OF A RUN ====================

    /** PLAY: start the match clock, the health checks and the match log. */
    public void startMatch(String opModeName, double matchLengthS) {
        startMatch(opModeName, matchLengthS, "");
    }

    /** The same, with one more line at the top of the match log (Auto writes its plan there). */
    public void startMatch(String opModeName, double matchLengthS, String logHeader) {
        Alerts.clear();
        clock.start(matchLengthS);
        health.start();
        MatchLogger.start(opModeName, battery.getVoltage(), logHeader);
    }

    /** STOP: every subsystem to its safe state, then close the match log. */
    public void endMatch() {
        stopMechanisms();
        update();                // push the "off" state to the motors
        MatchLogger.stop(health.getSummary());
    }

    /** Every subsystem to its safe state (not the drive). OpModes call it in stop(); gamepad 2 D-pad down too. */
    public void stopMechanisms() {
        for (Subsystem subsystem : subsystems) {
            subsystem.stop();
        }
    }

    /** @return true ONCE for each new urgent ALERT. The OpMode rumbles the gamepads. */
    public boolean hasNewUrgentAlert() { return Alerts.takeUrgent(); }

    /** The names of every FAULTED mechanism, for example "intake launcher", or "" if none. */
    public String getFaultedMechanisms() {
        String names = "";
        for (Subsystem subsystem : subsystems) {
            String faulted = subsystem.getFaultedNames();
            if (!faulted.isEmpty()) {
                names += faulted + " ";
            }
        }
        return names.trim();
    }

    // ==================== EVERY LOOP ====================

    /** Run one step of everything. Call once per loop, and ONLY once per loop. */
    public void update() {
        clock.update();

        // 1. SUBSYSTEMS: each one runs its state machines, then its mechanisms.
        for (Subsystem subsystem : subsystems) {
            subsystem.update();
        }

        // 2. DRIVE: Pedro reads the Pinpoint and sets the drive motors.
        drive.update();

        // 3. HEALTH: loop time, battery, Pinpoint, heading jumps. Raises ALERTs; never stops the robot.
        health.update();

        // 4. MATCH LOG: one DATA row, LogConfig.RATE_HZ times a second.
        if (MatchLogger.isRowDue()) {
            writeLogRow();
        }
    }

    private void writeLogRow() {
        MatchLogger.value("match_s_left", clock.secondsLeft());
        health.addLogData();
        drive.addLogData();
        for (Subsystem subsystem : subsystems) {
            subsystem.addLogData();
        }
        MatchLogger.value("up_cell_a", MatchInfo.isCellAUp());
        MatchLogger.value("tip_count", MatchInfo.getTipCount());
        MatchLogger.endRow();
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
        if (!drive.isFieldCentric()) {
            driveMode = "ROBOT";
        }
        String stage = "S2 ready";
        if (!drive.isPoseKnown()) {
            stage = "S1 only";
        }
        String timeLeft = "";
        if (clock.hasStarted()) {
            timeLeft = " | " + clock.getTimeLeftText() + " left";
        }
        t.addData("Match", MatchInfo.alliance + timeLeft
                + String.format(" | %.0f ms | %.1f V | %s %.0f deg | ", health.getLoopMs(),
                        battery.getVoltage(), driveMode, drive.getHeadingDegrees()) + stage);

        // Each subsystem says a few words about itself ("intake ON", "launcher READY").
        String game = "";
        for (Subsystem subsystem : subsystems) {
            String status = subsystem.getStatusText();
            if (!status.isEmpty()) {
                game += status + " | ";
            }
        }
        // NECTAR: the human player may enter one per TIP, and any with 1:00 left (G426).
        String nectarText = "NECTAR +" + MatchInfo.getNectarAllowed();
        if (clock.isTeleOp() && clock.secondsLeft() <= ControlsConfig.ONE_MINUTE_WARNING_S) {
            nectarText = "NECTAR open";
        }
        t.addData("Game", game + "up CELL " + MatchInfo.getUpCellName()
                + " | tips " + MatchInfo.getTipCount() + " | " + nectarText);

        String alert = Alerts.getMessage();
        if (!alert.isEmpty()) {
            t.addData("ALERT", alert);
        }
    }

    /** Header, then each part that is switched on in TelemetryConfig, then the field drawing. */
    public void addTelemetry(Telemetry t) {
        addHeader(t);
        if (TelemetryConfig.DRIVE) {
            drive.addTelemetry(t);
        }
        for (Subsystem subsystem : subsystems) {
            subsystem.addTelemetry(t);   // each part checks its own TelemetryConfig switch
        }
        drive.addDrawing();
        FieldDrawing.drawCell(FieldPaths.upCellPose(MatchInfo.isRed()));
        FieldDrawing.graph("launcher_rpm", shooter.getRpm());
        FieldDrawing.graph("launcher_target_rpm", shooter.getTargetRpm());
        FieldDrawing.graph("loop_ms", health.getLoopMs());
        FieldDrawing.graph("battery_v", battery.getVoltage());
        FieldDrawing.send();
    }
}
