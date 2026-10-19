# BioBuzz Software – High-Level Design (HLD)

Oct 19, 2026 · **Draft for review** · goes with Requirements v2.1 and `LLD.md`

This document explains how the BioBuzz robot software is put together, and why. It is written for students: read it from top to bottom once, then use `LLD.md` for the details of each class.

Words in CAPITALS (POLLEN, CELL, …) are game words from the BIOBUZZ Competition Manual.

---

## 1. The game and what the robot must do

**BIOBUZZ in one paragraph.** Two alliances of two robots collect POLLEN (yellow balls, 2.8 in) and NECTAR (red or blue balls, 3.6 in). They LAUNCH them into the upward-facing CELL of their HIVE, in the middle of the field. When enough land in the CELL, the HIVE TIPS: the other CELL now faces up, the alliance scores 20 points, and their human player may put one more NECTAR on the field. In the last 60 seconds, alliances can also put NECTAR into the FLOWERS on the walls, and every robot tries to PARK in its LOADING ZONE.

**Match timeline**

```
 0:00          0:30   0:38                          1:38           2:18   2:38
  |---- AUTO ----|-tr-|------------- TELEOP --------------|--------------|----|
  robot alone    8 s   drivers                    60 s left:      20 s left:  end
  LEAVE, launch  no    collect, launch, TIP       NECTAR unlocked, PARK
  preloads, PARK move                             FLOWERS open
  (times since the start of AUTO; TeleOp's own clock runs 2:00 -> 0:00)
```

**Points we plan to score**

| Action | Auto | TeleOp | How |
| --- | --- | --- | --- |
| LEAVE (move off the wall) | 3 | – | First auto step |
| HIVE TIP | 20 each | 20 each | Launch into the upward CELL |
| Element left in an upward CELL at the end | – | 2 each | Launching |
| PARK in the LOADING ZONE | 5 | 5 | Last auto step; S2 assist button in TeleOp |
| GARDEN (element in the GARDEN at the end) | – | 1 each | Pushing elements, no special code |
| FLOWER (Bottom NECTAR Bonus, owned FLOWER) | – | – | **Not planned**: the robot has no FLOWER placer |

**Ranking points we aim for:** SWARM (LEAVE + PARK ≥ 16 per alliance) and POLLINATOR (4 and 7 TIPS).

**Game rules that the software must respect** (details in Requirements v2.0, Game requirements)

| Rule | Software must |
| --- | --- |
| G304.H, G403: no movement after INIT, nor between AUTO and TELEOP | Never move an actuator in TeleOp INIT; in Auto INIT only grip the preloads |
| G407: control at most 4 elements | Count what Storage holds; stop the intake at 4 |
| G408: never control opponent NECTAR | Check colour at the intake; push opponent NECTAR straight back out |
| G402: stay on your own side in AUTO | Check every auto path at INIT |
| G409, G417: don't touch the HIVE or catch what it drops | Keep paths out of the HIVE footprint |

---

## 2. The robot

| Mechanism | Job in the game | Hardware |
| --- | --- | --- |
| **Drive** | Mecanum drivetrain; field-centric driving; Pedro Pathing paths | 4 drive motors (Control Hub), goBILDA Pinpoint odometry |
| **Intake** | Picks up POLLEN from the tiles and the FLOWER Retrieval Opening, and NECTAR from the LOADING ZONE | TODO(BioBuzz): roller motor |
| **Storage** (with feeder) | Holds up to 4 elements, counts them, knows their colour, feeds one at a time to the launcher | TODO(BioBuzz): feeder motor or servo; colour and distance sensors (Q5) |
| **Launcher** | Flywheel that launches POLLEN or NECTAR into the upward CELL | TODO(BioBuzz): flywheel motor(s) with encoder |
| **HIVE camera** | Looks up at the AprilTags under the CELLS: which CELL is up, and the aim offset | DFRobot HuskyLens, I2C |
| **Front camera** | Identifies field AprilTags, POLLEN and NECTAR ahead of the intake, so collecting can aim and opponent NECTAR can be avoided | Logitech USB webcam (SDK `VisionPortal`) |

Limits (R503, R701): at most 8 motors and 8 servos, a Control Hub and at most one Expansion Hub. The drive uses 4 motors.

---

## 3. The software layers

