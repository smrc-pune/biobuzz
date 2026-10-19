# BioBuzz Software – High-Level Design (HLD)

Oct 19, 2026 · **Draft for review** · goes with Requirements v2.5 and `LLD.md`

This document explains how the BioBuzz robot software fits together. It is written for our team: 7th graders who are new to Java. Read it once from top to bottom. Use `LLD.md` when you need the details of one file.

Words in CAPITALS (POLLEN, CELL, …) are game words from the BIOBUZZ Competition Manual.

---

## 1. The game and what the robot must do

**BIOBUZZ in one paragraph.** Two alliances of two robots collect POLLEN (yellow balls, 2.8 in) and NECTAR (red or blue balls, 3.6 in). They LAUNCH them into the upward-facing CELL of their HIVE, in the middle of the field. A robot can launch from anywhere: there are no launch zones. When enough balls land in the CELL, the HIVE TIPS: the other CELL now faces up, the alliance scores 20 points, and their human player may put one more NECTAR on the field. In the last 60 seconds, alliances can also put NECTAR into the FLOWERS on the walls, and every robot tries to PARK in its LOADING ZONE.

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
| Ball left in an upward CELL at the end | – | 2 each | Launching |
| PARK in the LOADING ZONE | 5 | 5 | Last auto step; a one-button assist in TeleOp |
| GARDEN (ball in the GARDEN at the end) | – | 1 each | Pushing balls, no special code |
| FLOWER | – | – | **Not planned**: the robot has no FLOWER placer |

**Ranking points we aim for:** SWARM (LEAVE + PARK ≥ 16 per alliance) and POLLINATOR (4 and 7 TIPS).

**Game rules the software must respect** (details in the Requirements, Game requirements)

| Rule | Software must |
| --- | --- |
| G304.H, G403: no movement after INIT, nor between AUTO and TELEOP | Never move anything in TeleOp INIT; in Auto INIT only grip the preloads |
| G407: control at most 4 balls | Nothing in software: the hopper physically holds at most 4 |
| G408: never control opponent NECTAR | Nothing in software: the drive team avoids it, and reverses the intake if one gets in |
| G402: stay on your own side in AUTO | Check every auto path at INIT |
| G409, G417: don't touch the HIVE or catch what it drops | Keep paths away from the HIVE |

---

## 2. The robot

| Part of the robot | Job in the game | Hardware |
| --- | --- | --- |
| **Drive** | Mecanum drivetrain; field-centric driving; Pedro Pathing paths | 4 drive motors (Control Hub), goBILDA Pinpoint odometry |
| **Intake** | Picks up POLLEN from the tiles and the FLOWER Retrieval Opening, and NECTAR from the LOADING ZONE | TODO(BioBuzz): roller motor |
| **Feeder** | Pushes one ball at a time from the hopper into the launcher | TODO(BioBuzz): motor or servo |
| **Launcher** | Flywheel that launches POLLEN or NECTAR into the upward CELL | TODO(BioBuzz): flywheel motor(s) with encoder |
| **HIVE camera** | Looks up at the AprilTags under the CELLS: which CELL is up, and where to aim | DFRobot HuskyLens, I2C |
| **Front camera** | Sees field AprilTags, POLLEN and NECTAR in front of the robot | Logitech USB webcam |

The hopper (where up to 4 balls wait) has no sensors, so the robot does not know how many balls it holds or their colours. A Storage mechanism with sensors is a future improvement.

Limits (R503, R701): at most 8 motors and 8 servos, a Control Hub and at most one Expansion Hub. The drive uses 4 motors.

---

## 3. How the code fits together

```
  +----------------------------------+   +----------------------------------+
  | teleop/BioBuzzTeleOp  (S1, S2)   |   | auto/BioBuzzAuto  (S3)           |
  |  gamepad 1 -> driving            |   |  a list of steps                 |
  |  gamepad 2 -> mechanisms         |   |  each step: start, check, timeout|
  +----------------+-----------------+   +----------------+-----------------+
                   |      call Robot methods              |
                   v                                      v
  +--------------------------------------------------------------------------+
  | Robot   the robot's "menu": toggleIntake(), startLaunching(), ...        |
  |         builds everything; update() once per loop:                       |
  |         1. state machines  2. mechanisms  3. Drive  4. always-on rules   |
  +------+-------------------+------------------------------+----------------+
         | starts, updates   | updates every mechanism      | drives
         v                   | once per loop                v
  statemachines/             |                        drive/
   LaunchStateMachine        |                         Drive       (sticks, paths, pose)
         | commands          |                         FieldPaths  (every path on the field)
         v                   v                              |
  mechanisms/ <--------------+                              v
   Intake  Feeder  Launcher                  ..........................................
   HiveCamera  FrontCamera                   : Pedro Pathing   (off the shelf)        :
   (each extends Mechanism)                  : follows paths, holds the robot still,  :
         |                                   : knows where the robot is. Set up in    :
         |                                   : the Quickstart's pedro/Constants.java  :
         |                                   :........................................:
         v                                                  v
  ..........................................................................
  : FTC SDK + FTC Dashboard   (off the shelf)                               :
  : hardwareMap, motors, servos, sensors, cameras, gamepads, telemetry      :
  :        -> Control Hub + Expansion Hub                                    :
  :..........................................................................:

  solid boxes = our code        dotted boxes = libraries we use as they come; we never edit them

  used everywhere:  configurations/  (every number and name we tune)
                    helpers/         (tools: timers, gamepad reading, match info, logging)
```

