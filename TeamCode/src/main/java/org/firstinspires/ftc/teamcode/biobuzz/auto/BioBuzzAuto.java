package org.firstinspires.ftc.teamcode.biobuzz.auto;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.Robot;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.AutoConfig;
import org.firstinspires.ftc.teamcode.biobuzz.drive.FieldPaths;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchClock;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.PoseMemory;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateLog;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;

/**
 * STUDENTS: you will change this file.
 *
 * BioBuzzAuto.java  -  the 30-second Autonomous (stage S3).                     ROLE: OpMode
 * PATTERN: a step list (enum Step + switch), laid out like a state machine.
 *
 *   WAIT_DELAY -> LEAVE -> LAUNCH_PRELOADS -> [COLLECT_GARDEN -> DRIVE_TO_LAUNCH -> LAUNCH_AGAIN] -> PARK -> DONE
 *                                              (only in the LAUNCH_COLLECT_PARK routine)
 * EACH STEP: on its first loop it STARTS something (a path, a launch); then it CHECKS
 * whether that is done; then it checks its own TIMEOUT (AutoConfig). A step that runs out
 * of time writes "TIMEOUT" to the match log and moves on, so one jam does not waste the
 * whole Autonomous. At AutoConfig.PARK_BY_S, whatever is running, the robot goes to PARK.
 *
 * BEFORE THE MATCH: routine, start and delay are set in AutoConfig. In INIT, gamepad 1
 * D-pad left/right picks the alliance; the screen shows the plan and checks every path
 * (stays on our side, misses the HIVE). PLACE THE ROBOT EXACTLY at the start position:
 * a wrong start is the most common cause of a bad Autonomous.
 *
 * TO ADD A STEP: a Step value, a "case" (first loop, done, timeout), a timeout in AutoConfig,
 * and the step table in docs/LLD.md (section 9.2). docs/AUTO_GUIDE.md explains more.
 */
@Autonomous(name = "BioBuzz Auto", group = "BioBuzz", preselectTeleOp = "BioBuzz TeleOp")
public class BioBuzzAuto extends OpMode {

    // ==================== 1. STEPS ====================
    private enum Step {
        WAIT_DELAY,        // wait AutoConfig.START_DELAY_S (to stay out of our partner's way)
        LEAVE,             // drive off the wall to our launch spot, facing the upward CELL (LEAVE points)
        LAUNCH_PRELOADS,   // launch the 4 balls we started with
        COLLECT_GARDEN,    // intake on, drive along our GARDEN
        DRIVE_TO_LAUNCH,   // back to the launch spot, facing the upward CELL
        LAUNCH_AGAIN,      // launch what we collected
        PARK,              // stop the mechanisms, drive into our LOADING ZONE (PARK points)
        DONE               // hold still until the end of AUTO
    }

    private Telemetry t;
    private GamepadInput driver;
    private Robot robot;
    private Step step = Step.WAIT_DELAY;
    private boolean stepStarted = false;            // false until a step's first loop has run
    private final StateTimer stepTimer = new StateTimer();
    private final StateLog log = new StateLog("Auto");
    private String pathWarnings = "";               // INIT: paths that leave our side or hit the HIVE

