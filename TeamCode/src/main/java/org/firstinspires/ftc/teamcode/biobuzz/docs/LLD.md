# BioBuzz Software – Low-Level Design (LLD)

Oct 19, 2026 · **Draft for review** · goes with Requirements v2.1 and `HLD.md`

This document describes each class: its job, its public methods, its config values and how it fits with the others. Code sketches show the **shape** of each class for review. They are not final code: names may change while building, and this document is then updated in the same commit.

`TODO(BioBuzz)` marks what waits for the robot's hardware or a team decision (see `DESIGN-REVIEW.md`, section 5).

## Contents

1. Conventions
2. Packages and files
3. Robot
4. The `Mechanism` base class
5. Mechanisms: Intake, Storage, Launcher, HiveCamera, FrontCamera
6. Drive
7. State machines: Collect, Launch
8. `game/`: Element, HiveState, FieldPaths
9. OpModes: TeleOp, Auto, tests
10. Helpers and configurations
11. How to extend: checklists
12. Telemetry, match log and ALERTs (game additions)
13. Known implementation challenges
14. Future improvements

---

## 1. Conventions

- **Units:** inches, degrees, milliseconds, RPM. The unit is in every constant's name (`_IN`, `_DEG`, `_MS`, `_RPM`).
- **Field frame** (TODO(BioBuzz): confirm, review Q8): the field image as drawn, origin at the bottom-left corner (red GARDEN corner), x to the right, y up, 0 to 144 in. The audience is along the bottom wall. Red ALLIANCE AREA is on the left wall, blue on the right. Heading 0° points along +x, counter-clockwise is positive (Pedro).
- **Alliances:** every pose is written once, for BLUE, in `FieldConfig`. RED = BLUE turned 180° about (72, 72) (`x → 144 − x`, `y → 144 − y`, `heading → heading + 180°`), done in one method, `FieldPaths.forAlliance(pose, isRed)`, by chaining both reflections: `mirrorX(72.0).mirrorY(72.0)` (agreed, Q7).
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
    FieldConfig.java             every field pose (BLUE), HIVE keep-out, start positions
    AutoConfig.java              step timeouts, PARK_BY_S, default routine and delay
    IntakeConfig.java  StorageConfig.java  LauncherConfig.java  CameraConfig.java
    TelemetryConfig.java  LogConfig.java  HealthConfig.java  FieldCheckConfig.java
  drive/
    Drive.java                   driving operations over Pedro (replaces parts/DriveBase + subsystems/Drive)
  mechanisms/
    Mechanism.java               the one base class (Template Method)
    Intake.java  Storage.java  Launcher.java  HiveCamera.java  FrontCamera.java
  statemachines/
    CollectStateMachine.java     Intake + Storage
    LaunchStateMachine.java      Storage + Launcher + HiveState + Drive (pose)
  game/
    Element.java                 enum: NONE, POLLEN, RED_NECTAR, BLUE_NECTAR
    HiveState.java               which CELL is up, TIP count, launch pause after a TIP
    FieldPaths.java              every Pedro path; alliance turn; own-side and HIVE checks
  teleop/BioBuzzTeleOp.java
  auto/BioBuzzAuto.java
  testing/   DriveTest, IntakeTest, StorageTest, LauncherTest, CameraTest, CollectTest, LaunchTest,
             PartsTest, RobotCheck, LogViewer
  fieldchecks/  FieldCheck01_Hardware ... FieldCheck13_Summary
  helpers/   StateTimer, StateLog, GamepadInput, InitMenu, MatchInfo, PoseMemory, MatchClock,
             DashboardTelemetry, FieldDrawing, MatchLogger, BatteryMonitor, LoopMonitor,
             HealthMonitor, FieldCheckHelper, LogSummary
  template/  TemplateMechanism.java, TemplateStateMachine.java, TemplateMechanismTest.java (@Disabled)
  docs/
