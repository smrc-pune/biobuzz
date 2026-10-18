# BioBuzz Robot Software – Requirements

Oct 18, 2026 · @Girish

**Version 1.3**

## Contents

1. Purpose and how to use this document
2. Confirmed requirements
3. Coding style rules (R3)
4. Base project and repository (R11, R12)
5. Code layout (R10)
6. Architecture patterns
7. Driving (R1, R2)
8. Operator controls (R2)
9. Autonomous
10. Telemetry and dashboard (R6)
11. Troubleshooting (R7, R8, R9)
    1. A. Confirming tuning on a new field (R7)
    2. B. Before every match: Robot Check
    3. C. Match-time issues: detect and recover (R8)
    4. D. Battery voltage as a signal
    5. E. Match logs for post-match analysis (R9)
    6. F. After the match: analysis and debrief
    7. G. TODO: Match Simulator
12. Testing and tuning
13. Documentation deliverables (R4, R5)
14. Open items and acceptance
15. Revision history

## Purpose and how to use this document

An AI assistant uses this document to produce the BioBuzz robot's software: the code, the design documents and the student guides. It turns the patterns proven in the DecodeBot `pedro` package into rules, so the new code reads like the old code.

**Audience.** The AI writes the code. Mentors check the result against the acceptance checklist at the end. Students in grades 7 to 12 read and change the code. Many of them have no Java experience.

**How the AI should use this document.**

1. Read every section before writing code. **MUST**, **SHOULD** and **MAY** have their usual requirement meanings.
2. Use the DecodeBot `TeamCode/.../teamcode/pedro` package as the reference implementation. When this document is silent, copy what DecodeBot does.
3. Game details (mechanisms, field positions, scoring) are not known yet. Leave them out rather than guess. Put a `// TODO(BioBuzz):` marker and a matching entry in Open items.
4. Work in this order: the skeleton (helpers, configurations, Robot, drive, teleop), then one mechanism end to end, then autonomous, then the documents.

**What to deliver.**

| Deliverable | Form | Section |
| --- | --- | --- |
| Robot code | Java under `teamcode/biobuzz/` | Code layout, Architecture |
| Test and tuning OpModes | Java under `biobuzz/testing/` and `biobuzz/fieldchecks/`. Pedro tuners stay in the Quickstart's `pedro/procedures/`. | Testing and tuning |
| Match logging and troubleshooting tools | Java plus `docs/TROUBLESHOOTING.md` | Troubleshooting |
| High-level design (HLD) | `docs/HLD.md` | Documentation deliverables |
| Low-level design (LLD) | `docs/LLD.md` | Documentation deliverables |
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
| R10 | The team code layout MUST follow the DecodeBot `pedro` package, placed in `teamcode/biobuzz/`. | Code layout |
| R11 | The project MUST be based on the Pedro Pathing Quickstart for Pedro 3.x. | Base project and repository |
| R12 | All code MUST live in a new `biobuzz` repository that stays up to date with the public Quickstart repository. Quickstart files and folders MUST NOT be moved. The team's `biobuzz` package sits next to the Quickstart's `pedro` package. | Base project and repository |

## Coding style rules (R3)

A student who has never used Java MUST be able to read any file and say what the robot does. When readability and cleverness conflict, readability wins. Some repetition is acceptable.

**Allowed Java**

| Use | Example |
| --- | --- |
| Classes with fields, a constructor and plain methods | `public class Gate { ... }` |
| `enum` for state lists only | `enum State { IDLE, INTAKING, FEEDING }` |
| `switch` on the state enum, with one `case` per state | `switch (state) { case IDLE: ... break; }` |
| `if` / `else`, `for` over a counted range, and fixed-size arrays | `for (int i = 0; i < 5; i++)` |
| `public static` fields in config classes, so FTC Dashboard can edit them live | `public static double ROW1_POWER = 0.8;` |
| `final` fields for parts and helpers | `private final StateTimer timer = new StateTimer();` |
| One-line methods | `public boolean isBusy() { return state != State.IDLE; }` |

**Forbidden Java** (the AI MUST NOT use these in student-facing code)

- Lambdas, method references and the streams API: `x -> ...`, `::`, `.stream()`.
- Generics written by the team, such as `class Machine<T>`. Using an SDK `List<String>` is acceptable only inside `helpers/`.
- Inheritance chains deeper than one level, and abstract base classes for subsystems. Each subsystem stands alone and follows the shared section layout. `OpMode` and `LinearOpMode` are the only superclasses used.
- Interfaces, anonymous classes, inner classes and static nested classes.
- `Optional`, `var`, ternary chains, bit operations, reflection and annotations other than the FTC ones (`@TeleOp`, `@Autonomous`, `@Config`, `@Override`, `@Disabled`).
- Threads, `sleep()` and busy-wait loops in OpModes or subsystems. Waiting is done with `StateTimer`.
- Exceptions used for control flow. `try`/`catch` is allowed only inside `helpers/`, for example around file writes.
- Command-based frameworks such as FTCLib commands or NextFTC. Pedro's own builder calls are allowed only in the Quickstart's `pedro/` folder and in `auto/AutoPaths.java`, which MUST explain them in comments.

**Naming and comments**

