// "package" says which folder this file lives in. Java uses it to find the file.
package org.firstinspires.ftc.teamcode.biobuzz.teleop;

// "import" lines bring in classes written in other files, so we can use them here.
// The first two come from the FTC SDK (off the shelf). The rest are our own files.
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.Robot;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.ControlsConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HealthConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchClock;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
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
 * EVERY LOOP: 1) read the gamepads and give commands  2) robot.update()  3) rumbles  4) telemetry
 * Rumbles: urgent ALERT = both gamepads, 1:00 left = both, 0:20 left (time to PARK) = gamepad 1.
 *
 * CONTROLS (the same list is in ControlsConfig, TelemetryConfig.GAMEPAD*_HELP and docs/CONTROLS.md):
 *   GAMEPAD 1 (driver)    left stick = move (field-centric)   right stick X = turn
 *                         Y = reset heading (facing away)     X = field-centric / robot-centric
 *                         A = drive to our launch spot (S2)   B = drive to our LOADING ZONE (S2)
 *                         right bumper (hold) = aim at the upward CELL while driving
 *                         D-pad left (hold 1 s) = set pose here (robot in our reset corner)
 *                         Back/Share = mark a moment in the match log
 *                         INIT: D-pad left/right = switch alliance RED / BLUE
 *   GAMEPAD 2 (operator)  A = intake on/off                   B (hold) = spit out (reverse intake)
 *                         right trigger = launch 4            Y = spin the launcher up early
 *                         left bumper = "our HIVE just TIPPED"
 *                         D-pad down = STOP ALL mechanisms    Back/Share = mark a moment in the match log
 *
 * TO CHANGE A BUTTON: edit its line in handleDriver() or handleOperator() below, then the
 * list in ControlsConfig and the cheat sheet in TelemetryConfig.GAMEPAD*_HELP.
 * TO CHANGE WHAT A BUTTON DOES: look in Robot.java, then the mechanism or drive file.
 *
 * TRACING A BUG: start in loop(). Was the button read in handleDriver()/handleOperator()?
 * Was the Robot method called? Did telemetry show a WARNING (the command was refused)?
 */
// @TeleOp puts this program on the Driver Station's TeleOp list, under this name and group.
@TeleOp(name = "BioBuzz TeleOp", group = "BioBuzz")
// "extends OpMode" means: this class IS an OpMode. The FTC SDK will call its init(), loop(), ...
public class BioBuzzTeleOp extends OpMode {

    // ==================== FIELDS: things this OpMode remembers between loops ====================
    // "private" means only this file can use them. They are filled in during init().

    private Telemetry t;             // where we write the text shown on the Driver Station AND Dashboard
    private GamepadInput driver;     // gamepad 1 - one GamepadInput per controller!
    private GamepadInput operator;   // gamepad 2
    private Robot robot;             // the whole robot: drive, mechanisms, state machines

    private boolean hasAutoPose = false;        // true if Autonomous left us the robot's position
    private boolean headingConfirmed = false;   // false until the driver presses Y (when no auto pose)

    /*
     * An OpMode is a set of methods the SDK calls for us:
     *   init()       ONCE, when INIT is pressed
     *   init_loop()  OVER AND OVER, while waiting for PLAY (the alliance is picked here)
     *   start()      ONCE, when PLAY is pressed
     *   loop()       OVER AND OVER, after PLAY (this is the match)
     *   stop()       ONCE, when STOP is pressed
     * Nothing waits or sleeps: each pass takes a few milliseconds.
     *
     * "@Override" above each one means: "this is our version of a method the SDK already
     * knows about". If you spell the name wrong, @Override makes the build fail and tells you.
     */

    // ---------------- INIT: nothing on the robot may move here (rule G403) ----------------
    @Override
    public void init() {
        // Build our tools and the robot. "new" makes a new object from a class.
        t = DashboardTelemetry.create(telemetry);   // shows on DS AND Dashboard
        driver = new GamepadInput(gamepad1);
        operator = new GamepadInput(gamepad2);
        robot = new Robot(hardwareMap);   // finds every motor and sensor by name; moves nothing

        // Did Autonomous save where the robot ended up? If yes, the assists (S2) can work.
        hasAutoPose = PoseMemory.hasPose();
    }