```

**Removed from today's code:** `parts/` and `subsystems/` (merged into `drive/` and `mechanisms/`), `Robot.buildDrive`, `Drive`'s states, `Drive.setDriverInput` (now `driveWithSticks`), the stick deadzone in Drive (now in `GamepadInput`).

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
    private final Storage storage;
    private final Launcher launcher;
    private final HiveCamera hiveCamera;      // HuskyLens, looks up at the CELL tags
    private final FrontCamera frontCamera;    // Logitech webcam: field tags, POLLEN, NECTAR
    private final Mechanism[] mechanisms;     // update, stop, telemetry and log loop over this

    private final HiveState hive;
    private final CollectStateMachine collect;
    private final LaunchStateMachine launch;

    public Robot(HardwareMap hardwareMap) {
        drive = new Drive(hardwareMap);
        intake = new Intake(hardwareMap);
        storage = new Storage(hardwareMap);
        launcher = new Launcher(hardwareMap);
        hiveCamera = new HiveCamera(hardwareMap);
        frontCamera = new FrontCamera(hardwareMap);
        mechanisms = new Mechanism[] { intake, storage, launcher, hiveCamera, frontCamera };   // ADD A MECHANISM HERE

        hive = new HiveState();
        collect = new CollectStateMachine(intake, storage);                    // given what it uses
        launch = new LaunchStateMachine(storage, launcher, hive, drive);
    }
```

**Public methods (the menu)**

| Method | Does | Cancels first |
| --- | --- | --- |
| `startCollecting()` | `collect.start()` | `launch` (both use Storage) |
| `reverseIntake()` / `stopIntake()` | Manual spit-out (operator holds B) | `collect` |
| `startLaunching(count)` | `launch.start(count)` | `collect` |
| `spinUpLauncher()` | Pre-spin for the next element and the current distance | – |
| `markHiveTipped()` | `hive.markTipped()` | – |
| `holdPreloads()` | Auto INIT only: grip the 4 POLLEN (G304) | – |
| `stopMechanisms()` | Cancel every state machine, then `runStop()` on every mechanism | everything |
| `driveToLaunchSpot()` / `driveToLoadingZone()` | S2 assists: refused with a warning if `!drive.isPoseKnown()`; else `drive.followPath(FieldPaths.toLaunchSpot(...))` | – |
| `setPoseAtLoadingCorner()` | "Set pose here" (Q4) | – |
| `getAimHeadingDeg()` | Heading that faces the upward CELL from the current pose (for the aim button) | – |
| `isCollecting()`, `isLaunching()`, `getHeldCount()`, `getUpCellName()` | Questions for OpModes | – |
| `hasNewUrgentAlert()` | True once per new urgent ALERT; the OpMode rumbles | – |
| `update()` | See below | – |
| `addTelemetry(t)`, `addLogData()` | Header, then each part, honoring `TelemetryConfig` | – |

**`update()` order** (once per loop, and only once)

```java
public void update() {
    // 1. STATE MACHINES: they decide and command mechanisms
    collect.update();                       // ADD A STATE MACHINE HERE
    launch.update();

    // 2. MECHANISMS: their own control loops and sensor reads
    for (int i = 0; i < mechanisms.length; i++) {
        mechanisms[i].runUpdate();
    }

    // 3. DRIVE: Pedro reads the Pinpoint and sets the drive motors
    drive.update();

    // 4. ALWAYS-ON RULES: see this loop's fresh pose and sensors
    // health.update(...)   step 4: battery, loop time, Pinpoint, heading jump -> ALERTs, robot-centric
    // hive confirm from the HIVE camera: optional, Q2
}
```

Why this order: state machines command first, so mechanisms act on this loop's commands; Drive comes after the mechanisms, so the pose is the newest when the rules run.

---

## 4. The `Mechanism` base class

