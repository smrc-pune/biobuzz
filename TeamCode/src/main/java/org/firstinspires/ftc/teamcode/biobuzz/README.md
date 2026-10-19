# BioBuzz Robot Code – Start Here

This guide explains how the BioBuzz code is organised and how one loop of the robot
flows through it. Read it before you open any Java file. You do not need to know Java
yet: every file starts with a comment that says what it does in plain English.

> **Keep this guide up to date.** When you add or change a file, update the matching
> section here in the same commit. The "What is built so far" table at the end says
> what exists today. The full design is in `docs/HLD.md` (big picture) and
> `docs/LLD.md` (every file).

---

## 1. The big idea in 60 seconds

The robot runs a **loop**: the same few steps, about 50 times a second, until the
match ends. Nothing in the code ever waits or sleeps. Each pass is a few milliseconds.

Every pass of the loop does three things, always in this order:

```
  1. READ + COMMAND   read the gamepads, tell the robot what you want   (handleDriver(), handleOperator())
  2. UPDATE           every part of the robot takes ONE small step      (robot.update())
  3. SHOW             telemetry on the Driver Station and Dashboard     (t.update())
```

---

## 2. The parts of the code

**Each part only calls the parts below it, never the ones above.**

```
  OpModes          BioBuzzTeleOp, Test: Drive, ...      read gamepads, call Robot
       |
       v
  Robot            Robot.java                           the robot's "menu"; update() once per loop
       |                 |                   |
       v                 v                   v
  state machines    mechanisms           drive/  Drive + FieldPaths
  (tasks that       (Intake, Feeder,          |
   take time)        Launcher, cameras)       v
       |  commands        ^              ..............................
       +------------------+              : Pedro Pathing (off the     :
                         |               : shelf: we never edit it)   :
                         v               :............................:
  ........................................................
  : FTC SDK (off the shelf): motors, servos, sensors,    :
  : cameras, gamepads  ->  Control Hub + Expansion Hub   :
  :......................................................:
```

| Part | Job | Never does |
| --- | --- | --- |
| OpMode | Reads the gamepads, calls Robot, then `robot.update()` and telemetry | Touch motors |
| Robot | Builds everything. One method for each thing the robot can do. Updates every mechanism once per loop | Read gamepads. Set motor powers itself |
| State machine | A job that takes time ("launch 4 balls": spin up, feed, wait, repeat). Gives mechanisms commands | Call another state machine |
| Mechanism | One group of devices, with action words (`collect()`) and questions (`isUpToSpeed()`) | Remember states or wait |
| Drive + FieldPaths | Driving: sticks, paths, where the robot is. The only code that uses Pedro | Decide when to drive a path |

**Why so strict?** Because then a bug can only be in one place.

**Who changes which files.** The first line of every file says which zone it is in.

