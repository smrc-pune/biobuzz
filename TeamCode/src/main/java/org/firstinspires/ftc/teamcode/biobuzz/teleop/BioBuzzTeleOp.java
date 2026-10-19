package org.firstinspires.ftc.teamcode.biobuzz.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.Robot;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.ControlsConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.InitMenu;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.PoseMemory;

/**
 * STUDENTS: you will change this file.
 *
 * BioBuzzTeleOp.java  -  the ONE match TeleOp. It turns gamepad buttons into commands.  ROLE: OpMode
 *
 * Gamepad 1 ONLY drives. Gamepad 2 ONLY runs mechanisms. Both maps are in this file:
 * handleDriver() and handleOperator(). Robot never reads a gamepad, which is why
 * Autonomous can use the same Robot.
 *
 * TWO STAGES OF DRIVING, both in this one TeleOp:
 *   S1 MANUAL    the driver drives with the sticks (field-centric).
 *   S2 ASSISTED  A or B makes the robot drive a short path by itself (to our launch spot,
 *                to our LOADING ZONE). Moving ANY stick takes the robot back at once.
 * Assists need to know where the robot is. After Auto they work straight away. If Auto
 * did not run (or the robot restarted), TeleOp starts as S1: drive into our reset corner
 * and hold D-pad left for 1 second ("set pose here") to turn the assists on.
 *
 * EVERY LOOP: 1) read the gamepads and give commands  2) robot.update()  3) telemetry
 *
 * CONTROLS (keep the same as ControlsConfig's list, TelemetryConfig.GAMEPAD*_HELP and docs/CONTROLS.md)
 *   INIT:   gamepad 1 D-pad = INIT menu (alliance, match type and number, battery)
 *   GAMEPAD 1 (driver):    left stick = move (field-centric)     right stick X = turn
 *                          Y = "facing away from drivers now"     X = field / robot-centric
 *                          A = drive to our launch spot (S2)      B = drive to our LOADING ZONE (S2)
 *                          D-pad left (hold 1 s) = set pose here (robot in our reset corner)
 *                          Back/Share = mark a moment in the match log
 *   GAMEPAD 2 (operator):  D-pad down = STOP ALL mechanisms
 *                          Back/Share = mark a moment in the match log
 *                          TODO(BioBuzz): mechanism buttons
 *
 * TO CHANGE A BUTTON: edit its line in handleDriver() or handleOperator() below.
 * TO CHANGE WHAT A BUTTON DOES: look in Robot.java, then the mechanism or drive file.
 */
@TeleOp(name = "BioBuzz TeleOp", group = "BioBuzz")
public class BioBuzzTeleOp extends OpMode {

    private Telemetry t;
    private GamepadInput driver;     // gamepad 1 - one wrapper per controller!
    private GamepadInput operator;   // gamepad 2
    private Robot robot;

    private final InitMenu menu = new InitMenu();
    private int allianceRow, matchTypeRow, matchNumberRow, batteryRow;
    private boolean hasAutoPose = false;
    private boolean headingConfirmed = false;   // false until the driver presses Y (when no auto pose)

    /*
     * An OpMode is a set of methods the SDK calls for us:
     *   init()       ONCE, when INIT is pressed
     *   init_loop()  OVER AND OVER, while waiting for PLAY (the INIT menu runs here)
     *   start()      ONCE, when PLAY is pressed
     *   loop()       OVER AND OVER, after PLAY (this is the match)
     *   stop()       ONCE, when STOP is pressed
     * Nothing waits or sleeps: each pass takes a few milliseconds.
     */

