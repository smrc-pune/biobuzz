# BioBuzz Robot Software – Requirements

Oct 19, 2026 · @Girish

**Version 2.0**

## Contents

1. Purpose and how to use this document
2. Confirmed requirements
3. Coding style rules (R3)
4. Base project and repository (R11, R12)
5. Code layout (R10)
6. Architecture
7. Game requirements (BIOBUZZ)
8. Driving (R1, R2)
9. Operator controls (R2)
10. Autonomous
11. Telemetry and dashboard (R6)
12. Troubleshooting (R7, R8, R9)
    1. A. Confirming tuning on a new field (R7)
    2. B. Before every match: Robot Check
    3. C. Match-time issues: detect and recover (R8)
    4. D. Battery voltage as a signal
    5. E. Match logs for post-match analysis (R9)
    6. F. After the match: analysis and debrief
    7. G. TODO: Match Simulator
13. Testing and tuning
14. Documentation deliverables (R4, R5)
15. Open items and acceptance
16. Revision history

## Purpose and how to use this document

An AI assistant uses this document to produce the BioBuzz robot's software: the code, the design documents and the student guides. It turns the patterns proven in the DecodeBot `pedro` package into rules, so the new code reads like the old code.

**Audience.** The AI writes the code. Mentors check the result against the acceptance checklist at the end. Students in grades 7 to 12 read and change the code. Many of them have no Java experience.

**How the AI should use this document.**

1. Read every section before writing code. **MUST**, **SHOULD** and **MAY** have their usual requirement meanings.
2. This document, `docs/HLD.md` and `docs/LLD.md` define the design. The DecodeBot `TeamCode/.../teamcode/pedro` package is the style reference only where they are silent.
3. The game is BIOBUZZ (Competition Manual TU05, from the FIRST website; not committed to the repository). What the manual does not settle (mechanism hardware, exact poses, team decisions) is left out rather than guessed: put a `// TODO(BioBuzz):` marker and a matching entry in Open items. `docs/DESIGN-REVIEW.md` lists the open questions.
4. Work in this order: the skeleton (helpers, configurations, Robot, Drive, teleop S1), then one mechanism end to end, then autonomous (S3), then the assists (S2), then the documents. Driving grows in three stages: S1 manual, S3 autonomous, S2 assisted (see Driving).

**What to deliver.**

| Deliverable | Form | Section |
| --- | --- | --- |
| Robot code | Java under `teamcode/biobuzz/` | Code layout, Architecture |
| Test and tuning OpModes | Java under `biobuzz/testing/` and `biobuzz/fieldchecks/`. Pedro tuners stay in the Quickstart's `pedro/procedures/`. | Testing and tuning |
| Match logging and troubleshooting tools | Java plus `docs/TROUBLESHOOTING.md` | Troubleshooting |
| High-level design (HLD) | `docs/HLD.md` | Documentation deliverables |
| Low-level design (LLD) | `docs/LLD.md` | Documentation deliverables |
| Design review (conflicts, gaps, questions) | `docs/DESIGN-REVIEW.md` | Documentation deliverables |
| Student how-to guides | `README.md` and `docs/*.md` | Documentation deliverables |

## Confirmed requirements

The team has confirmed these twelve requirements. Each one has its own detailed section below. They override anything else in this document that conflicts with them.

| ID | Requirement | Details in |
| --- | --- | --- |
| R1 | Driving MUST be field-centric. Robot-centric is used only as a fallback when the heading can't be trusted, or when the driver switches deliberately. | Driving |
| R2 | Gamepad 1 MUST drive the robot. Gamepad 2 MUST run the mechanisms. Neither gamepad does the other's job. | Driving, Operator controls |
| R3 | Code MUST use clean, simple abstractions that students can follow. No complex Java syntax or constructs. | Coding style rules |
| R4 | A high-level design (HLD) and a low-level design (LLD) MUST be delivered. | Documentation deliverables |
| R5 | How-to guides MUST let a student start working with the code and make changes. | Documentation deliverables |
| R6 | Telemetry MUST show on both the Driver Station and FTC Dashboard. | Telemetry and dashboard |
| R7 | There MUST be a quick way to confirm that tuning still works on a new field. | Troubleshooting |
| R8 | The software MUST detect and report match-time problems: loop lag, low battery, disconnects, stuck mechanisms. | Troubleshooting |
| R9 | Every match MUST write a log file for analysis after the match. | Troubleshooting |
| R10 | The team code layout MUST follow the Code layout section of this document, in `teamcode/biobuzz/`. It started from the DecodeBot `pedro` package, which stays the style reference where this document is silent. | Code layout |
| R11 | The project MUST be based on the Pedro Pathing Quickstart for Pedro 3.x. | Base project and repository |
| R12 | All code MUST live in a new `biobuzz` repository that stays up to date with the public Quickstart repository. Quickstart files and folders MUST NOT be moved. The team's `biobuzz` package sits next to the Quickstart's `pedro` package. | Base project and repository |

## Coding style rules (R3)

A student who has never used Java MUST be able to read any file and say what the robot does. When readability and cleverness conflict, readability wins. Some repetition is acceptable.

**Allowed Java**

| Use | Example |
| --- | --- |
| Classes with fields, a constructor and plain methods | `public class Gate { ... }` |
| `enum` for fixed lists: states, auto steps, routines, element types | `enum State { IDLE, INTAKING, FEEDING }` |
| `extends Mechanism`: every mechanism extends the one team base class, and overrides its hook methods with `@Override` | `public class Intake extends Mechanism { ... }` |
| `switch` on the state enum, with one `case` per state | `switch (state) { case IDLE: ... break; }` |
| `if` / `else`, `for` over a counted range, and fixed-size arrays | `for (int i = 0; i < 5; i++)` |
| `public static` fields in config classes, so FTC Dashboard can edit them live | `public static double ROW1_POWER = 0.8;` |
| `final` fields for devices, mechanisms and helpers | `private final StateTimer timer = new StateTimer();` |
| One-line methods | `public boolean isRunning() { return state != State.IDLE; }` |

**Forbidden Java** (the AI MUST NOT use these in student-facing code)

- Lambdas, method references and the streams API: `x -> ...`, `::`, `.stream()`.
- Generics written by the team, such as `class Machine<T>`. Using an SDK `List<String>` is acceptable only inside `helpers/`.
- Any team base class other than `mechanisms/Mechanism.java`, inheritance deeper than one level, and abstract classes. `Mechanism` is a plain class; mechanisms extend it and nothing extends a mechanism. State machines stand alone and follow the shared section layout. The other superclasses used are the SDK's `OpMode` and `LinearOpMode`.
- Interfaces, anonymous classes, inner classes and static nested classes.
- `Optional`, `var`, ternary chains, bit operations, reflection and annotations other than the FTC ones (`@TeleOp`, `@Autonomous`, `@Config`, `@Override`, `@Disabled`).
- Threads, `sleep()` and busy-wait loops anywhere in team code. Waiting is done with `StateTimer`.
- Exceptions used for control flow. `try`/`catch` is allowed only inside `helpers/` (for example around file writes) and in `Mechanism.runUpdate()` / `runStop()`, which turn an error in a mechanism into a FAULTED mechanism instead of a crash.
- Command-based frameworks such as FTCLib commands or NextFTC. Pedro's own calls are allowed only in the Quickstart's `pedro/` folder, in `drive/Drive.java` (the follower) and in `game/FieldPaths.java` (paths and poses), which MUST explain them in comments.

**Naming and comments**

- Use plain English names: `gate.open()`, `timer.isTimeUp(600)`, `shooter.isAtSpeed()`.
- Constants are `UPPER_SNAKE_CASE`, always with the unit in the name (`_MS`, `_IN`, `_DEG`, `_RPM`) or in a comment.
- Every file starts with a block comment. It says what the file is, its role (mechanism, state machine, Drive, Robot, game, config, OpMode or helper), the design pattern it uses by name (see Architecture), a small ASCII diagram for state machines, and how to trace a bug through the file.
- Comments explain WHY in student language. Mark each state's code with `// DO`, `// CHECK` and `// GO`.
- A file SHOULD stay under 250 lines. When it grows past that, split the work into another mechanism or state machine; do not add cleverness.

## Base project and repository (R11, R12)

