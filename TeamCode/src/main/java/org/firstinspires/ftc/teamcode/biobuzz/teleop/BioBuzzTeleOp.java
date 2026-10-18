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
 * BioBuzzTeleOp.java  -  the ONE match teleop. It turns gamepad buttons into COMMANDS.  ROLE: OpMode
 *
 * THE BIG IDEA:
 *   Gamepad button  --->  command on a brain  --->  brain's update() does the work
 *
 * Gamepad 1 ONLY drives. Gamepad 2 ONLY runs mechanisms. Both maps are in this file:
 * handleDriver() and handleOperator(). Robot and the brains never read a gamepad,
 * which is why Autonomous can reuse everything unchanged.
 *
 * EVERY LOOP: 1) READ the gamepads  2) COMMAND the robot  3) robot.update()  4) telemetry
 *
 * CONTROLS (keep the same as TelemetryConfig.GAMEPAD*_HELP and docs/CONTROLS.md)
 *   INIT:   gamepad 1 D-pad = INIT menu (alliance, match type and number, battery)
 *   GAMEPAD 1 (driver):    left stick = move (field-centric)     right stick X = turn
 *                          left trigger (hold) = slow mode       Y = "facing away from drivers now"
 *                          X = switch field-centric / robot-centric
 *                          Back/Share = mark a moment in the match log
 *   GAMEPAD 2 (operator):  D-pad down = STOP ALL mechanisms
 *                          Back/Share = mark a moment in the match log
 *                          TODO(BioBuzz): mechanism buttons
 *
 * START POSE: if Autonomous ran, TeleOp starts from the pose it saved (PoseMemory).
 * If not, it assumes the robot faces away from the drivers and shows a warning:
 * press Y with the robot facing away to be sure.
 *
 * TO CHANGE A BUTTON: edit the matching line in handleDriver() or handleOperator() below.
 * TO CHANGE WHAT A BUTTON DOES: look in subsystems/ (the brain), not here.
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

    // ---------------- INIT ----------------
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
            t.addData("Pose from auto", String.format("saved %.0f s ago", PoseMemory.secondsSinceSaved()));
        } else {
            t.addLine("No auto pose: press Y facing away from drivers");
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
            robot.drive.setStartingPose(PoseMemory.getPose());
            PoseMemory.clear();   // used once; a second TeleOp must not trust an old pose
            headingConfirmed = true;
        } else {
            robot.drive.setStartingPoseFacingAway();
        }
    }

    @Override
    public void loop() {
        handleDriver();          // 1+2. read gamepad 1, command Drive
        handleOperator();        // 1+2. read gamepad 2, command the mechanisms
        robot.update();          // 3. every brain takes one step
        showTelemetry();         // 4.
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
        boolean mark = driver.isBackPressed();

        // Sticks: up = +forward, right = +right / +clockwise. Drive handles deadzone and Pedro's signs.
        robot.drive.setDriverInput(driver.getLeftStickY(), driver.getLeftStickX(), driver.getRightStickX());
        robot.drive.setSlowMode(driver.isLeftTriggerHeld());

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
    }

    // ==================== GAMEPAD 2: MECHANISMS ====================

    private void handleOperator() {
        // Buttons are EDGE-DETECTED: read each one ONCE per loop and keep the answer.
        boolean stopAll = operator.isDpadDownPressed();
        boolean mark = operator.isBackPressed();
        // TODO(BioBuzz): one boolean per mechanism button, then one command each below.

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