```
  +----------------------------------+   +----------------------------------+
  | teleop/BioBuzzTeleOp             |   | auto/BioBuzzAuto                 |
  |  gamepad 1 -> driving (S1, S2)   |   |  enum Step + switch (S3)         |
  |  gamepad 2 -> mechanisms         |   |  one step = start, check, timeout|
  +----------------+-----------------+   +----------------+-----------------+
                   |  calls Robot methods                 |
                   v                                      v
  +--------------------------------------------------------------------------+
  | Robot   the robot's "menu": startCollecting(), startLaunching(), ...     |
  |         builds everything; update() once per loop:                       |
  |         1. state machines  2. mechanisms  3. Drive (Pedro)  4. rules     |
  +------+-------------------------------+-----------------------------------+
         |                               |
         v                               v
  statemachines/                    drive/Drive         game/
   CollectStateMachine               (Pedro follower)    FieldPaths  (every path)
   LaunchStateMachine                                    HiveState   (which CELL is up)
         | use
         v
  mechanisms/  (each extends Mechanism)
   Intake   Storage   Launcher   HiveCamera   FrontCamera
         |
         v
  Control Hub + Expansion Hub devices

  used by every layer:  configurations/ (tunable numbers, HardwareNames, FieldConfig)
                        helpers/        (StateTimer, StateLog, GamepadInput, MatchClock,
                                         MatchLogger, MatchInfo, PoseMemory, ...)
```

**What each layer does, and must never do**

| Layer | Does | Must never |
| --- | --- | --- |
| OpMode (`teleop/`, `auto/`, `testing/`, `fieldchecks/`) | Reads gamepads (only OpModes do). Calls Robot methods. Calls `robot.update()` and telemetry once per loop. Auto keeps its own step list | Touch a motor or servo. Hold mechanism logic |
| `Robot` | Builds everything. One method per robot action. Runs `update()` in a fixed order. Cancels state machines that share a mechanism. Holds the always-on rules (camera, safeguards) | Read gamepads. Set a motor power itself |
| State machine (`statemachines/`) | One task that takes time and may use several mechanisms. `enum State`, `StateTimer`, `StateLog`, a timeout on every waiting state | Call another state machine or a Robot method. Read gamepads |
| `Drive` (`drive/`) | Driving operations over Pedro: sticks, paths, pose | Decide when to drive a path (the OpMode decides) |
| Mechanism (`mechanisms/`) | Wraps one group of devices. Plain verbs (`collect()`, `stop()`) and sensor checks (`isUpToSpeed()`) | Keep states or timers. Know about other mechanisms |
| `game/` | What is special about BIOBUZZ: paths and the HIVE's state | Touch hardware |

**Where does my code go?**

| If it... | It goes in |
| --- | --- |
| happens when a button is pressed | TeleOp (`handleDriver()` or `handleOperator()`) |
| moves or reads one device group | that mechanism's class |
| takes several steps over time, or needs two mechanisms together | a state machine, started by a Robot method |
| must always happen, in every OpMode | `Robot.update()` |
| is a place or a path on the field | `FieldConfig` (poses) and `FieldPaths` (paths) |
| is a number someone will tune | a `*Config` class |

---

## 4. Design patterns (with their real names)

Professional programmers use names for designs that work well. Our code uses these, in their simplest form:

| Pattern | In our code | In one sentence |
| --- | --- | --- |
| **Layered architecture** | OpMode → Robot → state machines → mechanisms | Each layer only calls the layer below it |
| **Game Loop** and **Update Method** | `loop()` → `robot.update()` → every `update()` | Every loop, everything takes one small step; nothing waits |
| **Facade** | `Robot`, `Drive` | One simple front for many parts behind it |
| **Adapter** | `Intake`, `Launcher`, `HiveCamera`, `FrontCamera`, `GamepadInput` | Turns device calls into game words |
| **Template Method** | `Mechanism.runUpdate()` calls your `update()` | The base class fixes the order and safety; you fill in the steps |
| **State pattern** (enum + switch) | `LaunchStateMachine`, Auto's steps | The object behaves differently depending on its current state |
| **Mediator** | `Robot.startLaunching()` cancels collecting first | Objects that could clash don't talk to each other; one coordinator does |
| **Dependency injection** | `new LaunchStateMachine(storage, launcher, hive)` | A class is given what it needs, it doesn't go and find it |
| **Factory method** | `FieldPaths.toLaunchSpot(from, ...)` | One method makes objects of one kind, so every user gets the same result |

**Not used, on purpose:** the Command pattern and command schedulers (FTCLib, NextFTC), interfaces and lambdas. They are powerful, but they hide what runs when, and R3 keeps the code readable for new programmers.

---

## 5. The building blocks

**Robot** builds everything in its constructor, in this order: Drive, the mechanisms, `HiveState`, then the state machines (which are given the mechanisms they use). It keeps the mechanisms in one array, so `update()`, `stopMechanisms()`, telemetry and logging are a loop over the array.