    @Override
    public void init_loop() {
        // Runs over and over until PLAY. The alliance is the only thing picked here: it changes
        // every match. It starts as whatever was picked in Auto's INIT (MatchInfo keeps it), so
        // the drive team only has to check it during the 8-second transition.
        // Everything else (Auto routine, start, delay) is decided before the match, in the configs.
        boolean switchAlliance = driver.isDpadLeftPressed() || driver.isDpadRightPressed();
        if (switchAlliance) MatchInfo.switchAlliance();   // RED <-> BLUE
        t.addData("Alliance (D-pad left/right to change)", MatchInfo.alliance);
        // The battery's real voltage, so the drive team can swap a weak battery before PLAY.
        double volts = robot.battery.getVoltage();
        String batteryText = String.format("%.1f V", volts);
        if (volts < HealthConfig.INIT_LOW_BATTERY_V) batteryText += "  LOW - swap the battery";
        t.addData("Battery", batteryText);
        if (hasAutoPose) {
            t.addData("Pose from auto", String.format("saved %.0f s ago: assists (S2) ready",
                    PoseMemory.secondsSinceSaved()));
        } else {
            t.addLine("No auto pose: S1 only. Press Y facing away from drivers.");
        }
        t.update();   // send the text to the screen (nothing shows without this line)
    }

    // ---------------- PLAY ----------------
    @Override
    public void start() {
        // Start the match clock (2:00), the health checks and the match log file.
        robot.startMatch("BioBuzzTeleOp", MatchClock.TELEOP_S);

        // Tell Drive our alliance: it decides which way is "away from the drivers".
        robot.drive.setAlliance(MatchInfo.isRed());

        // Where does the robot start?
        if (hasAutoPose) {
            robot.drive.setStartingPose(PoseMemory.getPose());   // we know exactly: S2 ready
            PoseMemory.clear();   // used once; a second TeleOp must not trust an old pose
            headingConfirmed = true;
        } else {
            robot.drive.setStartingPoseFacingAway();             // we only know the heading: S1 only
        }
    }

    @Override
    public void loop() {
        // The match. These lines run about 50 times every second, always in this order.
        handleDriver();          // 1. gamepad 1 -> driving commands
        handleOperator();        // 1. gamepad 2 -> mechanism commands
        robot.update();          // 2. every part of the robot takes one small step
        rumbleForEvents();       // 3. buzz the gamepads for ALERTs and match time
        showTelemetry();         // 4. show what is happening
    }

    // ---------------- STOP ----------------
    @Override
    public void stop() {
        robot.endMatch();        // every mechanism to its safe state, then close the match log
    }

    // ==================== GAMEPAD 1: DRIVING ====================

    private void handleDriver() {
        // STEP 1: READ every button ONCE and keep the answer in a "boolean" (true or false).
        // Why once? "Pressed" reads are EDGE-DETECTED: true only on the one loop where the
        // button goes down. A second read in the same loop would say false.
        boolean faceAway = driver.isYPressed();
        boolean switchMode = driver.isXPressed();
        boolean goToLaunchSpot = driver.isAPressed();
        boolean goToLoadingZone = driver.isBPressed();
        boolean setPoseHere = driver.isDpadLeftHeldFor(ControlsConfig.SET_POSE_HOLD_MS);   // true once, after 1 s
        boolean mark = driver.isBackPressed();
        boolean aim = driver.isRightBumperHeld();        // true on every loop while held
        boolean sticksMoved = driver.areSticksMoved();   // any driving stick pushed past the deadzone?

        // STEP 2: COMMANDS. Each button calls one method.

        // ---- S2: assist buttons. ----
        // robot.driveToLaunchSpot() starts the path and answers true, or refuses and answers
        // false (position unknown, or the path would hit the HIVE; telemetry says which).
        // "&&" means AND, "!" means NOT. So the line reads: "if A was pressed AND the robot
        // did NOT accept it, buzz the gamepad".
        if (goToLaunchSpot && !robot.driveToLaunchSpot()) driver.rumble(ControlsConfig.RUMBLE_REFUSED_MS);
        if (goToLoadingZone && !robot.driveToLoadingZone()) driver.rumble(ControlsConfig.RUMBLE_REFUSED_MS);
        if (setPoseHere) {
            // The robot is sitting in our reset corner: now we know exactly where it is.
            robot.setPoseAtResetCorner();
            headingConfirmed = true;
            driver.rumble(ControlsConfig.RUMBLE_POSE_SET_MS);   // a long buzz = "S2 ready"
        }

        // ---- S1: the sticks. ----
        // "||" means OR. Drive with the sticks when a stick is pushed, OR when the robot is
        // not driving a path by itself. So moving ANY stick takes the robot back from an
        // assist, in this same loop. While a path runs and no stick is touched, we skip
        // this line and let Pedro drive.
        // Sticks: up = +forward, right = +right / +clockwise. Drive handles Pedro's signs.
        // Holding the right bumper AIMS: the driver still moves with the left stick, and Pedro
        // turns the robot to face the upward CELL. Aiming needs to know where we are (S2).
        if (sticksMoved || aim || !robot.drive.isDrivingItself()) {
            if (aim && robot.drive.isPoseKnown()) {
                robot.drive.driveWithSticksAimedAt(driver.getLeftStickY(), driver.getLeftStickX(), robot.getAimHeadingDeg());
            } else {
                robot.drive.driveWithSticks(driver.getLeftStickY(), driver.getLeftStickX(), driver.getRightStickX());
            }
        }

        // Y: the driver says "the robot faces away from us right now". This fixes a heading
        // that has drifted, and turns field-centric driving back on.
        if (faceAway) {
            robot.drive.resetHeading();
            headingConfirmed = true;
        }
        // X: switch between field-centric and robot-centric. A buzz tells the driver it happened.
        if (switchMode) {
            robot.drive.switchDriveMode();
            driver.rumble(ControlsConfig.RUMBLE_DRIVE_MODE_MS);
        }
        // Back: "something odd just happened" - find this moment in the match log afterwards.
        if (mark) MatchLogger.event("MARK", "driver");
    }