**Job:** the fixed frame every mechanism shares: name, FAULTED flag, safe update and stop, telemetry and log wrappers. **Pattern:** Template Method. It is the only team base class (R3), it is not abstract, and nothing extends a mechanism further.

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
- Public methods are verbs (`collect()`, `feedOne()`) and questions (`isUpToSpeed()`, `getHeldCount()`). No states, no timers, no knowledge of other mechanisms.
- Every number comes from its `*Config`.
- A faulted mechanism ignores its verbs (each verb starts with `if (isFaulted()) return;`).

---

## 5. Mechanisms

TODO(BioBuzz): device lists depend on the built robot (review Q5). The shapes below are what the state machines need.

### 5.1 Intake

| | |
| --- | --- |
| Devices | `MOTOR_INTAKE` (roller) |
| Verbs | `collect()` (power `IntakeConfig.COLLECT_POWER`), `reverse()` (`REVERSE_POWER`), `stop()` |
| Questions | `getPower()` |
| `update()` | Stall check (step 4): power above `STALL_POWER` while current above `STALL_CURRENT_A` for `STALL_MS` → `markFaulted("stalled")` |
| Log | `intake_power`, `intake_current_a` |

### 5.2 Storage (with feeder)

| | |
| --- | --- |
| Devices | `MOTOR_FEEDER` or `SERVO_FEEDER`; `COLOR_INTAKE` (colour sensor at the mouth); TODO(BioBuzz): slot sensors (Q5) |
| Verbs | `feedOne()` (push the next element into the launcher), `holdFeeder()`, `stop()` |
| Questions | `getHeldCount()` (0–5), `getNextElement()` (`Element`), `getNewestElement()`, `isOpponentNectar(isRed)` |
| `update()` | Read the colour sensor every `StorageConfig.SENSE_MS`; count an element in when one passes the mouth; count one out when `feedOne()` finishes |
| Config | `MAX_HELD = 4` (G407), colour thresholds for POLLEN / red / blue, `FEED_POWER`, `SENSE_MS` |
| Log | `held_count`, `newest_element` (code) |

### 5.3 Launcher

| | |
| --- | --- |
| Devices | `MOTOR_LAUNCHER` (flywheel with encoder; TODO(BioBuzz): a second motor?) |
| Verbs | `spinUpFor(element, distanceIn)`, `idle()` (`IDLE_RPM`), `stop()` |
| Questions | `isUpToSpeed()` (within `SPEED_TOLERANCE_RPM`), `getRpm()`, `getTargetRpm()` |
| `update()` | Motor velocity control is in the motor (`setVelocity`, PIDF from `LauncherConfig`); `update()` only re-applies a changed PIDF value from the Dashboard |
| Speed table | `LauncherConfig.DISTANCE_IN = {36, 48, 60, 72, 84}`, `POLLEN_RPM = {...}`, `NECTAR_RPM = {...}`; straight-line interpolation between neighbours, clamped at the ends (a counted `for` loop) |
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
| Used by | `HiveState` confirm (optional, Q2), `Robot.getAimHeadingDeg()` |

### 5.5 FrontCamera (Logitech webcam, looks ahead)

| | |
| --- | --- |
| Devices | `WEBCAM_FRONT` (USB, UVC). A `VisionPortal` built in the constructor with two processors: `AprilTagProcessor` and a colour-blob processor for POLLEN, red NECTAR and blue NECTAR. Live preview off unless `CameraConfig.PREVIEW` |
| Verbs | `pause()` / `resume()` (stop or restart streaming to save CPU when not needed), `stop()` closes the portal |
| Questions | `seesTag(id)` (real AprilTag IDs), `getTagPose(id)` (range in, bearing deg, yaw deg, from the SDK), `seesElement(element)`, `getElementBearingDeg(element)`, `getElementSizePx(element)`, `getAgeMs()` |
| `update()` | Copy the processors' newest results once per loop (cheap: vision runs on the SDK's own thread) |
| Config | Resolution, exposure and gain, decimation, colour ranges for each element, camera position and angle on the robot (for the tag pose), `PREVIEW` |
| Log | `front_cam_tag_id`, `front_cam_tag_range_in`, `front_cam_element` (code), `front_cam_element_bearing_deg`, `front_cam_fps` |
| Used by | Collect aim (S2 or auto), opponent NECTAR warning before the intake (G408), Field Check 12 |

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