**Mechanism base class.** Every mechanism `extends Mechanism`. The base class is plain (not abstract) and has empty "hook" methods that a mechanism fills in:

```
Robot calls (fixed, in Mechanism)          A mechanism fills in (hooks)
  runUpdate()  --skip if FAULTED,  ------>   update()        every loop: control loops, sensor reads
               catch any error              stop()          safe state (motors off)
  addTelemetry(t) --name + FAULT -------->   addDetails(t)   its own telemetry lines
  addLogData(row) ----------------------->   addLogValues(row)
```

An error inside a mechanism marks it **FAULTED** (with the reason on telemetry and in the log). The rest of the robot keeps running.

**State machines** are standalone classes in `statemachines/`, all with the same four sections: States, Commands (`start`, `cancel`), `update()` with DO / CHECK / GO, and Bookkeeping (`goTo`, `isRunning`, `getState`, `addTelemetry`). Robot lists each one by hand in `update()`, `stopMechanisms()` and telemetry.

| State machine | Uses | Job |
| --- | --- | --- |
| `CollectStateMachine` | Intake, Storage | Run the intake until Storage holds 4; reject opponent NECTAR; stop at 4 (G407, G408) |
| `LaunchStateMachine` | Storage, Launcher, HiveState | Spin up for the element type and distance, feed one, wait, repeat; pause after a TIP |

**Drive** is not a Mechanism: stop-all never stops the drivetrain, and it must update after the mechanisms. It has two groups of operations. **Driver driving:** `driveWithSticks`, `resetHeading`, `switchDriveMode`. **Path driving:** `followPath`, `isDrivingItself`, `isPathDone`, `holdHere`, `stopDrivingItself`. Speed limits for paths come from Pedro's `Constants`.