| Student zone (you will change these) | Mentor zone (read, don't change) |
| --- | --- |
| `configurations/`, `mechanisms/` (except `Mechanism.java`), `statemachines/`, `teleop/`, `auto/`, `drive/FieldPaths.java`, the "ADD ... HERE" spots in `Robot.java`, `testing/` | `mechanisms/Mechanism.java`, `drive/Drive.java`, `helpers/`, `pedro/` |

---

## 3. Where everything lives

```
teamcode/
  pedro/                    Pedro Pathing Quickstart files. NOT ours: do not move or add files.
    Constants.java          Pedro's robot setup (filled in at drive tuning)
  biobuzz/                  ALL our code
    README.md               this guide
    Robot.java              the whole robot in one object
    configurations/         numbers and names only
      HardwareNames.java      every device name, and the hub and port it plugs into
      DriveConfig.java        stick deadzone, "away from drivers" headings
      FieldConfig.java        every place we drive to (written for RED)
      ControlsConfig.java     trigger threshold, hold times, rumble lengths, button list
      HealthConfig.java       how often the battery voltage is read (alerts come in step 4)
      TelemetryConfig.java    what shows on screen
    drive/
      Drive.java              the drivetrain: sticks, paths, pose (talks to Pedro)
      FieldPaths.java         every path the robot drives by itself
    mechanisms/
      Mechanism.java          what every mechanism has in common (FAULTED safety)
    teleop/
      BioBuzzTeleOp.java      the match TeleOp: both gamepad maps (S1 and S2)
    testing/
      DriveTest.java          "Test: Drive" – drive only, to check the drivetrain
    helpers/                tools (see section 8)
    docs/                   requirements, design documents, guides
```

Coming later (see section 9): the mechanisms (Intake, Feeder, Launcher, cameras),
`statemachines/`, `auto/`, `fieldchecks/`, `template/`, and more helpers.

**How a mechanism finds its devices.** The robot has two hubs: the **Control Hub**
(drive motors, Pinpoint) and the **Expansion Hub** (most mechanism motors). Each
mechanism gets its devices with the normal SDK call and a name from `HardwareNames`:

```java
roller = hardwareMap.get(DcMotorEx.class, HardwareNames.MOTOR_INTAKE);
```

The comment next to each name in `HardwareNames.java` says which hub and port it uses,
so that file is also our wiring sheet.

---

## 4. One loop, step by step

This is the most useful thing in this guide. When something goes wrong, follow the
same path and look at each step.

### "The driver pushes the stick" (S1)

```
BioBuzzTeleOp.loop()
 |
 |-- handleDriver()
 |     driver.areSticksMoved()                          READ: a stick is pushed (tiny values = 0)
 |     robot.drive.driveWithSticks(fwd, right, turn)    COMMAND: Drive turns the stick direction to
 |                                                      match the field, then tells Pedro
 |-- handleOperator()                                   (gamepad 2: mechanisms)
 |
 |-- robot.update()                                     UPDATE
 |     every mechanism's runUpdate()                    (none yet)
 |     drive.update()                                   Pedro reads the Pinpoint and
 |                                                      sets the four motor powers
 |-- showTelemetry()                                    SHOW
       robot.addTelemetry(t), t.update()                header line, drive mode, pose,
                                                        field drawing on the Dashboard
```

### "The driver presses A" (S2: drive to our launch spot)

1. `handleDriver()` reads `driver.isAPressed()` once and keeps the answer.
2. It calls `robot.driveToLaunchSpot()`. Robot checks two things:
   - Is the robot's position known? If not: refused, a warning on screen, a short rumble.
   - Would the path hit the HIVE? If so: refused the same way.
3. Otherwise `FieldPaths.toLaunchSpot(...)` makes a straight path from where the robot
   is, and `drive.followPath(...)` starts it. Pedro drives it, then holds the robot still.
4. Each loop after that, the sticks are not used while the robot drives itself...
5. ...until the driver moves **any** stick: then `driveWithSticks(...)` runs again and
   the driver has the robot back, in the same loop.

### "The driver presses Y"

Y means "the robot is facing away from the drivers right now". `robot.drive.resetHeading()`
sets the heading to the "away" angle in `DriveConfig` and turns field-centric back on.

---

## 5. The life of an OpMode

An OpMode is a program you pick on the Driver Station. The FTC SDK calls its
methods for us, at these moments:

```
 press INIT ──> init()          ONCE: build the Robot. Nothing may move!
                init_loop()     OVER AND OVER until PLAY: pick the alliance here
 press PLAY ──> start()         ONCE: set the alliance and the starting pose
                loop()          OVER AND OVER: this is the match (section 4)
 press STOP ──> stop()          ONCE: put every mechanism in a safe state
```

| Step | What BioBuzzTeleOp does |
| --- | --- |
| `init()` | Builds the `Robot`. |
| `init_loop()` | Gamepad 1 D-pad left/right switches the alliance (RED / BLUE). Shows the battery voltage and whether there is a position from Autonomous. |
| `start()` | Tells Drive the alliance. With a position from Auto: S2 ready. Without: S1 only, and the robot is assumed to face away from the drivers. |
| `loop()` | `handleDriver()`, `handleOperator()`, `robot.update()`, telemetry. |
| `stop()` | `robot.stopMechanisms()`. |

---

## 6. Driving: S1 and S2

**Field-centric** (the normal mode): push the stick away from you and the robot moves
away from you, whichever way it is facing. **Robot-centric**: "forward" is the robot's
own front. Used only when the driver presses X, or when the heading can't be trusted.

| Stage | Who drives |
| --- | --- |
| **S1 Manual** | The driver, with the sticks |
| **S2 Assisted** | The driver, plus A and B: the robot drives itself to a fixed place. **Any stick takes the robot back** |
| S3 Autonomous | The robot (coming later) |

S2 needs to know where the robot is. After Autonomous it does. If Autonomous did not run,
TeleOp starts as **S1 only** (the header says so). To turn the assists on: drive into our
**reset corner** (back against our wall, side against the far wall) and hold D-pad left
for 1 second. The gamepad buzzes and the header says "S2 ready".

| Gamepad 1 | Action |
| --- | --- |
| Left stick | Move |
| Right stick X | Turn |
| Y | "Robot faces away from the drivers now" – resets the heading, field-centric on |
| X | Switch field-centric / robot-centric |
| A | S2: drive to our launch spot |
| B | S2: drive to our LOADING ZONE |
| D-pad left (hold 1 s) | Set pose here (robot in our reset corner) |
| D-pad left/right (INIT only) | Switch alliance RED / BLUE |

| Gamepad 2 | Action |
| --- | --- |
| D-pad down | Stop all mechanisms |
| (mechanisms) | Coming with the mechanisms |

All places are written **for RED** in `configurations/FieldConfig.java`. For BLUE,
`FieldPaths.place(...)` turns them half a turn around the middle of the field.

---

## 7. Mechanisms and state machines (coming next)

**A mechanism** is one file in `mechanisms/` that `extends Mechanism`. It has action
words and questions, and fills in up to three methods: `update()` (every loop),
`stop()` (safe state) and `addDetails(t)` (telemetry). If it hits an error, it is marked
**FAULTED** and stops, and the rest of the robot keeps running.

**A state machine** is one file in `statemachines/` for a job that takes time. A **state**
is what it is doing right now; it changes state only in `goTo(state, "reason")`. Every file
has the same four sections: 1. the list of states, 2. commands (`start()`, `cancel()`),
3. `update()` with `// DO`, `// CHECK`, `// GO` in each state, 4. bookkeeping (`isBusy()`,
telemetry). It waits with a `StateTimer` (`timer.isTimeUp(500)`), never by sleeping, and
every waiting state has a timeout.

**To add one**, follow the checklist in `docs/LLD.md`, section 11. In `Robot.java`, look
for the `ADD A MECHANISM HERE` and `ADD A STATE MACHINE HERE` comments.

---

## 8. The helpers

| Helper | What it is for |
| --- | --- |
| `StateTimer` | Stopwatch for "how long have we been in this state?" |
| `StateLog` | Remembers changes and warnings; shows them on telemetry |
| `GamepadInput` | Reads one gamepad. `isAPressed()` = once per press, `isAHeld()` = every loop while held, `isDpadLeftHeldFor(1000)` = once after holding 1 s. Sticks have the deadzone applied |
| `AllianceColor` | The two alliances as an `enum`: `AllianceColor.RED`, `AllianceColor.BLUE` |
| `MatchInfo` | `MatchInfo.alliance` (RED until chosen) – kept from Auto INIT into TeleOp |
| `PoseMemory` | Carries the robot's position from Autonomous into TeleOp |
| `DashboardTelemetry` | One telemetry that shows on the Driver Station AND FTC Dashboard |
| `FieldDrawing` | Draws the robot and its path on the Dashboard field |
| `BatteryMonitor` | The battery's real voltage, measured by the hubs (the lowest of the two), read at most every 100 ms |

**Edge detection (important!).** The loop is much faster than your finger. If the
code asked "is A down?", one press would count dozens of times. `isAPressed()` is
true only on the one loop when the button goes from up to down. Read each button
**once per loop** into a local `boolean`, then use that variable.

---

## 9. What is built so far

| Step | What | Status |
| --- | --- | --- |
| 1 | Repository, Gradle (FTC Dashboard) | Done |
| 2 | Helpers: StateTimer, StateLog, GamepadInput, AllianceColor, MatchInfo, PoseMemory, DashboardTelemetry, FieldDrawing | Done |
| 3 | New design (Requirements v2.3): Robot, Drive, FieldPaths, Mechanism, BioBuzzTeleOp with S1 and S2, Test: Drive | Done (not yet run on the robot; FieldConfig places are first guesses) |
| 4 | Battery and loop monitors, health alerts, match logging, heading safeguards, recovery buttons | Next |
| 5 | Robot Check, Test: Parts, Log Viewer, templates | Planned |
| 6 | Field Checks 01–13 | Planned |
| 7 | Autonomous (S3) | Planned |
| 8 | Mechanisms: Intake, Feeder, Launcher, HIVE camera, front camera; LaunchStateMachine | When the robot's mechanisms are built |
| 9 | The other student guides | Planned |

> **Important:** Pedro's `pedro/Constants.java` is still the empty Quickstart file.
> Until it is filled in at drive tuning, every BioBuzz OpMode stops at INIT with the
> message "DRIVE NOT SET UP". That is expected.

---

## 10. When something goes wrong

1. **Read the telemetry first.** The header line shows alliance, battery voltage, drive
   mode, heading and "S1 only" or "S2 ready". A full battery reads about 13 V at rest;
   it drops while the motors work hard. The Drive lines show who is driving, the pose,
   any WARNING and the last change.
2. **Follow the loop** (section 4): was the button read? Was the Robot method called?
   Was it refused (look for a WARNING)? What did Drive tell Pedro?
3. **`Unable to find a hardware device with name "..."`** at INIT: a name in
   `configurations/HardwareNames.java` doesn't match the Robot Controller
   configuration. Check the hub and port in the comment next to that name. If every
   Expansion Hub device is missing, check that hub's power and its RS-485 cable.
4. **Field-centric points the wrong way:** face the robot away from the drivers and
   press Y. Use "Test: Drive" to check.
5. **A or B does nothing but buzz:** the position is unknown (S1 only), or the path
   would hit the HIVE. The WARNING line says which. Use "set pose here".
6. **The robot drives to the wrong place:** check the numbers in `FieldConfig`, and that
   the right alliance was picked during INIT.
7. **A button does nothing:** check it is read only once per loop.

---

## 11. Words you will see

| Word | Meaning |
| --- | --- |
| OpMode | A program you pick on the Driver Station (TeleOp, Autonomous, a test) |
| Loop | One pass of read → command → update → show. About 50 per second |
| Mechanism | One group of devices (intake, launcher) with action words and questions |
| State machine | A job that takes time, done one small step per loop |
| State | What a state machine is doing right now (for example `SPINNING_UP`) |
| FAULTED | A mechanism that hit an error and stopped; the rest keeps going |
| Pose | Where the robot is: x and y in inches, heading in degrees |
| Pose known | We trust x and y (after Auto, or "set pose here"), so S2 can be used |
| Heading | Which way the robot faces |
| Field-centric | Stick directions follow the field, not the robot |
| S1 / S2 / S3 | Manual driving / assisted driving / autonomous |
| Assist | A button that makes the robot drive a short path by itself |
| Deadzone | Tiny stick values treated as 0, so the robot doesn't creep |
| Off the shelf | Code written by others that we use as it comes (Pedro Pathing, the FTC SDK) |
| Telemetry | Text the robot sends to the Driver Station and Dashboard |
| Dashboard | FTC Dashboard, a web page at `http://192.168.43.1:8080/dash` |
| Config | A file of numbers you can change live in the Dashboard |
| enum | A fixed list of names, like `AllianceColor { RED, BLUE }`; a typo is caught when the code is built |