Every state machine has the same four sections (R3): **1. States** (the `enum`, one comment per value), **2. Commands** (`start(...)`, `cancel()`: they only call `goTo`), **3. `update()`** (one `case` per state, marked DO / CHECK / GO), **4. Bookkeeping** (`goTo(state, reason)`, `isRunning()`, `getState()`, `addTelemetry(t)`, `addLogData()`). Every waiting state has a timeout that goes to `IDLE` with a logged reason. They get their mechanisms in the constructor (dependency injection) and never call each other or Robot.

### 7.1 CollectStateMachine (Intake + Storage)

```
            start()                       held >= MAX_HELD
   IDLE  ------------>  INTAKING  ------------------------->  IDLE
    ^                    |    ^
    |   cancel()/timeout |    | REJECT_MS passed
    +--------------------+    |
                         v    |
                       REJECTING   (opponent NECTAR seen, or a 5th element)
```

| State | DO | CHECK → GO | Timeout |
| --- | --- | --- | --- |
| `IDLE` | `intake.stop()` (once, on entry) | – | – |
| `INTAKING` | `intake.collect()` | held ≥ `MAX_HELD` → `IDLE` "full"; opponent NECTAR or held > `MAX_HELD` → `REJECTING` | `IntakeConfig.COLLECT_TIMEOUT_MS` → `IDLE` "collect timeout" |
| `REJECTING` | `intake.reverse()` | timer ≥ `REJECT_MS` → `INTAKING` | (the timer itself) |

Commands: `start()` (refused with a warning if Storage is full), `cancel()` → `IDLE`. Telemetry: state, seconds in it, held count.

### 7.2 LaunchStateMachine (Storage + Launcher + HiveState + Drive)

```
          start(count)          up to speed            FEED_MS passed
   IDLE ------------> SPINNING_UP ----------> FEEDING ---------------> RECOVERING
    ^                    |   ^                                            |
    |  timeout/cancel    |   +----- more to launch, RECOVER_MS passed ----+
    +--------------------+                                                |
    +--------------------- nothing left, or count reached ----------------+
```

| State | DO | CHECK → GO | Timeout |
| --- | --- | --- | --- |
| `IDLE` | `storage.holdFeeder()`, `launcher.idle()` (once, on entry) | – | – |
| `SPINNING_UP` | `launcher.spinUpFor(storage.getNextElement(), distance to the upward CELL)` | up to speed and no TIP pause → `FEEDING` | `LauncherConfig.SPIN_UP_TIMEOUT_MS` → `IDLE` "spin-up timeout" (warn) |
| `FEEDING` | `storage.feedOne()` | timer ≥ `FEED_MS` → `RECOVERING` | (the timer itself) |
| `RECOVERING` | `storage.holdFeeder()` | timer ≥ `RECOVER_MS`: launched < count and held > 0 → `SPINNING_UP`; else → `IDLE` "done" | (the timer itself) |

The distance comes from `drive.getPose()` and `hive.getUpCellPose(isRed)`. After the operator presses "TIPPED", `hive.isTipPauseActive()` holds the next feed for `LauncherConfig.TIP_PAUSE_MS` (manual 10.5.1).

---

## 8. `game/`

### 8.1 Element (enum)

`NONE, POLLEN, RED_NECTAR, BLUE_NECTAR`. `Storage` returns it; `Launcher` picks a speed table from it. In the log it is a code (0–3).

### 8.2 HiveState