- Use plain English names: `gate.open()`, `timer.isTimeUp(600)`, `shooter.isAtSpeed()`.
- Constants are `UPPER_SNAKE_CASE`, always with the unit in the name (`_MS`, `_IN`, `_DEG`, `_RPM`) or in a comment.
- Every file starts with a block comment. It says what the file is, its role (part, brain, config, OpMode or helper), a small ASCII diagram for state machines, and how to trace a bug through the file.
- Comments explain WHY in student language. Mark each state's code with `// DO`, `// CHECK` and `// GO`.
- A file SHOULD stay under 250 lines. When it grows past that, split the work into another part or subsystem; do not add cleverness.

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
- Team code reaches Pedro only through the follower factory in `pedro/Constants.java` (`Constants.create(hardwareMap)`). `biobuzz/parts/DriveBase.java` is the only team class that calls it. Pedro's `PinpointLocalizer` keeps its Pinpoint driver private, so the Pinpoint status, update rate and recalibration that Troubleshooting needs are not reachable through the follower. How `DriveBase` gets them is decided during implementation and written up in the LLD (Known implementation challenges).
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

The team package MUST be `org.firstinspires.ftc.teamcode.biobuzz`, in `teamcode/biobuzz/` next to the Quickstart's `teamcode/pedro/`. It has the same folders as DecodeBot's `pedro` package, except for the Pedro files (`Constants.java`, `Tuning.java`, `procedures/`), which stay in the Quickstart's `pedro/` (see Base project and repository). Each folder holds one kind of file, so a student always knows where to look.

```
teamcode/pedro/           Quickstart-owned: Constants.java, Tuning.java, procedures/ (not moved)
teamcode/biobuzz/
  README.md               start here: reading order, vocabulary, first changes
  Robot.java              builds every part + subsystem, update() once per loop, links subsystems
  configurations/         tunable numbers ONLY (@Config, public static)
    HardwareNames.java    every device name + motor direction, nowhere else
    DriveConfig.java      speeds, slow mode, field-heading offsets
    ControlsConfig.java   stick deadband, trigger threshold, which button does what (as comments + constants)
    AutoConfig.java       poses (inches, degrees, BLUE side), timeouts, alliance switch
    TelemetryConfig.java  on/off switches per subsystem
    LogConfig.java        match-log switches and rate
    HealthConfig.java     alert, heading-safeguard and battery thresholds
    FieldCheckConfig.java Field Check home baseline + tolerances
    <Mechanism>Config.java  one per mechanism
  parts/                  hardware wrappers: one device group each, no decisions
  subsystems/             state machines ("brains"): Drive + one per mechanism
  teleop/                 BioBuzzTeleOp.java: both match gamepad maps (driver + operator)
  auto/                   AutoPaths.java (all paths), BioBuzzAuto.java (step machine)
  testing/                one test OpMode per subsystem + RobotCheck + LogViewer (+ MatchSimulator, TODO)
  fieldchecks/            FieldCheck01_Hardware ... FieldCheck13_Summary: one OpMode per new-field test
  helpers/                StateTimer, StateLog, DashboardTelemetry, GamepadInput, HardwareLookup,
                          MatchLogger, MatchInfo, BatteryMonitor, LoopMonitor, HealthMonitor,
                          FieldCheckHelper, PoseMemory, InitMenu, FieldDrawing, LogSummary
  template/               TemplatePart.java, TemplateSubsystem.java to copy for a new mechanism
  docs/                   HLD.md, LLD.md, guides (see Documentation deliverables)
TeamCode/src/main/assets/images/field-biobuzz.png    BioBuzz field image for the Dashboard field view
tools/plot_match.py       laptop only, mentor-side (repo root, not built into the app)
```

**Layout rules**

- Device names MUST appear only in `HardwareNames.java`. Robot passes the names to the parts, and `pedro/Constants.java` imports them.
- Tunable numbers MUST appear only in `configurations/`. There are no magic numbers in parts, subsystems or OpModes.
- The DecodeBot `GamepadConfig` wrapper moves to `helpers/GamepadInput.java`, because it is a tool, not a set of numbers. It keeps the edge-detected `isAPressed()` style and adds `isAHeld()` reads for hold-to-use controls. Back/Share is read from `gamepad.back` (in the SDK, `options` is the same button as Start).
- `helpers/InitMenu.java` is the one pre-match menu (see Autonomous). `helpers/FieldDrawing.java` draws on the Dashboard field view (see Telemetry and dashboard). `helpers/LogSummary.java` reads log files for `LogViewer`, so file reading and its `try`/`catch` stay in `helpers/`.
- Each subsystem MUST have a public static `Robot.buildX(hardwareMap)` method, so a test OpMode can build that one subsystem alone.
- `template/` MUST compile. It MUST be annotated `@Disabled` where it is an OpMode.

## Architecture patterns

The code has four layers, and each layer talks only to the layer below it. Only `Robot` links one subsystem to another. These are the rules DecodeBot follows, written as requirements.

```
  BioBuzzTeleOp                      BioBuzzAuto
  (pad 1 = drive, pad 2 = mechanisms) (step machine + AutoPaths)
          |                                  |
          v                                  v
  +--------------------------------------------------+     +-------------------------+
  | Robot                                            | --> | Driver Station          |
  |  builds every part and subsystem (HardwareNames) |     |  + FTC Dashboard        |
  |  update() once per loop: each subsystem one step | --> | Match log (CSV)         |
  |  the ONLY place subsystems talk to each other    |     |  one file per match     |
  +--------------------------------------------------+     +-------------------------+
          |
          v
  subsystems: state machines (DO / CHECK / GO)            used by every layer:
    Drive            Mechanism A       Mechanism B          configurations/
    (field-centric,  (TODO BioBuzz)    (TODO BioBuzz)         tunable numbers only,
     Pedro paths)                                             live edit on Dashboard,
       |                 |                 |                  HardwareNames
       v                 v                 v                helpers/
  parts: hardware wrappers, no decisions                      StateTimer, StateLog,
    DriveBase        Part A            Part B                 GamepadInput, MatchLogger,
    (Pedro+Pinpoint) (motors, servos)  (sensors)              LoopMonitor, HealthMonitor,
          |                                                   DashboardTelemetry
          v
  Control Hub + Expansion Hub devices
```