The BioBuzz code MUST start from the [Pedro Pathing Quickstart](https://github.com/Pedro-Pathing/Quickstart) for Pedro 3.x, in a new git repository named `biobuzz`. The Quickstart's files stay exactly where it puts them, so its updates merge cleanly. All team code goes in one new package, `teamcode/biobuzz/`, next to the Quickstart's `teamcode/pedro/`.

**Repository setup**

1. Create `biobuzz` by cloning the Quickstart's `master` branch with its full git history, then pushing it to the team's new repository. Do not use GitHub's "Use this template": it drops the history that later merges need.
2. Set up the remotes:
   - `origin` points to the team's `biobuzz` repository.
   - `quickstart` points to `https://github.com/Pedro-Pathing/Quickstart.git`, and brings in Pedro updates.
   - `ftc` (`https://github.com/FIRST-Tech-Challenge/FtcRobotController.git`) is optional. Normally the FTC SDK arrives through the Quickstart. Add `ftc` only when an SDK release is needed before the Quickstart picks it up.
3. The first commit after the clone adds `teamcode/biobuzz/`, `tools/` and `UPSTREAM.md`, and touches no Quickstart file except the Gradle dependency block (described below). `pedro/Constants.java` and `pedro/Tuning.java` stay as the Quickstart's stubs until drive tuning (see the rules below).

**Where everything lives**

```
biobuzz/                                  repo root = Quickstart root
  FtcRobotController/                     FTC SDK (Quickstart-owned, never edited)
  TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
    pedro/                                Quickstart-owned: DO NOT move, rename or add files
      Constants.java                      Quickstart stub; filled in ONCE at tuning, then VALUES only
      Tuning.java                         Quickstart stub; registers our tuners ONCE at tuning
      procedures/                         unchanged Pedro tuners and Tests
    biobuzz/                              ALL team code (see Code layout)
  TeamCode/src/main/assets/images/        team: BioBuzz field image (the Dashboard serves /images/)
  build.dependencies.gradle               Quickstart-owned; team additions in ONE marked block
  tools/plot_match.py                     team, laptop only
  UPSTREAM.md                             team: every change to a Quickstart file + merge log
```

**Rules for Quickstart-owned files**

- No file or folder that comes from the Quickstart may be moved, renamed or deleted. This includes `teamcode/pedro/`, `pedro/procedures/`, the Gradle files and `FtcRobotController/`.
- Team code MUST NOT go inside `teamcode/pedro/`. DecodeBot put its code there; BioBuzz does not.
- `pedro/Constants.java` and `pedro/Tuning.java` ship from the Quickstart as empty stubs (`Constants.create()` returns `null`; `Tuning` registers no tuners). They stay that way until drive tuning.
- **At tuning, once:** `Constants.java` gets its full structure, following DecodeBot: the follower factory `create(hardwareMap)` plus the drivetrain, localizer and Foresight configs. `Tuning.java` registers the tuners we use. This one-time structural change is recorded in `UPSTREAM.md`. After it, only values change in `Constants.java` (motor directions, pod offsets, tuned constants), so a merge conflicts only on values.
- `Constants.java` reads device names from `biobuzz/configurations/HardwareNames.java` (an import), so each name is written in one place. This is the only place a Quickstart file depends on team code, and it is recorded in `UPSTREAM.md`.
- Lambdas (`c -> ...`) in `Constants.java` and `Tuning.java` are Pedro's configuration style and are allowed there. The Coding style rules apply to `biobuzz/` only.
- Team code reaches Pedro only through the follower factory in `pedro/Constants.java` (`Constants.create(hardwareMap)`). `biobuzz/drive/Drive.java` is the only team class that calls it. Pedro's `PinpointLocalizer` keeps its Pinpoint driver private, so the Pinpoint status, update rate and recalibration that Troubleshooting needs are not reachable through the follower. How `Drive` gets them is decided during implementation and written up in the LLD (Known implementation challenges).
- Team Gradle additions go in one block at the end of `build.dependencies.gradle`, marked `// ---- BioBuzz additions ----`. Today that block holds FTC Dashboard (`com.acmerobotics.dashboard:dashboard` and the `https://maven.brott.dev/` repository). Pedro library versions (`com.pedropathing:revhub`, `com.pedropathing:tuning`) come from the Quickstart: on a merge, take its versions instead of editing them.
- Local toolchain workarounds, such as DecodeBot's Android Gradle Plugin downgrade in `build.gradle`, MUST NOT be committed unless the whole team needs them. Committed workarounds are recorded in `UPSTREAM.md`.
- `UPSTREAM.md` lists every change to a Quickstart-owned file: the file, what changed, and why.

**Keeping Pedro up to date**

1. Check for Quickstart updates once a month in the off-season and before each competition. Never merge an update in the 7 days before an event.
2. On a branch named `update-quickstart-<yyyy-mm-dd>`, run `git fetch quickstart`, then `git merge quickstart/master`.
3. If `pedro/Constants.java` or `pedro/Tuning.java` conflicts, keep our values and tuner list, and take the Quickstart's new structure.
4. Build. Run the Quickstart's `Tests` OpModes, then Field Checks 01 to 09 at home, and compare them with the baseline.
5. Merge to `main` only if everything passes. Record the Quickstart commit and the test results in `UPSTREAM.md`.
6. `docs/UPDATING_PEDRO.md` MUST walk a student through these steps, including how to resolve a `Constants.java` conflict.

## Code layout (R10)

The team package MUST be `org.firstinspires.ftc.teamcode.biobuzz`, in `teamcode/biobuzz/` next to the Quickstart's `teamcode/pedro/`. The Pedro files (`Constants.java`, `Tuning.java`, `procedures/`) stay in the Quickstart's `pedro/` (see Base project and repository). Each folder holds one kind of file, so a student always knows where to look.

```
teamcode/pedro/           Quickstart-owned: Constants.java, Tuning.java, procedures/ (not moved)
teamcode/biobuzz/
  README.md               start here: reading order, vocabulary, first changes
  Robot.java              builds everything; one method per robot action; update() once per loop
  configurations/         tunable numbers ONLY (@Config, public static), plus HardwareNames
    HardwareNames.java    every device name + hub and port + motor direction; ALL_DEVICES; motor/servo count
    DriveConfig.java      stick deadzone, driver headings, assist values
    ControlsConfig.java   trigger threshold, rumble lengths, which button does what (as comments)
    FieldConfig.java      every field pose (inches, degrees, BLUE side), HIVE keep-out, start positions
    AutoConfig.java       step timeouts, park-by time, default routine and delay
    TelemetryConfig.java  on/off switches
    LogConfig.java        match-log switches and rate
    HealthConfig.java     alert, heading-safeguard and battery thresholds
    FieldCheckConfig.java Field Check home baseline + tolerances
    <Mechanism>Config.java  one per mechanism: IntakeConfig, StorageConfig, LauncherConfig, CameraConfig
  drive/                  Drive.java: driving operations over Pedro (sticks, paths, pose)
  mechanisms/             Mechanism.java (the one base class) + one class per mechanism: operations, no states
  statemachines/          <Task>StateMachine.java: tasks that take time and may use several mechanisms
  game/                   BIOBUZZ-specific logic: Element, HiveState, FieldPaths (every path)
  teleop/                 BioBuzzTeleOp.java: both match gamepad maps (driver + operator), stages S1 and S2
  auto/                   BioBuzzAuto.java: the step list (stage S3)
  testing/                Test: Drive, one test per mechanism and per state machine, Parts, RobotCheck,
                          LogViewer (+ MatchSimulator, TODO)
  fieldchecks/            FieldCheck01_Hardware ... FieldCheck13_Summary: one OpMode per new-field test
  helpers/                StateTimer, StateLog, DashboardTelemetry, GamepadInput, MatchClock,
                          MatchLogger, MatchInfo, BatteryMonitor, LoopMonitor, HealthMonitor,
                          FieldCheckHelper, PoseMemory, InitMenu, FieldDrawing, LogSummary
  template/               TemplateMechanism, TemplateStateMachine, TemplateMechanismTest to copy
  docs/                   HLD.md, LLD.md, DESIGN-REVIEW.md, guides (see Documentation deliverables)
TeamCode/src/main/assets/images/field-biobuzz.png    BioBuzz field image for the Dashboard field view
tools/plot_match.py       laptop only, mentor-side (repo root, not built into the app)
```

**Layout rules**

- Device names MUST appear only in `HardwareNames.java`. Each mechanism gets its own devices in its constructor with `hardwareMap.get(...)` and those names; `pedro/Constants.java` imports them for the drivetrain and the Pinpoint.
- Tunable numbers MUST appear only in `configurations/`. There are no magic numbers in mechanisms, state machines, Drive, `game/` or OpModes.
- The DecodeBot `GamepadConfig` wrapper moves to `helpers/GamepadInput.java`, because it is a tool, not a set of numbers. It keeps the edge-detected `isAPressed()` style, adds `isAHeld()` and `wasAReleased()` reads, applies the stick deadzone and answers `areSticksMoved()`. Back/Share is read from `gamepad.back` (in the SDK, `options` is the same button as Start).
- `helpers/InitMenu.java` is the one pre-match menu (see Autonomous). `helpers/FieldDrawing.java` draws on the Dashboard field view (see Telemetry and dashboard). `helpers/LogSummary.java` reads log files for `LogViewer`, so file reading and its `try`/`catch` stay in `helpers/`. `helpers/MatchClock.java` keeps match time (see Game requirements).
- A test OpMode builds just the mechanism it tests, with `new Intake(hardwareMap)`; there are no `Robot.buildX` methods.
- `template/` MUST compile. It MUST be annotated `@Disabled` where it is an OpMode.

## Architecture

The design is explained in `docs/HLD.md` (why) and `docs/LLD.md` (each class). This section gives the rules the code MUST follow.

```
  BioBuzzTeleOp (S1, S2)                  BioBuzzAuto (S3)
  pad 1 = driving, pad 2 = mechanisms     enum Step + switch
          |  Robot methods, robot.drive          |
          v                                      v
  +--------------------------------------------------------------+
  | Robot   one method per robot action; builds everything        |
  |         update(): 1. state machines 2. mechanisms 3. Drive    |
  |                   4. always-on rules (safeguards, camera)     |
  +------+---------------------------+---------------------------+
         |                           |
  statemachines/              drive/Drive (Pedro)          game/ FieldPaths, HiveState
   Collect, Launch, ...
         | use (given in the constructor)
  mechanisms/ (each extends Mechanism): Intake, Storage, Launcher, Camera
         |
  Control Hub + Expansion Hub devices
```

| Layer | Does | MUST NOT |
| --- | --- | --- |
| OpMode (`teleop/`, `auto/`, `testing/`, `fieldchecks/`) | Reads gamepads (only OpModes do). Calls Robot methods for mechanisms and `robot.drive` for driving. Calls `robot.update()` and telemetry once per loop. Auto keeps its own step list. Does all gamepad rumbles and LEDs | Touch a motor or servo. Hold mechanism logic |
| `Robot` | Builds everything. Has one method per robot action, and cancels any state machine that shares a mechanism before starting another or before a manual command. Runs `update()` in the fixed order above. Holds the always-on rules. Keeps the newest urgent ALERT for the OpMode to rumble | Read gamepads. Set a motor power itself |
| State machine (`statemachines/`) | One task that takes time and may use several mechanisms, for example "launch everything held" | Call another state machine or Robot. Read gamepads |
| `Drive` (`drive/`) | Driving operations over Pedro: sticks (field-centric), paths, pose. It is not a Mechanism: stop-all never stops it | Decide when to follow a path (the OpMode decides) |
| Mechanism (`mechanisms/`) | Wraps one group of devices with verbs (`collect()`, `feedOne()`) and questions (`isUpToSpeed()`) | Keep states or timers. Know about other mechanisms. Move an actuator in its constructor |
| `game/` | BIOBUZZ knowledge: every path (`FieldPaths`), which CELL is up (`HiveState`), element types | Touch hardware |

**Where code goes:** a button → TeleOp; one device group → its mechanism; several steps over time or several mechanisms → a state machine started by a Robot method; something that must always happen → `Robot.update()`; a place or path → `FieldConfig` and `FieldPaths`; a tunable number → a `*Config`.

**Mechanism rules**

- Every mechanism extends `mechanisms/Mechanism.java` and overrides only its hooks: `update()` (every loop; never waits), `stop()` (safe state), `addDetails(t)` (telemetry) and `addLogValues()` (up to 8 log values). Robot and tests call the fixed methods `runUpdate()`, `runStop()`, `addTelemetry(t)` and `addLogData()`.
- An error inside a mechanism marks it FAULTED with a reason (shown on telemetry, logged, ALERT). A faulted mechanism ignores its verbs; the rest of the robot keeps running.
- Robot keeps the mechanisms in one array (`Mechanism[]`), so update, stop-all, telemetry and logging are loops over it. Adding a mechanism is two edits in Robot: a field, and one entry in the array.

**State machine rules**

- Every state machine MUST use this section order: 1. **States** (the `enum`, one comment per value); 2. **Commands** (`start(...)`, `cancel()`: they only call `goTo(state, "reason")`; a command that cannot run calls `log.warn("...")` and returns); 3. **`update()`** (a `switch` with one `case` per state, marked DO, CHECK and GO); 4. **Bookkeeping** (`goTo()`, `isRunning()`, `getState()`, `addTelemetry()`, `addLogData()`).
- Every state that waits MUST have a timeout that goes to a safe state, with the reason logged (for example `"spin-up timeout"`). `goTo()` with the current state does nothing, so repeated commands are safe.
- A state machine gets the mechanisms it uses in its constructor. It never finds them itself, never calls another state machine and never calls Robot.
- Robot lists each state machine by hand in `update()`, `stopMechanisms()` and telemetry (state machines have no base class).

**Robot rules**

- OpModes reach mechanisms only through Robot methods (`robot.startCollecting()`), so Robot can cancel the state machine that owns a mechanism first. OpModes call `robot.drive` directly, because no state machine drives the robot.
- `Robot.stopMechanisms()` cancels every state machine, then puts every mechanism in its safe state. OpModes call it in `stop()`, and D-pad down on gamepad 2 calls it.
- No button and no Robot method may command both driving and mechanisms (R2).

**Design patterns.** The code uses these standard patterns, in their simplest form, and each file's header names the one it uses (details in `docs/HLD.md`): Layered architecture; Game Loop and Update Method; Facade (`Robot`, `Drive`); Adapter (mechanisms, `Camera`, `GamepadInput`); Template Method (`Mechanism`); State pattern in its enum-and-switch form (state machines, Auto); Mediator (Robot's start methods); Dependency injection through constructors, with Robot as the composition root; Factory method (`FieldPaths`). Command-pattern frameworks are not used (see Coding style rules).