**Job:** remember which of our two CELLS faces up, how many TIPS we have, and when the last TIP was. Its fields are `static` so they survive from Auto to TeleOp, like `MatchInfo`.

| Method | Does |
| --- | --- |
| `resetForNewMatch()` | Auto `init()` only: up CELL = the setup position (`FieldConfig.START_UP_CELL`), TIPS = 0 |
| `markTipped()` | Flip the up CELL, TIPS + 1, start the TIP pause, log `HIVE_TIP:<n>` |
| `getUpCellPose(isRed)` | The upward CELL's centre on the floor plan, for aiming and distance |
| `getUpCellName()` | `"A"` or `"B"` for the header |
| `getTipCount()`, `getNectarAllowed()` | For the header: the human player may enter one NECTAR per TIP (G426) until 60 s left |
| `isTipPauseActive()` | True for `LauncherConfig.TIP_PAUSE_MS` after a TIP |

### 8.3 FieldPaths

**Job:** build every Pedro path, in one file. **Pattern:** Factory method. Pedro's path API (`Paths.line`, `Paths.curve`, `PoseFactory`) appears only here and in `Drive`.

| Method | Path |
| --- | --- |
| `forAlliance(bluePose, isRed)` | RED = BLUE turned 180° about (72, 72): `PoseFactory` `mirrorX(72.0).mirrorY(72.0)` |
| `startPose(start, isRed)` | One of `FieldConfig.START_*` |
| `toLaunchSpot(from, isRed, upCell)` | Straight line to `FieldConfig.LAUNCH_SPOT_A/B` (one per CELL) |
| `toLoadingZone(from, isRed)` | Curve through `LOADING_APPROACH` to `LOADING_PARK` |
| `toGarden(from, isRed)`, `throughGarden(...)` | Auto collection along our GARDEN line |
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
    boolean collect = operator.isAPressed();
    boolean spitOut = operator.isBHeld();
    boolean spitOutReleased = operator.wasBReleased();
    boolean launchAll = operator.isRightTriggerPressed();
    boolean tipped = operator.isLeftBumperPressed();
    boolean spinUp = operator.isYPressed();
    boolean stopAll = operator.isDpadDownPressed();

    if (collect) robot.startCollecting();
    if (spitOut) robot.reverseIntake();
    if (spitOutReleased) robot.stopIntake();
    if (launchAll) robot.startLaunching(StorageConfig.MAX_HELD);
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

INIT menu rows: Alliance, Start, Routine, Delay (0–10 s), Match type, Match number, Battery, Logging. `init()`: build Robot, `HiveState.resetForNewMatch()`, `robot.holdPreloads()` (the only actuator move in any INIT). `init_loop()`: menu, then build the routine's paths for the chosen alliance and start; show a red warning if any path fails `isOnOwnSide` or `avoidsHive`. `start()`: `MatchClock.startAuto()`, `drive.setStartingPose(...)`. `loop()`: park-by guard, `runStep()`, `robot.update()`, `PoseMemory.save(...)`, telemetry. `stop()`: `robot.stopMechanisms()`, save the pose.

| Step | First loop | Done when | Timeout (`AutoConfig`) |
| --- | --- | --- | --- |
| `WAIT_DELAY` | – | delay from the menu passed | – |
| `DRIVE_TO_LAUNCH` | `drive.followPath(startToLaunch)` | `drive.isPathDone()` | `DRIVE_TIMEOUT_MS` → `holdHere()`, go on |
| `LAUNCH_PRELOADS` | `robot.startLaunching(4)` | `!robot.isLaunching()` | `LAUNCH_TIMEOUT_MS` → cancel, go on |
| `COLLECT_GARDEN` | `robot.startCollecting()`, `followPath(throughGarden)` | path done | `COLLECT_TIMEOUT_MS` |
| `DRIVE_TO_LAUNCH_2` | `followPath(gardenToLaunch)` | path done | `DRIVE_TIMEOUT_MS` |
| `LAUNCH_2` | `robot.startLaunching(4)` | `!robot.isLaunching()` | `LAUNCH_TIMEOUT_MS` |
| `PARK` | `robot.stopMechanisms()`, `followPath(toLoadingZone)` | path done | `PARK_TIMEOUT_MS` |
| `DONE` | – | – | – |