    // ==================== 2. THE OPMODE ====================

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        driver = new GamepadInput(gamepad1);
        robot = new Robot(hardwareMap);
        MatchInfo.resetForNewMatch();   // a new match: the HIVE as set up, no TIPS
        PoseMemory.clear();             // TeleOp must not use the last match's pose
        robot.shooter.holdPreloads();   // the ONLY move allowed in INIT (rule G304)
    }

    @Override
    public void init_loop() {
        if (driver.isDpadLeftPressed() || driver.isDpadRightPressed()) MatchInfo.switchAlliance();
        checkPaths();
        t.addData("Alliance (D-pad left/right to change)", MatchInfo.alliance);
        t.addData("Routine / start / delay", AutoConfig.ROUTINE + " / " + AutoConfig.START + " / "
                + AutoConfig.START_DELAY_S + " s   (set in AutoConfig)");
        t.addData("Up CELL at start", MatchInfo.getUpCellName());
        if (pathWarnings.isEmpty()) t.addLine("Paths: all on our side and clear of the HIVE");
        else t.addLine("!!! PATH WARNING: " + pathWarnings + " - check FieldConfig !!!");
        t.addData("Battery", "%.1f V", robot.battery.getVoltage());
        FieldDrawing.send();
        t.update();
    }

    @Override
    public void start() {
        robot.startMatch("BioBuzzAuto", MatchClock.AUTO_S, "routine=" + AutoConfig.ROUTINE
                + " start=" + AutoConfig.START + " delay_s=" + AutoConfig.START_DELAY_S);
        robot.drive.setAlliance(MatchInfo.isRed());
        robot.drive.setStartingPose(FieldPaths.startPose(AutoConfig.START, MatchInfo.isRed()));
        if (!pathWarnings.isEmpty()) MatchLogger.event("AUTO_PATH_OFFSIDE", pathWarnings);
        goToStep(Step.WAIT_DELAY, "PLAY");
    }

    @Override
    public void loop() {
        // PARK-BY GUARD: always PARK in time, whatever step is running (rule 10.5).
        if (robot.clock.secondsSinceStart() > AutoConfig.PARK_BY_S && step.ordinal() < Step.PARK.ordinal()) {
            MatchLogger.event("PARK_BY", step.toString());
            goToStep(Step.PARK, "park-by time");
        }
        runStep();
        robot.update();
        PoseMemory.save(robot.drive.getPose());   // every loop, so TeleOp has it even if Auto is cut short

        t.addData("Step", step + String.format("  (%.1f s)", stepTimer.seconds()));
        if (!log.getLastChange().isEmpty()) t.addData("Auto last change", log.getLastChange());
        robot.addTelemetry(t);
        t.update();
    }

    @Override
    public void stop() {
        robot.endMatch();
        PoseMemory.save(robot.drive.getPose());   // TeleOp starts from here (pose known: S2)
    }

    // ==================== 3. THE STEPS ====================

    private void runStep() {
        boolean firstLoop = !stepStarted;
        stepStarted = true;
        boolean isRed = MatchInfo.isRed();
        Pose here = robot.drive.getPose();

        switch (step) {
            case WAIT_DELAY:
                if (stepTimer.seconds() >= AutoConfig.START_DELAY_S) goToStep(Step.LEAVE, "delay over");
                break;

            case LEAVE:
                if (firstLoop) robot.drive.followPath(FieldPaths.leaveWall(here, isRed));
                if (robot.drive.isPathDone()) goToStep(Step.LAUNCH_PRELOADS, "at launch spot");
                else if (isTimedOut(AutoConfig.DRIVE_TIMEOUT_MS)) driveTimedOut(Step.LAUNCH_PRELOADS);
                break;

            case LAUNCH_PRELOADS:
                if (firstLoop) robot.shooter.launch(4);
                Step afterLaunch = Step.PARK;
                if (AutoConfig.ROUTINE == AutoConfig.Routine.LAUNCH_COLLECT_PARK) afterLaunch = Step.COLLECT_GARDEN;
                if (!robot.shooter.isLaunching()) goToStep(afterLaunch, "launched");
                else if (isTimedOut(AutoConfig.LAUNCH_TIMEOUT_MS)) launchTimedOut(afterLaunch);
                break;

            case COLLECT_GARDEN:
                if (firstLoop) {
                    robot.intake.collect();
                    robot.drive.followPath(FieldPaths.throughGarden(here, isRed));
                }
                if (robot.drive.isPathDone()) goToStep(Step.DRIVE_TO_LAUNCH, "garden done");
                else if (isTimedOut(AutoConfig.COLLECT_TIMEOUT_MS)) driveTimedOut(Step.DRIVE_TO_LAUNCH);
                break;

            case DRIVE_TO_LAUNCH:
                if (firstLoop) robot.drive.followPath(FieldPaths.gardenToLaunchSpot(here, isRed));
                if (robot.drive.isPathDone()) goToStep(Step.LAUNCH_AGAIN, "at launch spot");
                else if (isTimedOut(AutoConfig.DRIVE_TIMEOUT_MS)) driveTimedOut(Step.LAUNCH_AGAIN);
                break;

            case LAUNCH_AGAIN:
                if (firstLoop) {
                    robot.intake.stop();
                    robot.shooter.launch(4);
                }
                if (!robot.shooter.isLaunching()) goToStep(Step.PARK, "launched");
                else if (isTimedOut(AutoConfig.LAUNCH_TIMEOUT_MS)) launchTimedOut(Step.PARK);
                break;

            case PARK:
                if (firstLoop) {
                    robot.stopMechanisms();
                    robot.drive.followPath(FieldPaths.toLoadingZone(here, isRed));
                }
                if (robot.drive.isPathDone()) goToStep(Step.DONE, "parked");
                else if (isTimedOut(AutoConfig.PARK_TIMEOUT_MS)) driveTimedOut(Step.DONE);
                break;

            case DONE:
                // Pedro holds the robot where the last path ended.
                break;
        }
    }

    // ==================== 4. BOOKKEEPING ====================

    private void goToStep(Step newStep, String reason) {
        log.record(step, newStep, reason);
        step = newStep;
        stepStarted = false;
        stepTimer.reset();
    }

    private boolean isTimedOut(double timeoutMs) { return stepTimer.isTimeUp(timeoutMs); }

    /** A drive step ran out of time: stop where we are, write it down, go on. */
    private void driveTimedOut(Step next) {
        robot.drive.holdHere();
        MatchLogger.event("TIMEOUT", step.toString());
        goToStep(next, "TIMEOUT");
    }

    /** A launch ran out of time: stop launching, write it down, go on. */
    private void launchTimedOut(Step next) {
        robot.shooter.cancelLaunch();
        MatchLogger.event("TIMEOUT", step.toString());
        goToStep(next, "TIMEOUT");
    }

    /**
     * INIT: build this routine's paths from the planned places (for this alliance), draw them
     * on the Dashboard field, and check each one (rules G402, G409, G417).
     */
    private void checkPaths() {
        boolean isRed = MatchInfo.isRed();
        Pose start = FieldPaths.startPose(AutoConfig.START, isRed);
        Path leave = FieldPaths.leaveWall(start, isRed);
        Pose launchSpot = leave.endPose();
        pathWarnings = "";
        checkOnePath("LEAVE", leave, isRed);
        if (AutoConfig.ROUTINE == AutoConfig.Routine.LAUNCH_COLLECT_PARK) {
            Path garden = FieldPaths.throughGarden(launchSpot, isRed);
            checkOnePath("COLLECT_GARDEN", garden, isRed);
            checkOnePath("DRIVE_TO_LAUNCH", FieldPaths.gardenToLaunchSpot(garden.endPose(), isRed), isRed);
        }
        checkOnePath("PARK", FieldPaths.toLoadingZone(launchSpot, isRed), isRed);
        FieldDrawing.drawRobot(start.x(), start.y(), Math.toDegrees(start.heading()));
    }

    private void checkOnePath(String name, Path path, boolean isRed) {
        FieldDrawing.drawPath(path);
        if (!FieldPaths.isOnOwnSide(path, isRed)) pathWarnings += name + " leaves our side. ";
        if (!FieldPaths.avoidsHive(path)) pathWarnings += name + " hits the HIVE. ";
    }
}
