# BioBuzz Software – Low-Level Design (LLD)

Oct 19, 2026 · **Draft for review** · goes with Requirements v2.4 and `HLD.md`

This document describes each class: its job, its public methods, its config values and how it fits with the others. Code sketches show the **shape** of each class for review. They are not final code: names may change while building, and this document is then updated in the same commit.

**Who this is for.** The code is owned during the season by rookie 7th graders with no Java background. Student-zone files (configurations, mechanisms, state machines, TeleOp, Auto, `FieldPaths`, Robot's marked spots, tests) MUST read like the Intake sketch in section 5.1: short methods, plain words, one idea per line. Mentor-zone files (`Mechanism`, `Drive`, helpers, Field Checks) may be a little more technical, but still follow R3.

`TODO(BioBuzz)` marks what waits for the robot's hardware or a team decision (see `DESIGN-REVIEW.md`, section 5).

## Contents

1. Conventions
2. Packages and files
3. Robot
4. The `Mechanism` base class
5. Mechanisms: Intake, Feeder, Launcher, HiveCamera, FrontCamera
6. Drive
7. State machines: Launch
8. Paths and game state: FieldPaths, the HIVE in MatchInfo
9. OpModes: TeleOp, Auto, tests
10. Helpers and configurations
11. How to extend: checklists
12. Telemetry, match log and ALERTs (game additions)
13. Known implementation challenges
14. Future improvements

---

## 1. Conventions

- **Units:** inches, degrees, milliseconds, RPM. The unit is in every constant's name (`_IN`, `_DEG`, `_MS`, `_RPM`).
- **Field frame** (agreed, Q8): the field image as drawn, origin at the bottom-left corner (red GARDEN corner), x to the right, y up, 0 to 144 in. The audience is along the bottom wall. Red ALLIANCE AREA is on the left wall, blue on the right. Heading 0° points along +x, counter-clockwise is positive (Pedro).
- **Alliances:** RED is the default alliance. Every pose is written once, for RED, in `FieldConfig`. BLUE = RED turned 180° about (72, 72) (`x → 144 − x`, `y → 144 − y`, `heading → heading + 180°`), done in one method, `FieldPaths.forAlliance(redPose, isRed)`, by chaining both reflections: `mirrorX(72.0).mirrorY(72.0)` (agreed, Q7).
- **No launch zones:** the robot may launch from anywhere. `FieldConfig.LAUNCH_SPOT` is just one known good spot for the S2 assist and for Auto.
- **Lists, not index numbers:** arrays are written by name and walked with a "for each" loop; no `[i]` in student-zone code.
- **Telemetry words:** state names in CAPITALS; every value has its unit.

---

## 2. Packages and files

```
teamcode/biobuzz/
  Robot.java                     Facade + composition root
  configurations/
    HardwareNames.java           every device name, hub and port; ALL_DEVICES array; motor/servo count
    DriveConfig.java             stick deadzone, driver headings, assist timeout
    ControlsConfig.java          trigger threshold, rumble lengths, button list (comments)
    FieldConfig.java             every field pose (RED), LAUNCH_SPOT, HIVE keep-out, start positions
    AutoConfig.java              step timeouts, PARK_BY_S, default routine and delay
    IntakeConfig.java  FeederConfig.java  LauncherConfig.java  CameraConfig.java
    TelemetryConfig.java  LogConfig.java  HealthConfig.java  FieldCheckConfig.java
  drive/
    Drive.java                   MENTORS: driving over Pedro (replaces parts/DriveBase + subsystems/Drive)
    FieldPaths.java              STUDENTS: every Pedro path; alliance turn; own-side and HIVE checks
  mechanisms/
    Mechanism.java               MENTORS: the one base class (Template Method)
    Intake.java  Feeder.java  Launcher.java  HiveCamera.java  FrontCamera.java      STUDENTS
  statemachines/                 STUDENTS
    LaunchStateMachine.java      Feeder + Launcher + Drive (where we are); the HIVE from MatchInfo
  teleop/BioBuzzTeleOp.java
  auto/BioBuzzAuto.java
  testing/   DriveTest, IntakeTest, FeederTest, LauncherTest, CameraTest, LaunchTest,
             PartsTest, RobotCheck, LogViewer
  fieldchecks/  FieldCheck01_Hardware ... FieldCheck13_Summary
  helpers/   StateTimer, StateLog, GamepadInput, InitMenu, MatchInfo, PoseMemory, MatchClock,
             DashboardTelemetry, FieldDrawing, MatchLogger, BatteryMonitor, LoopMonitor,
             HealthMonitor, FieldCheckHelper, LogSummary
  template/  TemplateMechanism.java, TemplateStateMachine.java, TemplateMechanismTest.java (@Disabled)
  docs/
```

**Removed from today's code:** `parts/` and `subsystems/` (merged into `drive/` and `mechanisms/`), `Robot.buildDrive`, `Drive`'s states, `Drive.setDriverInput` (now `driveWithSticks`), the stick deadzone in Drive (now in `GamepadInput`). There is no `game/` folder: paths live with Drive, and the HIVE with the other match facts in `MatchInfo`. There is no Storage mechanism and no `CollectStateMachine` yet: the robot has no sensors to count balls (see Future improvements).

---

## 3. Robot

**Job:** build everything; be the one "menu" of robot actions; run `update()` in a fixed order; keep the always-on rules. **Patterns:** Facade, Composition root, Mediator.

**Who calls what**

- OpModes call `robot.drive.*` directly for driving (Drive is never shared with a state machine).
- OpModes call **only Robot methods** for mechanisms. This lets Robot cancel the state machine that owns a mechanism before a manual command (Mediator).

```java
public class Robot {
    public final Drive drive;                 // OpModes drive through this directly
    private final Intake intake;              // mechanisms: only through Robot methods
    private final Feeder feeder;
    private final Launcher launcher;
    private final HiveCamera hiveCamera;      // HuskyLens, looks up at the CELL tags
    private final FrontCamera frontCamera;    // Logitech webcam: field tags, POLLEN, NECTAR
    private final Mechanism[] mechanisms;     // every mechanism, by name: update, stop, telemetry, log

    private final LaunchStateMachine launch;

    public Robot(HardwareMap hardwareMap) {
        drive = new Drive(hardwareMap);
        intake = new Intake(hardwareMap);
        feeder = new Feeder(hardwareMap);
        launcher = new Launcher(hardwareMap);
        hiveCamera = new HiveCamera(hardwareMap);
        frontCamera = new FrontCamera(hardwareMap);
        mechanisms = new Mechanism[] { intake, feeder, launcher, hiveCamera, frontCamera };    // ADD A MECHANISM HERE

        launch = new LaunchStateMachine(feeder, launcher, drive);              // given what it uses
        // ADD A STATE MACHINE HERE
    }
```

**Public methods (the menu)**

| Method | Does | Cancels first |
| --- | --- | --- |
| `toggleIntake()`, `startIntake()`, `stopIntake()` | Intake on / off (operator A; Auto's COLLECT step) | – |
| `reverseIntake()` | Spit out while the operator holds B; `stopIntake()` on release | – |
| `startLaunching(count)` | `launch.start(count)`; feeding an empty slot does no harm | any other state machine that uses the Feeder or Launcher (none yet) |
| `spinUpLauncher()` | Pre-spin for the current distance to the upward CELL | – |
| `markHiveTipped()` | `MatchInfo.markHiveTipped()` | – |
| `holdPreloads()` | Auto INIT only: grip the 4 POLLEN (G304) | – |
| `stopMechanisms()` | Cancel every state machine, then `runStop()` on every mechanism | everything |
| `driveToLaunchSpot()` / `driveToLoadingZone()` | S2 assists: refused with a warning if `!drive.isPoseKnown()`; else `drive.followPath(FieldPaths.toLaunchSpot(drive.getPose(), MatchInfo.isRed()))` | – |
| `setPoseAtResetCorner()` | "Set pose here" (Q4) | – |
| `getAimHeadingDeg()` | Heading that faces the upward CELL from the current pose (for the aim button) | – |
| `isIntakeOn()`, `isLaunching()` | Questions for OpModes (`isLaunching()` = `launch.isBusy()`) | – |
| `hasNewUrgentAlert()` | True once per new urgent ALERT; the OpMode rumbles | – |
| `update()` | See below | – |
| `addTelemetry(t)`, `addLogData()` | Header, then each part, honoring `TelemetryConfig` | – |

**`update()` order** (once per loop, and only once)

```java
public void update() {
    // 1. STATE MACHINES: they decide and give mechanisms commands
    launch.update();                        // ADD A STATE MACHINE HERE

    // 2. MECHANISMS: every one, once, whether or not a state machine is using it
    for (Mechanism mechanism : mechanisms) {           // "for each mechanism in the list"
        mechanism.runUpdate();
    }

    // 3. DRIVE: Pedro reads the Pinpoint and sets the drive motors
    drive.update();

    // 4. ALWAYS-ON RULES: see this loop's fresh pose and sensors
    // health.update(...)   step 4: battery, loop time, Pinpoint, heading jump -> ALERTs, robot-centric
    // hive confirm from the HIVE camera: optional, Q2
}
```

Why this order: state machines command first, so mechanisms act on this loop's commands; Drive comes after the mechanisms, so the pose is the newest when the rules run.

**Why Robot, and not the state machines, updates the mechanisms.** A layer may call any layer below it. Every mechanism needs its `update()` once per loop even when no state machine is using it (the cameras read every loop; the launcher holds its speed), and a mechanism shared by two state machines must still be updated exactly once. So Robot owns "update every mechanism", and state machines only call mechanism verbs (`feeder.feedOne()`), never `update()`.

**Why a list and not a key-value map.** A `Map<String, Mechanism>` would let code look a mechanism up by a word, but it adds Java generics, string keys that can be mistyped (and only fail while the robot runs), and a harder loop. Robot never needs to look a mechanism up: each one has its own field name (`intake`), and the list is only for "do this to every mechanism". The list is written by name and walked with "for each", so there are no index numbers to remember.

---

## 4. The `Mechanism` base class

**Zone:** mentors. Students never edit this file; they only fill in the four hooks in their own mechanism. **Job:** the fixed frame every mechanism shares: name, FAULTED flag, safe update and stop, telemetry and log wrappers. **Pattern:** Template Method. It is the only team base class (R3), it is not abstract, and nothing extends a mechanism further.

```java
public class Mechanism {
    private final String name;
    private boolean faulted = false;
    private String faultReason = "";

    public Mechanism(String name) { this.name = name; }

    // ===== HOOKS: your mechanism overrides these (all optional) =====
    /** Every loop: control loops and sensor reads. Never wait or sleep here. */
    protected void update() { }
    /** Put the devices in their safe state: motors off, servos where they hold nothing. */
    protected void stop() { }
    /** Your own telemetry lines (powers, speeds, sensor values). */
    protected void addDetails(Telemetry t) { }
    /** Up to 8 values for the match log: MatchLogger.value("intake_power", power); */
    protected void addLogValues() { }

    // ===== FIXED: Robot and test OpModes call these. Do not override. =====
    public final void runUpdate() {
        if (faulted) return;
        try {
            update();
        } catch (RuntimeException e) {
            markFaulted("error: " + e.getMessage());
        }
    }

    public final void runStop() {
        try {
            stop();
        } catch (RuntimeException e) {
            markFaulted("stop error: " + e.getMessage());
        }
    }

    public final void addTelemetry(Telemetry t) {
        if (faulted) t.addData(name, "FAULTED: " + faultReason);
        else addDetails(t);
    }

    public final void addLogData() {
        MatchLogger.value(name + "_faulted", faulted);
        addLogValues();
    }

    public final boolean isFaulted() { return faulted; }
    public final String getName() { return name; }

    /** A mechanism may call this itself, for example when a stall is detected. */
    protected final void markFaulted(String reason) {
        if (faulted) return;
        faulted = true;
        faultReason = reason;
        MatchLogger.event("FAULT", name + ": " + reason);   // also raises an ALERT (step 4)
    }
}
```

**Rules for every mechanism**

- The constructor gets its devices with `hardwareMap.get(...)` and `HardwareNames`, sets directions and modes, and **never moves an actuator** (G304.H, G403).
- Public methods are verbs (`collect()`, `feedOne()`) and questions (`isUpToSpeed()`, `isOn()`). No states, no timers, no knowledge of other mechanisms.
- Every number comes from its `*Config`.
- A faulted mechanism ignores its verbs (each verb starts with `if (isFaulted()) return;`).

---

## 5. Mechanisms

TODO(BioBuzz): device lists depend on the built robot (review Q5). The shapes below are what the state machines need.

### 5.1 Intake

This is what every student-zone mechanism looks like:

```java
/*
 * STUDENTS: you will change this file.
 * Intake.java - the roller that picks up POLLEN and NECTAR.            ROLE: mechanism
 * It only does what it is told: collect, reverse or stop. It never decides anything.
 */
public class Intake extends Mechanism {

    private DcMotorEx roller;

    public Intake(HardwareMap hardwareMap) {
        super("intake");
        roller = hardwareMap.get(DcMotorEx.class, HardwareNames.MOTOR_INTAKE);
        roller.setDirection(HardwareNames.MOTOR_INTAKE_DIRECTION);
        // Nothing moves here: robots must stay still during INIT (rule G304.H).
    }

    // ---- What the intake can do ----
    public void collect() { roller.setPower(IntakeConfig.COLLECT_POWER); }
    public void reverse() { roller.setPower(IntakeConfig.REVERSE_POWER); }
    public boolean isOn()  { return roller.getPower() != 0; }

    // ---- Filled in for Mechanism ----
    @Override
    protected void stop() { roller.setPower(0); }

    @Override
    protected void addDetails(Telemetry t) {
        t.addData("Intake power", roller.getPower());
    }
}
```

| | |
| --- | --- |
| Devices | `MOTOR_INTAKE` (roller) |
| Verbs | `collect()` (power `IntakeConfig.COLLECT_POWER`), `reverse()` (`REVERSE_POWER`), `stop()` |
| Questions | `isOn()`, `getPower()` |
| `update()` | Stall check (step 4): power above `STALL_POWER` while current above `STALL_CURRENT_A` for `STALL_MS` → `markFaulted("stalled")` |
| Log | `intake_power`, `intake_current_a` |

### 5.2 Feeder

| | |
| --- | --- |
| Devices | `MOTOR_FEEDER` or `SERVO_FEEDER` (TODO(BioBuzz): which, once the launcher is built) |
| Verbs | `feedOne()` (push the next ball from the hopper into the launcher), `hold()` (keep the next ball back), `stop()` |
| Questions | `isFeeding()` |
| `update()` | Nothing (the timing of a feed is the state machine's job) |
| Config | `FeederConfig.FEED_POWER` or `FEED_POSITION`, `HOLD_POSITION` |
| Log | `feeder_power` |

There is no Storage mechanism: the hopper has no sensors, so the robot does not know how many balls it holds or their colours. Storage sensing is a future improvement (section 14).

### 5.3 Launcher

| | |
| --- | --- |
| Devices | `MOTOR_LAUNCHER` (flywheel with encoder; TODO(BioBuzz): a second motor?) |
| Verbs | `spinUpFor(distanceIn)`, `idle()` (`IDLE_RPM`), `stop()` |
| Questions | `isUpToSpeed()` (within `SPEED_TOLERANCE_RPM`), `getRpm()`, `getTargetRpm()` |
| `update()` | Motor velocity control is in the motor (`setVelocity`, PIDF from `LauncherConfig`); `update()` only re-applies a changed PIDF value from the Dashboard |
| Speed table | `LauncherConfig.DISTANCE_IN = {36, 48, 60, 72, 84}`, `RPM = {...}`: one table for both POLLEN and NECTAR, because the robot does not know which ball is next; straight-line interpolation between neighbours, clamped at the ends (a counted `for` loop, mentor-written) |
| Log | `launcher_rpm`, `launcher_target_rpm` (also on the Dashboard graph) |

### 5.4 HiveCamera (HuskyLens, looks up)

| | |
| --- | --- |
| Devices | `HUSKYLENS_HIVE` (I2C), `selectAlgorithm(TAG_RECOGNITION)` in the constructor (not an actuator) |
| Verbs | none |
| Questions | `seesTag(learnedId)`, `getTagOffsetX(learnedId)` (pixels from the image centre, + = right), `getTagWidthPx(learnedId)`, `getAgeMs()` |
| `update()` | Every `CameraConfig.HIVE_SAMPLE_MS` (default 100), read `blocks()` and keep them with their time |
| Config | `HIVE_SAMPLE_MS`, the HuskyLens's learned ID for each CELL's tags (`BLUE_CELL_A_ID`, …), mounting angle |
| Log | `hive_cam_tag_id`, `hive_cam_offset_px`, `hive_cam_age_ms` |
| Used by | Confirming `MatchInfo`'s upward CELL (optional, Q2), `Robot.getAimHeadingDeg()` |

### 5.5 FrontCamera (Logitech webcam, looks ahead)

| | |
| --- | --- |
| Devices | `WEBCAM_FRONT` (USB, UVC). A `VisionPortal` built in the constructor with two processors: `AprilTagProcessor` and a colour-blob processor for POLLEN, red NECTAR and blue NECTAR. Live preview off unless `CameraConfig.PREVIEW` |
| Verbs | `pause()` / `resume()` (stop or restart streaming to save CPU when not needed), `stop()` closes the portal |
| Questions | `seesTag(id)` (real AprilTag IDs), `getTagPose(id)` (range in, bearing deg, yaw deg, from the SDK), `seesPollen()`, `seesRedNectar()`, `seesBlueNectar()`, `getPollenBearingDeg()`, `getAgeMs()` |
| `update()` | Copy the processors' newest results once per loop (cheap: vision runs on the SDK's own thread) |
| Config | Resolution, exposure and gain, decimation, colour ranges for POLLEN, red NECTAR and blue NECTAR, camera position and angle on the robot (for the tag pose), `PREVIEW` |
| Log | `front_cam_tag_id`, `front_cam_tag_range_in`, `front_cam_sees` (what is in front), `front_cam_fps` |
| Used by | Telemetry (what is in front of the intake), Field Check 12; later, aiming the intake |

---

## 6. Drive

**Job:** driving operations over Pedro's follower. **Patterns:** Facade (Pedro's follower), Adapter (stick values → `DrivePowers`). Replaces `parts/DriveBase` and `subsystems/Drive`. It is the only team class that calls `Constants.create(hardwareMap)`. It is not a Mechanism: stop-all never stops the drivetrain.

| Group | Methods |
| --- | --- |
| Setup | `Drive(hardwareMap)`, `setAlliance(isRed)`, `setStartingPose(pose)` (pose known), `setStartingPoseFacingAway()` (heading only, pose **not** known) |
| Driver driving (S1) | `driveWithSticks(forward, right, clockwise)`: field-centric unless robot-centric is on; `driveWithSticksAimedAt(forward, right, targetHeadingDeg)` (Pedro `ManualDrive.headingLock`); `resetHeading()`; `switchDriveMode()`; `useRobotCentric(reason)`, `useFieldCentric(reason)`; `isFieldCentric()` |
| Path driving (S2, S3) | `followPath(path)`; `isDrivingItself()` (Pedro FOLLOW or HOLD); `isPathDone()`; `holdHere()`; `stopDrivingItself()` (back to manual, zero power) |
| Pose | `getPose()`, `getX()`, `getY()`, `getHeadingDegrees()` (−180 to 180), `setPose(pose, reason)` (logged), `isPoseKnown()`, `isStill()` (speed under `DriveConfig.STILL_IN_PER_S`) |
| Loop | `update()` (Pedro's `follower.update()`, every loop), `addTelemetry(t)`, `addLogData()`, `addDrawing()` |

**Rules**

- Field-centric: `ManualDrive.fieldCentric(powers, heading, −driverHeading)`; robot-centric skips the rotation. Every switch is logged (`RECOVER_*` / `SAFEGUARD_*` events).
- `isPoseKnown()` is true after `setStartingPose` (auto, or the pose from `PoseMemory`) or `setPose` (set-pose-here, camera); false after `setStartingPoseFacingAway()` and after a Pinpoint fault. Assist buttons check it.
- Speed and acceleration limits for paths come from Pedro's `ForesightConfig` in `pedro/Constants.java`. Sticks drive at full power.
- Pinpoint status, rate and recalibration: see Known implementation challenges.

---

## 7. State machines

Every state machine has the same four sections (R3): **1. States** (the `enum`, one comment per value), **2. Commands** (`start(...)`, `cancel()`: they only call `goTo`), **3. `update()`** (one `case` per state, marked DO / CHECK / GO), **4. Bookkeeping** (`goTo(state, reason)`, `isBusy()`, `getState()`, `addTelemetry(t)`, `addLogData()`). Every waiting state has a timeout that goes to `IDLE` with a logged reason. They get their mechanisms in the constructor (dependency injection) and never call each other or Robot.

### 7.1 LaunchStateMachine (Feeder + Launcher + Drive)

```
          start(count)          up to speed            FEED_MS passed
   IDLE ------------> SPINNING_UP ----------> FEEDING ---------------> RECOVERING
    ^                    |   ^                                            |
    |  timeout/cancel    |   +----- more to launch, RECOVER_MS passed ----+
    +--------------------+                                                |
    +--------------------------- count reached ---------------------------+
```

| State | DO | CHECK → GO | Timeout |
| --- | --- | --- | --- |
| `IDLE` | `feeder.hold()`, `launcher.idle()` (once, on entry) | – | – |
| `SPINNING_UP` | `launcher.spinUpFor(distance to the upward CELL)` | up to speed and no TIP pause → `FEEDING` | `LauncherConfig.SPIN_UP_TIMEOUT_MS` → `IDLE` "spin-up timeout" (warn) |
| `FEEDING` | `feeder.feedOne()` | timer ≥ `FEED_MS` → `RECOVERING` | (the timer itself) |
| `RECOVERING` | `feeder.hold()` | timer ≥ `RECOVER_MS`: launched < count → `SPINNING_UP`; else → `IDLE` "done" | (the timer itself) |

The robot cannot see how many balls it holds, so `start(count)` feeds `count` times (4 for "launch everything"); feeding an empty slot does no harm.

The robot may launch from anywhere (no launch zones). The distance comes from `drive.getPose()` and `FieldPaths.upCellPose(MatchInfo.isRed())`. After the operator presses "TIPPED", `MatchInfo.secondsSinceTip()` holds the next feed for `LauncherConfig.TIP_PAUSE_MS` (manual 10.5.1).

---

## 8. Paths and game state

### 8.1 The HIVE, in `MatchInfo`

Which of our two CELLS faces up, and how many TIPS we have, are match facts like the alliance, so they live in `helpers/MatchInfo.java`. Its fields are `static`, so they survive from Auto to TeleOp.

| Method | Does |
| --- | --- |
| `resetForNewMatch()` | Auto `init()` only: up CELL = the setup position (`FieldConfig.START_CELL_A_UP`), TIPS = 0 |
| `markHiveTipped()` | Flip the up CELL, TIPS + 1, note the time, log `HIVE_TIP:<n>` |
| `isCellAUp()` | Which CELL faces up |
| `getTipCount()`, `getNectarAllowed()` | For the header: the human player may enter one NECTAR per TIP (G426) until 60 s left |
| `secondsSinceTip()` | For the launch pause after a TIP |

### 8.2 FieldPaths (`drive/FieldPaths.java`)

**Zone:** students. **Job:** build every Pedro path, in one file. **Pattern:** Factory method. Pedro's path API (`Paths.line`, `Paths.curve`, `PoseFactory`) appears only here and in `Drive`.

| Method | Path |
| --- | --- |
| `forAlliance(redPose, isRed)` | RED: the pose as written. BLUE: turned 180° about (72, 72), `PoseFactory` `mirrorX(72.0).mirrorY(72.0)` |
| `startPose(start, isRed)` | One of `FieldConfig.START_*` |
| `upCellPose(isRed)` | `FieldConfig.CELL_A` or `CELL_B`, from `MatchInfo.isCellAUp()` |
| `toLaunchSpot(from, isRed)` | Straight line to `FieldConfig.LAUNCH_SPOT` |
| `toLoadingZone(from, isRed)` | Curve through `LOADING_APPROACH` to `LOADING_PARK` |
| `leaveWall(start, isRed)` | Short path off the wall (LEAVE), facing the upward CELL |
| `throughGarden(from, isRed)` | Auto collection along our GARDEN line |
| `isOnOwnSide(path, isRed)` | Samples 20 points: all within our half (G402) |
| `avoidsHive(path)` | Samples 20 points: none inside `FieldConfig.HIVE_KEEP_OUT` (G409, G417) |

---

## 9. OpModes

### 9.1 BioBuzzTeleOp

```java
public void init()       { build Robot, GamepadInputs, InitMenu rows (alliance, match, battery, logging) }
                           // NO actuator commands here (G403)
public void init_loop()  { menu with gamepad 1 D-pad; show pose-from-auto or "S1: no auto pose" }
public void start()      { copy menu to MatchInfo; MatchClock.startTeleOp();
                           pose from PoseMemory -> drive.setStartingPose (pose known)
                           else drive.setStartingPoseFacingAway() (S1) }
public void loop()       { handleDriver(); handleOperator(); robot.update(); signals(); showTelemetry(); }
public void stop()       { robot.stopMechanisms(); robot.update(); }

private void handleDriver() {
    boolean faceAway = driver.isYPressed();
    boolean switchMode = driver.isXPressed();
    boolean goToLaunchSpot = driver.isAPressed();
    boolean goToLoadingZone = driver.isBPressed();
    boolean aim = driver.isRightBumperHeld();
    boolean sticksMoved = driver.areSticksMoved();      // deadzone is inside GamepadInput

    // S2 assists: Robot refuses them (with a warning) while the pose is not known
    if (goToLaunchSpot) robot.driveToLaunchSpot();
    if (goToLoadingZone) robot.driveToLoadingZone();

    // Manual driving. Moving a stick ALWAYS takes the robot back, even in the middle of a path.
    if (sticksMoved || !robot.drive.isDrivingItself()) {
        if (aim) robot.drive.driveWithSticksAimedAt(driver.getLeftStickY(), driver.getLeftStickX(), robot.getAimHeadingDeg());
        else robot.drive.driveWithSticks(driver.getLeftStickY(), driver.getLeftStickX(), driver.getRightStickX());
    }
    if (faceAway) robot.drive.resetHeading();
    if (switchMode) robot.drive.switchDriveMode();
    // snap to 90, recalibrate, set pose here, mark: step 4
}

private void handleOperator() {
    boolean intakeOnOff = operator.isAPressed();
    boolean spitOut = operator.isBHeld();
    boolean spitOutReleased = operator.wasBReleased();
    boolean launchAll = operator.isRightTriggerPressed();
    boolean tipped = operator.isLeftBumperPressed();
    boolean spinUp = operator.isYPressed();
    boolean stopAll = operator.isDpadDownPressed();

    if (intakeOnOff) robot.toggleIntake();
    if (spitOut) robot.reverseIntake();        // also how opponent NECTAR goes back out (G408)
    if (spitOutReleased) robot.stopIntake();
    if (launchAll) robot.startLaunching(4);    // works from anywhere
    if (tipped) robot.markHiveTipped();
    if (spinUp) robot.spinUpLauncher();
    if (stopAll) robot.stopMechanisms();
}

/** Rumbles: only OpModes touch gamepads. */
private void signals() {
    if (robot.hasNewUrgentAlert()) { driver.rumble(...); operator.rumble(...); }
    if (MatchClock.justReachedSecondsLeft(60)) { both rumble }   // NECTAR + FLOWERS unlocked
    if (MatchClock.justReachedSecondsLeft(20)) { driver rumble }  // time to PARK
}
```

### 9.2 BioBuzzAuto

INIT menu rows: Alliance, Start, Routine, Delay (0–10 s), Logging. `init()`: build Robot, `MatchInfo.resetForNewMatch()`, `robot.holdPreloads()` (the only actuator move in any INIT). `init_loop()`: menu, then build the routine's paths for the chosen alliance and start; show a red warning if any path fails `isOnOwnSide` or `avoidsHive`. `start()`: `MatchClock.startAuto()`, `drive.setStartingPose(...)`. `loop()`: park-by guard, `runStep()`, `robot.update()`, `PoseMemory.save(...)`, telemetry. `stop()`: `robot.stopMechanisms()`, save the pose.

| Step | First loop | Done when | Timeout (`AutoConfig`) |
| --- | --- | --- | --- |
| `WAIT_DELAY` | – | delay from the menu passed | – |
| `LEAVE` | `drive.followPath(FieldPaths.leaveWall(...))` | `drive.isPathDone()` | `DRIVE_TIMEOUT_MS` → `holdHere()`, go on |
| `LAUNCH_PRELOADS` | `robot.startLaunching(4)` (from where the robot is: no launch zones) | `!robot.isLaunching()` | `LAUNCH_TIMEOUT_MS` → cancel, go on |
| `COLLECT_GARDEN` (`LAUNCH_COLLECT_PARK` only) | `robot.startIntake()`, `followPath(throughGarden)` | path done → `robot.stopIntake()` | `COLLECT_TIMEOUT_MS` |
| `LAUNCH_AGAIN` (`LAUNCH_COLLECT_PARK` only) | `robot.startLaunching(4)` from the end of the GARDEN path | `!robot.isLaunching()` | `LAUNCH_TIMEOUT_MS` |
| `PARK` | `robot.stopMechanisms()`, `followPath(toLoadingZone)` | path done | `PARK_TIMEOUT_MS` |
| `DONE` | – | – | – |

Park-by guard: `if (MatchClock.autoSeconds() > AutoConfig.PARK_BY_S && step is before PARK) goToStep(PARK, "park-by")`.

### 9.3 Test OpModes (group "Tests")

| OpMode | Builds | Buttons |
| --- | --- | --- |
| `Test: Drive` | `new Drive(hardwareMap)` | sticks, Y, X, A/B assists (pose set to the field centre at start) |
| `Test: Intake`, `Test: Feeder`, `Test: Launcher` | that mechanism only | one button per verb; telemetry shows every question's answer |
| `Test: Camera` | both cameras | shows what each camera sees, the webcam's frame rate and tag poses; X toggles the webcam preview |
| `Test: Launch` | `Robot` | start / cancel, shows state and history |
| `Test: Parts` | nothing but `hardwareMap` | D-pad picks a motor or servo from `HardwareNames.ALL_DEVICES`, sticks move it |

---

## 10. Helpers and configurations

**Helpers** (public methods; `try`/`catch` and `List` allowed here only)

| Helper | Public methods |
| --- | --- |
| `StateTimer` | `reset()`, `seconds()`, `milliseconds()`, `isTimeUp(ms)` |
| `StateLog` | `record(from, to, reason)`, `warn(msg)`, `getLastChange()`, `getWarning()`, `addTo(t, name, state, timer, busy)` |
| `GamepadInput` | sticks with deadzone (`getLeftStickY()` …), `areSticksMoved()`, `isXPressed()` / `isXHeld()` / `wasXReleased()` per button, `rumble(ms)`, `setLedColor(r, g, b)`, `getLastUpdateMs()` |
| `InitMenu` | `addChoiceRow(label, choices, startChoice)` (choices can be an enum's `values()`), `addNumberRow`, `update(up, down, left, right)`, `getChoice(row)` (read back with the type in front: `(AllianceColor) menu.getChoice(row)`), `getNumber(row)`, `getText(row)`, `addTo(t)` |
| `AllianceColor` | `enum AllianceColor { RED, BLUE }` |
| `MatchInfo` | static `alliance` (an `AllianceColor`, RED until chosen), `fieldName`; `isRed()`, `switchAlliance()`; the HIVE: `resetForNewMatch()`, `markHiveTipped()`, `isCellAUp()`, `getTipCount()`, `getNectarAllowed()`, `secondsSinceTip()` |
| `PoseMemory` | `save(pose)`, `hasPose()`, `getPose()`, `secondsSinceSaved()`, `clear()` |
| `MatchClock` (new) | `startAuto()`, `startTeleOp()`, `autoSeconds()`, `teleOpSecondsLeft()`, `justReachedSecondsLeft(s)`, `getTimeLeftText()` |
| `DashboardTelemetry` | `create(telemetry)` |
| `FieldDrawing` | `drawRobot(x, y, headingDeg)`, `drawPath(xs, ys)`, `drawPoint(x, y)`, `send()` |
| `MatchLogger` (step 4) | `start(opModeName)`, `value(column, number)`, `event(type, text)`, `endRow()`, `stop()` |
| `BatteryMonitor` | `getVoltage()`: the lowest hub voltage, read at most every `HealthConfig.BATTERY_SAMPLE_MS`; shown on the header line and in TeleOp INIT. Step 4 adds resting voltage, lowest since PLAY and alerts |
| `LoopMonitor`, `HealthMonitor` (step 4) | see Requirements, Troubleshooting C and D |
| `FieldCheckHelper`, `LogSummary` | steps 5 and 6 |

**Configurations** (`@Config`, `public static`, not `final`, except `HardwareNames`)

| Class | Main values (starting values, TODO(BioBuzz) to tune) |
| --- | --- |
| `HardwareNames` | device names with hub and port; `ALL_DEVICES` array; motor and servo count (≤ 8 each) |
| `DriveConfig` | `STICK_DEADZONE`, `BLUE_DRIVER_HEADING_DEG`, `RED_DRIVER_HEADING_DEG`, `ASSIST_TIMEOUT_MS`, `STILL_IN_PER_S` |
| `FieldConfig` | `START_*` poses, `LAUNCH_SPOT`, `CELL_A`, `CELL_B`, `START_CELL_A_UP`, `LOADING_APPROACH`, `LOADING_PARK`, `RESET_CORNER`, `GARDEN_*`, `HIVE_KEEP_OUT_*` (all RED, inches/degrees) |
| `AutoConfig` | step timeouts (including `COLLECT_TIMEOUT_MS`), `PARK_BY_S` (start at 26), default routine (`LAUNCH_PARK`) and delay |
| `IntakeConfig` | `COLLECT_POWER`, `REVERSE_POWER`, stall values |
| `FeederConfig` | `FEED_POWER` or `FEED_POSITION`, `HOLD_POSITION` |
| `LauncherConfig` | PIDF, `IDLE_RPM`, `SPEED_TOLERANCE_RPM`, the speed table, `SPIN_UP_TIMEOUT_MS`, `FEED_MS`, `RECOVER_MS`, `TIP_PAUSE_MS` |
| `CameraConfig` | `HIVE_SAMPLE_MS`, HuskyLens learned tag IDs; webcam resolution, exposure, decimation, colour ranges, position and angle, `PREVIEW` |
| `ControlsConfig` | `TRIGGER_THRESHOLD`, rumble lengths, the button list as comments |

---

## 11. How to extend: checklists

**Add a mechanism** (example: a FLOWER arm)

1. Copy `template/TemplateMechanism.java` to `mechanisms/FlowerArm.java`; rename; `extends Mechanism`; fill in the hooks you need.
2. Add its devices to `HardwareNames` (with hub and port, and in `ALL_DEVICES`); check the motor and servo count.
3. Add `configurations/FlowerArmConfig.java`.
4. In `Robot`: one field, build it in the constructor, add its name to the `mechanisms` list. (Update, stop-all, telemetry and log now include it.)
5. Add Robot methods for its actions (cancel any state machine that uses it).
6. Copy `template/TemplateMechanismTest.java` to `testing/FlowerArmTest.java`.
7. Add gamepad 2 buttons in `handleOperator()`, and update the three cheat sheets.
8. Add a `TelemetryConfig` switch if its lines are long.
9. Update `HLD.md` (robot table), `LLD.md` (section 5) and `README.md`.

**Add a state machine**

1. Copy `template/TemplateStateMachine.java` to `statemachines/<Task>StateMachine.java`; list its states; give every waiting state a timeout in a `*Config`.
2. In `Robot`: one field, build it with the mechanisms it uses; one line each in `update()` (section 1, before the mechanisms), `stopMechanisms()` and telemetry. It never calls a mechanism's `update()`: Robot already does.
3. Add a Robot start method; it cancels every state machine that shares a mechanism with this one.
4. Add a test OpMode (start / cancel) and the buttons or auto steps that start it.
5. Update `HLD.md`, `LLD.md` (section 7, with the state table) and `README.md`.

**Add an assist (S2):** a pose in `FieldConfig` → a path method in `FieldPaths` → a Robot method that checks `drive.isPoseKnown()` → one line in `handleDriver()` → cheat sheets.

**Add an auto step:** a `Step` value → a `case` (first loop, done, timeout) → a timeout in `AutoConfig` → the step table in this document.

---

## 12. Telemetry, match log and ALERTs (game additions)

**Header line:** `RED | 1:42 left | 18 ms | 12.6 V | FIELD 3° | intake ON | launcher READY | up CELL A | tips 2 | ALERT ...`

**New DATA columns:** `intake_power`, `feeder_power`, `launcher_rpm`, `launcher_target_rpm`, `up_cell`, `tip_count`, `launch_state`, `<mechanism>_faulted`, `pose_known`, `match_s_left`.

**New EVENTS:** `HIVE_TIP:<n>`, `INTAKE_ON`, `INTAKE_OFF`, `INTAKE_REVERSE`, `FAULT:<mechanism>:<reason>`, `ASSIST:<name>`, `ASSIST_REFUSED:pose unknown`, `ASSIST_CANCELLED:stick`, `POSE_SET:<how>:<old>-><new>`, `AUTO_PATH_OFFSIDE:<path>`, `PARK_BY`.

**New ALERTs:** mechanism FAULTED (urgent), auto path off our side (INIT, red), launcher can't reach speed (spin-up timeout).

---

## 13. Known implementation challenges

| Challenge | Options | Plan |
| --- | --- | --- |
| **Pinpoint access.** Pedro's `PinpointLocalizer` keeps its driver private; safeguards need status, rate and `recalibrateIMU()` | A second handle from `hardwareMap` in `Drive`; reflection (forbidden); a team localizer class | Second handle, read-only except a recalibrate when the robot is still; record the risks (two objects on one device, reset during a path) |
| **Dashboard field background** | `TelemetryPacket(false)` + `drawImage("/images/field-biobuzz.png", 0, 0, 144, 144)`; flicker between packets | Already in `FieldDrawing`; the image is at `TeamCode/src/main/assets/images/field-biobuzz.png`; check flicker on the robot |
| **Constants depends on team code** | `pedro/Constants.java` imports `HardwareNames` | Keep; record in `UPSTREAM.md` |
| **HuskyLens is 2D** | Box size → rough distance; tag learned IDs must be trained on each unit; one algorithm at a time; I2C read takes a few ms | Read every 100 ms; use for "which CELL" and aim offsets only; document training in `CAMERA_GUIDE.md` |
| **Webcam CPU load** | `VisionPortal` runs on its own thread, but AprilTag and colour processing at full resolution can slow the whole Control Hub, and so the loop | Low resolution (for example 640 × 480), AprilTag decimation, preview off in matches, `pause()` when not needed; measure the loop time with the webcam streaming |
| **Which CELL is up** | Operator button; camera; counting launched elements | Operator button first (Q2) |
| **Detecting a disconnected device** | The SDK seldom throws; a motor on an unplugged hub keeps "working" | Watch `LynxModule` health and motor current; mark FAULTED; acceptance test: pull a cable |
| **Arrays in `@Config`** (launcher speed tables) | FTC Dashboard may not edit arrays live | If not, use named fields (`POLLEN_RPM_36_IN`, …) |
| **Pedro 3 path headings** | Paths need a heading plan (tangent, constant, linear) | Check `paths/interpolator` in Pedro 3.0.1 when writing `FieldPaths` |
| **Static state between OpModes** (`MatchInfo` with the HIVE, `PoseMemory`) | Static fields survive OpMode changes but not an app restart | Auto `init()` resets them; TeleOp only reads them; telemetry shows where each value came from |

---

## 14. Future improvements

| Improvement | Gains | Costs | When |
| --- | --- | --- | --- |
| **Bulk reads** (`LynxModule` caching, AUTO or MANUAL) | 1–2 ms per sensor read saved | MANUAL: a missed clear freezes sensor values; needs a stored hub list in a helper | When `LoopMonitor` shows loops near the slow-loop limit; record loop time before and after |
| **Storage sensing** (a Storage mechanism) | Knowing how many balls are held and their colours: stop the intake at 4 (G407), push opponent NECTAR out automatically (G408), launch exactly what is held, pick a speed table per ball type | Sensors in the hopper (a colour sensor at the mouth, or one per slot); a `Storage` mechanism and a `CollectStateMachine` (intake until 4, reject opponent NECTAR) | When the robot's hopper gets sensors |
| Camera pose fix (front webcam tag pose) | x/y recovery without the reset corner | Camera position calibration; the CELL tags face down, so they are seen at a steep angle; wrong fixes are worse than none | After Field Check 12 passes reliably |
| Launch while moving | Faster cycles | Needs velocity-aware aiming | After consistent stationary launching |
| FLOWER placer | Bottom NECTAR Bonus, FLOWER ownership | A new mechanism (checklist in section 11) | If the team builds one |
| Limelight 3A | Real 3D AprilTag pose | Cost, mounting | If HuskyLens is not good enough |