Park-by guard: `if (MatchClock.autoSeconds() > AutoConfig.PARK_BY_S && step is before PARK) goToStep(PARK, "park-by")`.

### 9.3 Test OpModes (group "Tests")

| OpMode | Builds | Buttons |
| --- | --- | --- |
| `Test: Drive` | `new Drive(hardwareMap)` | sticks, Y, X, A/B assists (pose set to the field centre at start) |
| `Test: Intake`, `Test: Storage`, `Test: Launcher` | that mechanism only | one button per verb; telemetry shows every question's answer |
| `Test: Camera` | both cameras | shows what each camera sees, the webcam's frame rate and tag poses; X toggles the webcam preview |
| `Test: Collect`, `Test: Launch` | `Robot` | start / cancel, shows state and history |
| `Test: Parts` | nothing but `hardwareMap` | D-pad picks a motor or servo from `HardwareNames.ALL_DEVICES`, sticks move it |

---

## 10. Helpers and configurations

**Helpers** (public methods; `try`/`catch` and `List` allowed here only)

| Helper | Public methods |
| --- | --- |
| `StateTimer` | `reset()`, `seconds()`, `milliseconds()`, `isTimeUp(ms)` |
| `StateLog` | `record(from, to, reason)`, `warn(msg)`, `getLastChange()`, `getWarning()`, `addTo(t, name, state, timer, busy)` |
| `GamepadInput` | sticks with deadzone (`getLeftStickY()` …), `areSticksMoved()`, `isXPressed()` / `isXHeld()` / `wasXReleased()` per button, `rumble(ms)`, `setLedColor(r, g, b)`, `getLastUpdateMs()` |
| `InitMenu` | `addChoiceRow`, `addNumberRow`, `update(up, down, left, right)`, `getIndex(row)`, `getText(row)`, `addTo(t)` |
| `MatchInfo` | static `alliance`, `matchType`, `matchNumber`, `batteryLabel`, `fieldName`; `getMatchId()`, `isRed()`, `getAllianceName()` |
| `PoseMemory` | `save(pose)`, `hasPose()`, `getPose()`, `secondsSinceSaved()`, `clear()` |
| `MatchClock` (new) | `startAuto()`, `startTeleOp()`, `autoSeconds()`, `teleOpSecondsLeft()`, `justReachedSecondsLeft(s)`, `getTimeLeftText()` |
| `DashboardTelemetry` | `create(telemetry)` |
| `FieldDrawing` | `drawRobot(x, y, headingDeg)`, `drawPath(xs, ys)`, `drawPoint(x, y)`, `send()` |
| `MatchLogger` (step 4) | `start(opModeName)`, `value(column, number)`, `event(type, text)`, `endRow()`, `stop()` |
| `BatteryMonitor`, `LoopMonitor`, `HealthMonitor` (step 4) | see Requirements, Troubleshooting C and D |
| `FieldCheckHelper`, `LogSummary` | steps 5 and 6 |

**Configurations** (`@Config`, `public static`, not `final`, except `HardwareNames`)