OpModes give commands to `Robot` subsystems. `Robot.update()` runs every state machine once, and only the parts touch hardware.

| Layer | Does | MUST NOT |
| --- | --- | --- |
| OpMode (`teleop/`, `auto/`) | Reads gamepads (teleop only) or runs auto steps. Calls subsystem commands, then `robot.update()` and telemetry once per loop. | Touch motors or servos. Hold mechanism logic. |
| `Robot` | Builds parts and subsystems. Calls each `update()` in a fixed order. Holds the links between subsystems (for example, "while the shooter fires, the intake feeds"). Feeds telemetry and the match log. | Read gamepads. Set motor powers. |
| Subsystem (brain) | Holds one `enum State`, one `StateTimer` and one `StateLog`. `update()` runs DO, CHECK and GO for the current state. | Call another subsystem. Sleep or wait in a loop. |
| Part | Wraps one group of devices. Has plain verbs (`open()`, `setPower()`) and getters for sensors. | Make decisions, keep state machines or use timers. |

**Every subsystem file MUST use this section order** (copied from `Intake.java`):

1. **States**: the `enum`, with a one-line comment on each value.
2. **Commands**: public methods that only call `goTo(state, "reason")`. Commands never touch parts. A command that cannot run calls `log.warn("...")` and returns.
3. **`update()`**: a `switch` with one `case` per state. Each case does DO (set parts), CHECK (a timer or sensor) and GO (`goTo`).
4. **Bookkeeping**: `goTo()` (records the change, sets the new state, resets the timer), `isBusy()`, `getState()` and `addTelemetry()`.

**Other rules**

- Every state that waits for something MUST have a timeout that goes to a safe state. The timeout reason is logged, for example `"intake timeout"`.
- `goTo()` with the current state does nothing. This makes repeated commands safe.
- `Robot.isBusy()` is true when any subsystem is busy. Autonomous uses it to move to the next step.
- `Robot.stopMechanisms()` puts every mechanism in a safe state. OpModes call it in `stop()`.

## Driving (R1, R2)

The robot MUST always drive field-centric: pushing the stick away from the drivers moves the robot away from them, whichever way it faces. Gamepad 1 controls driving and nothing else.

**Field-centric rules**

- Field-centric MUST be the normal mode. DecodeBot's `DriveConfig.ROBOT_CENTRIC` switch is removed. Robot-centric is used only as a safeguard fallback (Pinpoint not `READY`, or a heading jump) or when the driver presses X, and every switch is written to the match log (see Troubleshooting C).
- In field-centric mode, `DriveBase.driveManual(forward, right, turn)` MUST call Pedro's `ManualDrive.fieldCentric(powers, heading, offset)` every loop. The heading comes from the Pinpoint through the Pedro follower. In robot-centric mode (the fallback, or X) the stick powers go to the follower without the rotation.
- `offset` is the alliance's driver-facing angle. It comes from `DriveConfig.BLUE_DRIVER_HEADING_DEG` and `DriveConfig.RED_DRIVER_HEADING_DEG`, picked by the alliance chosen in the INIT menu (kept in `MatchInfo`; the default is `AutoConfig.ALLIANCE_RED`). This means "away from the drivers" is correct on both alliances without a heading reset. TODO(BioBuzz): confirm both angles on the BioBuzz field drawing.
- **Pose hand-off:** at the end of autonomous, the final Pedro pose MUST be saved in `helpers/PoseMemory.java` as a plain `public static` field. Teleop `init()` MUST start from that pose. If no auto ran in this app session, teleop uses the alliance's default start pose and shows a telemetry warning: `"No auto pose: press Y facing away from drivers"`.
- **Heading reset:** gamepad 1 **Y** sets "the way the robot faces now = away from the drivers". This fixes drift during a match and turns field-centric back on after a fallback. Each reset MUST be written to the match log.
- Stick input passes through a deadband (`DriveConfig.STICK_DEADZONE`) and a speed multiplier. It is never sent raw.