**Why Robot updates the mechanisms itself, not the state machines.** A part may call any part below it, never one above. Every mechanism needs its `update()` once per loop even when no state machine is using it: the cameras read every loop, and the launcher holds its speed. A mechanism used by two state machines must still be updated exactly once. So Robot updates every mechanism, and state machines only give mechanisms commands (`feeder.feedOne()`).

**What each part does, and must never do**

| Part | Does | Must never |
| --- | --- | --- |
| OpMode (`teleop/`, `auto/`, `testing/`) | Reads the gamepads (only OpModes do). Calls Robot methods. Calls `robot.update()` and telemetry once per loop. Auto keeps its list of steps | Touch a motor or servo |
| `Robot` | Builds everything. One method for each thing the robot can do. Runs `update()` in a fixed order. Stops two state machines from fighting over one mechanism | Read gamepads. Set a motor power itself |
| State machine (`statemachines/`) | One job that takes time, maybe using several mechanisms (for example "launch 4 balls": spin up, feed, wait, repeat). It gives mechanisms commands | Call another state machine, or a mechanism's `update()`. Read gamepads |
| `Drive` and `FieldPaths` (`drive/`) | Driving: sticks, paths, where the robot is. The only code that talks to Pedro | Decide when to drive a path (the OpMode decides) |
| Mechanism (`mechanisms/`) | One group of devices, with action words (`collect()`, `stop()`) and questions (`isUpToSpeed()`) | Remember states or wait. Know about other mechanisms |
| Libraries (Pedro, FTC SDK, Dashboard) | Do the hard work: path following, talking to hardware, the Dashboard | Get edited by us (Pedro only gets tuned numbers in `pedro/Constants.java`) |

**Where does my code go?**

| If it... | It goes in |
| --- | --- |
| happens when a button is pressed | TeleOp: `handleDriver()` or `handleOperator()` |
| moves or reads one group of devices | that mechanism's file |
| takes several steps over time, or needs two mechanisms together | a state machine, started by a Robot method |
| must always happen, in every OpMode | `Robot.update()` |
| is a place or a path on the field | `FieldConfig` (places) and `FieldPaths` (paths) |
| is a number someone will tune | a `...Config` file |

**Who changes which files.** Students change the **student zone**: `configurations/`, `mechanisms/` (except `Mechanism.java`), `statemachines/`, `teleop/`, `auto/`, `drive/FieldPaths.java`, the marked spots in `Robot.java`, and the tests. The **mentor zone** (`Mechanism.java`, `Drive.java`, `helpers/`, `fieldchecks/`, `pedro/`) works quietly in the background. Every file says which zone it is in on its first line.

---

## 4. The building blocks

**Robot** builds everything when the OpMode starts: Drive, the mechanisms, then the state machines (each is handed the mechanisms it uses). Robot keeps the mechanisms in one list, written by name:

```java
mechanisms = new Mechanism[] { intake, feeder, launcher, hiveCamera, frontCamera };
```

and goes through it with a "for each" loop, so there are no index numbers to remember:

```java
for (Mechanism mechanism : mechanisms) {
    mechanism.runUpdate();
}
```

Everywhere else, a mechanism is used by its name: `intake.collect()`.

**Every mechanism extends `Mechanism`.** You only fill in up to four methods; `Mechanism` (mentor zone) does the safety around them:

```
What you write in your mechanism         What Mechanism does around it
  update()        every loop               skips it if the mechanism is FAULTED, catches any error
  stop()          safe state (motors off)  catches any error
  addDetails(t)   your telemetry lines     shows "FAULTED: reason" instead, when faulted
  addLogValues()  your match-log values    adds the faulted flag
```

If something goes wrong inside a mechanism (for example a cable comes out), that mechanism is marked **FAULTED**, stops, and an ALERT names it. The rest of the robot keeps running.