| Class | Main values (starting values, TODO(BioBuzz) to tune) |
| --- | --- |
| `HardwareNames` | device names with hub and port; `ALL_DEVICES` array; motor and servo count (≤ 8 each) |
| `DriveConfig` | `STICK_DEADZONE`, `BLUE_DRIVER_HEADING_DEG`, `RED_DRIVER_HEADING_DEG`, `ASSIST_TIMEOUT_MS`, `STILL_IN_PER_S` |
| `FieldConfig` | `START_*` poses, `LAUNCH_SPOT_A/B`, `CELL_A/B`, `START_UP_CELL`, `LOADING_APPROACH`, `LOADING_PARK`, `LOADING_CORNER`, `GARDEN_*`, `HIVE_KEEP_OUT_*` (all BLUE, inches/degrees) |
| `AutoConfig` | step timeouts, `PARK_BY_S` (start at 26), default routine and delay |
| `IntakeConfig` | `COLLECT_POWER`, `REVERSE_POWER`, `COLLECT_TIMEOUT_MS`, `REJECT_MS`, stall values |
| `StorageConfig` | `MAX_HELD = 4`, colour thresholds, `FEED_POWER`, `SENSE_MS` |
| `LauncherConfig` | PIDF, `IDLE_RPM`, `SPEED_TOLERANCE_RPM`, speed tables, `SPIN_UP_TIMEOUT_MS`, `FEED_MS`, `RECOVER_MS`, `TIP_PAUSE_MS` |
| `CameraConfig` | `HIVE_SAMPLE_MS`, HuskyLens learned tag IDs; webcam resolution, exposure, decimation, colour ranges, position and angle, `PREVIEW` |
| `ControlsConfig` | `TRIGGER_THRESHOLD`, rumble lengths, the button list as comments |

---

## 11. How to extend: checklists

**Add a mechanism** (example: a FLOWER arm)

1. Copy `template/TemplateMechanism.java` to `mechanisms/FlowerArm.java`; rename; `extends Mechanism`; fill in the hooks you need.
2. Add its devices to `HardwareNames` (with hub and port, and in `ALL_DEVICES`); check the motor and servo count.
3. Add `configurations/FlowerArmConfig.java`.
4. In `Robot`: one field, build it in the constructor, add it to the `mechanisms` array. (Update, stop-all, telemetry and log now include it.)
5. Add Robot methods for its actions (cancel any state machine that uses it).
6. Copy `template/TemplateMechanismTest.java` to `testing/FlowerArmTest.java`.
7. Add gamepad 2 buttons in `handleOperator()`, and update the three cheat sheets.
8. Add a `TelemetryConfig` switch if its lines are long.
9. Update `HLD.md` (robot table), `LLD.md` (section 5) and `README.md`.

**Add a state machine**

1. Copy `template/TemplateStateMachine.java` to `statemachines/<Task>StateMachine.java`; list its states; give every waiting state a timeout in a `*Config`.
2. In `Robot`: one field, build it with the mechanisms it uses; one line each in `update()` (section 1), `stopMechanisms()` and telemetry.
3. Add a Robot start method; it cancels every state machine that shares a mechanism with this one.
4. Add a test OpMode (start / cancel) and the buttons or auto steps that start it.
5. Update `HLD.md`, `LLD.md` (section 7, with the state table) and `README.md`.

**Add an assist (S2):** a pose in `FieldConfig` → a path method in `FieldPaths` → a Robot method that checks `drive.isPoseKnown()` → one line in `handleDriver()` → cheat sheets.

**Add an auto step:** a `Step` value → a `case` (first loop, done, timeout) → a timeout in `AutoConfig` → the step table in this document.

---

## 12. Telemetry, match log and ALERTs (game additions)

**Header line:** `BLUE Q12 | 1:42 left | 18 ms | 12.6 V | FIELD 3° | held 3 P P B | up CELL A | tips 2 | ALERT ...`

**New DATA columns:** `held_count`, `next_element`, `launcher_rpm`, `launcher_target_rpm`, `up_cell`, `tip_count`, `collect_state`, `launch_state`, `<mechanism>_faulted`, `pose_known`, `match_s_left`.

**New EVENTS:** `HIVE_TIP:<n>`, `REJECT_OPPONENT_NECTAR`, `OVER_LIMIT:<count>`, `FAULT:<mechanism>:<reason>`, `ASSIST:<name>`, `ASSIST_REFUSED:pose unknown`, `ASSIST_CANCELLED:stick`, `POSE_SET:<how>:<old>-><new>`, `AUTO_PATH_OFFSIDE:<path>`, `PARK_BY`.