**game/** holds what will change next season: `FieldPaths` (every Pedro path, auto and assists, the only place Pedro's path builder is used) and `HiveState` (which CELL is up, how many TIPS, NECTAR the human player may enter).

---

## 6. Driving: three stages

| Stage | Who drives | Needs |
| --- | --- | --- |
| **S1 Manual** | Driver: field-centric sticks, Y = heading reset, X = field/robot-centric | The heading only |
| **S2 Assisted** | Driver, plus buttons that drive one short fixed path (to the launch spot for the upward CELL, to the LOADING ZONE). The robot holds at the end. **Any stick takes the robot back in the same loop** | The full pose (x, y, heading) |
| **S3 Autonomous** | Auto's step list | The start pose |

S1 and S2 are the same TeleOp: assists are just buttons. **TeleOp starts as S1 when there is no auto pose** (robot restarted, or no auto ran): assist buttons are refused with a warning until the pose is known again (Q1, Q4 in the review).

```
TeleOp, gamepad 1, every loop:
  if assist button pressed  -> Robot builds a path from the current pose and Drive follows it
  if a stick moved, or the robot is not driving itself -> drive with the sticks
```

---

## 7. Controls

**Gamepad 1 (driver)** – driving only (R2)

| Control | Action |
| --- | --- |
| Left stick / right stick X | Move (field-centric) / turn |
| Y | "Robot faces away from the drivers now" (heading reset, field-centric back on) |
| X | Switch field-centric / robot-centric |
| A | S2: drive to the launch spot for the upward CELL |
| B | S2: drive to our LOADING ZONE (park, or pick up NECTAR) |
| Right bumper (hold) | Aim: keep facing the upward CELL while driving (Pedro `headingLock`) |
| Right stick button | Snap heading to the nearest 90° (robot against a wall) |
| Left stick button (hold 1 s) | Recalibrate the gyro |
| D-pad left (hold 1 s) | Set pose here: robot backed into the LOADING ZONE corner (Q4) |
| Back / Share | Mark a moment in the match log |
| D-pad (INIT only) | INIT menu |

**Gamepad 2 (operator)** – mechanisms only (R2). TODO(BioBuzz): confirm once the robot is built.

| Control | Action |
| --- | --- |
| A | Start collecting (stops by itself at 4) |
| B (hold) | Reverse intake (spit out) |
| Right trigger | Launch everything held |
| Left bumper | "The HIVE just TIPPED" (flips `HiveState`) |
| Y | Spin the launcher up early |
| D-pad down | **Stop all mechanisms** |
| Back / Share | Mark a moment in the match log |

---

## 8. Autonomous plan

Auto is one OpMode with routines picked in the INIT menu (Alliance, Start, Routine, Delay, match info). Each step starts something on its first loop, then checks "done" and "timeout". A **park-by guard** jumps to PARK at `AutoConfig.PARK_BY_S`, whatever step is running, because PARK and LEAVE feed the SWARM RP.

```
NEAR routine (TODO(BioBuzz): confirm, Q10)
  WAIT_DELAY -> DRIVE_TO_LAUNCH -> LAUNCH_PRELOADS -> COLLECT_GARDEN -> DRIVE_TO_LAUNCH_2
             -> LAUNCH_2 -> PARK -> DONE
FAR routine
  WAIT_DELAY -> DRIVE_TO_LAUNCH -> LAUNCH_PRELOADS -> PARK -> DONE
```

Poses are written once for BLUE in `FieldConfig`; RED is the same pose turned 180° about the field centre (the field is rotationally symmetric; `mirrorX(72).mirrorY(72)`). Every path is checked at INIT: it must stay on our side (G402) and out of the HIVE footprint (G409, G417).

---

## 9. Vision (two cameras)

The AprilTags are under the CELLS, facing down.

| Camera | Device | Sees | Use |
| --- | --- | --- | --- |
| HIVE camera (looks up) | HuskyLens, I2C | CELL tags as 2D boxes, with IDs it learned itself | Which CELL is up (the operator's TIP button stays the first source, the camera confirms); aim: is the upward CELL left or right of centre |
| Front camera (looks ahead) | Logitech USB webcam, `VisionPortal` | Real AprilTag IDs with a tag pose, and POLLEN / NECTAR colour blobs, both at once | Aim the collection; see opponent NECTAR before the intake takes it (G408); field tags when in view |

Fixing the robot's x/y from a camera is not planned at first. The webcam's tag pose makes it possible later (LLD, Future improvements); until then "set pose here" is the recovery.

Each camera is a mechanism (`HiveCamera`, `FrontCamera`, both `extends Mechanism`). They only answer questions; Robot decides whether to use an answer. The webcam's vision runs on its own thread inside the SDK, so it never slows the loop directly, but it uses Control Hub CPU: the live preview is off in matches.

---

## 10. Data flow

```
 mechanisms, state machines, Drive, helpers
        |  addTelemetry / addLogData / StateLog / ALERTs
        v
      Robot ----> Driver Station + FTC Dashboard (DashboardTelemetry, FieldDrawing)
        |  ------> match log CSV  (MatchLogger: DATA rows 20 Hz + EVENT rows)
        |  ------> newest urgent ALERT -> the OpMode rumbles the gamepad
        v
 drive team (screen, rumble)      pits (Log Viewer)      laptop (plot_match.py)
```

The telemetry header always shows: alliance, match time left, loop time, battery, field/robot-centric, held elements and their colours, the upward CELL, and any ALERT.

---

## 11. Growing the robot during the season

| To add... | Do this (exact steps in `LLD.md`, section 11) |
| --- | --- |
| A mechanism | Copy `template/TemplateMechanism.java`; names in `HardwareNames`; a `*Config`; in Robot add a field and one entry in the mechanisms array; copy the test OpMode; add gamepad 2 buttons through a Robot method |
| A state machine | Copy `template/TemplateStateMachine.java`; build it in Robot with the mechanisms it uses; add one line each to `update()`, `stopMechanisms()` and telemetry; add a Robot start method that cancels any state machine sharing a mechanism |
| An assist (S2) | A pose in `FieldConfig`, a path method in `FieldPaths`, a Robot method, one gamepad 1 line |
| An auto step | One `enum Step` value and one `case`, with its timeout in `AutoConfig` |

---

## 12. Risks and fallbacks

| Risk | What happens |
| --- | --- |
| Odometry (Pinpoint) fails mid-match | Drive switches to robot-centric with a rumble and an ALERT; assists are refused; the driver drives as S1 |
| Heading drifts | Y (facing away), snap to 90°, recalibrate (gamepad 1) |
| Pose lost after a restart | TeleOp starts as S1; "set pose here" in the LOADING ZONE corner brings S2 back |
| A mechanism device disconnects | That mechanism is FAULTED and stops; ALERT names it; the rest keeps working |
| Storage miscounts (holds 5) | Intake reverses briefly, ALERT, rumble (G407) |
| Opponent NECTAR picked up | Pushed back out at once and logged (G408) |
| `HiveState` wrong (missed TIP button) | The operator presses the TIP button again to flip it; the header shows the upward CELL so the drive team can see it |
| Auto runs late | Park-by guard: PARK always happens |
| Loop gets slow | ALERT; first fix is bulk reads (LLD, Future improvements) |
| Low battery | ALERT and rumble; logged with every row for the debrief |
