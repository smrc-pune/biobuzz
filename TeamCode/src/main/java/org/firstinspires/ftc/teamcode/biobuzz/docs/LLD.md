# BioBuzz Software – Low-Level Design (LLD)

> **For mentors.** Students: you do not need this file to get started. Begin with the
> student guide, `biobuzz/README.md` ("Start here").

Oct 26, 2026 · **Matches the code after steps 1–13** (not yet run on the robot) · goes with Requirements v2.14 and `HLD.md`

This document describes each class: its job, its public methods, its config values and how it fits with the others. Code sketches show the **shape** of each class; the code itself is the final word, and this document is updated in the same commit as any change.

**Who this is for.** The code is owned during the season by rookie 7th graders with no Java background. Student-zone files (configurations, subsystems, mechanisms, state machines, TeleOp, Auto, `FieldPaths`, `DriveToSpot`, Robot's marked spot, tests) MUST read like the Intake sketch in section 5.1: short methods, plain words, one idea per line. Mentor-zone files (`Subsystem`, `Mechanism`, `StateMachine`, `Drive`, helpers, Field Checks) may be a little more technical, but still follow R3.

`TODO(BioBuzz)` marks what waits for the robot's hardware or a team decision (see `DESIGN-REVIEW.md`, section 5).

## Contents

1. Conventions
2. Packages and files
3. Robot and the subsystems
4. The `Mechanism` base class
5. Mechanisms: Intake, Feeder, Launcher, HiveCamera
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
- **No launch zones:** the robot may launch from anywhere, but the launcher has ONE speed (`LauncherConfig.LAUNCH_RPM`), tuned from `FieldConfig.LAUNCH_SPOT`, the spot the S2 assist and Auto drive to. So launches land best from there.
- **Lists, not index numbers:** arrays are written by name and walked with a "for each" loop; no `[i]` in student-zone code.
- **Telemetry words:** state names in CAPITALS; every value has its unit.

---

## 2. Packages and files

```
teamcode/biobuzz/
  Robot.java                     composition root: Drive, DriveToSpot, the subsystems
  configurations/
    HardwareNames.java           every device name, hub and port; ALL_DEVICES array; motor/servo count
    DriveConfig.java             stick deadzone, driver headings, assist timeout
    ControlsConfig.java          trigger threshold, rumble lengths, button list (comments)
    FieldConfig.java             every field pose (RED), LAUNCH_SPOT, HIVE keep-out, start positions
    AutoConfig.java              enum Start; START, START_DELAY_S; step timeouts, PARK_BY_S
    IntakeConfig.java  FeederConfig.java  LauncherConfig.java  CameraConfig.java
    TelemetryConfig.java  LogConfig.java  HealthConfig.java  FieldCheckConfig.java
    TemplateConfig.java          numbers for template/ (copied with it)
  drive/
    Drive.java                   MENTORS: driving over Pedro (replaces parts/DriveBase + subsystems/Drive)
    FieldPaths.java              STUDENTS: every Pedro path; alliance turn; own-side and HIVE checks
    DriveToSpot.java                 STUDENTS: the S2 buttons (pose and HIVE checks, then a path)
  subsystems/
    Subsystem.java               MENTORS: the base class (Composition + Template Method)
    IntakeSubsystem.java  ShooterSubsystem.java  VisionSubsystem.java              STUDENTS
  mechanisms/
    Mechanism.java               MENTORS: the one base class (Template Method)
    Intake.java  Feeder.java  Launcher.java                                          STUDENTS
    HiveCamera.java                       MENTORS (SDK camera details; their numbers are in CameraConfig)
  statemachines/                 STUDENTS
    StateMachine.java            MENTORS: the base class every state machine extends
    LaunchStateMachine.java      Feeder + Launcher + Drive (where we are); the HIVE from MatchInfo
  teleop/BioBuzzTeleOp.java
  auto/BioBuzzAuto.java
  testing/   DriveTest, IntakeTest, FeederTest, LauncherTest, CameraTest, LaunchTest,
             PartsTest, RobotCheck, LogViewer
  fieldchecks/  FieldCheck01_Hardware ... FieldCheck13_Summary
  helpers/   StateTimer, StateLog, GamepadInput, AllianceColor, MatchInfo, PoseMemory, MatchClock,
             DashboardTelemetry, FieldDrawing, MatchLogger, BatteryMonitor, HealthMonitor,
             Alerts, FieldCheckHelper, LogSummary
  template/  TemplateSubsystem.java, TemplateMechanism.java, TemplateStateMachine.java,
             TemplateMechanismTest.java (@Disabled)
  docs/
```

**Removed from today's code:** `parts/` and the old `subsystems/` (one state-machine "brain" per mechanism; merged into `drive/` and `mechanisms/` in v2.0; today's `subsystems/` are menus without states, v2.8), `Robot.buildDrive`, `Drive`'s states, `Drive.setDriverInput` (now `driveWithSticks`), the stick deadzone in Drive (now in `GamepadInput`). There is no `game/` folder: paths live with Drive, and the HIVE with the other match facts in `MatchInfo`. There is no Storage mechanism and no `CollectStateMachine` yet: the robot has no sensors to count balls (see Future improvements).

---

## 3. Robot and the subsystems

**Job of Robot:** build Drive, DriveToSpot and the subsystems; run `update()` in a fixed order; health checks, match log, telemetry header. **Patterns:** Composition root.
**Job of a subsystem:** one job of the robot (collect, shoot, see). It OWNS its mechanisms and the state machines that use them, updates, stops, shows and logs them, and its public methods are the menu OpModes call. **Patterns:** Composition, Facade, Template Method (`Subsystem`), Mediator (inside one subsystem).

**Who calls what**

- OpModes call `robot.drive.*` for driving and `robot.driveToSpot.*` for the S2 buttons (Drive is never shared with a state machine).
- OpModes call **only subsystem menu methods** for mechanisms (`robot.shooter.launch(4)`), never a mechanism. A mechanism belongs to exactly ONE subsystem, and every state machine that uses it lives in that same subsystem, so the subsystem can cancel its state machine before a manual command (Mediator).
- The game facts are static helpers: OpModes call `MatchInfo.markHiveTipped()` directly (as they call `MatchInfo.switchAlliance()`).

```java
public class Robot {
    public final Drive drive;                    // OpModes drive through this directly
    public final DriveToSpot driveToSpot;                // the S2 buttons
    public final IntakeSubsystem intake;         // the menus
    public final ShooterSubsystem shooter;
    public final VisionSubsystem vision;
    public final BatteryMonitor battery;
    public final MatchClock clock = new MatchClock();
    private final HealthMonitor health;
    private final Subsystem[] subsystems;        // every subsystem, in update order

    public Robot(HardwareMap hardwareMap) {
        drive = new Drive(hardwareMap);
        driveToSpot = new DriveToSpot(drive);
        battery = new BatteryMonitor(hardwareMap);
        health = new HealthMonitor(drive, battery);
        intake = new IntakeSubsystem(hardwareMap);
        shooter = new ShooterSubsystem(hardwareMap, drive);          // given what it uses
        vision = new VisionSubsystem(hardwareMap);
        subsystems = new Subsystem[] { intake, shooter, vision };    // ADD A SUBSYSTEM HERE
    }
```

**Robot's public methods**