## Game requirements (BIOBUZZ)

These come from the BIOBUZZ Competition Manual TU05. Rule numbers (G…, R…) are the manual's. TODO(BioBuzz) items need the built robot or a team decision (see `docs/DESIGN-REVIEW.md`).

**Match and scoring.** AUTO 30 s, then an 8 s transition, then TELEOP 2:00. Points: LEAVE 3 (auto), PARK in the LOADING ZONE 5 (auto and end of match), HIVE TIP 20, element left in an upward CELL 2, GARDEN 1, FLOWER points in the last 60 s. RP: SWARM (LEAVE + PARK ≥ 16 per alliance), POLLINATOR (4 and 7 TIPS). FLOWER scoring is not planned: the robot has no FLOWER placer (TODO(BioBuzz): confirm).

| # | Requirement | From |
| --- | --- | --- |
| GR1 | `helpers/MatchClock.java` MUST keep match time: auto seconds since PLAY, TeleOp seconds left. The telemetry header shows the time left | 10.4 |
| GR2 | Auto MUST always end with PARK: a park-by guard jumps to the PARK step at `AutoConfig.PARK_BY_S` (starting value 26 s), whatever step is running | 10.5, SWARM RP |
| GR3 | Mechanism constructors MUST NOT move an actuator. TeleOp `init()` and `init_loop()` MUST NOT command an actuator: the first command is in `start()`. Auto `init()` MAY grip the preloads (`robot.holdPreloads()`), and nothing else | G304.H, G403 |
| GR4 | Storage MUST count the elements held. `CollectStateMachine` MUST stop the intake at `StorageConfig.MAX_HELD = 4`; a 5th element makes the intake reverse for `REJECT_MS` with an urgent ALERT | G407 |
| GR5 | The colour of each element MUST be checked at the intake. Opponent NECTAR (from the alliance in `MatchInfo`) MUST be pushed straight back out and logged as `REJECT_OPPONENT_NECTAR` | G408 |
| GR6 | `game/HiveState` MUST know which of our two CELLS faces up: the setup position at Auto `init()`, flipped by each TIP. A TIP is entered by the operator (gamepad 2 left bumper); the HuskyLens MAY confirm it later. After a TIP, launching pauses for `LauncherConfig.TIP_PAUSE_MS` | 9.6, 10.5.1 |
| GR7 | The launch spot (S2 assist) and the aim heading MUST follow the upward CELL. The launcher speed MUST depend on the element type (POLLEN 2.8 in, NECTAR 3.6 in) and the distance to the upward CELL, from tables in `LauncherConfig` | 9.6, 9.8 |
| GR8 | Every auto path MUST be checked at INIT: it stays on our side of the field (columns A–C red, D–F blue) and out of the HIVE footprint plus `FieldConfig` margin. A failing path shows a red warning on the INIT screen | G402, G409, G417 |
| GR9 | Start positions MUST be a fixed list in `FieldConfig` (touching the wall, our side, not in the LOADING ZONE), picked in the INIT menu | G304 |
| GR10 | Poses are written for BLUE. RED poses MUST be the BLUE pose turned 180° about the field centre (72, 72), because the field is rotationally symmetric. TODO(BioBuzz): confirm on Figure 9-2 or the CAD | 9.3 |
| GR11 | TeleOp MUST rumble both gamepads at 1:00 left (NECTAR may be entered, FLOWERS open) and gamepad 1 at 0:20 left (time to PARK). The header shows how many NECTAR the human player may enter (one per TIP before 1:00 left) | G410, G426 |
| GR12 | The robot MUST stay within 8 motors and 8 servos, one Control Hub and at most one Expansion Hub. `HardwareNames` keeps the count in its header | R503, R701 |
| GR13 | The HuskyLens (I2C) looks up at the AprilTags under the CELLS. It MUST be read no more often than `CameraConfig.SAMPLE_MS`. Its learned IDs map to CELLS in `CameraConfig`. It is used for "which CELL is up" and the aim offset; it does not set the pose | 9.9 |

## Driving (R1, R2)

The robot MUST always drive field-centric: pushing the stick away from the drivers moves the robot away from them, whichever way it faces. Gamepad 1 controls driving and nothing else.

**Field-centric rules**

- Field-centric MUST be the normal mode. DecodeBot's `DriveConfig.ROBOT_CENTRIC` switch is removed. Robot-centric is used only as a safeguard fallback (Pinpoint not `READY`, or a heading jump) or when the driver presses X, and every switch is written to the match log (see Troubleshooting C).
- In field-centric mode, `Drive.driveWithSticks(forward, right, clockwise)` MUST call Pedro's `ManualDrive.fieldCentric(powers, heading, offset)` every loop. The heading comes from the Pinpoint through the Pedro follower. In robot-centric mode (the fallback, or X) the stick powers go to the follower without the rotation.
- `offset` is the alliance's driver-facing angle. It comes from `DriveConfig.BLUE_DRIVER_HEADING_DEG` and `DriveConfig.RED_DRIVER_HEADING_DEG`, picked by the alliance chosen in the INIT menu (kept in `MatchInfo`; BLUE until chosen). This means "away from the drivers" is correct on both alliances without a heading reset. In our field frame (the field image as drawn, origin bottom-left, red ALLIANCE AREA on the left wall) red drivers face 0° and blue drivers face 180°. TODO(BioBuzz): confirm on the field.
- **Pose hand-off:** auto saves its Pedro pose in `helpers/PoseMemory.java` every loop and in `stop()`. TeleOp `start()` MUST start from that pose, and the pose is then **known**. If no auto ran in this app session, TeleOp starts as **S1**: it assumes the robot faces away from the drivers (heading only, pose **not** known) and shows `"No auto pose: S1 only. Press Y facing away from drivers"`.
- **Heading reset:** gamepad 1 **Y** sets "the way the robot faces now = away from the drivers". This fixes drift during a match and turns field-centric back on after a fallback. Each reset MUST be written to the match log.
- Stick input passes through a deadband (`DriveConfig.STICK_DEADZONE`, applied in `GamepadInput`), then goes to Pedro at full scale (no speed multiplier).
- Speed and acceleration limits come from Pedro's tuning parameters in `pedro/Constants.java` (`ForesightConfig`: `maxPathSpeed`, `maxVelocityConstraint`, `maxAccelerationConstraint`, `maxDecelerationConstraint`, `maxBrakingPower`), not from team code. In Pedro 3.0.1 these apply only while following a path or holding a pose. In manual driving (`Follower.manual`) the stick powers go straight to the motors, so teleop driving is at full power. Drive brakes when the sticks are released (`MecanumConfig.manualBrakeMode`, default on).

**Three stages of driving.** The layers below the OpMode are the same in every stage.

| Stage | Who drives | Where | Needs |
| --- | --- | --- | --- |
| S1 Manual | The driver, field-centric | TeleOp | The heading only |
| S2 Assisted | The driver, plus buttons that drive one short fixed path (to the launch spot for the upward CELL, to our LOADING ZONE), then hold | Same TeleOp | The full pose |
| S3 Autonomous | The robot | `auto/BioBuzzAuto.java` | The start pose |