    // ---------------- INIT: nothing on the robot may move here (rule G403) ----------------
    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);   // shows on DS AND Dashboard
        driver = new GamepadInput(gamepad1);
        operator = new GamepadInput(gamepad2);
        robot = new Robot(hardwareMap);

        // The menu starts from what was entered in Auto's INIT (MatchInfo keeps it).
        allianceRow = menu.addChoiceRow("Alliance", MatchInfo.ALLIANCES, MatchInfo.alliance);
        matchTypeRow = menu.addChoiceRow("Match type", MatchInfo.MATCH_TYPES, MatchInfo.matchType);
        matchNumberRow = menu.addNumberRow("Match number", 0, MatchInfo.MAX_MATCH_NUMBER, MatchInfo.matchNumber);
        batteryRow = menu.addChoiceRow("Battery", MatchInfo.BATTERY_LABELS, MatchInfo.batteryLabel);
        // TODO(BioBuzz) step 4: a "Logging" row.

        hasAutoPose = PoseMemory.hasPose();
    }

    @Override
    public void init_loop() {
        menu.update(driver.isDpadUpPressed(), driver.isDpadDownPressed(),
                driver.isDpadLeftPressed(), driver.isDpadRightPressed());
        menu.addTo(t);
        if (hasAutoPose) {
            t.addData("Pose from auto", String.format("saved %.0f s ago: assists (S2) ready",
                    PoseMemory.secondsSinceSaved()));
        } else {
            t.addLine("No auto pose: S1 only. Press Y facing away from drivers.");
        }
        t.update();
    }

    // ---------------- PLAY ----------------
    @Override
    public void start() {
        MatchInfo.alliance = menu.getIndex(allianceRow);
        MatchInfo.allianceChosen = true;
        MatchInfo.matchType = menu.getIndex(matchTypeRow);
        MatchInfo.matchNumber = menu.getIndex(matchNumberRow);
        MatchInfo.batteryLabel = menu.getIndex(batteryRow);

        robot.drive.setAlliance(MatchInfo.isRed());
        if (hasAutoPose) {
            robot.drive.setStartingPose(PoseMemory.getPose());   // S2 ready
            PoseMemory.clear();   // used once; a second TeleOp must not trust an old pose
            headingConfirmed = true;
        } else {
            robot.drive.setStartingPoseFacingAway();             // S1 only
        }
    }

    @Override
    public void loop() {
        handleDriver();          // gamepad 1 -> driving
        handleOperator();        // gamepad 2 -> mechanisms
        robot.update();          // every part takes one step
        showTelemetry();
    }

    // ---------------- STOP ----------------
    @Override
    public void stop() {
        robot.stopMechanisms();
        robot.update();          // push the "off" state to the motors before we finish
    }

    // ==================== GAMEPAD 1: DRIVING ====================

    private void handleDriver() {
        // Buttons are EDGE-DETECTED: read each one ONCE per loop and keep the answer.
        boolean faceAway = driver.isYPressed();
        boolean switchMode = driver.isXPressed();
        boolean goToLaunchSpot = driver.isAPressed();
        boolean goToLoadingZone = driver.isBPressed();
        boolean setPoseHere = driver.isDpadLeftHeldFor(ControlsConfig.SET_POSE_HOLD_MS);
        boolean mark = driver.isBackPressed();
        boolean sticksMoved = driver.areSticksMoved();

        // ---- S2: assist buttons. Robot refuses them (warning + rumble) while the position is unknown.
        if (goToLaunchSpot && !robot.driveToLaunchSpot()) driver.rumble(ControlsConfig.RUMBLE_REFUSED_MS);
        if (goToLoadingZone && !robot.driveToLoadingZone()) driver.rumble(ControlsConfig.RUMBLE_REFUSED_MS);
        if (setPoseHere) {
            robot.setPoseAtResetCorner();
            headingConfirmed = true;
            driver.rumble(ControlsConfig.RUMBLE_POSE_SET_MS);
        }

        // ---- S1: the sticks. Moving a stick ALWAYS takes the robot back, even in the middle of a path.
        // Sticks: up = +forward, right = +right / +clockwise. Drive handles Pedro's signs.
        if (sticksMoved || !robot.drive.isDrivingItself()) {
            robot.drive.driveWithSticks(driver.getLeftStickY(), driver.getLeftStickX(), driver.getRightStickX());
        }

        if (faceAway) {
            robot.drive.resetHeading();
            headingConfirmed = true;
        }
        if (switchMode) {
            robot.drive.switchDriveMode();
            driver.rumble(ControlsConfig.RUMBLE_DRIVE_MODE_MS);
        }
        if (mark) {
            // TODO(BioBuzz) step 4: MatchLogger "DRIVER MARK" event.
        }
        // TODO(BioBuzz) step 4: right stick button = snap heading to 90,
        //                       hold left stick button 1 s = recalibrate the gyro.
        // TODO(BioBuzz): right bumper (hold) = aim at the upward CELL, once the launcher exists.
    }

    // ==================== GAMEPAD 2: MECHANISMS ====================

    private void handleOperator() {
        // Buttons are EDGE-DETECTED: read each one ONCE per loop and keep the answer.
        boolean stopAll = operator.isDpadDownPressed();
        boolean mark = operator.isBackPressed();
        // TODO(BioBuzz): one boolean per mechanism button, then one Robot method each below.

        if (stopAll) robot.stopMechanisms();
        if (mark) {
            // TODO(BioBuzz) step 4: MatchLogger "OPERATOR MARK" event.
        }
    }

    // ==================== TELEMETRY ====================

    private void showTelemetry() {
        if (!headingConfirmed) t.addLine("No auto pose: press Y facing away from drivers");
        robot.addTelemetry(t);   // header first, then the switches in TelemetryConfig
        if (TelemetryConfig.GAMEPAD_HELP) {
            t.addLine(TelemetryConfig.GAMEPAD1_HELP);
            t.addLine(TelemetryConfig.GAMEPAD2_HELP);
        }
        t.update();              // ALWAYS call update(): it is what actually sends the text
    }
}