| Method | Does |
| --- | --- |
| `startMatch(opModeName, lengthS)`, `startMatch(opModeName, lengthS, logHeader)` | PLAY: clears ALERTs, starts `clock`, the health checks and the match log (Auto adds its plan as a header line) |
| `endMatch()` | STOP: `stopMechanisms()`, one last `update()`, then the log's END line |
| `stopMechanisms()` | `stop()` on every subsystem (each cancels its state machines, then stops its mechanisms). Not the drive |
| `hasNewUrgentAlert()` | True once per new urgent ALERT (`Alerts.takeUrgent()`); the OpMode rumbles |
| `getFaultedMechanisms()` | Names of every FAULTED mechanism, or "" (Field Check 11) |
| `clock` (field) | The `MatchClock`: `getTimeLeftText()`, `justReachedSecondsLeft(s)`, `secondsSinceStart()` |
| `update()` | See below; also writes the match-log DATA row |
| `addHeader(t)`, `addTelemetry(t)` | Header (two lines and the newest ALERT; the second line joins each subsystem's `getStatusText()`), then Drive and each subsystem, honoring `TelemetryConfig`; draws the robot, path and upward CELL; sends the Dashboard graph values (`launcher_rpm`, `launcher_target_rpm`, `loop_ms`, `battery_v`) |

**`update()` order** (once per loop, and only once)

```java
public void update() {
    clock.update();
    // 1. SUBSYSTEMS: each runs its state machines (they decide, give commands), then its mechanisms
    for (Subsystem subsystem : subsystems) {
        subsystem.update();
    }
    // 2. DRIVE: Pedro reads the Pinpoint and sets the drive motors
    drive.update();
    // 3. HEALTH: loop time, battery, Pinpoint, heading jump -> ALERTs, robot-centric
    health.update();
    // 4. MATCH LOG: one DATA row, LogConfig.RATE_HZ times a second
    if (MatchLogger.isRowDue()) {
        writeLogRow();   // match_s_left, health, drive, each subsystem, up_cell_a, tip_count
    }
}
```

Why this order: inside each subsystem, state machines command first, so mechanisms act on this loop's commands; Drive comes after the mechanisms, so the pose is the newest when the health rules run.

### 3.1 `Subsystem` (mentor zone, `subsystems/Subsystem.java`)

```java
public class Subsystem {
    protected Subsystem(String name)
    protected final void addMechanism(Mechanism mechanism)            // constructor only
    protected final void addStateMachine(StateMachine stateMachine)   // constructor only, after its mechanisms
    public String getStatusText()              // hook: a few words for the header, "" = nothing
    public final void update()                 // every state machine's update(), then every mechanism's runUpdate()
    public final void stop()                   // every state machine's cancel(), then every mechanism's runStop()
    public final void addTelemetry(Telemetry t)    // state machines, then mechanisms (each checks its own switch)
    public final void addLogData()             // mechanisms, then state machines
    public final String getFaultedNames()
}
```

Log columns are read by name (`LogSummary`, `plot_match.py`), so their order may change when a part moves between subsystems.

### 3.2 `StateMachine` (mentor zone, `statemachines/StateMachine.java`)

A plain base class, like `Mechanism`, with the five methods every state machine already had: `update()`, `cancel()`, `isBusy()`, `addTelemetry(t)`, `addLogData()`. They do nothing (`isBusy()` is false); each state machine `extends StateMachine` and overrides all five with `@Override`, so `Subsystem` can run any of them. The build does NOT catch a missing override, so the template has all five and the checklist says so (an interface would catch it, but R3 keeps interfaces out: they are a second idea for students to learn). The commands that start a job (`start(count)`) are each state machine's own. A state machine's `addTelemetry()` checks its own `TelemetryConfig` switch (`LaunchStateMachine`: `TelemetryConfig.LAUNCH`).

### 3.3 The subsystems (student zone)

| Subsystem (`robot.`…) | Owns | Menu | Mediator rule | `getStatusText()` |
| --- | --- | --- | --- | --- |
| `intake` (`IntakeSubsystem`) | Intake | `toggle()`, `collect()`, `reverse()`, `stop()`, `isOn()` | – (no state machine) | `intake ON` / `intake off` |
| `shooter` (`ShooterSubsystem`) | Feeder, Launcher, LaunchStateMachine (+ Drive, only to ask where we are, for the aim) | `launch(count)`, `cancelLaunch()`, `spinUp()`, `holdPreloads()`, `stop()`; questions `isLaunching()`, `isUpToSpeed()`, `getRpm()`, `getTargetRpm()`, `getAimHeadingDeg()` | `spinUp()` does nothing while launching (the state machine controls the speed) | `launcher READY` / `launcher spinning` / `launcher -` |
| `vision` (`VisionSubsystem`) | HiveCamera | none yet: questions are added when an OpMode needs one | – | – |

Menu details: `launch(count)` = `launchStateMachine.start(count)` (feeding an empty slot does no harm); `cancelLaunch()` = `launchStateMachine.cancel()` (Auto's launch timeout, Test: Launch): balls held, flywheel at idle; `stop()` (from `Subsystem`) also stops the flywheel. `holdPreloads()`: Auto INIT only, grip the 4 POLLEN (G304). `getAimHeadingDeg()`: the heading that faces the upward CELL, plus `LauncherConfig.AIM_OFFSET_DEG`.

**A state machine that needs two subsystems' mechanisms** means those jobs belong together: merge the subsystems (mentor decision), never let a state machine reach into another subsystem.

### 3.4 `DriveToSpot` (student zone, `drive/DriveToSpot.java`)

| Method | Does |
| --- | --- |
| `launchSpot()` / `loadingZone()` | S2 assists: refused (false, with a Drive warning) if `!drive.isPoseKnown()` or if the path fails `FieldPaths.avoidsHive`; else `drive.followPath(FieldPaths.toLaunchSpot(drive.getPose(), MatchInfo.isRed()))` and EVENT `ASSIST:<name>` |

"Set pose here" (Q4) is not a drive-to-spot, so it lives in `Drive`: `robot.drive.setPoseAtResetCorner()` = `setPose(FieldPaths.resetCorner(isRed), "set pose here")`.

**Why lists and not a key-value map.** A `Map<String, Subsystem>` would let code look one up by a word, but it adds string keys that can be mistyped (and only fail while the robot runs) and a harder loop. Nothing needs to look a part up: each has its own field name (`shooter`), and the lists are only for "do this to every part". They are written by name and walked with "for each", so there are no index numbers to remember.

---

## 4. The `Mechanism` base class

**Zone:** mentors. Students never edit this file; they only fill in the four hooks in their own mechanism. **Job:** the fixed frame every mechanism shares: name, FAULTED flag, safe update and stop, telemetry and log wrappers. **Pattern:** Template Method. It is the only team base class (R3), it is not abstract, and nothing extends a mechanism further.

```java
public class Mechanism {
    private final String name;
    private boolean faulted = false;
    private String faultReason = "";

    public Mechanism(String name) {
        this.name = name;
    }

    // ===== HOOKS: your mechanism overrides these (all optional) =====
    /** Every loop: control loops and sensor reads. Never wait or sleep here. */
    protected void update() {
        // empty here: filled in with @Override
    }
    /** Put the devices in their safe state: motors off, servos where they hold nothing. */
    protected void stop() {
        // empty here: filled in with @Override
    }
    /** Your own telemetry lines (powers, speeds, sensor values). */
    protected void addDetails(Telemetry t) {
        // empty here: filled in with @Override
    }
    /** Match-log columns, the same every row: MatchLogger.value("intake_power", power);
     *  Log values kept in fields; do NOT read hardware here (this also runs while FAULTED). */
    protected void addLogValues() {
        // empty here: filled in with @Override
    }
    /** Your TelemetryConfig switch (return TelemetryConfig.INTAKE;). A FAULTED line always shows. */
    protected boolean isTelemetryOn() {
        return true;
    }

    // ===== FIXED: Robot and test OpModes call these. Do not override. =====
    public final void runUpdate() {
        if (faulted) {
            return;
        }
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
        if (faulted) {
            t.addData(name, "FAULTED: " + faultReason);
        } else if (isTelemetryOn()) {
            addDetails(t);
        }
    }

    public final void addLogData() {
        MatchLogger.value(name + "_faulted", faulted);
        addLogValues();
    }

    public final boolean isFaulted() {
        return faulted;
    }
    public final String getName() {
        return name;
    }

    /** A mechanism may call this itself, for example when it sees a problem. */
    protected final void markFaulted(String reason) {
        if (faulted) {
            return;
        }
        faulted = true;
        faultReason = reason;
        runStop();
        Alerts.raise(name + " FAULTED: " + reason, true);   // urgent; also an EVENT row
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

TODO(BioBuzz): device names, ports and numbers are first guesses until the robot is built. Every mechanism logs `<name>_faulted` plus the values below, and overrides `isTelemetryOn()` with its switch (`INTAKE`, `FEEDER`, `LAUNCHER`, `CAMERAS`).

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
    public void collect() {
        roller.setPower(IntakeConfig.COLLECT_POWER);
    }
    public void reverse() {
        roller.setPower(IntakeConfig.REVERSE_POWER);
    }
    public boolean isOn() {
        return roller.getPower() != 0;
    }

    // ---- Filled in for Mechanism ----
    @Override
    protected void stop() {
        roller.setPower(0);
    }

    @Override
    protected void addDetails(Telemetry t) {
        t.addData("Intake power", roller.getPower());
    }
}
```

| | |
| --- | --- |
| Devices | `MOTOR_INTAKE` (roller) |
| Verbs | `collect()` (power `IntakeConfig.COLLECT_POWER`), `reverse()` (`REVERSE_POWER`); off = `runStop()` |
| Questions | `isCollecting()`, `isReversing()`, `getPower()` |
| `update()` | Nothing yet (stall detection is a future improvement, section 14) |
| Log | `intake_power`; EVENT `INTAKE:power <p>` / `INTAKE:off` on each change. (Motor current is not logged: reading it is a hub command every loop; it comes with stall detection) |

### 5.2 Feeder

| | |
| --- | --- |
| Devices | `SERVO_FEEDER` (TODO(BioBuzz): a servo is the first guess; if it becomes a motor, keep the verbs) |
| Verbs | `feedOne()` (push the next ball from the hopper into the launcher), `hold()` (keep the next ball back); `stop()` = hold |
| Questions | `isFeeding()` |
| `update()` | Nothing (the timing of a feed is the state machine's job). A position is sent only when it changes |
| Config | `FeederConfig.FEED_POSITION`, `HOLD_POSITION` |
| Log | `feeder_feeding` |

There is no Storage mechanism: the hopper has no sensors, so the robot does not know how many balls it holds or their colours. Storage sensing is a future improvement (section 14).

### 5.3 Launcher

| | |
| --- | --- |
| Devices | `MOTOR_LAUNCHER` (flywheel with encoder; TODO(BioBuzz): a second motor?) |
| Verbs | `spinUp()` (`LAUNCH_RPM`), `idle()` (`IDLE_RPM`), `stop()` |
| Questions | `isUpToSpeed()` (within `SPEED_TOLERANCE_RPM`), `getRpm()`, `getTargetRpm()` |
| `update()` | Reads the speed once per loop (`getVelocity()`, ticks/s → RPM with `TICKS_PER_REV`). Velocity control is in the motor (`RUN_USING_ENCODER`, `setVelocity`, PIDF from `LauncherConfig.SPEED_P/I/D/F`); `update()` re-sends the PIDF only when a Dashboard edit changed it. Zero power = FLOAT (coast) |
| Launch speed | `LauncherConfig.LAUNCH_RPM` (3100, a guess): ONE speed, tuned from `FieldConfig.LAUNCH_SPOT`, for both POLLEN and NECTAR (the robot does not know which ball is next). The launch spot is about the same distance from both CELLS (31 in to A, 33 in to B, from the guessed poses), so one speed serves before and after a TIP. A speed that follows the distance is a future improvement (section 14) |
| Log | `launcher_rpm`, `launcher_target_rpm` (also on the Dashboard graph) |

### 5.4 HiveCamera (HuskyLens, looks up)

| | |
| --- | --- |
| Devices | `HUSKYLENS_HIVE` (I2C), `selectAlgorithm(TAG_RECOGNITION)` in the constructor (not an actuator) |
| Verbs | none |
| Questions | `seesTag(learnedId)`, `getTagOffsetPx(learnedId)` (pixels from the image centre, + = right), `getTagWidthPx(learnedId)`, `getTagCount()`, `getFirstTagId()`, `getAgeMs()` |
| `update()` | Every `CameraConfig.HIVE_SAMPLE_MS` (default 100), read `blocks()` and keep them with their time |
| Config | `HIVE_SAMPLE_MS`, `HIVE_IMAGE_WIDTH_PX` (320), the HuskyLens's learned ID for each CELL's tags (`RED_CELL_A_TAG_ID`, …); the manual's AprilTag numbers as `final` values (`RED_CELL_A_FIRST_APRILTAG` = 34, `RED_CELL_B_…` = 30, `BLUE_CELL_A_…` = 42, `BLUE_CELL_B_…` = 38; the four tags of each CELL are listed in the comment) |
| Log | `hive_cam_tag_id`, `hive_cam_offset_px`, `hive_cam_age_ms` |
| Used by | Test: Camera and Field Check 12 today. Planned: confirming `MatchInfo`'s upward CELL (Q2) and a finer aim offset. It gives no distance, so no pose fix |

---

## 6. Drive

**Job:** driving operations over Pedro's follower. **Patterns:** Facade (Pedro's follower), Adapter (stick values → `DrivePowers`). Replaces `parts/DriveBase` and `subsystems/Drive`. It is the only team class that calls `Constants.create(hardwareMap)`. It is not a Mechanism: stop-all never stops the drivetrain.

| Group | Methods |
| --- | --- |
| Setup | `Drive(hardwareMap)`, `setAlliance(isRed)`, `setStartingPose(pose)` (pose known), `setStartingPoseFacingAway()` (heading only, pose **not** known) |
| Driver driving (S1) | `driveWithSticks(forward, right, clockwise)`: field-centric unless robot-centric is on; `driveWithSticksAimedAt(forward, right, targetHeadingDeg)`: field-centric powers, then Pedro's `ManualDrive.headingLock(follower, PIDController, powers, target)` replaces the turn (gains `DriveConfig.AIM_P`, `AIM_D`; in robot-centric mode it drives without turning); `resetHeading()`; `switchDriveMode()`; `useRobotCentric(reason)`, `useFieldCentric(reason)`; `isFieldCentric()` |
| Path driving (S2, S3) | `followPath(path)`; `isDrivingItself()` (Pedro FOLLOW or HOLD); `isPathDone()`; `holdHere()`; `stopDrivingItself()` (back to manual, zero power) |
| Pose | `getPose()`, `getX()`, `getY()`, `getHeadingDegrees()` (−180 to 180), `setPose(pose, reason)` (logged), `setPoseAtResetCorner()` ("set pose here", D-pad left), `isPoseKnown()`, `isStill()` (speed under `DriveConfig.STILL_IN_PER_S`) |
| Health | `getPinpointStatus()` (the same Pinpoint object Pedro reads; status only), `getHeadingSetCount()` (+1 whenever our code sets the heading, so it is not taken for a jump), `forgetPose(reason)` |
| Loop | `update()` (Pedro's `follower.update()`, every loop), `addTelemetry(t)`, `addLogData()` (pose, heading speed, Pinpoint status, Pedro mode, drive mode, pose known, sticks), `addDrawing()` |
| Field Checks only | `updatePoseOnly()` (the localizer only: an idle follower brakes the motors in every `update()`, which fights a hand push), `zeroPose()`, `driveDirectly(forward, left)` and `coast()` (Field Check 07: `follower.drivetrain.drive(...)` / `stop(false)`), `getForwardSpeedInPerS()`, `getLeftSpeedInPerS()`, `getSpeedInPerS()` |

**Rules**

- Field-centric: `ManualDrive.fieldCentric(powers, heading, −driverHeading)`; robot-centric skips the rotation. Every switch is a `CHANGE:Drive` EVENT (from `StateLog`). Moving a stick while a path runs logs `ASSIST_CANCELLED:stick`.
- `isPoseKnown()` is true after `setStartingPose` (auto, or the pose from `PoseMemory`) or `setPose` (set-pose-here, camera); false after `setStartingPoseFacingAway()` and after a Pinpoint fault. Assist buttons check it.
- Speed and acceleration limits for paths come from Pedro's `ForesightConfig` in `pedro/Constants.java`. Sticks drive at full power.
- Pinpoint status: see Known implementation challenges.

---

## 7. State machines

Every state machine has the same four sections (R3): **1. States** (the `enum`, one comment per value), **2. Commands** (`start(...)`, `cancel()`: they only call `goTo`), **3. `update()`** (one `case` per state, marked DO / CHECK / GO), **4. Bookkeeping** (`goTo(state, reason)`, `isBusy()`, `getState()`, `addTelemetry(t)`, `addLogData()`). Every waiting state has a timeout that goes to `IDLE` with a logged reason. They `implement StateMachine` (section 3.2), get their mechanisms in the constructor (dependency injection), live in the subsystem that owns those mechanisms, and never call each other, a subsystem or Robot.

### 7.1 LaunchStateMachine (Feeder + Launcher + Drive; in ShooterSubsystem)

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
| `SPINNING_UP` | `launcher.spinUp()` (every loop, so a Dashboard edit of `LAUNCH_RPM` counts at once) | up to speed and no TIP pause → `FEEDING` | `LauncherConfig.SPIN_UP_TIMEOUT_MS` → `IDLE` "spin-up timeout" (warn, and an urgent ALERT `LAUNCHER TIMEOUT`: the gamepads rumble) |
| `FEEDING` | `feeder.feedOne()`, keep the speed | timer ≥ `FEED_MS` → `RECOVERING` | (the timer itself) |
| `RECOVERING` | `feeder.hold()`, keep the speed | timer ≥ `RECOVER_MS`: launched + 1 (EVENT `LAUNCH:<n> of <count>`); launched < count → `SPINNING_UP`; else → `IDLE` "all launched" | (the timer itself) |

`start(count)` is refused with a warning when the feeder or launcher is FAULTED, or while already launching.

The robot cannot see how many balls it holds, so `start(count)` feeds `count` times (4 for "launch everything"); feeding an empty slot does no harm.

The flywheel always spins at `LAUNCH_RPM`, so the state machine does not need to know where the robot is (it does not use Drive). After the operator presses "TIPPED", `MatchInfo.secondsSinceTip()` holds the next feed for `LauncherConfig.TIP_PAUSE_MS` (manual 10.5.1).

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
| `place(xIn, yIn, headingDeg, isRed)` | RED: the pose as written. BLUE: turned 180° about (72, 72), `PoseFactory.degrees().mirrorX(72).mirrorY(72)`. (The Requirements call this `forAlliance`.) `launchSpot`, `loadingPark`, `loadingApproach`, `resetCorner` use it |
| `startPose(start, isRed)` | `FieldConfig.START_NEAR_GARDEN_*` or `START_MIDDLE_*`, from `AutoConfig.Start` |
| `upCellPose(isRed)` | `FieldConfig.CELL_A_*` or `CELL_B_*`, from `MatchInfo.isCellAUp()` |
| `aimHeadingDeg(robot, isRed)` | For the aim button |
| `toLaunchSpot(from, isRed)` | Straight line to `FieldConfig.LAUNCH_SPOT_*` |
| `toLoadingZone(from, isRed)` | Curve through `LOADING_APPROACH` to `LOADING_PARK` |
| `leaveWall(start, isRed)` | Line from the start to the launch spot, ending facing the upward CELL (LEAVE) |
| `gardenToLaunchSpot(from, isRed)` | Back to the launch spot, facing the upward CELL (same as `leaveWall`) |
| `straightAhead(from, in)`, `square(from, sideIn)` | Field Checks 08 and 09, from wherever the robot is, constant heading |
| `isOnOwnSide(path, isRed)` | Samples 20 points: the centre stays `OWN_SIDE_MARGIN_IN` inside our half (G402) |
| `avoidsHive(path)` | Samples 20 points: none inside `FieldConfig.HIVE_KEEP_OUT_*` (G409, G417) |

Every path names its heading plan (`.linear(...)` or `.constant(...)`), or Pedro 3 stops with an error.

---

## 9. OpModes

### 9.1 BioBuzzTeleOp

```java
public void init()       { build Robot, GamepadInputs }
                           // NO actuator commands here (G403)
public void init_loop()  { D-pad left/right switches the alliance; show battery and pose-from-auto or "S1: no auto pose" }
public void start()      { robot.startMatch("BioBuzzTeleOp", MatchClock.TELEOP_S); drive.setAlliance(MatchInfo.isRed());
                           pose from PoseMemory -> drive.setStartingPose (pose known)
                           else drive.setStartingPoseFacingAway() (S1) }
public void loop() {
    handleDriver();
    handleOperator();
    robot.update();
    rumbleForEvents();
    showTelemetry();
}
public void stop() {
    robot.endMatch();
}

private void handleDriver() {
    boolean faceAway = driver.isYPressed();
    boolean switchMode = driver.isXPressed();
    boolean goToLaunchSpot = driver.isAPressed();
    boolean goToLoadingZone = driver.isBPressed();
    boolean aim = driver.isRightBumperHeld();
    boolean sticksMoved = driver.areSticksMoved();      // deadzone is inside GamepadInput

    // S2 assists: DriveToSpot refuses them (false, with a warning) while the pose is not known
    if (goToLaunchSpot && !robot.driveToSpot.launchSpot()) {
        driver.rumble(RUMBLE_REFUSED_MS);
    }
    if (goToLoadingZone && !robot.driveToSpot.loadingZone()) {
        driver.rumble(RUMBLE_REFUSED_MS);
    }

    // Manual driving. Moving a stick ALWAYS takes the robot back, even in the middle of a path.
    if (sticksMoved || !robot.drive.isDrivingItself()) {
        if (aim) {
            robot.drive.driveWithSticksAimedAt(driver.getLeftStickY(), driver.getLeftStickX(), robot.shooter.getAimHeadingDeg());
        } else {
            robot.drive.driveWithSticks(driver.getLeftStickY(), driver.getLeftStickX(), driver.getRightStickX());
        }
    }
    if (faceAway) {
        robot.drive.resetHeading();
    }
    if (switchMode) {
        robot.drive.switchDriveMode();
    }
    // D-pad left held 1 s: robot.drive.setPoseAtResetCorner();  Back: MatchLogger.event("MARK", "driver")
}

private void handleOperator() {
    boolean intakeOnOff = operator.isAPressed();
    boolean spitOut = operator.isBHeld();
    boolean spitOutReleased = operator.wasBReleased();
    boolean launchAll = operator.isRightTriggerPressed();
    boolean tipped = operator.isLeftBumperPressed();
    boolean spinUp = operator.isYPressed();
    boolean stopAll = operator.isDpadDownPressed();

    if (intakeOnOff) {
        robot.intake.toggle();
    }
    if (spitOut) {
        robot.intake.reverse();   // also how opponent NECTAR goes back out (G408)
    }
    if (spitOutReleased) {
        robot.intake.stop();
    }
    if (launchAll) {
        robot.shooter.launch(4);   // lands best from our launch spot (driver's A)
    }
    if (tipped) {
        MatchInfo.markHiveTipped();
    }
    if (spinUp) {
        robot.shooter.spinUp();
    }
    if (stopAll) {
        robot.stopMechanisms();
    }
}

/** Rumbles: only OpModes touch gamepads. */
private void rumbleForEvents() {
    if (robot.hasNewUrgentAlert()) {
        driver.rumble(RUMBLE_ALERT_MS);
        operator.rumble(RUMBLE_ALERT_MS);
    }
    if (robot.clock.justReachedSecondsLeft(ONE_MINUTE_WARNING_S)) {   // NECTAR + FLOWERS unlocked
        driver.rumble(...);
        operator.rumble(...);
    }
    if (robot.clock.justReachedSecondsLeft(PARK_WARNING_S)) {   // time to PARK
        driver.rumble(...);
    }
}
```

### 9.2 BioBuzzAuto

INIT: gamepad 1 D-pad left/right switches the alliance; the screen also shows `AutoConfig.START` and `START_DELAY_S` (set before the match; no menu). `init()`: build Robot, `MatchInfo.resetForNewMatch()`, `PoseMemory.clear()`, `robot.shooter.holdPreloads()` (the only actuator move in any INIT). `init_loop()`: alliance, then build Auto's paths (LEAVE, PARK) from the planned places for the chosen alliance, draw them, and show `!!! PATH WARNING` if any path fails `isOnOwnSide` or `avoidsHive`. `start()`: `robot.startMatch("BioBuzzAuto", MatchClock.AUTO_S, "start=... delay_s=...")`, `drive.setStartingPose(...)`, EVENT `AUTO_PATH_OFFSIDE` if INIT warned. `loop()`: park-by guard, `runStep()`, `robot.update()`, `PoseMemory.save(...)`, telemetry. `stop()`: `robot.endMatch()`, save the pose. Paths are built when their step starts, from where the robot really is.

| Step | First loop | Done when | Timeout (`AutoConfig`) |
| --- | --- | --- | --- |
| `WAIT_DELAY` | – | `AutoConfig.START_DELAY_S` passed | – |
| `LEAVE` | `drive.followPath(FieldPaths.leaveWall(...))` | `drive.isPathDone()` | `DRIVE_TIMEOUT_MS` → `holdHere()`, go on |
| `LAUNCH_PRELOADS` | `robot.shooter.launch(4)` (from where the robot is: no launch zones) | `!robot.shooter.isLaunching()` | `LAUNCH_TIMEOUT_MS` → cancel, go on |
| `PARK` | `robot.stopMechanisms()`, `followPath(toLoadingZone)` | path done | `PARK_TIMEOUT_MS` |
| `DONE` | – | – | – |

Park-by guard: `if (robot.clock.secondsSinceStart() > AutoConfig.PARK_BY_S && step is before PARK) goToStep(PARK, "park-by time")`, with EVENT `PARK_BY:<step>`. Every timeout writes EVENT `TIMEOUT:<step>`; every step change is a `CHANGE:Auto` EVENT (`StateLog`).

### 9.3 Test OpModes (group "Tests")

| OpMode | Builds | Buttons |
| --- | --- | --- |
| `Test: Drive` | `new Drive(hardwareMap)` | sticks, Y, X, A/B assists (pose set to the field centre at start) |
| `Test: Intake`, `Test: Feeder`, `Test: Launcher` | that mechanism only | one button per verb; telemetry shows every question's answer |
| `Test: Camera` | `HiveCamera` only | shows the tags in view: learned id, offset, width |
| `Test: Launch` | `Robot` | start / cancel, shows state and history |
| `Test: Parts` | nothing but `hardwareMap` (`tryGet`) | D-pad up/down picks a device from `HardwareNames.ALL_DEVICES` (missing ones listed); left stick Y = motor power (≤ `ControlsConfig.PARTS_TEST_MAX_POWER`), right stick Y = servo nudge, X = servo middle |
| `Robot Check` | Pinpoint and drive motors from `hardwareMap.tryGet` (no follower) | Steps BATTERY, PINPOINT, GYRO_STILL, DRIVE_MOTORS, LOGGER, DONE; A = next, B = repeat; in DRIVE_MOTORS A = forward, X = wrong way; limits `HealthConfig.CHECK_*`; ends READY FOR MATCH |
| `Log Viewer` | nothing (`helpers/LogSummary`) | D-pad picks one of the 10 newest logs; A reloads. Works in INIT |

Every test OpMode writes a match log (its name is in the file name), except Test: Camera and Log Viewer.

### 9.4 Field Checks (group "Field Checks")

All are mentor zone and share `helpers/FieldCheckHelper` (grades, results, `FIELDCHECK_<nn>` events) and `configurations/FieldCheckConfig`. Each shows its instructions on screen.

| OpMode | Builds | How it measures |
| --- | --- | --- |
| 01 Hardware | `hardwareMap` only | `tryGet` every `ALL_DEVICES` name; hubs answering (`LynxModule`, not "not responding", voltage > 0); Pinpoint READY after 2 s of `update()`; HuskyLens `knock()` |
| 02 Battery | `BatteryMonitor` | 2 s average; sets the resting voltage and "battery differs" for 07–09 |
| 03 Still | `Drive` (`updatePoseOnly`) | Worst heading creep in 10 s; Pinpoint always READY |
| 04 Push | `Drive` (`updatePoseOnly`) | `zeroPose()`, crew pushes 48 in forward then 48 in sideways; distance and drift |
| 05 Spin | `Drive` (`updatePoseOnly`) | Adds up every heading change (short way) over 4 hand turns |
| 06 FieldCentric | `Drive` | Driver drives; A = moved away, B = didn't |
| 07 Brake | `Drive` (`updatePoseOnly`, `driveDirectly`, `coast`) | Copy of Pedro's deceleration tuner: power 1 to `BRAKE_TEST_SPEED_IN_PER_S`, coast, average slow-down until < 1 in/s; forward then left; vs the baseline (15 %) |
| 08 Line | `Drive` + `FieldPaths.straightAhead` | End error after `SETTLE_MS`, overshoot, settle time; vs baseline + 0.75 in |
| 09 Square | `Drive` + `FieldPaths.square` | Back to (0, 0): position and heading error |
| 10 Wall | `Drive` (`updatePoseOnly`) | Zero on one wall, square on another: distance from the nearest 90° |
| 11 Mechanisms | `Robot` | Intake runs 1 s; launcher up to speed within `SPIN_UP_TIMEOUT_MS`; no mechanism FAULTED |
| 12 Camera | `HiveCamera` | Robot on our launch spot facing the upward CELL: the share of HuskyLens reads that see that CELL's learned tag, over 1 s after first seeing it (PASS ≥ `CAMERA_SEEN_FRACTION`, WARN below, FAIL if not seen in 10 s) |
| 13 Summary | nothing | Every result of this app session, battery sag, FAIL/WARN count; logs `SUMMARY_<nn>` |

---

## 10. Helpers and configurations

**Helpers** (public methods; `try`/`catch` and `List` allowed here only)

| Helper | Public methods |
| --- | --- |
| `StateTimer` | `reset()`, `seconds()`, `milliseconds()`, `isTimeUp(ms)` |
| `StateLog` | `new StateLog(name)`; `record(from, to, reason)` and `warn(msg)` (each also a match-log EVENT), `getLastChange()`, `getWarning()`, `addTo(t, state, timer, busy)` |
| `GamepadInput` | sticks with deadzone (`getLeftStickY()` …), `areSticksMoved()`, `isXPressed()` / `isXHeld()` per button, `wasAReleased()`, `wasBReleased()`, `wasXReleased()`, `wasYReleased()`, `wasLeft/RightBumperReleased()` (their own memory, so a press read and a release read of one button don't clash), `isDpadLeftHeldFor(ms)`, `rumble(ms)`, `setLedColor(r, g, b)`, `getLastUpdateMs()` |
| `AllianceColor` | `enum AllianceColor { RED, BLUE }` |
| `MatchInfo` | static `alliance` (an `AllianceColor`, RED until chosen), `fieldName`; `isRed()`, `switchAlliance()`; the HIVE: `resetForNewMatch()`, `markHiveTipped()` (EVENT `HIVE_TIP`), `isCellAUp()`, `getUpCellName()`, `getTipCount()`, `getNectarAllowed()` (one per TIP; the header says "NECTAR open" with 1:00 left in TeleOp), `secondsSinceTip()` |
| `PoseMemory` | `save(pose)`, `hasPose()`, `getPose()`, `secondsSinceSaved()`, `clear()` |
| `MatchClock` | One per Robot (`robot.clock`). `AUTO_S`, `TELEOP_S`; `start(lengthS)` (0 = tests: no clock), `update()`, `hasStarted()`, `isTeleOp()`, `secondsSinceStart()`, `secondsLeft()`, `justReachedSecondsLeft(s)`, `getTimeLeftText()` |
| `DashboardTelemetry` | `create(telemetry)` |
| `FieldDrawing` | `drawRobot(x, y, headingDeg)`, `drawPath(path)`, `drawCell(pose)`, `drawPoint(x, y)`, `graph(name, value)` (`TelemetryPacket.put`, the Dashboard graph), `send()` (every `DS_TRANSMIT_INTERVAL_MS`) |
| `MatchLogger` | static. `start(opModeName, batteryVolts)`, `start(opModeName, batteryVolts, extraHeader)`, `isRowDue()`, `value(column, number or boolean)`, `endRow()`, `event(type, text)`, `stop(endText)`, `getFolder()`, `getFreeStorageMb()`. No thread: a `BufferedWriter` saved every `LogConfig.FLUSH_S`; deletes old files at PLAY; a file error turns logging off with one ALERT |
| `BatteryMonitor` | `getVoltage()`: the lowest hub voltage, read at most every `HealthConfig.BATTERY_SAMPLE_MS` |
| `HealthMonitor` | One per Robot. `start()`, `update()` (loop time, low battery, Pinpoint not READY, heading jump), `getLoopMs()`, `addLogData()` (`loop_ms`, `worst_loop_ms`, `battery_v`), `getSummary()` (for the END line) |
| `Alerts` | static. `raise(text, urgent)` (also an EVENT row), `getMessage()` (shown `TelemetryConfig.ALERT_SHOW_S`), `takeUrgent()`, `clear()` |
| `FieldCheckHelper` | static. `start(opModeName, volts)` (sets `MatchInfo.fieldName`), `stop()`, `grade(error, tolerance)`, `gradeAgainstBaseline(measured, baseline, fraction)` (no baseline = WARN), `worse(a, b)`, `record(n, grade, text)` (static arrays + `FIELDCHECK_<nn>` EVENT; a 07–09 FAIL becomes WARN when the battery differs), `getGrade(n)`, `getText(n)`, `show(t, n)`, `setRestingBattery(v)`, `noteDrivingBattery(v)` |
| `LogSummary` | `LogSummary.newest(count)` (newest first); per file: `getStartText()`, `getOpModeName()`, `getAlliance()`, `getDurationS()`, `getAlertCount()`, `getLastAlert()`, `getWorstLoopMs()`, `getLowestBatteryV()`, `getBatteryAtPlayV()`, `getTimeoutCount()`, `getRowCount()`, `hasEndLine()`, `getShortText()` |

**Configurations** (`@Config`, `public static`, not `final`, except `HardwareNames`)

| Class | Main values (starting values, TODO(BioBuzz) to tune) |
| --- | --- |
| `HardwareNames` | device names with hub and port; `ALL_DEVICES` array; motor and servo count (≤ 8 each) |
| `DriveConfig` | `STICK_DEADZONE` (0.05), `RED_DRIVER_HEADING_DEG` (0), `BLUE_DRIVER_HEADING_DEG` (180), `AIM_P` (1.0), `AIM_D` (0.05) |
| `FieldConfig` | `FIELD_CENTRE_IN`; `START_NEAR_GARDEN_*`, `START_MIDDLE_*`; `OWN_SIDE_MARGIN_IN`; `LAUNCH_SPOT_*`; `LOADING_APPROACH_*`, `LOADING_PARK_*`; `RESET_CORNER_*`; `CELL_A_*`, `CELL_B_*` (from the field picture), `START_CELL_A_UP`; `HIVE_KEEP_OUT_*` (all RED, inches/degrees; guesses) |
| `AutoConfig` | `enum Start`; `START` (`WALL_NEAR_GARDEN`), `START_DELAY_S` (0) – decided before the match; `DRIVE_TIMEOUT_MS` 4000, `LAUNCH_TIMEOUT_MS` 6000, `PARK_TIMEOUT_MS` 4000, `PARK_BY_S` 26 |
| `IntakeConfig` | `COLLECT_POWER` (1.0), `REVERSE_POWER` (−0.6) |
| `FeederConfig` | `HOLD_POSITION` (0.2), `FEED_POSITION` (0.6) |
| `LauncherConfig` | `TICKS_PER_REV` (28), `SPEED_P/I/D/F`, `IDLE_RPM` (0), `SPEED_TOLERANCE_RPM` (100), `LAUNCH_RPM` (3100), `SPIN_UP_TIMEOUT_MS` (2500), `FEED_MS` (250), `RECOVER_MS` (300), `TIP_PAUSE_MS` (1500), `AIM_OFFSET_DEG` (0) |
| `CameraConfig` | `HIVE_SAMPLE_MS`, `HIVE_IMAGE_WIDTH_PX`, HuskyLens learned tag IDs |
| `ControlsConfig` | `TRIGGER_THRESHOLD`, `SET_POSE_HOLD_MS`, rumble lengths, match-time warnings, `PARTS_TEST_*`, `ROBOT_CHECK_MOTOR_*`, the button list as comments |
| `HealthConfig` | battery, loop-time and heading-jump limits; Robot Check limits `CHECK_*` |
| `TelemetryConfig` | switches `DRIVE`, `INTAKE`, `FEEDER`, `LAUNCHER`, `CAMERAS`, `LAUNCH`, `HISTORY`, `DETAILS`, `GAMEPAD_HELP`; the cheat sheet; `FIELD_DRAWING`, `FIELD_IMAGE` (on), `FIELD_IMAGE_PATH` |
| `FieldCheckConfig` | `FIELD_NAME`, `BASELINE_DATE`, every `BASELINE_*` (0 = not recorded), tolerances and test lengths per check, the camera check's placement and tag |
| `TemplateConfig` | `RUN_POWER`, `RUN_TIME_MS`, `TIMEOUT_MS` (for `template/`) |

---

## 11. How to extend: checklists

**Add a mechanism** (example: a FLOWER arm)

1. Copy `template/TemplateMechanism.java` to `mechanisms/FlowerArm.java`; rename; `extends Mechanism`; fill in the hooks you need.
2. Add its devices to `HardwareNames` (with hub and port, and in `ALL_DEVICES`); check the motor and servo count.
3. Add `configurations/FlowerArmConfig.java`.
4. Decide which subsystem owns it (the job it helps). There: one field, `flowerArm = new FlowerArm(hardwareMap);` and `addMechanism(flowerArm);`. (Update, stop-all, telemetry and log now include it; `Robot` does not change.)
5. Add menu methods in that subsystem for its actions (cancel any state machine there that uses it).
6. Copy `template/TemplateMechanismTest.java` to `testing/FlowerArmTest.java`.
7. Add gamepad 2 buttons in `handleOperator()`, and update the three cheat sheets.
8. Add a `TelemetryConfig` switch if its lines are long.
9. Update `HLD.md` (robot table), `LLD.md` (section 5) and `README.md`.

**Add a state machine**

1. Copy `template/TemplateStateMachine.java` to `statemachines/<Task>StateMachine.java`; list its states; give every waiting state a timeout in a `*Config`.
2. Keep `extends StateMachine` and all five overrides. In the subsystem that owns its mechanisms: one field, build it with its mechanisms, and `addStateMachine(...)` after `addMechanism(...)`. (Update before the mechanisms, stop-all, telemetry and log now include it.) It never calls a mechanism's `update()`: the subsystem already does. If its mechanisms are in two subsystems, ask a mentor (section 3.3).
3. Add a menu method that starts it; it cancels every state machine in that subsystem that shares a mechanism with this one.
4. Add a test OpMode (start / cancel) and the buttons or auto steps that start it.
5. Update `HLD.md`, `LLD.md` (section 7, with the state table) and `README.md`.

**Add a subsystem** (a new job of the robot): copy `template/TemplateSubsystem.java` to `subsystems/`; `addMechanism` / `addStateMachine` in its constructor; its menu; in `Robot` a public field, build it, add it to `subsystems` (`ADD A SUBSYSTEM HERE`); update `HLD.md`, `LLD.md` (section 3.3) and `README.md`.

**Add an assist (S2):** a pose in `FieldConfig` → a path method in `FieldPaths` → a method in `DriveToSpot` that checks the pose and the HIVE → one line in `handleDriver()` → cheat sheets.

**Add an auto step:** a `Step` value → a `case` (first loop, done, timeout) → a timeout in `AutoConfig` → the step table in this document.

---

## 12. Telemetry, match log and ALERTs (game additions)

**Header line:** `RED | 1:42 left | 18 ms | 12.6 V | FIELD 3° | intake ON | launcher READY | up CELL A | tips 2 | ALERT ...`

**New DATA columns:** `intake_power`, `feeder_power`, `launcher_rpm`, `launcher_target_rpm`, `up_cell`, `tip_count`, `launch_state`, `<mechanism>_faulted`, `pose_known`, `match_s_left`.

**Header line 2:** `Game: intake ON | launcher READY | up CELL A | tips 2 | NECTAR +2` ("NECTAR open" with 1:00 left in TeleOp).

**New DATA columns (as built):** `intake_power`, `feeder_feeding`, `launcher_rpm`, `launcher_target_rpm`, `hive_cam_tag_id`, `hive_cam_offset_px`, `hive_cam_age_ms`, `<mechanism>_faulted`, `launch_state` (0 IDLE, 1 SPINNING_UP, 2 FEEDING, 3 RECOVERING), `up_cell_a`, `tip_count`.

**EVENTS:** `PLAY`, `STOP`, `CHANGE:<part>: <from> -> <to> (<reason>)` (parts: Drive, Launch, Auto, ...), `WARN:<part>: <message>` (refused assists and launches among them), `ALERT:<text>`, `PINPOINT:<status>`, `ASSIST:<name>`, `ASSIST_CANCELLED:stick` / `:aim`, `MARK:driver`, `MARK:operator`, `HIVE_TIP:<n> (up CELL A)`, `INTAKE:power <p>` / `INTAKE:off`, `LAUNCH:<n> of <count>`, `TIMEOUT:<step>`, `PARK_BY:<step>`, `AUTO_PATH_OFFSIDE:<text>`, `FIELDCHECK_<nn>:<grade> <text>`, `SUMMARY_<nn>`, `ROBOTCHECK:<text>`, `PARTS:selected <name>`.

**New ALERTs:** `<mechanism> FAULTED: <reason>` (urgent), `LAUNCHER TIMEOUT: <rpm> of <target> RPM` (urgent). An auto path off our side is a red INIT warning and an EVENT, not an ALERT.

---

## 13. Known implementation challenges

| Challenge | Options | Plan |
| --- | --- | --- |
| **Pinpoint access.** Pedro's `PinpointLocalizer` keeps its `GoBildaPinpointDriver` private (checked in revhub 3.0.1 and 3.0.2); the safeguard needs `getDeviceStatus()` | `hardwareMap.get(GoBildaPinpointDriver.class, HardwareNames.PINPOINT)` in `Drive`; reflection (forbidden); a team localizer class | **Done:** `hardwareMap.get` returns the same object Pedro made (one object per configured device), so there is no second device object. `getDeviceStatus()` only reads the value Pedro's `update()` already fetched: no extra I2C read. Drive never writes to it. Needs the localizer in `Constants` to be the Pinpoint (it is). `recalibrateIMU()` (a write) waits with the recalibrate button (section 14) |
| **Dashboard field background** | `TelemetryPacket(false)` + `drawImage("/images/field-biobuzz.png", 0, 0, 144, 144)`; flicker between packets | Already in `FieldDrawing`; the image is at `TeamCode/src/main/assets/images/field-biobuzz.png`; check flicker on the robot |
| **Constants depends on team code** | `pedro/Constants.java` imports `HardwareNames` | Keep; record in `UPSTREAM.md` |
| **HuskyLens is 2D** | Box size → rough distance; tag learned IDs must be trained on each unit; one algorithm at a time; I2C read takes a few ms | Read every 100 ms; use for "which CELL" and aim offsets only; document training in `CAMERA_GUIDE.md` |
| **Which CELL is up** | Operator button; camera; counting launched elements | Operator button first (Q2) |
| **Detecting a disconnected device** | The SDK seldom throws; a motor on an unplugged hub keeps "working" | Watch `LynxModule` health and motor current; mark FAULTED; acceptance test: pull a cable |
| **Arrays in `@Config`** (a launcher speed table, later) | FTC Dashboard may not edit arrays live | **Done:** Dashboard 0.6.0 has an `ArrayProvider` for config arrays, so `double[]` fields are used. Check on the robot that edits apply |
| **Pedro 3 path headings** | Paths need a heading plan (tangent, constant, linear) | **Done:** every path ends with `.linear(from, to)` or `.constant(heading)`; `Paths.path(...)` to chain the square |
| **An idle follower brakes** | Pedro's `Follower.update()` in IDLE mode calls `drivetrain.stop()` every loop, so the motors brake | Field Checks that the crew pushes by hand (03, 04, 05, 10) and the brake test (07) call `Drive.updatePoseOnly()` (the localizer only) instead of `update()` |
| **Aim with Pedro** | `ManualDrive.headingLock(follower, controller, powers, targetRad)` replaces only the turn power and keeps forward/strafe | `Drive.driveWithSticksAimedAt` turns the stick powers field-centric first, then calls it with a `PIDController` whose gains are copied from `DriveConfig` each call |
| **Files over 250 lines** | `Robot` (~300), `Drive` (~300), `RobotCheck`, `BioBuzzTeleOp` (mostly comments) | Kept whole for now: splitting would add files rookies must follow. Move Field-Check-only Drive methods out first if Drive grows again |
| **Mechanisms need every device** | `Robot` builds every mechanism with `hardwareMap.get`, so BioBuzz TeleOp/Auto stop at INIT until all mechanism devices are in the robot configuration | Intended (problems found before the match). Until the mechanisms are built, drive with Test: Drive |
| **Static state between OpModes** (`MatchInfo` with the HIVE, `PoseMemory`) | Static fields survive OpMode changes but not an app restart | Auto `init()` resets them; TeleOp only reads them; telemetry shows where each value came from |

---

## 14. Future improvements

| Improvement | Gains | Costs | When |
| --- | --- | --- | --- |
| **Bulk reads** (`LynxModule` caching, AUTO or MANUAL) | 1–2 ms per sensor read saved | MANUAL: a missed clear freezes sensor values; needs a stored hub list in a helper | When `loop_ms` or a SLOW LOOP ALERT shows loops near the slow-loop limit; record loop time before and after |
| Snap heading to 90° (gamepad 1 right stick button, robot flat on a wall) | Fast heading fix without facing away | About 15 lines in Drive and TeleOp (`follower.setHeading`), refuse if more than 20° off | If Y is not enough in matches |
| Gyro recalibrate (hold left stick button) and INIT drift checks | Fixes a slow heading creep | `pinpoint.recalibrateIMU()` while still; the status reads `CALIBRATING` for about 0.25 s (must not trip the Pinpoint safeguard); INIT "hands off" and drift screens | If logs show the heading creeping while still |
| Gamepad-disconnect, pose-jump and motor-stall detection | More ALERTs | Gamepad `timestamp` watch and a drive stop; pose-step check; per-mechanism current and velocity checks with config values | When a match log shows one of these happened |
| Gamepad LED colours (PS4/PS5) | Field / robot-centric and low battery at a glance | `GamepadInput.setLedColor` exists; only on Sony gamepads | If the drive team asks |
| Background log writer thread | Storage saves never touch the loop | A thread and a queue (R3 bans threads in team code; would need an exception for `helpers/`) | If `worst_loop_ms` shows spikes every `FLUSH_S` |
| **Hide the test OpModes with one switch** (for competition) | Only BioBuzz TeleOp and BioBuzz Auto on the Driver Station: nobody picks a test by mistake in a match | One mentor-zone file `TestOpModes.java` with `public static final boolean SHOW_TESTS` and `SHOW_FIELD_CHECKS`, and an `@OpModeRegistrar` method that calls `manager.register(...)` for each test only when its switch is on (checked: in SDK 12.0). The 12 tests and 13 Field Checks lose their `@TeleOp` line (or they show twice), so each name moves into that file, and every NEW test needs one more line there (add it to `HOW_TO_ADD_A_MECHANISM.md` and the template). Not a Dashboard switch: the list is built when the app starts, and Dashboard values reset on restart, so changing it means build and install. Two switches, because Field Checks are run at the event (practice field) | If the drive team picks the wrong OpMode, or before the first competition build |
| **Build check on pull requests** (GitHub Actions) | A pull request that does not compile shows a red X before anyone reviews it; with branch protection, `master` only accepts pull requests that pass | One workflow file in `.github/workflows/` running `./gradlew :TeamCode:assembleDebug` (about 5–10 min per run, free for the team's usage). Branch protection is a repository setting (an admin). The three workflows there today are Pedro's own and only run in Pedro's repository | When students start opening pull requests |
| **Version line at INIT** | Anyone can see which code is on the robot ("BioBuzz v0.2 · adcdc3c"): one robot, many laptops | The release tag and commit written into `BuildConfig` by Gradle (a few lines in `TeamCode/build.gradle`, a Quickstart file: record it in `UPSTREAM.md`), shown in the telemetry header | Before students deploy from their own laptops |
| Config snapshot in the log header | Know every tuned value of a run | One hand-written line per value (no reflection) | When tuning values change often between matches |
| **Storage sensing** (a Storage mechanism) | Knowing how many balls are held and their colours: stop the intake at 4 (G407), push opponent NECTAR out automatically (G408), launch exactly what is held, pick a speed table per ball type | Sensors in the hopper (a colour sensor at the mouth, or one per slot); a `Storage` mechanism and a `CollectStateMachine` (intake until 4, reject opponent NECTAR) | When the robot's hopper gets sensors |
| **Auto: collect and launch again** (`LAUNCH_COLLECT_PARK`) | 4 more balls in Autonomous | Steps `COLLECT_GARDEN` (intake on, a path through our GARDEN), `DRIVE_TO_LAUNCH` (back to the launch spot: the GARDEN path ends facing away from the CELL), `LAUNCH_AGAIN`; GARDEN places in `FieldConfig`; `COLLECT_TIMEOUT_MS`; an `enum Routine` in `AutoConfig` to pick it. Built and taken out in v2.12 (commit `295413f` has it) | When `LAUNCH_PARK` works every time and fits in 30 s with time to spare |
| Camera pose fix | x/y recovery without the reset corner | The HuskyLens gives 2D boxes, not distances: only a rough fix from the box size. A real fix needs a camera with a tag pose (a webcam tilted up at the CELL tags, or a Limelight) and its mount measured; wrong fixes are worse than none. The webcam code was built and taken out in v2.12 (commit `295413f` has it) | After drive tuning, if "set pose here" is not enough |
| **Launch speed by distance** (a speed table) | Launch from anywhere, not only the launch spot | `LauncherConfig.DISTANCE_IN[]` / `RPM[]` measured from 4–5 distances; `Launcher.spinUpFor(distanceIn)` with straight-line in-between values; `FieldPaths.distanceToUpCellIn()`; a default distance when the pose is unknown (S1); `LaunchStateMachine` asks Drive where we are again. Built and then taken out in v2.11 (commit `054aa10` has it) | When the students tune `LAUNCH_RPM` easily and want more launch spots |
| Launch while moving | Faster cycles | Needs velocity-aware aiming and the speed table | After consistent stationary launching |
| FLOWER placer | Bottom NECTAR Bonus, FLOWER ownership | A new mechanism (checklist in section 11) | If the team builds one |
| Limelight 3A | Real 3D AprilTag pose | Cost, mounting | If HuskyLens is not good enough |