**New ALERTs:** mechanism FAULTED (urgent), over 4 elements (urgent), opponent NECTAR rejected, auto path off our side (INIT, red), launcher can't reach speed (spin-up timeout).

---

## 13. Known implementation challenges

| Challenge | Options | Plan |
| --- | --- | --- |
| **Pinpoint access.** Pedro's `PinpointLocalizer` keeps its driver private; safeguards need status, rate and `recalibrateIMU()` | A second handle from `hardwareMap` in `Drive`; reflection (forbidden); a team localizer class | Second handle, read-only except a recalibrate when the robot is still; record the risks (two objects on one device, reset during a path) |
| **Dashboard field background** | `TelemetryPacket(false)` + `drawImage("/images/field-biobuzz.png", 0, 0, 144, 144)`; flicker between packets | Already in `FieldDrawing`; the image is at `TeamCode/src/main/assets/images/field-biobuzz.png`; check flicker on the robot |
| **Constants depends on team code** | `pedro/Constants.java` imports `HardwareNames` | Keep; record in `UPSTREAM.md` |
| **HuskyLens is 2D** | Box size → rough distance; tag learned IDs must be trained on each unit; one algorithm at a time; I2C read takes a few ms | Read every 100 ms; use for "which CELL", aim offsets and element colour only; document training in `CAMERA_GUIDE.md` |
| **Webcam CPU load** | `VisionPortal` runs on its own thread, but AprilTag and colour processing at full resolution can slow the whole Control Hub, and so the loop | Low resolution (for example 640 × 480), AprilTag decimation, preview off in matches, `pause()` when not needed; measure the loop time with the webcam streaming |
| **Counting held elements** | One colour sensor at the mouth (count in/out); a sensor per slot; flywheel speed dip to count shots | Wait for the Storage design (Q5); the count must never be above the real number by more than one |
| **Which CELL is up** | Operator button; camera; counting launched elements | Operator button first (Q2) |
| **Detecting a disconnected device** | The SDK seldom throws; a motor on an unplugged hub keeps "working" | Watch `LynxModule` health and motor current; mark FAULTED; acceptance test: pull a cable |
| **Arrays in `@Config`** (launcher speed tables) | FTC Dashboard may not edit arrays live | If not, use named fields (`POLLEN_RPM_36_IN`, …) |
| **Pedro 3 path headings** | Paths need a heading plan (tangent, constant, linear) | Check `paths/interpolator` in Pedro 3.0.1 when writing `FieldPaths` |
| **Static state between OpModes** (`MatchInfo`, `PoseMemory`, `HiveState`) | Static fields survive OpMode changes but not an app restart | Auto `init()` resets them; TeleOp only reads them; telemetry shows where each value came from |

---

## 14. Future improvements

| Improvement | Gains | Costs | When |
| --- | --- | --- | --- |
| **Bulk reads** (`LynxModule` caching, AUTO or MANUAL) | 1–2 ms per sensor read saved | MANUAL: a missed clear freezes sensor values; needs a stored hub list in a helper | When `LoopMonitor` shows loops near the slow-loop limit; record loop time before and after |
| Camera pose fix (front webcam tag pose) | x/y recovery without the LOADING ZONE corner | Camera position calibration; the CELL tags face down, so they are seen at a steep angle; wrong fixes are worse than none | After Field Check 12 passes reliably |
| Launch while moving | Faster cycles | Needs velocity-aware aiming | After consistent stationary launching |
| FLOWER placer | Bottom NECTAR Bonus, FLOWER ownership | A new mechanism (checklist in section 11) | If the team builds one |
| Limelight 3A | Real 3D AprilTag pose | Cost, mounting | If HuskyLens is not good enough |