**State machines** all look the same inside: 1. the list of states, 2. commands (`start()`, `cancel()`), 3. `update()` with DO / CHECK / GO in each state, 4. bookkeeping (`goTo()`, `isBusy()`, telemetry). Every state that waits has a timeout.

| State machine | Uses | Job |
| --- | --- | --- |
| `LaunchStateMachine` | Feeder, Launcher, Drive (where are we?) | Spin up for the distance to the upward CELL, feed one ball, wait, repeat; wait a moment after a TIP |

The intake needs no state machine: the operator turns it on and off.

**Drive** is not a mechanism: stop-all never stops the wheels. It has driver actions (`driveWithSticks`, `resetHeading`, `switchDriveMode`) and path actions (`followPath`, `isDrivingItself`, `isPathDone`, `holdHere`, `stopDrivingItself`). **FieldPaths** builds every path, for Auto and for the TeleOp assist buttons. They are the only two files that use Pedro.

**The HIVE.** Which of our two CELLS faces up, and how many TIPS we have, are kept in `MatchInfo` with the alliance, so TeleOp knows what happened in Auto. The operator presses a button at each TIP.

---

## 5. Driving: three stages

| Stage | Who drives | Needs |
| --- | --- | --- |
| **S1 Manual** | The driver: field-centric sticks, Y = heading reset, X = field/robot-centric | The heading only |
| **S2 Assisted** | The driver, plus buttons that drive one short fixed path (to our favourite launch spot, to our LOADING ZONE). The robot holds still at the end. **Any stick takes the robot back straight away** | The full position (x, y, heading) |
| **S3 Autonomous** | Auto's list of steps | The start position |

S1 and S2 are the same TeleOp: assists are just buttons. **TeleOp starts as S1 when there is no position from Auto** (the robot restarted, or no Auto ran): assist buttons say "position unknown" until the driver uses "set pose here".