- An assist is one button, one fixed destination in `FieldConfig`, and one straight line or curve from where the robot is now, built by `game/FieldPaths`. **Moving any stick takes the robot back in the same loop**, even in the middle of a path. No mode or toggle is needed.
- An assist is refused with a warning (and the event `ASSIST_REFUSED`) while the pose is not known: no auto pose, or after a Pinpoint fault. "Set pose here" (D-pad left, held 1 s, robot backed into our LOADING ZONE corner) makes the pose known again. TODO(BioBuzz): confirm this button (review Q4).
- `Drive` has no states. It has driver operations (`driveWithSticks`, `driveWithSticksAimedAt`, `resetHeading`, `switchDriveMode`) and path operations (`followPath`, `isDrivingItself`, `isPathDone`, `holdHere`, `stopDrivingItself`). The OpMode decides which to use.

**Gamepad 1 map** (only the driver's controls; changes go in `teleop/` and the cheat sheet)

| Control | Action |
| --- | --- |
| Left stick | Move (field-centric) |
| Right stick X | Turn |
| Y | Reset field heading ("facing away from the drivers"); field-centric back on |
| X | Switch field-centric / robot-centric by hand |
| A | S2: drive to the launch spot for the upward CELL |
| B | S2: drive to our LOADING ZONE (park, or pick up NECTAR) |
| Right bumper (hold) | Aim: keep facing the upward CELL while driving (Pedro `ManualDrive.headingLock`) |
| Right stick button (robot against a wall) | Snap heading to the nearest 90° (see Troubleshooting C) |
| Left stick button (hold 1 s, robot still) | Recalibrate the gyro, keeping the heading |
| D-pad left (hold 1 s, robot in our LOADING ZONE corner) | Set pose here (pose known again) |
| D-pad (INIT only) | The INIT menu: up/down picks a row, left/right changes it (see Autonomous) |
| Back / Share | Mark a moment in the match log (`"DRIVER MARK"`), for post-match analysis |

## Operator controls (R2)

Gamepad 2 runs every mechanism, and only gamepad 2 does. The button map stays in one method, `handleOperator()` in `teleop/BioBuzzTeleOp.java`. A student can change a button there without opening a mechanism.

**Rules**

- Only OpModes (teleop, auto, test and Field Check OpModes) read `gamepad1` or `gamepad2`, always through `helpers/GamepadInput`. Only OpModes rumble a gamepad or set its LED. `Robot`, state machines, mechanisms and the other helpers never touch a gamepad; `InitMenu` is given the button presses by its OpMode. Autonomous reuses `Robot` unchanged because of this.
- For matches, `teleop/BioBuzzTeleOp.java` holds both gamepad maps: gamepad 1 in `handleDriver()`, gamepad 2 in `handleOperator()`.
- Each loop, a button is read once into a local `boolean` and then used. This is the DecodeBot `handleOperator()` pattern, and it exists because button reads are edge-detected.
- Every gamepad 2 button calls one `Robot` method. Logic like "only launch when up to speed" lives in the state machine, which calls `log.warn(...)` when it refuses a command.
- No button may command both driving and mechanisms (R2). The driver drives to the launch spot; the operator launches.
- One button MUST always be the **stop-all** button. It is D-pad down, as in DecodeBot, and it calls `robot.stopMechanisms()`.
- Analog controls (triggers, sticks) MAY be used for proportional mechanisms, through a deadband in `ControlsConfig`.
- Rumble feedback SHOULD tell the drive team about events they can't see well: an urgent ALERT (Robot keeps it; the OpMode rumbles), "storage full", "launcher timed out", 1:00 and 0:20 left. Each rumble event MUST be named in `ControlsConfig`.
- The controls cheat sheet MUST be in three places, kept the same by hand: the teleop file's header comment, the `TelemetryConfig.GAMEPAD_HELP` lines, and `docs/CONTROLS.md`, which has a printable diagram for the drive team.

**Gamepad 2 map.** TODO(BioBuzz): confirm once the robot is built.

| Control | Action |
| --- | --- |
| A | Start collecting (`robot.startCollecting()`; stops by itself at 4) |
| B (hold) | Reverse the intake, to spit out (`robot.reverseIntake()`) |
| Right trigger | Launch everything held (`robot.startLaunching(4)`) |
| Y | Spin the launcher up early (`robot.spinUpLauncher()`) |
| Left bumper | "Our HIVE just TIPPED" (`robot.markHiveTipped()`) |
| D-pad down | Stop all mechanisms (fixed) |
| Back / Share | Mark a moment in the match log (`"OPERATOR MARK"`) |

## Autonomous

Autonomous (stage S3) is a step list that drives the same `Robot` as teleop. It uses the same section layout as a state machine, so students learn the pattern once.

**Structure**

- `auto/BioBuzzAuto.java` holds `enum Step` and a `switch` in `runStep()`. Each step does three things: on its first loop it starts something (`robot.drive.followPath(...)`, `robot.startLaunching(4)`); then it checks whether that thing is done (`robot.drive.isPathDone()`, `!robot.isLaunching()`); then it checks its own timeout from `AutoConfig`.
- Each loop does the park-by guard (GR2), `runStep()`, `robot.update()`, `PoseMemory.save(...)`, then telemetry and the match log.
- A step that times out writes `"TIMEOUT <step>"` to the match log and moves on, so a jam does not waste the whole autonomous. A drive step that times out calls `drive.holdHere()` first.
- Auto `init()` resets `HiveState` and `MatchInfo`'s per-match values and may grip the preloads (GR3). Nothing else moves before PLAY.

**Paths and positions**

- `game/FieldPaths.java` builds every path, for Auto and for the TeleOp assists. Pedro's path API (`Paths.line`, `Paths.curve`, `PoseFactory`) appears only in this file and in `drive/Drive.java`.
- Positions live in `configurations/FieldConfig.java` in inches and degrees, written for the BLUE alliance. `FieldPaths.forAlliance(pose, isRed)` turns them for RED (GR10).
- The routine's paths are built in `init_loop()` whenever the menu changes, drawn on the Dashboard field, and checked against GR8.
- `robot.drive.setStartingPose(...)` is called in `start()`, from the chosen alliance and start position. The start pose MUST match where the robot is physically placed, which is the main cause of autonomous errors.

**Pre-match selection**

- During INIT, gamepad 1 uses one menu, `helpers/InitMenu.java`, instead of a button per setting. D-pad up/down moves between rows; D-pad left/right changes the value in that row, wrapping round at the ends. The OpMode reads the D-pad and passes the presses to the menu.
- Auto's INIT rows: **Alliance** (BLUE / RED), **Start** position, **Routine**, **Delay** (0 to 10 s, for alliance partner coordination), **Match type** (P / Q / E), **Match number**, **Battery** label (A to H) and **Logging** (on / off). A routine is one value of an `enum Routine`. TODO(BioBuzz): the first routines (review Q10), for example NEAR (launch 4, collect our GARDEN, launch, park) and FAR (launch 4, park).
- TeleOp's INIT shows the rows it needs (alliance, match type and number, battery, logging), starting from what was entered in Auto's INIT (`MatchInfo`).
- The choices show on telemetry and are written to the match log header.
- The `AutoConfig` and `LogConfig` values are the defaults, so the robot still works if nobody presses anything.

**Vision** (HuskyLens, GR13). `mechanisms/Camera.java` answers questions only: is a learned tag in view, how far it is from the image centre, its size. Robot decides whether to use an answer. The camera does not correct the Pedro pose in this version (see `docs/LLD.md`, Future improvements). `docs/CAMERA_GUIDE.md` explains mounting and training the HuskyLens on the CELL tags.

## Telemetry and dashboard (R6)

Every OpMode MUST send the same telemetry to the Driver Station and FTC Dashboard through `DashboardTelemetry.create(telemetry)`. `t.update()` is called once per loop. The Driver Station screen stays short enough to read during a match; detail goes to the Dashboard.

**Required content**

| Group | Lines | Switch in `TelemetryConfig` |
| --- | --- | --- |
| Match header (always first) | Alliance, routine, match time left, loop time (ms), battery (V), FIELD/ROBOT and heading, elements held and their colours, the upward CELL, TIPS, any active **ALERT** | Always on |
| Each state machine | State and seconds in it, running, the last warning (shown 4 s) | One switch per state machine |
| Each mechanism | Its key values, or `FAULTED: <reason>` | One switch per mechanism |
| State history | Last change, its reason and age, plus the 4 changes before it (`StateLog`) | `HISTORY` |
| Mechanism details | Motor powers, velocities, currents, servo positions, sensor readings | `DETAILS` (off by default) |
| Drive pose | x, y, heading (deg), whether the pose is known (S2 allowed), and where it came from | `DRIVE` |
| Controls cheat sheet | One line per gamepad | `GAMEPAD_HELP` |

**Dashboard**

- Every `*Config` class MUST be `@Config` with `public static` (not `final`) fields, so values can be changed live at `http://192.168.43.1:8080/dash`. Each config file's header MUST say that live edits are lost on restart and good values must be typed back into the file.
- The Dashboard field view MUST draw the robot pose, the current path (auto and assists) and the upward CELL, through `helpers/FieldDrawing.java` (the Dashboard canvas). Pedro 3's tuning package has no drawing helper to reuse.
- The field view SHOULD use the BioBuzz field as its background, from `TeamCode/src/main/assets/images/field-biobuzz.png` (the Dashboard serves that folder at `/images/`; it does not serve other asset folders), so poses and paths can be checked against field elements. How Dashboard 0.6 loads a custom background is checked during implementation and written up in the LLD.
- The Dashboard graph SHOULD plot the key numbers: flywheel (or similar) target vs. actual velocity, loop time, and battery voltage. Add them with `TelemetryPacket.put(...)` in `helpers/` and in no other folder.

**Performance:** telemetry MUST NOT push the loop time over its budget (see Troubleshooting). Driver Station telemetry SHOULD be sent no more than every 100 ms (`telemetry.setMsTransmissionInterval`).

## Troubleshooting (R7, R8, R9)

Troubleshooting is one loop with four stages: check the robot, detect and recover during the match, record every run, and analyse it afterwards so the next check is better. Each tool reports in plain words on telemetry and writes to the match log. A student SHOULD NOT need a laptop to find out what went wrong during a match; the laptop is for the analysis afterwards.

| Stage | When | Who | Tool |
| --- | --- | --- | --- |
| 1a. New field | First practice-field slot at each event, and after a big repair | Pit crew + a driver | `fieldchecks/` OpModes (A) |
| 1b. Before every match | In the pits, before queuing | Pit crew | `testing/RobotCheck.java` + `docs/MATCH_DAY.md` checklist (B) |
| 2. Detect and recover | During the match | Driver + operator | `helpers/LoopMonitor`, `helpers/HealthMonitor`, heading safeguards, gamepad 1 recovery buttons (C) |
| 3. Record | Always, when logging is on | Nobody: automatic | `helpers/MatchLogger` + `helpers/BatteryMonitor` (D, E) |
| 4. Analyse | Back in the pits, about 5 minutes per match | Data lead + drive team | `tools/plot_match.py`, `Log Viewer`, match sheet (F) |

Each analysis can add a new check, a new alert or a fix, which feeds back into stage 1.

### A. Confirming tuning on a new field (R7)

A new field is a new robot: tile grip, tile wear and walls differ from our practice field, so braking distance and path accuracy can change. The Field Checks MUST re-measure what the field can change, confirm that the robot-only calibration survived the trip, and compare every result with our **home baseline**. The full set MUST fit in two short practice-field slots (about 5 minutes each).

**One folder, one OpMode per test.** The Field Checks live in `fieldchecks/`, group `"Field Checks"` on the Driver Station, numbered in running order. A failed test can be re-run on its own instead of repeating the whole set. Shared work (field name, baseline compare, PASS/WARN/FAIL, results kept between OpModes, telemetry) lives in `helpers/FieldCheckHelper.java`. Baseline values and tolerances live in `configurations/FieldCheckConfig.java`.

| # | OpMode | Robot does | PASS when (starting values) | Field-dependent? |
| --- | --- | --- | --- | --- |
| 01 | `FieldCheck01_Hardware` | Nothing | Every device in `HardwareNames.ALL_DEVICES` is found (`hardwareMap.tryGet`, so every missing one is listed), both hubs answer, Pinpoint is `READY`, the HuskyLens answers | No |
| 02 | `FieldCheck02_Battery` | Nothing, 2 s | Resting voltage at least 12.5 V. Also compared with the baseline battery voltage: more than 0.5 V apart marks tests 07 to 09 "battery differs from baseline" (at most WARN) | No, but it changes 07 to 09 |
| 03 | `FieldCheck03_Still` | Nothing, 10 s, hands off | Heading creeps no more than 0.5°; Pinpoint stays `READY` | No |
| 04 | `FieldCheck04_Push` | Crew pushes it 2 tiles (48 in) along a seam, then 2 tiles sideways | Distance within ±0.75 in, sideways drift within ±0.5 in | Mostly no |
| 05 | `FieldCheck05_Spin` | Crew turns it 4 full turns by hand, back to the seam | Heading error within ±2° | No |
| 06 | `FieldCheck06_FieldCentric` | Robot turned 90°; driver pushes the stick away from the drivers | Driver confirms it moved away: A = yes, B = no | No |
| 07 | `FieldCheck07_Brake` | Full power to a set speed, then coast; forward, then sideways | Deceleration within 15% of `naturalForwardDeceleration` / `naturalStrafeDeceleration` in `Constants` and the baseline | **Yes** |
| 08 | `FieldCheck08_Line` | Pedro straight line, 48 in, stop on a seam | End error within the baseline + 0.75 in; overshoot and settle time shown | **Yes** |
| 09 | `FieldCheck09_Square` | Pedro 48 in square, back to start | End error under 1.5 in and 3° | **Yes** |
| 10 | `FieldCheck10_Wall` | Robot squared against a wall | Heading within 3° of the nearest 90°; then snap-to-90° is tried | Partly |
| 11 | `FieldCheck11_Mechanisms` | One bench check per mechanism, through Robot methods (for example a flywheel reaches its target velocity within its spin-up time) | Per mechanism. TODO(BioBuzz): define one check per mechanism | No |
| 12 | `FieldCheck12_Camera` | Only if Vision is used: robot on a seam crossing in view of a tag | Camera position within ±2 in of the Pinpoint. Event lighting is a common cause of failure | **Yes** |
| 13 | `FieldCheck13_Summary` | Nothing | Shows every result, plus battery sag (resting from 02, lowest during 07 to 09) | – |

**Rules**

- Positions MUST be checked against **tile seams** (every 24 in) and walls, never tape: we may not mark event fields.
- Test 07 MUST use the same method as Pedro's `ForwardDeceleration` and `StrafeDeceleration` tuners (power 1 until the target speed, then coast with `drivetrain.stop(false)`, average the slow-down until speed is under 1 in/s), so its numbers compare directly with `Constants`. Those tuners are internal to the Quickstart's `procedures/ForesightTuner.java` and cannot be called from team code, so `FieldCheck07_Brake` carries its own copy of the method, with a comment naming the tuner it copies. It compares with `naturalForwardDeceleration` / `naturalStrafeDeceleration` in `Constants.foresightConfig`, which exists once Constants is filled in at tuning. On a WARN or FAIL the screen prints the measured value to use for this event.
- Paths in 08 and 09 start from wherever the robot is placed, so they work anywhere on the field.
- Results: **PASS** within tolerance, **WARN** between 1× and 2× tolerance (look at it), **FAIL** beyond 2× or an absolute limit broken (fix before matches). Each result is kept in a `static` field of `FieldCheckHelper` for the Summary, and logged as a `FIELDCHECK_<nn>` event so nothing is lost if the app restarts.
- The field name (`home`, `event-practice-1`, …) is chosen in the first check's INIT menu and offered again in the next ones. Every Field Check writes a match log with `FIELD` in place of the match id.
- **Baseline:** the same tests run at home, and the values printed by the Summary are copied into `FieldCheckConfig` with the date. They MUST be re-recorded after any re-tune or hardware change.
- A FAIL tells the student which tuning procedure to run next. That procedure lives in the Quickstart's `pedro/procedures/` and is written up in `docs/TUNING.md`. The first time the robot runs on a field, it MUST pass every test.

| Outcome | Action before qualification matches |
| --- | --- |
| 02 FAIL | Charge or swap the battery and start again. Don't judge a field on a weak battery |
| 03, 04 or 05 FAIL | Hardware damaged in travel (pod, Pinpoint mount, cable): fix before anything else |
| 07 WARN or FAIL | Use the measured deceleration for this event, then re-run 08 |
| 08 or 09 FAIL | Adjust auto end poses for this field, or re-run Pedro's tuners for the failing direction |
| 07 to 09 "battery differs" | Re-run with a battery closer to the baseline voltage before changing any tuning |
| 12 FAIL | Re-check the camera mount numbers before trusting AprilTag correction |

### B. Before every match: Robot Check

`testing/RobotCheck.java` (group `"Tests"`) MUST take about one minute in the pits and end with **READY FOR MATCH** only if every step passes. A = next step, B = repeat. It writes a match log like any OpMode.

| Step | Robot | Measures | PASS when (starting values) |
| --- | --- | --- | --- |
| 1. Battery | Still | Resting voltage over 2 s | At least 12.5 V |
| 2. Pinpoint | Still | Status and update rate (`getFrequency()`) | `READY`; rate steady near the value noted on the first run |
| 3. Gyro still-test | Still, 10 s | Heading change | No more than 1° |
| 4. Drive motors | Clear around the robot | Each motor at 0.3 power for 0.5 s, one at a time, taken straight from `hardwareMap` (the Pedro follower is not built); the screen names the motor | Crew confirms each wheel turns the right way |
| 5. Logger | – | `LogConfig.ENABLED`, free storage | On, and more than 50 MB free |

Things code cannot see (connectors seated, Pinpoint mounted firmly, battery strapped down, battery label noted) stay on the paper checklist in `docs/MATCH_DAY.md`.

### C. Match-time issues: detect and recover (R8)

Two helpers watch the robot every loop. `helpers/LoopMonitor.java` tracks loop timing and `helpers/HealthMonitor.java` checks the hardware, the heading and the battery. They MUST raise an **ALERT**, which goes on the telemetry header line, in the match log, and, for urgent alerts, as a gamepad rumble done by the OpMode (`robot.hasNewUrgentAlert()`). They MUST keep the robot running instead of crashing it.

| Problem | How it is detected | What the robot does |
| --- | --- | --- |
| Slow loop | Loop time averaged over 1 s is above 25 ms, or any single loop takes over 100 ms | ALERT with the slowest mechanism or state machine if known. Bulk reads are off for now; turning them on is the first fix to try (LLD, Future improvements) |
| Low battery | Below 12.0 V at INIT, or below 9.0 V for over 0.5 s during play | ALERT, rumble once. A low reading at INIT does not stop the OpMode; it is shown in red |
| Pinpoint not `READY` | Status is anything but `READY` (a fault, or it restarted after a power blip) | Drive switches to **robot-centric**, the pose is no longer known (assists refused), rumble, ALERT. Driving is never stopped for this |
| Heading jump | The heading changes more than `MAX_TURN_DEG_PER_S` × loop time + 10° in one loop (720°/s is faster than any FTC robot turns), measured the short way round | Drive switches to **robot-centric**, rumble, ALERT, log the jump |
| Pose jump | The pose moves more than 6 in within one loop | ALERT and log the jump |
| Device disconnected | A hub stops answering, or a mechanism device read throws an error | That mechanism is marked FAULTED (`Mechanism.runUpdate()`) and its outputs are set to zero. ALERT names it. The rest of the robot keeps working; a state machine that needs it stops with a warning |
| Mechanism stuck | A state machine's timeout fires, or a motor is commanded above 0.3 power while its velocity stays near zero for 500 ms | Go to a safe state and `log.warn(...)`. ALERT and a rumble on gamepad 2 |
| Gamepad disconnected | The gamepad's timestamp does not change for 1 s | ALERT. Drive powers are set to zero and any assist path is stopped until input returns |
| Too many elements | Storage counts more than 4 | Intake reverses for `REJECT_MS`, urgent ALERT (G407) |
| Robot restarted mid-match | `PoseMemory` is empty in teleop | TeleOp runs as S1 (assists refused): warning and instructions for a heading reset and for "set pose here" (see Driving) |

**Heading safeguards.** Field-centric driving is only as good as the heading. The checks above catch **sudden** problems (Pinpoint reset, fault, accidental reset). A **slow** creep from a bad gyro calibration is prevented at INIT instead:

| Seen in INIT | Screen | Action |
| --- | --- | --- |
| Pinpoint `CALIBRATING` | "CALIBRATING - DON'T TOUCH THE ROBOT" | Wait |
| `READY`, but turning faster than 1°/s | "ROBOT MOVING - hands off" | After 1 s still, `recalibrateIMU()` again |
| `READY`, still, heading changed more than 0.5° in 5 s | "Gyro drifting - recalibrated" | `recalibrateIMU()`, start the 5 s again |
| `READY`, still, steady | "Ready" | Record the resting battery voltage |

**Recovery buttons (gamepad 1).** The driver owns heading recovery, so gamepad 2 stays on mechanisms (R2). These are added to the Gamepad 1 map in Driving:

| Button | Action |
| --- | --- |
| Y | "The robot now faces away from the drivers": heading reset, and field-centric back on |
| Right stick button (robot flat against a wall) | **Snap heading to the nearest 90°**. Refused with an ALERT if more than 20° from a multiple of 90° (the robot is not square to the wall) |
| Hold left stick button 1 s (robot still) | **Recalibrate the gyro** (`recalibrateIMU()`, about 0.25 s), keeping the heading. Refused while moving. The Pinpoint `CALIBRATING` status is ignored for 500 ms afterwards |
| X | Switch field-centric / robot-centric by hand, if the heading can't be trusted and nothing else works |

**Signals to the drive team.** Telemetry header shows `FIELD` or `ROBOT` drive mode, the heading (−180° to 180°, so "away" reads near 0 instead of flickering between 0 and 360), Pinpoint status and the battery. Urgent ALERTs rumble the gamepad. On PS4/PS5 gamepads the LED (`Gamepad.setLedColor`) shows green for field-centric trusted, orange for the robot-centric fallback, red for low battery.

**Rules**

- Mechanisms MUST get their devices in their constructor with the plain SDK call, `hardwareMap.get(DcMotorEx.class, HardwareNames.MOTOR_INTAKE)`, so a missing device crashes at INIT with the SDK's message (`Unable to find a hardware device with name "intake" and type DcMotorEx`) and problems are found before the match. There is no team wrapper around `hardwareMap.get`. After PLAY, nothing MUST throw: errors in a mechanism are caught by `Mechanism.runUpdate()` (FAULTED), and other faults are caught in `helpers/` and turned into ALERTs.
- Every safeguard trip and every recovery press MUST be written to the match log as an EVENT.
- Thresholds live in `configurations/HealthConfig.java`. The values above are starting values, to be set from our own logs.
- `docs/MATCH_DAY.md` MUST give a one-page pre-match checklist (battery and its label, Pinpoint light, alliance and routine chosen, start pose placed, wires) and a between-match checklist, and MUST teach the drive team the recovery buttons.

### D. Battery voltage as a signal

Battery voltage is a likely hidden cause of match-only problems: a dip can reset sensors, slow motors and change how paths are followed. It MUST be logged in every DATA row and drawn on the **same time axis** as heading, loop time, drive effort and Pinpoint status, so a cause and its effect line up.

`helpers/BatteryMonitor.java`:

- Reads every hub's `VoltageSensor` (`hardwareMap.voltageSensor`) and keeps the **lowest**, because a dip on either hub matters.
- Each read is a separate hub command, so it MUST be sampled every `HealthConfig.BATTERY_SAMPLE_MS` (100 ms), not every loop.
- Keeps the **resting voltage** (averaged during INIT with the robot still), the **lowest since PLAY**, and the lowest since the last log row, so short dips are never missed at a 20 Hz log rate.
- Is shared by `HealthMonitor` (low-battery ALERT), `MatchLogger`, `RobotCheck` and the Field Checks.

| Question | What the log shows |
| --- | --- |
| Did the Pinpoint reset because of a voltage dip? | Pinpoint status change or heading jump at the same moment as a deep dip |
| Is this battery worn out? | Large sag (resting minus lowest) for normal driving, compared across matches by battery label |
| Why did the auto end short? | Lower voltage, so less motor power |
| Does one action draw too much? | Dips line up with a mechanism event |

Every battery MUST carry a label (A to H). The label is chosen in INIT and written in the log header, so battery health can be tracked across matches. The thresholds (12.5 V resting to start a match, 12.0 V INIT warning, 9.0 V play warning, sag limit) are starting values and MUST be set from about 10 logged runs.

### E. Match logs for post-match analysis (R9)

`helpers/MatchLogger.java` MUST record every OpMode run to a CSV file on the Control Hub. A mentor or student can then rebuild what happened without having watched the robot, down to "what happened 2 s into the match".

**File**

- Folder: `/sdcard/FIRST/data/biobuzz-logs/`. **Not** `/sdcard/FIRST/matchlogs/`: the SDK writes its own match logs there (`AppUtil.MATCH_LOG_FOLDER`), and ours must not mix with them.
- Name: `<match>_<OpMode>_<ALLIANCE>_<yyyy-MM-dd_HH-mm-ss>.csv`, for example `Q12_BioBuzzAuto_RED_2026-10-18_14-32-05.csv`. The match id comes first so one match's Auto and TeleOp files sort together. `<match>` is `Q12`, `P03` (practice), `E2`, or `NA` if none was entered; Field Checks use `FIELD` and test OpModes `TEST`. Two files in the same second get `_2`, `_3`.
- Header block (`#` lines): OpMode, alliance, routine, match id, battery label, field name, battery resting voltage at INIT, `started_epoch_ms` (Control Hub clock at PLAY, to the millisecond), and a snapshot of every `*Config` value in use.
- Footer: `# END rows=… dropped=… min_battery_v=… worst_loop_ms=…`, written only by a clean stop. **A file without `# END` means the OpMode crashed or the robot lost power.**

**Match id and battery label in INIT.** These are rows of the INIT menu (Match type, Match number, Battery; see Autonomous, Pre-match selection). They are kept in `static` fields of `helpers/MatchInfo.java` (the same idea as `PoseMemory`), so TeleOp's INIT starts with what was entered in Auto's INIT. The drive team only confirms them during the transition.

**Match time.** Each OpMode's `t_s` restarts at 0 at PLAY, and Auto and TeleOp are separate files with a transition of varying length between them. The plot tool joins one match's files using `started_epoch_ms`: `match_s = t_s + (file start − Auto start) / 1000`, so `match_s` is 0 at Auto PLAY and TeleOp continues from the real moment it started. Only the difference between two clock readings on the same hub is used, so a wrong date on the hub does not matter.

**Rows:** two kinds in one file, told apart by a `type` column.

- `DATA` rows at `LogConfig.RATE_HZ` (default 20 Hz). Standard columns in every file, so one plot tool reads them all:

| Column | Unit | Notes |
| --- | --- | --- |
| `t_s` | s | Since PLAY of this OpMode |
| `loop_ms`, `worst_loop_ms` | ms | Latest loop, and the worst since the last row (so spikes are not missed at 20 Hz) |
| `battery_v`, `battery_min_v` | V | Latest sample, and the lowest since the last row (D) |
| `x_in`, `y_in`, `heading_deg`, `heading_vel_dps` | in, °, °/s | Pedro pose; heading −180 to 180 |
| `pinpoint_status` | code | Enum order: 0 `NOT_READY`, 1 `READY`, 2 `CALIBRATING`, 3 to 7 faults |
| `follower_mode` | code | 0 `FOLLOW`, 1 `HOLD`, 2 `MANUAL`, 3 `IDLE` |
| `stick_forward`, `stick_right`, `stick_turn` | −1 to 1 | Driver's stick values after deadband |
| `drive_mode` | code | 1 field-centric, 0 robot-centric |
| `<statemachine>_state` | code | Each state machine's state |
| `<mechanism>_faulted`, mechanism values | – | Each mechanism's fault flag and up to 8 key values, chosen in its `addLogValues()` |
| `held_count`, `next_element`, `launcher_rpm`, `launcher_target_rpm`, `up_cell`, `tip_count`, `pose_known`, `match_s_left` | –, code, RPM, code, count, 0/1, s | Game values (see `docs/LLD.md`) |

- `EVENT` rows whenever something happens: `PLAY` and `STOP`; every state change and its reason (logged automatically from `StateLog.record`); every `log.warn`; every ALERT; every safeguard trip (`SAFEGUARD_PINPOINT_<STATUS>`, `SAFEGUARD_HEADING_JUMP:<deg>`, `SAFEGUARD_LOW_BATTERY:<V>`); every recovery (`RECOVER_FACE_AWAY`, `RECOVER_SNAP90:<from>-><to>`, `RECOVER_RECALIBRATE`, `RECOVER_ROBOT_CENTRIC`, `RECOVER_FIELD_CENTRIC`, `POSE_SET:<how>`); game events (`HIVE_TIP:<n>`, `REJECT_OPPONENT_NECTAR`, `OVER_LIMIT:<n>`, `ASSIST:<name>`, `ASSIST_REFUSED`, `ASSIST_CANCELLED`, `PARK_BY`); `FAULT:<mechanism>:<reason>`; button presses that send commands; auto step changes, paths and timeouts; Field Check results (`FIELDCHECK_<nn>:<result>`); `LOGGER_DROPPED:<n>`; and driver or operator MARKs.

**Cost and safety**

- Logging MUST NOT add more than 2 ms to the loop. The loop only copies numbers into a reused row and hands it to a queue. A background writer thread turns rows into text (no `String.format` per row) and writes them through a buffered writer, flushing at most every 250 ms and in `stop()`. The loop never waits for storage: if the queue (about 10 s of rows) is full, the row is dropped and counted.
- A file error turns logging off with one ALERT and never stops the robot.
- Storage: roughly 3 KB/s at 20 Hz, under 1 MB per match.

**On/off and retention**

- `LogConfig.ENABLED` is the default and is what runs at competition. It can be changed in Dashboard during practice, and the INIT menu's Logging row changes it for that run. The choice is read once, at PLAY, so a file is never half-written. When off, every logger call returns at once: no file, no thread.
- Keep the newest 100 files and delete older ones at INIT.

### F. After the match: analysis and debrief

**On the Driver Station.** `testing/LogViewer.java` lists the last 10 runs. Each shows its match id, duration, alert count, worst loop time, lowest battery, number of timeouts, and whether it ended cleanly (`# END`).

**On a laptop.** A mentor-side `tools/plot_match.py` (Python, pandas + matplotlib) MAY be used. It is not student code.

- `plot_match.py <file>` draws stacked graphs on one shared time axis, with EVENTs as vertical lines through every graph: heading (shaded by field or robot-centric), **battery** (resting voltage and the warning level as lines), drive effort, loop time, Pinpoint status, state machine states, held elements and launcher speed, and a field view of the path coloured by battery voltage.
- `--match Q12` joins that match's Auto and TeleOp files on match time and shades Auto, transition and TeleOp.
- `--at 2.0` (or `--at 1:45`) prints the rows and events from 1 s before to 1 s after that match time.
- `--compare` overlays the battery voltage of several files, labelled by battery, to show which batteries sag most.
- `--fieldcheck` builds the Field Check table (one row per field, one column per test) from the `FIELDCHECK_*` events.
- A printed summary gives duration, rows, dropped rows, clean end, resting and lowest voltage, worst loop time, and for every `SAFEGUARD_*` event the battery voltage at that moment and the lowest in the 2 s before it: the quickest test of "was it the battery?".

**Debrief, about 5 minutes per match**

1. Pull the files with `adb pull /sdcard/FIRST/data/biobuzz-logs/` or Android Studio's Device Explorer.
2. Run `plot_match.py --match <id>`.
3. Ask the drive team what felt wrong, and when. Find it on the plot and in the summary.
4. Fill one row of the match sheet: match, field, battery label, resting and lowest voltage, clean end, symptom (in the drive team's words), evidence (time, plot), cause, fix.
5. Anything new goes into the symptom table in `docs/DEBUGGING.md`.

`docs/MATCH_LOGS.md` explains how to copy logs (with `adb pull` or Android Studio), open them in a spreadsheet or the plot tool, and keep the match sheet. Match video from a phone or the event stream can be lined up with the log using the `PLAY` event time.

| Symptom | Likely causes | Evidence in the log | Fix |
| --- | --- | --- | --- |
| Field-centric suddenly rotated | Pinpoint reset, accidental Y, heading jump | Heading jumps; Pinpoint status change; `RECOVER_*` event | Secure the Pinpoint cable; check for a voltage dip at the same moment; snap to 90° |
| Field-centric slowly rotated | Bad gyro calibration at INIT, Pinpoint mounting | Heading creeps while still; worse after many turns | Hands off at INIT; recalibrate; remount |
| Sluggish late in the match | Battery sag | Voltage trending down, deep dips under load | Fresh battery; retire that label |
| Auto fine at home, off at the event | Different tile grip, so different braking | Field Check 07 to 09 vs baseline | Use the measured deceleration; adjust end poses |
| Laggy or jerky drive | Slow loop | `worst_loop_ms` spikes; dropped rows | Remove slow code; fewer hardware reads; bulk reads (LLD, Future improvements) |
| Launches fall short or long | Wrong upward CELL, wrong distance, low battery | `up_cell`, pose, `launcher_rpm` vs target, `battery_v` at each launch | Press TIP when the HIVE tips; retune the speed table; fresh battery |
| Log ends early | Crash, power loss, disconnect | No `# END`; last voltage reading | Robot Controller log for the crash; check power connectors |

### G. TODO: Match Simulator

Not designed yet; recorded so it is not lost. A `testing/MatchSimulator.java` OpMode SHOULD keep the robot busy for a full match length (an auto-like part, the transition pause, then a teleop-like part) the way a real match would: Pedro paths to random safe positions, turns, sprints and stops, and the mechanisms' game actions through Robot methods, with the match logger on. It reuses `MatchLogger`, `BatteryMonitor`, `HealthMonitor` and the plot tool unchanged.

What it is for: battery behaviour under a realistic load without a real match; comparing batteries by label; reproducing match-only problems at home (Pinpoint resets under deep dips, heading jumps after hard turns, slow loops, hot motors); setting real voltage thresholds from data; and soak tests after repairs.

Questions for its design:

- [ ] Safe random positions: a list of allowed zones away from walls and field elements, and a seed so a run can be repeated exactly
- [ ] Load profiles: gentle, typical (taken from our own match logs) and worst case
- [ ] One game action per mechanism that the simulator can call. TODO(BioBuzz): once the mechanisms exist
- [ ] Safety: clear field, STOP always works, mechanism limits respected, a max-speed setting
- [ ] Results: a match log plus a summary of lowest voltage, sag, worst loop time and safeguard trips

## Testing and tuning

Each mechanism and each state machine MUST have its own test OpMode, so a student can check one thing without the full teleop. Drive tuning uses the Quickstart's own tuners in `teamcode/pedro/`, unchanged.

**Test OpModes** (`testing/`, group `"Tests"`)

| OpMode | Builds | Lets a student |
| --- | --- | --- |
| `Test: Drive` | `new Drive(hardwareMap)` only | Drive field-centric, see the pose, reset the heading, try the assists |
| `Test: <Mechanism>` (one each) | That mechanism only (`new Intake(hardwareMap)`) | Call each verb from a button, see every question's answer and the FAULTED flag |
| `Test: <Task>` (one per state machine) | `Robot` | Start and cancel it, see its state and history |
| `Test: Parts` | Only `hardwareMap` | Move one motor or servo from `HardwareNames.ALL_DEVICES` at a time with the D-pad, to check wiring and direction |
| `Robot Check` | Full robot | Run the one-minute check before every match (see Troubleshooting B) |
| `Field Check 01–13` (fieldchecks/, group "Field Checks") | Full robot | Run one new-field test at a time and see all results in the Summary (see Troubleshooting A) |
| `Match Simulator` (TODO) | Full robot | Run a full-length simulated match for battery and soak tests (see Troubleshooting G) |
| `Log Viewer` | Nothing | Read summaries of recent match logs |

**Rules**

- Test OpModes use the same mechanism, state machine and Drive classes as teleop. They contain no copied logic.
- Each test OpMode's header lists its buttons, and shows them on telemetry.
- Test OpModes also write match logs, with `TEST` in the file name.

**Tuning** (the Quickstart's `pedro/Tuning.java` and `pedro/procedures/`)

- Use the Quickstart's tuners where they are. Tuned values go into `pedro/Constants.java`, values only (see Base project and repository).
- `docs/TUNING.md` MUST give the order: localizer offsets and pod directions, forward and lateral ticks, then the follower and drive constants. For each step it says what is measured, which number to change, and what "good" looks like.
- Mechanism tuning (velocity PIDF, servo positions, delays) is done live through Dashboard `*Config` values. Good values are then typed back into the config file. The guide says to commit them to git with the date and the field type.

## Documentation deliverables (R4, R5)

The documents MUST be Markdown files in `docs/` in the repository, so they change in the same commits as the code. They MUST be written for a student reader: short sentences, words defined on first use, and ASCII diagrams that show in Android Studio.

### High-level design: `docs/HLD.md`

1. The game and what the robot must do: match phases and timings, the points we plan to score, the game rules the software must respect.
2. The robot: each mechanism, its job in the game, and its hardware.
3. The software layers: the diagram, what each layer does and must never do, and "where does my code go".
4. Design patterns, by name, and where each is used; patterns not used, and why.
5. The building blocks: Robot, the `Mechanism` base class, the state machines, Drive, `game/`.
6. The three driving stages (S1, S2, S3).
7. Both gamepad maps.
8. Autonomous plan: routines and step lists.
9. Vision.
10. Data flow: telemetry, match log and alerts.
11. Growing the robot during the season (summary of the LLD checklists).
12. Risks and fallbacks.

### Low-level design: `docs/LLD.md`

1. Conventions: units, the field frame, alliance turning.
2. Packages and files.
3. Robot: fields, public methods, the `update()` order and why.
4. The `Mechanism` base class: hooks and fixed methods.
5. One section per mechanism: devices, `HardwareNames` entries, verbs, questions, `update()`, config values, log values.
6. Drive: driver and path operations, pose-known rules.
7. One section per state machine: an ASCII state diagram; a table of each state with its DO, CHECK and GO and its timeout; commands; config values.
8. `game/`: Element, HiveState, FieldPaths.
9. OpModes: TeleOp structure, Auto step table (command, finish condition, timeout), test OpModes.
10. Helpers (public methods) and configurations (values and starting values).
11. How to extend: checklists to add a mechanism, a state machine, an assist and an auto step.
12. Telemetry, match log columns, EVENT types and ALERTs.
13. **Known implementation challenges**: each problem found while building, the options considered, what was chosen and why. It MUST cover at least:
    - **Pinpoint access.** Pedro's `PinpointLocalizer` keeps its `GoBildaPinpointDriver` private, but the safeguards need `getDeviceStatus()`, `getFrequency()` and `recalibrateIMU()`. One option is a second handle to the same device from `hardwareMap` in `Drive`. The LLD records what was chosen and its risks: two objects talking to one device, recalibrating while the follower reads it, and resetting the pose after a Pinpoint restart.
    - **Dashboard field background.** How the BioBuzz field image is loaded by the Dashboard canvas (from the app's assets or a URL), and its scale and origin in inches.
    - **Constants depends on team code.** `pedro/Constants.java` imports `HardwareNames`, and what that means for Quickstart merges.
    - **HuskyLens**, **counting held elements**, **which CELL is up**, **detecting a disconnected device**, **arrays in `@Config`**, **Pedro 3 path headings** and **static state between OpModes**.
14. **Future improvements**: optimizations left out on purpose to keep the code simple, each with what it gains, what it costs and when to do it. It MUST cover at least:
    - **Bulk reads.** Off for now: every sensor and encoder read is its own hub command (about 1–2 ms each, slower on the Expansion Hub). Turning on `LynxModule` bulk caching makes each hub answer all its motor, digital and analog values in one read per loop; I2C devices (Pinpoint, color sensors, HuskyLens) are never bulk-read. Options: `AUTO` (one loop in the `Robot` constructor, values can never go stale, an extra read when one value is read twice in a loop) or `MANUAL` (fastest, but the cache MUST be cleared once per loop at the top of `Robot.update()`, and a missed clear freezes sensor values with no error; it also needs a stored hub `List`, so it belongs in a small helper under R3). Do this when `LoopMonitor` shows loop times near the Slow loop threshold. Record the measured loop time before and after.

### Design review: `docs/DESIGN-REVIEW.md`

The conflicts, misalignments and game gaps found when the design changed, how each was resolved, and the open questions for the team. It is updated whenever the requirements change direction.

### Student guides

| File | Answers |
| --- | --- |
| `README.md` | Where do I start? It gives the reading order (as in DecodeBot), state machines in 60 seconds, and a vocabulary list. |
| `docs/GETTING_STARTED.md` | How do I install Android Studio, open the project, build, deploy to the robot, and pick an OpMode? |
| `docs/FIRST_CHANGES.md` | Five small exercises in order: change a speed, change a button, add a telemetry line, add a state with a timeout, add an auto step. Each exercise lists the files to open and how to check the result. |
| `docs/HOW_TO_ADD_A_MECHANISM.md` | How do I add a mechanism (copy `template/TemplateMechanism.java`, names, config, two edits in `Robot`, a Robot method, a test OpMode, buttons)? How do I add a state machine (copy `template/TemplateStateMachine.java`, build it in `Robot` with its mechanisms, a start method that cancels conflicts)? |
| `docs/AUTO_GUIDE.md` | How do I measure poses, edit `FieldConfig` and `AutoConfig`, add a step, check how RED poses are turned, and test with the robot on blocks? |
| `docs/CONTROLS.md` | What does each button do? A printable diagram of both gamepads. |
| `docs/DEBUGGING.md` | What is wrong? The 6 questions (from DecodeBot) plus ALERTs and how to read a match log. |
| `docs/TUNING.md` | What order do I tune in, and what does good look like? |
| `docs/MATCH_DAY.md` | What do I check before and between matches, and what is the field check? |
| `docs/MATCH_LOGS.md` | How do I get logs off the robot, read them, and find the cause of a bad match? |
| `docs/CAMERA_GUIDE.md` | How do I mount the HuskyLens, train it on the CELL tags, and check it with `Test: Camera`? |
| `docs/GLOSSARY.md` | What do the words mean? Mechanism, state machine, state, hook, FAULTED, pose, pose known, field-centric, assist, S1/S2/S3, loop, deadband, PIDF, and each design pattern's name. The game words (POLLEN, NECTAR, CELL, TIP, …). |
| `docs/UPDATING_PEDRO.md` | How do I merge a new Quickstart release, resolve a Constants.java conflict, test the result and record it in UPSTREAM.md? |

Every guide MUST name exact file paths and method names, and MUST stay correct as the code changes. A pull request that changes behavior also updates the guides it affects.

## Open items and acceptance

The team MUST give the AI the facts below before it writes BioBuzz-specific code. Until then, the AI builds the skeleton, drive, troubleshooting tools and documents. It marks every gap with `TODO(BioBuzz)`.

**Open items (team to supply)** – see `docs/DESIGN-REVIEW.md`, section 5, for the full questions

- [x] BIOBUZZ game manual (TU05)
- [x] BioBuzz field image (`TeamCode/src/main/assets/images/field-biobuzz.png`)
- [ ] Mechanism hardware: Intake, Storage (with feeder) and Launcher are planned; their motors, servos and sensors, especially how Storage counts elements and reads colour (Q5)
- [ ] Robot configuration names from the Control Hub, for `HardwareNames`
- [ ] Field frame, rotational symmetry and driver headings confirmed (Q7, Q8)
- [ ] Field poses: start positions, launch spots for each CELL, LOADING ZONE approach and park, GARDEN line, LOADING ZONE corner
- [ ] Odometry: Pinpoint pod type and offsets (DecodeBot uses Pinpoint with goBILDA 4-bar pods)
- [ ] HuskyLens mounting and the learned tag IDs (Q9)
- [ ] Which CELL is up: operator button only, or the camera as well (Q2)
- [ ] Gamepad 2 map confirmed, once the mechanisms are built
- [ ] Autonomous routines for the first competition (Q10)
- [ ] FLOWER scoring: planned or not (Q3)

**Acceptance checklist** (mentor signs off)

- [ ] The project builds with no warnings in the `biobuzz` package. DecodeBot code is unchanged.
- [ ] No forbidden Java constructs appear in `biobuzz/` outside `helpers/` (see Coding style rules).
- [ ] Driving is field-centric, robot-centric is used only by the safeguard fallback or X, and the pose is handed from auto to teleop.
- [ ] Without an auto pose, TeleOp runs as S1 and refuses assists with a warning. With a known pose, an assist drives to its spot, and any stick takes over in the same loop.
- [ ] Only OpModes read gamepads, through `GamepadInput`. In a match, gamepad 1 only drives and gamepad 2 only runs mechanisms.
- [ ] Every mechanism extends `Mechanism`, never moves an actuator in its constructor, and has a test OpMode. Every state machine follows the four-section layout, has timeouts on its waiting states, and has a test OpMode. Adding a mechanism needs two edits in `Robot`.
- [ ] TeleOp INIT moves nothing (G403). Auto always parks by `PARK_BY_S`. Auto paths that leave our side or enter the HIVE footprint are flagged at INIT.
- [ ] Collecting stops at 4 elements, and opponent NECTAR is pushed back out.
- [ ] Telemetry shows on both the Driver Station and the Dashboard, with the match header line and the per-mechanism and per-state-machine switches. The Dashboard field view shows the robot pose on the BioBuzz field.
- [ ] Every Field Check OpMode runs and writes its log, and `Field Check 13: Summary` shows all results against the home baseline. `Robot Check` ends with READY FOR MATCH.
- [ ] Pulling a motor cable during teleop raises an ALERT, marks that mechanism FAULTED, and does not crash the OpMode.
- [ ] A full teleop run writes a match log with DATA and EVENT rows. `Log Viewer` summarizes it.
- [ ] Unplugging the Pinpoint while driving switches to robot-centric with a rumble and an ALERT, and Y returns to field-centric.
- [ ] A practice match's Auto and TeleOp logs are named with the same match id, and `plot_match.py --match <id> --at 2.0` shows what happened 2 s into the match, with battery voltage.
- [ ] The loop time averages under 25 ms with logging and telemetry on.
- [ ] HLD, LLD, the design review and every guide in Documentation deliverables exist, and their file and method names match the code.
- [ ] A student new to the code completes `FIRST_CHANGES.md` exercise 1 to 3 without help.

## Revision history

| Version | Date | Changes |
| --- | --- | --- |
| 2.0 | 2026-10-19 | New architecture, and the BIOBUZZ game. Mechanisms are operation classes (no states) that extend one plain base class, `Mechanism` (Template Method; R3 amended); tasks that take time are standalone state machines in `statemachines/`; `parts/` and `subsystems/` are replaced by `drive/`, `mechanisms/`, `statemachines/` and `game/` (R10 amended). `Drive` has no states. Robot is the one menu of robot actions and cancels state machines that share a mechanism (Mediator). Driving grows in three stages (S1 manual, S2 assisted with fixed-path buttons, S3 autonomous); with no auto pose TeleOp runs as S1. New Game requirements section (GR1–GR13) from the Competition Manual TU05, including rotational symmetry for RED. Vision is a HuskyLens. Design patterns are named. HLD, LLD and DESIGN-REVIEW drafted. |
| 1.4 | 2026-10-19 | Removed `HardwareLookup`: parts call `hardwareMap.get(...)` directly with `HardwareNames` constants, and a missing device stops at INIT with the SDK's message. `HardwareNames` records each device's hub and port (Control Hub and Expansion Hub). Bulk reads are off and moved to a new LLD section, Future improvements. Removed slow mode (gamepad 1 left trigger is free) and the stick speed multipliers: sticks drive at full scale after the deadzone. Speed limits come from Pedro's `ForesightConfig` (paths and holding only). |
| 1.3 | 2026-10-18 | Renamed the file to `BioBuzz-Requirements.md`; the version lives in the document only. `Constants.java` and `Tuning.java` stay as Quickstart stubs until tuning, then get a one-time structural fill-in recorded in `UPSTREAM.md`; `Constants` imports `HardwareNames`. Only OpModes read gamepads (through `GamepadInput`). One INIT menu (`InitMenu`, D-pad rows) replaces the separate INIT buttons. Field drawing goes through `helpers/FieldDrawing.java` with a BioBuzz field image asset. Field Check 07 carries its own copy of Pedro's deceleration method. Added `LogSummary`, `GamepadInput` held reads, the field-centric wording fix, and the LLD's Known implementation challenges (Pinpoint access first). |
| 1.2 | 2026-10-18 | Added R11 and R12 and a new section, Base project and repository. The base is the Pedro Pathing Quickstart for Pedro 3.x, in a new `biobuzz` repository that stays up to date with the public Quickstart. Quickstart files are never moved, and team code goes in `teamcode/biobuzz/` next to `teamcode/pedro/`. Updated the code layout, testing and tuning, deliverables and acceptance sections to match. |
| 1.1 | 2026-10-18 | Confirmed the change to R1: field-centric is the normal mode, and robot-centric is a safeguard fallback or a manual switch on gamepad 1 X. Added the table of contents and this revision history. |
| 1.0 | 2026-10-18 | First version: the confirmed requirements, coding style, code layout, architecture, driving, operator controls, autonomous, telemetry, troubleshooting (sections A to G), testing, documentation deliverables, open items and acceptance. |