    // ==================== GAMEPAD 2: MECHANISMS ====================

    private void handleOperator() {
        // Same pattern as handleDriver(): read every button ONCE, then one Robot method per button.
        // TODO(BioBuzz): confirm this map once the robot is built.
        boolean intakeOnOff = operator.isAPressed();
        boolean spitOut = operator.isBHeld();             // every loop while held
        boolean spitOutReleased = operator.wasBReleased();   // the one loop B comes back up
        boolean launchAll = operator.isRightTriggerPressed();
        boolean spinUp = operator.isYPressed();
        boolean tipped = operator.isLeftBumperPressed();
        boolean stopAll = operator.isDpadDownPressed();
        boolean mark = operator.isBackPressed();

        if (intakeOnOff) robot.toggleIntake();
        if (spitOut) robot.reverseIntake();          // also how opponent NECTAR goes back out (G408)
        if (spitOutReleased) robot.stopIntake();
        if (launchAll) robot.startLaunching(4);      // we can launch from anywhere
        if (spinUp) robot.spinUpLauncher();
        if (tipped) robot.markHiveTipped();          // the other CELL faces up now: aim and speed follow it
        if (stopAll) robot.stopMechanisms();         // the emergency button: every mechanism stops
        if (mark) MatchLogger.event("MARK", "operator");
    }

    // ==================== RUMBLES (only OpModes touch gamepads) ====================

    private void rumbleForEvents() {
        if (robot.hasNewUrgentAlert()) {   // the ALERT text is on the screen
            driver.rumble(ControlsConfig.RUMBLE_ALERT_MS);
            operator.rumble(ControlsConfig.RUMBLE_ALERT_MS);
        }
        if (robot.clock.justReachedSecondsLeft(ControlsConfig.ONE_MINUTE_WARNING_S)) {
            driver.rumble(ControlsConfig.RUMBLE_ONE_MINUTE_MS);
            operator.rumble(ControlsConfig.RUMBLE_ONE_MINUTE_MS);
        }
        if (robot.clock.justReachedSecondsLeft(ControlsConfig.PARK_WARNING_S)) {
            driver.rumble(ControlsConfig.RUMBLE_PARK_MS);   // time to drive to the LOADING ZONE
        }
    }

    // ==================== TELEMETRY ====================

    private void showTelemetry() {
        // A reminder at the top until the driver has told us which way is "away".
        if (!headingConfirmed) t.addLine("No auto pose: press Y facing away from drivers");
        robot.addTelemetry(t);   // header line first, then each part switched on in TelemetryConfig
        if (TelemetryConfig.GAMEPAD_HELP) {
            t.addLine(TelemetryConfig.GAMEPAD1_HELP);   // the controls cheat sheet
            t.addLine(TelemetryConfig.GAMEPAD2_HELP);
        }
        t.update();              // ALWAYS call update(): it is what actually sends the text
    }
}