We build the stages in this order: S1 (works today), then S3 (Auto proves the paths and the position), then S2 (reuses Auto's paths).

---

## 6. Controls

**Gamepad 1 (driver)** – driving only

| Control | Action |
| --- | --- |
| Left stick / right stick X | Move (field-centric) / turn |
| Y | "The robot faces away from the drivers now" (heading reset, field-centric back on) |
| X | Switch field-centric / robot-centric |
| A | S2: drive to our favourite launch spot |
| B | S2: drive to our LOADING ZONE (park, or pick up NECTAR) |
| Right bumper (hold) | Aim: keep facing the upward CELL while driving |
| Right stick button | Snap heading to the nearest 90° (robot against a wall) |
| Left stick button (hold 1 s) | Recalibrate the gyro |
| D-pad left (hold 1 s) | Set pose here: robot sitting in our reset corner (back on the left wall, side on the far wall) |
| Back / Share | Mark a moment in the match log |
| D-pad left/right (INIT only) | Switch alliance RED / BLUE |

**Gamepad 2 (operator)** – mechanisms only. TODO(BioBuzz): confirm once the robot is built.

| Control | Action |
| --- | --- |
| A | Intake on / off |
| B (hold) | Reverse the intake (spit out, also for opponent NECTAR) |
| Right trigger | Launch 4 balls |
| Left bumper | "Our HIVE just TIPPED" |
| Y | Spin the launcher up early |
| D-pad down | **Stop all mechanisms** |
| Back / Share | Mark a moment in the match log |

---

## 7. Autonomous plan

Auto is one OpMode. In INIT the drive team picks only the alliance (RED or BLUE): it changes every match. The routine, start position and start delay are agreed with the alliance partner before the match and set in `AutoConfig`. Each step starts something on its first loop, then checks "done?" and "out of time?". A **park-by guard** jumps to PARK at `AutoConfig.PARK_BY_S`, whatever step is running, because PARK and LEAVE feed the SWARM ranking point.

There are no launch zones, so the robot launches its preloads right after it leaves the wall:

```
LAUNCH_PARK
  WAIT_DELAY -> LEAVE -> LAUNCH_PRELOADS -> PARK -> DONE
LAUNCH_COLLECT_PARK
  WAIT_DELAY -> LEAVE -> LAUNCH_PRELOADS -> COLLECT_GARDEN -> LAUNCH_AGAIN -> PARK -> DONE
```

Every position is written once, **for RED**, in `FieldConfig`. For BLUE, `FieldPaths` turns it 180° around the middle of the field (`mirrorX(72).mirrorY(72)`), because the field looks the same from both sides after a half turn. Every path is checked at INIT: it must stay on our side (G402) and away from the HIVE (G409, G417).

---

## 8. Vision (two cameras)

The AprilTags are stuck under the CELLS, facing down.

| Camera | Device | Sees | Used for |
| --- | --- | --- | --- |
| HIVE camera (looks up) | HuskyLens | CELL tags | Checking which CELL is up (the operator's TIP button comes first); aiming at the upward CELL |
| Front camera (looks ahead) | Logitech webcam | Field AprilTags, POLLEN and NECTAR, all at once | Shows the drive team what is in front of the intake; later, aiming the intake |

Each camera is a mechanism (`HiveCamera`, `FrontCamera`). Cameras only answer questions; Robot decides what to do with the answers. Setting the robot's position from a camera comes later, if at all; until then, "set pose here" fixes a lost position.

---

## 9. What the drive team sees

```
 mechanisms, state machines, Drive
        |  telemetry lines, match-log values, warnings, ALERTs
        v
      Robot ----> Driver Station screen + FTC Dashboard (field drawing, graphs)
        |  ------> match log file (20 rows a second + every event)
        |  ------> an urgent ALERT -> the OpMode rumbles the gamepad
        v
 drive team (screen, rumble)      pits (Log Viewer)      laptop (plot tool)
```

The first telemetry line always shows: alliance, time left, loop time, battery, FIELD or ROBOT driving, intake on or off, launcher ready, the upward CELL, and any ALERT.

---

## 10. Growing the robot during the season

| To add... | Do this (step-by-step checklists in `LLD.md`, section 11) |
| --- | --- |
| A mechanism | Copy `template/TemplateMechanism.java`; add its names to `HardwareNames`; add a `...Config`; in Robot add a field and its name in the mechanisms list; copy the test OpMode; add buttons through a Robot method |
| A state machine | Copy `template/TemplateStateMachine.java`; build it in Robot with the mechanisms it uses; add one line each to `update()`, `stopMechanisms()` and telemetry; add a Robot start method |
| An assist button (S2) | A place in `FieldConfig`, a path in `FieldPaths`, a Robot method, one line in TeleOp |
| An auto step | One name in the `Step` list and one `case`, with its timeout in `AutoConfig` |

---

## 11. Risks and fallbacks

| Risk | What happens |
| --- | --- |
| Odometry (Pinpoint) fails mid-match | Drive switches to robot-centric with a rumble and an ALERT; assists say "position unknown"; the driver drives as S1 |
| Heading drifts | Y (facing away), snap to 90°, recalibrate (gamepad 1) |
| Position lost after a restart | TeleOp starts as S1; "set pose here" in our reset corner brings S2 back |
| A mechanism's cable comes out | That mechanism is FAULTED and stops; an ALERT names it; the rest keeps working |
| Opponent NECTAR picked up | The operator holds B to push it back out (G408) |
| Upward CELL wrong (TIP button missed) | The operator presses the TIP button again; the screen shows which CELL is up |
| Auto runs late | The park-by guard: PARK always happens |
| Loop gets slow | ALERT; first fix is bulk reads (LLD, Future improvements) |
| Low battery | ALERT and rumble; logged in every row for the debrief |

---

## 12. For mentors and curious students: the design patterns

Professional programmers have names for designs that work well. You don't need these names to work on the code, but here is where each one is, in its simplest form:

| Pattern | In our code | In one sentence |
| --- | --- | --- |
| **Layered architecture** | OpMode → Robot → state machines → mechanisms | Each layer only calls the layer below it |
| **Game Loop** and **Update Method** | `loop()` → `robot.update()` → every `update()` | Every loop, everything takes one small step; nothing waits |
| **Facade** | `Robot`, `Drive` | One simple front for many parts behind it |
| **Adapter** | `Intake`, `Launcher`, `HiveCamera`, `FrontCamera`, `GamepadInput` | Turns device calls into game words |
| **Template Method** | `Mechanism.runUpdate()` calls your `update()` | The base class fixes the order and the safety; you fill in the steps |
| **State pattern** (enum + switch) | `LaunchStateMachine`, Auto's steps | The object behaves differently depending on its current state |
| **Mediator** | Robot's start methods cancel any state machine that shares a mechanism | Parts that could clash don't talk to each other; one coordinator does |
| **Dependency injection** | `new LaunchStateMachine(feeder, launcher, drive)` | A class is given what it needs; it doesn't go and find it |
| **Factory method** | `FieldPaths.toLaunchSpot(from, isRed)` | One method makes every path of one kind, so everyone gets the same one |
| **Third-party library** | Pedro Pathing, FTC SDK, FTC Dashboard | Use well-tested code as it comes, behind one or two of our own classes |

**Not used, on purpose:** the Command pattern and command schedulers (FTCLib, NextFTC), interfaces and lambdas. They are powerful, but they hide what runs when, and our team is new to Java.