**Gamepad 1 map** (only the driver's controls; changes go in `teleop/` and the cheat sheet)

| Control | Action |
| --- | --- |
| Left stick | Move (field-centric), scaled by `SPEED_NORMAL` |
| Right stick X | Turn, scaled by `TURN_SPEED_NORMAL` |
| Left trigger (hold) | Slow mode (`SPEED_SLOW`, `TURN_SPEED_SLOW`) |
| Y | Reset field heading ("facing away from the drivers"); field-centric back on |
| Right stick button (robot against a wall) | Snap heading to the nearest 90° (see Troubleshooting C) |
| Left stick button (hold 1 s, robot still) | Recalibrate the gyro, keeping the heading |
| X | Switch field-centric / robot-centric by hand |
| D-pad (INIT only) | The INIT menu: up/down picks a row, left/right changes it (alliance, match id, battery label, logging; see Autonomous) |
| Right bumper (hold) | Heading lock toward the scoring target, using Pedro's `ManualDrive.headingLock`. MAY be added after the game reveal; TODO(BioBuzz). |
| Back / Share | Mark a moment in the match log (`"DRIVER MARK"`), for post-match analysis |

`Drive` has three states, as in DecodeBot: `DRIVER_CONTROL`, `FOLLOWING_PATH` and `HOLDING`. In teleop, any stick movement past the deadband cancels a path and returns control to the driver.

## Operator controls (R2)

Gamepad 2 runs every mechanism, and only gamepad 2 does. The button map stays in one method, `handleOperator()` in `teleop/BioBuzzTeleOp.java`. A student can change a button there without opening a subsystem.

**Rules**

- Only OpModes (teleop, auto, test and Field Check OpModes) read `gamepad1` or `gamepad2`, always through `helpers/GamepadInput`. `Robot`, subsystems, parts and the other helpers never read a gamepad; `InitMenu` is given the button presses by its OpMode. Autonomous reuses `Robot` unchanged because of this.
- For matches, `teleop/BioBuzzTeleOp.java` holds both gamepad maps: gamepad 1 in `handleDriver()`, gamepad 2 in `handleOperator()`.
- Each loop, a button is read once into a local `boolean` and then used. This is the DecodeBot `handleOperator()` pattern, and it exists because button reads are edge-detected.
- Every button calls one subsystem command. Logic like "only fire when ready" lives in the subsystem, which calls `log.warn(...)` when it refuses a command.
- One button MUST always be the **stop-all** button. It is D-pad down, as in DecodeBot, and it calls `robot.stopMechanisms()`.
- Analog controls (triggers, sticks) MAY be used for proportional mechanisms, through a deadband in `ControlsConfig`.
- Rumble feedback SHOULD tell the operator about events they can't see well, using `gamepad2.rumble(ms)`. Examples: "ready to score" and "mechanism timed out". Each rumble event MUST be named in `ControlsConfig`.
- The controls cheat sheet MUST be in three places, kept the same by hand: the teleop file's header comment, the `TelemetryConfig.GAMEPAD_HELP` lines, and `docs/CONTROLS.md`, which has a printable diagram for the drive team.

**Gamepad 2 map.** TODO(BioBuzz): fill this in after the mechanisms are designed. Use the DecodeBot map as the pattern.

| Control | DecodeBot action (pattern) | BioBuzz action |
| --- | --- | --- |
| A | Intake on/off | TODO |
| B | Eject on/off | TODO |
| X / Y | Shooter near / far on/off | TODO |
| Right trigger | Fire (refused with a warning unless ready) | TODO |
| D-pad down | Stop all mechanisms | Stop all mechanisms (fixed) |
| Back / Share | (unused) | Mark a moment in the match log (`"OPERATOR MARK"`) |

## Autonomous

Autonomous is a step machine that drives the same `Robot` as teleop. It uses the same states and code layout as a subsystem, so students learn the pattern once. It follows `DecodeAuto.java`.

**Structure**

- `auto/BioBuzzAuto.java` holds `enum Step` and a `switch` in `updateAuto()`. A `firstLoop` flag makes one-time commands (`followPath`, `fire`) run only on a step's first loop.
- Each loop does `updateAuto()`, then `robot.update()`, then telemetry and the match log.
- Every step MUST have a timeout from `AutoConfig`. A step that times out writes `"TIMEOUT <step>"` to the match log and moves on, so a jam does not waste the whole autonomous.
- A step finishes when `!robot.isBusy()` or its timeout runs out, as in DecodeBot's `isDone()` check.

**Paths and positions**

- `auto/AutoPaths.java` builds every path once, in its constructor. Pedro's API (`line`, `curve`, `PoseFactory`) appears only in this file.
- Positions live in `AutoConfig` in inches and degrees, written for the BLUE alliance. `AutoPaths.poses()` mirrors them for RED. TODO(BioBuzz): check the mirror axis (`mirrorX` or `mirrorY`) on the field drawing.
- `robot.drive.setStartingPose(paths.start)` is called in `init()`. The start pose MUST match where the robot is physically placed, which is the main cause of autonomous errors.
- At `stop()`, the final pose is saved to `PoseMemory` for teleop (see Driving).

**Pre-match selection** (new compared with DecodeBot)

- During INIT, gamepad 1 uses one menu, `helpers/InitMenu.java`, instead of a button per setting. D-pad up/down moves between rows; D-pad left/right changes the value in that row, wrapping round at the ends. The OpMode reads the D-pad and passes the presses to the menu.
- Auto's INIT rows: **Alliance** (BLUE / RED), **Start** position, **Routine**, **Delay** (0 to 10 s, for alliance partner coordination), **Match type** (P / Q / E), **Match number**, **Battery** label (A to H) and **Logging** (on / off). A routine is one value of an `enum Routine`, for example a near-side or far-side start.
- TeleOp's INIT shows the rows it needs (alliance, match type and number, battery, logging), starting from what was entered in Auto's INIT (`MatchInfo`).
- The choices show on telemetry and are written to the match log header.
- The `AutoConfig` and `LogConfig` values are the defaults, so the robot still works if nobody presses anything.

**Vision** is optional. If the BioBuzz game needs it, `subsystems/Vision.java` reads AprilTags. It MAY correct the Pedro pose while the robot is stopped, following `docs/CAMERA_GUIDE.md`. Every correction MUST be logged with the old and new pose.

## Telemetry and dashboard (R6)

Every OpMode MUST send the same telemetry to the Driver Station and FTC Dashboard through `DashboardTelemetry.create(telemetry)`. `t.update()` is called once per loop. The Driver Station screen stays short enough to read during a match; detail goes to the Dashboard.

**Required content**

| Group | Lines | Switch in `TelemetryConfig` |
| --- | --- | --- |
| Match header (always first) | Alliance, routine, match time left, loop time (ms), battery (V), any active **ALERT** | Always on |
| Each subsystem | State and seconds in it, busy, the last warning (shown 4 s) | One switch per subsystem |
| State history | Last change, its reason and age, plus the 4 changes before it (`StateLog`) | `HISTORY` |
| Parts | Motor powers, velocities, currents, servo positions, sensor readings | `PARTS` (off by default) |
| Drive pose | x, y, heading (deg), and whether the pose came from auto | `DRIVE` |
| Controls cheat sheet | One line per gamepad | `GAMEPAD_HELP` |

**Dashboard**

- Every `*Config` class MUST be `@Config` with `public static` (not `final`) fields, so values can be changed live at `http://192.168.43.1:8080/dash`. Each config file's header MUST say that live edits are lost on restart and good values must be typed back into the file.
- The Dashboard field view MUST draw the robot pose, and in auto the current path, through `helpers/FieldDrawing.java` (the Dashboard canvas). Pedro 3's tuning package has no drawing helper to reuse.
- The field view SHOULD use the BioBuzz field as its background, from `TeamCode/src/main/assets/images/field-biobuzz.png` (the Dashboard serves that folder at `/images/`; it does not serve other asset folders), so poses and paths can be checked against field elements. TODO(BioBuzz): the image comes from the game manual's field drawing. How Dashboard 0.6 loads a custom background is checked during implementation and written up in the LLD.
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
| 01 | `FieldCheck01_Hardware` | Nothing | Every device in `HardwareNames` is found, both hubs answer, Pinpoint is `READY`. A missing device names the part and the config entry to fix | No |
| 02 | `FieldCheck02_Battery` | Nothing, 2 s | Resting voltage at least 12.5 V. Also compared with the baseline battery voltage: more than 0.5 V apart marks tests 07 to 09 "battery differs from baseline" (at most WARN) | No, but it changes 07 to 09 |
| 03 | `FieldCheck03_Still` | Nothing, 10 s, hands off | Heading creeps no more than 0.5°; Pinpoint stays `READY` | No |
| 04 | `FieldCheck04_Push` | Crew pushes it 2 tiles (48 in) along a seam, then 2 tiles sideways | Distance within ±0.75 in, sideways drift within ±0.5 in | Mostly no |
| 05 | `FieldCheck05_Spin` | Crew turns it 4 full turns by hand, back to the seam | Heading error within ±2° | No |
| 06 | `FieldCheck06_FieldCentric` | Robot turned 90°; driver pushes the stick away from the drivers | Driver confirms it moved away: A = yes, B = no | No |
| 07 | `FieldCheck07_Brake` | Full power to a set speed, then coast; forward, then sideways | Deceleration within 15% of `naturalForwardDeceleration` / `naturalStrafeDeceleration` in `Constants` and the baseline | **Yes** |
| 08 | `FieldCheck08_Line` | Pedro straight line, 48 in, stop on a seam | End error within the baseline + 0.75 in; overshoot and settle time shown | **Yes** |
| 09 | `FieldCheck09_Square` | Pedro 48 in square, back to start | End error under 1.5 in and 3° | **Yes** |
| 10 | `FieldCheck10_Wall` | Robot squared against a wall | Heading within 3° of the nearest 90°; then snap-to-90° is tried | Partly |
| 11 | `FieldCheck11_Mechanisms` | One bench check per subsystem, through its public commands (for example a flywheel reaches its target velocity within its spin-up time) | Per mechanism. TODO(BioBuzz): define one check per mechanism | No |
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
| 4. Drive motors | Clear around the robot | Each motor at 0.3 power for 0.5 s, one at a time; the screen names the motor | Crew confirms each wheel turns the right way |
| 5. Logger | – | `LogConfig.ENABLED`, free storage | On, and more than 50 MB free |

Things code cannot see (connectors seated, Pinpoint mounted firmly, battery strapped down, battery label noted) stay on the paper checklist in `docs/MATCH_DAY.md`.

### C. Match-time issues: detect and recover (R8)

Two helpers watch the robot every loop. `helpers/LoopMonitor.java` tracks loop timing and `helpers/HealthMonitor.java` checks the hardware, the heading and the battery. They MUST raise an **ALERT**, which goes on the telemetry header line, in the match log, and as a gamepad rumble for urgent alerts. They MUST keep the robot running instead of crashing it.

| Problem | How it is detected | What the robot does |
| --- | --- | --- |
| Slow loop | Loop time averaged over 1 s is above 25 ms, or any single loop takes over 100 ms | ALERT with the slowest subsystem if known. Bulk reads are already on (`LynxModule.BulkCachingMode.MANUAL`, cleared once per loop) |
| Low battery | Below 12.0 V at INIT, or below 9.0 V for over 0.5 s during play | ALERT, rumble once. A low reading at INIT does not stop the OpMode; it is shown in red |
| Pinpoint not `READY` | Status is anything but `READY` (a fault, or it restarted after a power blip) | Drive switches to **robot-centric**, rumble, ALERT. Driving is never stopped for this |
| Heading jump | The heading changes more than `MAX_TURN_DEG_PER_S` × loop time + 10° in one loop (720°/s is faster than any FTC robot turns), measured the short way round | Drive switches to **robot-centric**, rumble, ALERT, log the jump |
| Pose jump | The pose moves more than 6 in within one loop | ALERT and log the jump |
| Device disconnected | A hub stops answering, or a mechanism device read throws an error | That subsystem goes to a `FAULT` state and its outputs are set to zero. ALERT names the part. Other subsystems keep working |
| Mechanism stuck | A state's timeout fires, or a motor is commanded above 0.3 power while its velocity stays near zero for 500 ms | Go to a safe state and `log.warn(...)`. ALERT and a rumble on gamepad 2 |
| Gamepad disconnected | The gamepad's timestamp does not change for 1 s | ALERT. Drive powers are set to zero until input returns |
| Robot restarted mid-match | `PoseMemory` is empty in teleop | Warning and instructions for a heading reset (see Driving) |

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

- `HardwareLookup` MUST crash at INIT for a missing device, with its helpful message, so problems are found before the match. After PLAY, nothing MUST throw. Faults are caught in `helpers/` and turned into ALERTs.
- Every safeguard trip and every recovery press MUST be written to the match log as an EVENT.
- Thresholds live in `configurations/HealthConfig.java`. The values above are starting values, to be set from our own logs.
- `docs/MATCH_DAY.md` MUST give a one-page pre-match checklist (battery and its label, Pinpoint light, alliance and routine chosen, start pose placed, wires) and a between-match checklist, and MUST teach the drive team the recovery buttons.

### D. Battery voltage as a signal

Battery voltage is a likely hidden cause of match-only problems: a dip can reset sensors, slow motors and change how paths are followed. It MUST be logged in every DATA row and drawn on the **same time axis** as heading, loop time, drive effort and Pinpoint status, so a cause and its effect line up.

`helpers/BatteryMonitor.java`:

- Reads every hub's `VoltageSensor` (`hardwareMap.voltageSensor`) and keeps the **lowest**, because a dip on either hub matters.
- Each read is a separate hub command (not part of a bulk read), so it MUST be sampled every `HealthConfig.BATTERY_SAMPLE_MS` (100 ms), not every loop.
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
| `<subsystem>_state`, part values | – | Each subsystem's state and up to 8 key part values, chosen in its `addLogData()` |

- `EVENT` rows whenever something happens: `PLAY` and `STOP`; every state change and its reason (logged automatically from `StateLog.record`); every `log.warn`; every ALERT; every safeguard trip (`SAFEGUARD_PINPOINT_<STATUS>`, `SAFEGUARD_HEADING_JUMP:<deg>`, `SAFEGUARD_LOW_BATTERY:<V>`); every recovery (`RECOVER_FACE_AWAY`, `RECOVER_SNAP90:<from>-><to>`, `RECOVER_RECALIBRATE`, `RECOVER_ROBOT_CENTRIC`, `RECOVER_FIELD_CENTRIC`); button presses that send commands; auto step changes, paths and timeouts; Field Check results (`FIELDCHECK_<nn>:<result>`); `LOGGER_DROPPED:<n>`; and driver or operator MARKs.

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

- `plot_match.py <file>` draws stacked graphs on one shared time axis, with EVENTs as vertical lines through every graph: heading (shaded by field or robot-centric), **battery** (resting voltage and the warning level as lines), drive effort, loop time, Pinpoint status, subsystem states, and a field view of the path coloured by battery voltage.
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
| Laggy or jerky drive | Slow loop | `worst_loop_ms` spikes; dropped rows | Remove slow code; fewer hardware reads |
| Log ends early | Crash, power loss, disconnect | No `# END`; last voltage reading | Robot Controller log for the crash; check power connectors |

### G. TODO: Match Simulator

Not designed yet; recorded so it is not lost. A `testing/MatchSimulator.java` OpMode SHOULD keep the robot busy for a full match length (an auto-like part, the transition pause, then a teleop-like part) the way a real match would: Pedro paths to random safe positions, turns, sprints and stops, and each mechanism's game actions, with the match logger on. It reuses `MatchLogger`, `BatteryMonitor`, `HealthMonitor` and the plot tool unchanged.

What it is for: battery behaviour under a realistic load without a real match; comparing batteries by label; reproducing match-only problems at home (Pinpoint resets under deep dips, heading jumps after hard turns, slow loops, hot motors); setting real voltage thresholds from data; and soak tests after repairs.

Questions for its design:

- [ ] Safe random positions: a list of allowed zones away from walls and field elements, and a seed so a run can be repeated exactly
- [ ] Load profiles: gentle, typical (taken from our own match logs) and worst case
- [ ] One game action per mechanism that the simulator can call. TODO(BioBuzz): once the mechanisms exist
- [ ] Safety: clear field, STOP always works, mechanism limits respected, a max-speed setting
- [ ] Results: a match log plus a summary of lowest voltage, sag, worst loop time and safeguard trips

## Testing and tuning

Each subsystem MUST have its own test OpMode, so a student can check one mechanism without the full teleop. Drive tuning uses the Quickstart's own tuners in `teamcode/pedro/`, unchanged.

**Test OpModes** (`testing/`, group `"Tests"`)

| OpMode | Builds | Lets a student |
| --- | --- | --- |
| `Test: Drive` | `Robot.buildDrive` only | Drive field-centric, see the pose, reset the heading |
| `Test: <Mechanism>` (one each) | That subsystem only | Send each command from a button, see state, history and part values |
| `Test: Parts` | Every part, no subsystems | Move one motor or servo at a time with the D-pad, to check wiring and direction |
| `Robot Check` | Full robot | Run the one-minute check before every match (see Troubleshooting B) |
| `Field Check 01–13` (fieldchecks/, group "Field Checks") | Full robot | Run one new-field test at a time and see all results in the Summary (see Troubleshooting A) |
| `Match Simulator` (TODO) | Full robot | Run a full-length simulated match for battery and soak tests (see Troubleshooting G) |
| `Log Viewer` | Nothing | Read summaries of recent match logs |

**Rules**

- Test OpModes use the same subsystem classes and commands as teleop. They contain no copied logic.
- Each test OpMode's header lists its buttons, and shows them on telemetry.
- Test OpModes also write match logs, with `TEST` in the file name.

**Tuning** (the Quickstart's `pedro/Tuning.java` and `pedro/procedures/`)

- Use the Quickstart's tuners where they are. Tuned values go into `pedro/Constants.java`, values only (see Base project and repository).
- `docs/TUNING.md` MUST give the order: localizer offsets and pod directions, forward and lateral ticks, then the follower and drive constants. For each step it says what is measured, which number to change, and what "good" looks like.
- Mechanism tuning (velocity PIDF, servo positions, delays) is done live through Dashboard `*Config` values. Good values are then typed back into the config file. The guide says to commit them to git with the date and the field type.

## Documentation deliverables (R4, R5)

The documents MUST be Markdown files in `docs/` in the repository, so they change in the same commits as the code. They MUST be written for a student reader: short sentences, words defined on first use, and ASCII diagrams that show in Android Studio.

### High-level design: `docs/HLD.md`

1. Goals and constraints: the BioBuzz game summary, the match phases (auto, teleop, endgame; confirm the timings in the BioBuzz game manual), and what the robot must do in each.
2. Robot overview: each mechanism, its job in the game, and its hardware (motors, servos, sensors).
3. Software layers: the architecture diagram and the layer rules in this document.
4. Subsystem list: one paragraph each, covering purpose, states (names only), commands, and the subsystems it links to through `Robot`.
5. Control scheme: both gamepad maps.
6. Autonomous plan: routines, the path drawn on the field, and step lists.
7. Data flow: telemetry, match log and alerts. Says where each goes and who reads it.
8. Risks and fallbacks: for example, what happens if odometry fails mid-match.

### Low-level design: `docs/LLD.md`

1. One section per subsystem: an ASCII state diagram; a table of each state with its DO, CHECK and GO and its timeout; each command and which states accept it; config values with units and starting values; telemetry and log fields.
2. One section per part: devices, `HardwareNames` entries, directions, verbs and sensor getters.
3. The `Robot.update()` order, and each subsystem link with the reason for it.
4. Autonomous: a table of each step with its command, its finish condition and its timeout. Lists every `AutoConfig` pose.
5. Helpers: the public methods of `MatchLogger`, `MatchInfo`, `BatteryMonitor`, `LoopMonitor`, `HealthMonitor`, `FieldCheckHelper`, `StateLog`, `StateTimer`, `GamepadInput`, `InitMenu`, `PoseMemory`, `FieldDrawing` and `LogSummary`.
6. Match log format: every column with its unit, and every EVENT type.
7. The ALERT list, with each threshold and config name.
8. The heading safeguards and recovery buttons: each check, its threshold, and what the drive does.
9. Field Checks: each test, what it measures, its baseline value and tolerance, and the action for WARN and FAIL.
10. **Known implementation challenges**: each problem found while building, the options considered, what was chosen and why. It MUST cover at least:
    - **Pinpoint access.** Pedro's `PinpointLocalizer` keeps its `GoBildaPinpointDriver` private, but the safeguards need `getDeviceStatus()`, `getFrequency()` and `recalibrateIMU()`. One option is a second handle to the same device from `hardwareMap` in `DriveBase`. The LLD records what was chosen and its risks: two objects talking to one device, recalibrating while the follower reads it, and resetting the pose after a Pinpoint restart.
    - **Dashboard field background.** How the BioBuzz field image is loaded by the Dashboard canvas (from the app's assets or a URL), and its scale and origin in inches.
    - **Constants depends on team code.** `pedro/Constants.java` imports `HardwareNames`, and what that means for Quickstart merges.

### Student guides

| File | Answers |
| --- | --- |
| `README.md` | Where do I start? It gives the reading order (as in DecodeBot), state machines in 60 seconds, and a vocabulary list. |
| `docs/GETTING_STARTED.md` | How do I install Android Studio, open the project, build, deploy to the robot, and pick an OpMode? |
| `docs/FIRST_CHANGES.md` | Five small exercises in order: change a speed, change a button, add a telemetry line, add a state with a timeout, add an auto step. Each exercise lists the files to open and how to check the result. |
| `docs/HOW_TO_ADD_A_MECHANISM.md` | How do I copy `template/`, add names and config, build it in `Robot`, add a test OpMode, wire buttons, and add log fields? |
| `docs/AUTO_GUIDE.md` | How do I measure poses, edit `AutoConfig`, add a step, mirror for red, and test with the robot on blocks? |
| `docs/CONTROLS.md` | What does each button do? A printable diagram of both gamepads. |
| `docs/DEBUGGING.md` | What is wrong? The 6 questions (from DecodeBot) plus ALERTs and how to read a match log. |
| `docs/TUNING.md` | What order do I tune in, and what does good look like? |
| `docs/MATCH_DAY.md` | What do I check before and between matches, and what is the field check? |
| `docs/MATCH_LOGS.md` | How do I get logs off the robot, read them, and find the cause of a bad match? |
| `docs/CAMERA_GUIDE.md` | How do I add vision? Only needed if Vision is used. |
| `docs/GLOSSARY.md` | What do the words mean? State, command, part, subsystem, pose, field-centric, loop, deadband, PIDF. |
| `docs/UPDATING_PEDRO.md` | How do I merge a new Quickstart release, resolve a Constants.java conflict, test the result and record it in UPSTREAM.md? |

Every guide MUST name exact file paths and method names, and MUST stay correct as the code changes. A pull request that changes behavior also updates the guides it affects.

## Open items and acceptance

The team MUST give the AI the facts below before it writes BioBuzz-specific code. Until then, the AI builds the skeleton, drive, troubleshooting tools and documents. It marks every gap with `TODO(BioBuzz)`.

**Open items (team to supply)**

- [ ] BioBuzz game manual: scoring elements, scoring locations, match phases and timings, endgame tasks
- [ ] Mechanism list, with each mechanism's job and its motors, servos and sensors
- [ ] Robot configuration names from the Control Hub, for `HardwareNames`
- [ ] Field drawing: start positions, scoring poses, mirror axis between alliances, driver-facing heading per alliance
- [ ] Odometry: Pinpoint or other localizer, pod type and offsets (DecodeBot uses Pinpoint with goBILDA 4-bar pods)
- [ ] Whether Vision (AprilTags) is needed, and for what
- [ ] BioBuzz field image for the Dashboard background (`field-biobuzz.png`), from the game manual's field drawing
- [ ] Gamepad 2 button map, once the mechanisms are known
- [ ] Autonomous routines wanted for the first competition

**Acceptance checklist** (mentor signs off)

- [ ] The project builds with no warnings in the `biobuzz` package. DecodeBot code is unchanged.
- [ ] No forbidden Java constructs appear in `biobuzz/` outside `helpers/` (see Coding style rules).
- [ ] Driving is field-centric, robot-centric is used only by the safeguard fallback or X, and the pose is handed from auto to teleop.
- [ ] Only OpModes read gamepads, through `GamepadInput`. In a match, gamepad 1 only drives and gamepad 2 only runs mechanisms.
- [ ] Every subsystem follows the four-section layout, has timeouts on its waiting states, and has a test OpMode.
- [ ] Telemetry shows on both the Driver Station and the Dashboard, with the match header line and per-subsystem switches. The Dashboard field view shows the robot pose on the BioBuzz field.
- [ ] Every Field Check OpMode runs and writes its log, and `Field Check 13: Summary` shows all results against the home baseline. `Robot Check` ends with READY FOR MATCH.
- [ ] Pulling a motor cable during teleop raises an ALERT, puts that subsystem in `FAULT`, and does not crash the OpMode.
- [ ] A full teleop run writes a match log with DATA and EVENT rows. `Log Viewer` summarizes it.
- [ ] Unplugging the Pinpoint while driving switches to robot-centric with a rumble and an ALERT, and Y returns to field-centric.
- [ ] A practice match's Auto and TeleOp logs are named with the same match id, and `plot_match.py --match <id> --at 2.0` shows what happened 2 s into the match, with battery voltage.
- [ ] The loop time averages under 25 ms with logging and telemetry on.
- [ ] HLD, LLD and every guide in Documentation deliverables exist, and their file and method names match the code.
- [ ] A student new to the code completes `FIRST_CHANGES.md` exercise 1 to 3 without help.

## Revision history

| Version | Date | Changes |
| --- | --- | --- |
| 1.3 | 2026-10-18 | Renamed the file to `BioBuzz-Requirements.md`; the version lives in the document only. `Constants.java` and `Tuning.java` stay as Quickstart stubs until tuning, then get a one-time structural fill-in recorded in `UPSTREAM.md`; `Constants` imports `HardwareNames`. Only OpModes read gamepads (through `GamepadInput`). One INIT menu (`InitMenu`, D-pad rows) replaces the separate INIT buttons. Field drawing goes through `helpers/FieldDrawing.java` with a BioBuzz field image asset. Field Check 07 carries its own copy of Pedro's deceleration method. Added `LogSummary`, `GamepadInput` held reads, the field-centric wording fix, and the LLD's Known implementation challenges (Pinpoint access first). |
| 1.2 | 2026-10-18 | Added R11 and R12 and a new section, Base project and repository. The base is the Pedro Pathing Quickstart for Pedro 3.x, in a new `biobuzz` repository that stays up to date with the public Quickstart. Quickstart files are never moved, and team code goes in `teamcode/biobuzz/` next to `teamcode/pedro/`. Updated the code layout, testing and tuning, deliverables and acceptance sections to match. |
| 1.1 | 2026-10-18 | Confirmed the change to R1: field-centric is the normal mode, and robot-centric is a safeguard fallback or a manual switch on gamepad 1 X. Added the table of contents and this revision history. |
| 1.0 | 2026-10-18 | First version: the confirmed requirements, coding style, code layout, architecture, driving, operator controls, autonomous, telemetry, troubleshooting (sections A to G), testing, documentation deliverables, open items and acceptance. |
