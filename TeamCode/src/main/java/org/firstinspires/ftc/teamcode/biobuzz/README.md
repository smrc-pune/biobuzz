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
  OpModes          BioBuzzTeleOp, BioBuzzAuto, tests    read gamepads, call robot.intake.toggle() ...
       |
       v
  Robot            Robot.java                           builds everything; update() once per loop
       |                                   |
       v                                   v
  subsystems       intake, shooter, vision         drive/  Drive, DriveToSpot, FieldPaths
  (the menus)      robot.shooter.launch(4)                  |
       |                |                                   v
       v                v                          ..............................
  state machines --> mechanisms                    : Pedro Pathing (off the     :
  (jobs that take    (Intake, Feeder, Launcher,    : shelf: we never edit it)   :
   time)              HiveCamera, FrontCamera)     :............................:
                         |
                         v
  ........................................................
  : FTC SDK (off the shelf): motors, servos, sensors,    :
  : cameras, gamepads  ->  Control Hub + Expansion Hub   :
  :......................................................:
```

| Part | Job | Never does |
| --- | --- | --- |
| OpMode | Reads the gamepads, calls subsystem menus (`robot.intake.toggle()`), then `robot.update()` and telemetry | Touch mechanisms or motors |
| Robot | Builds the drive and the subsystems. Updates them once per loop; health, match log, telemetry header | Read gamepads. Know what is inside a subsystem |
| Subsystem | One job of the robot (intake, shooter, vision). OWNS its mechanisms and the state machines that use them. Its methods are the menu OpModes call | Have states of its own. Use another subsystem's mechanism |
| State machine | A job that takes time ("launch 4 balls": spin up, feed, wait, repeat). Gives mechanisms commands | Call another state machine |
| Mechanism | One group of devices, with action words (`collect()`) and questions (`isUpToSpeed()`) | Remember states or wait |
| Drive + FieldPaths | Driving: sticks, paths, where the robot is. The only code that uses Pedro | Decide when to drive a path |
| DriveToSpot | The S2 buttons: the robot drives itself to a spot (`robot.driveToSpot.launchSpot()`). Checks the pose and the HIVE, then starts a path | Drive with the sticks |

**Why subsystems?** Each one is a short file you can read by itself. To find what a
button does, follow the dots: `robot.shooter.launch(4)` is the `launch` method in
`subsystems/ShooterSubsystem.java`. And because a mechanism belongs to ONE subsystem,
two commands can only fight over a motor inside that one file.

**Why so strict?** Because then a bug can only be in one place.

**Who changes which files.** The first line of every file says which zone it is in.

| Student zone (you will change these) | Mentor zone (read, don't change) |
| --- | --- |
| `configurations/`, `subsystems/` (except `Subsystem.java`), `mechanisms/` (except `Mechanism.java`), `statemachines/` (except `StateMachine.java`), `teleop/`, `auto/`, `drive/FieldPaths.java`, `drive/DriveToSpot.java`, the "ADD A SUBSYSTEM HERE" spot in `Robot.java`, `testing/` | `subsystems/Subsystem.java`, `mechanisms/Mechanism.java`, `statemachines/StateMachine.java`, `drive/Drive.java`, `helpers/`, `pedro/` |

---

## 3. Where everything lives

```
teamcode/
  pedro/                    Pedro Pathing Quickstart files. NOT ours: do not move or add files.
    Constants.java          Pedro's robot setup (filled in at drive tuning)
  biobuzz/                  ALL our code
    README.md               this guide
    Robot.java              the whole robot in one object: drive, assists and the subsystems
    configurations/         numbers and names only
      HardwareNames.java      every device name, and the hub and port it plugs into
      DriveConfig.java        stick deadzone, "away from drivers" headings
      FieldConfig.java        every place we drive to (written for RED): starts, launch spot, CELLS, GARDEN
      AutoConfig.java         the Autonomous plan (routine, start, delay), step time limits, PARK_BY_S
      ControlsConfig.java     trigger threshold, hold times, rumble lengths, button list
      HealthConfig.java       limits for ALERTs (low battery, slow loop, heading jump) and Robot Check
      IntakeConfig.java       intake powers
      FeederConfig.java       the feeder servo's two positions
      LauncherConfig.java     flywheel speed control, the launch speed, launch timing, aim offset
      CameraConfig.java       both cameras: HuskyLens tag ids, webcam size, exposure, colours
      LogConfig.java          match log on/off, rows per second, how many files to keep
      TelemetryConfig.java    what shows on screen
      TemplateConfig.java     numbers for the templates (copy it with them)
      FieldCheckConfig.java   field name, home baseline and tolerances for the Field Checks
    drive/
      Drive.java              the drivetrain: sticks, paths, pose (talks to Pedro)
      FieldPaths.java         every path the robot drives by itself
      DriveToSpot.java            the S2 buttons: drive to the launch spot / LOADING ZONE, set pose here
    subsystems/             the menus OpModes call: robot.intake..., robot.shooter..., robot.vision
      Subsystem.java          what every subsystem has in common (updates, stops, shows, logs its parts)
      IntakeSubsystem.java    owns Intake: toggle(), collect(), reverse(), stop()
      ShooterSubsystem.java   owns Feeder + Launcher + LaunchStateMachine: launch(4), spinUp(), aim
      VisionSubsystem.java    owns HiveCamera + FrontCamera (answers questions only)
    mechanisms/
      Mechanism.java          what every mechanism has in common (FAULTED safety)
      Intake.java             the roller that picks up balls
      Feeder.java             the servo that pushes one ball into the flywheel
      Launcher.java           the flywheel: spins up to the one launch speed
      HiveCamera.java         HuskyLens looking up at the CELL tags (answers questions only)
      FrontCamera.java        Logitech webcam: field tags, POLLEN, NECTAR (answers questions only)
    statemachines/
      StateMachine.java       what every state machine has in common (base class, like Mechanism)
      LaunchStateMachine.java "launch 4": spin up, feed, recover, repeat
    teleop/
      BioBuzzTeleOp.java      the match TeleOp: both gamepad maps (S1 and S2)
    auto/
      BioBuzzAuto.java        the match Autonomous (S3): a list of steps
    testing/
      DriveTest.java          "Test: Drive" – drive only, to check the drivetrain
      PartsTest.java          "Test: Parts" – move one motor or servo at a time (wiring, direction)
      RobotCheck.java         "Robot Check" – the one-minute check before every match
      LogViewer.java          "Log Viewer" – summaries of the last 10 match logs
      IntakeTest.java, FeederTest.java, LauncherTest.java, CameraTest.java   one mechanism each
      LaunchTest.java         "Test: Launch" – the launch state machine with real balls
    fieldchecks/            "Field Check 01" ... "Field Check 13": is our tuning still good on THIS field?
    template/               files to COPY when you add something new
      TemplateSubsystem.java      a subsystem that owns one mechanism and one state machine
      TemplateMechanism.java      a mechanism with one motor
      TemplateStateMachine.java   a state machine with the four sections
      TemplateMechanismTest.java  a test OpMode for one mechanism (hidden with @Disabled)
    helpers/                tools (see section 8)
    docs/                   requirements, design documents, guides (see section 12)
```

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
 |     every subsystem's update()                       intake, shooter, vision take one step
 |     drive.update()                                   Pedro reads the Pinpoint and
 |                                                      sets the four motor powers
 |-- showTelemetry()                                    SHOW
       robot.addTelemetry(t), t.update()                header line, drive mode, pose,
                                                        field drawing on the Dashboard
```

### "The driver presses A" (S2: drive to our launch spot)

1. `handleDriver()` reads `driver.isAPressed()` once and keeps the answer.
2. It calls `robot.driveToSpot.launchSpot()` (in `drive/DriveToSpot.java`). It checks two things:
   - Is the robot's position known? If not: refused, a warning on screen, a short rumble.
   - Would the path hit the HIVE? If so: refused the same way.
3. Otherwise `FieldPaths.toLaunchSpot(...)` makes a straight path from where the robot
   is, and `drive.followPath(...)` starts it. Pedro drives it, then holds the robot still.
4. Each loop after that, the sticks are not used while the robot drives itself...
5. ...until the driver moves **any** stick: then `driveWithSticks(...)` runs again and
   the driver has the robot back, in the same loop.

### "The operator pulls the right trigger" (launch 4)

```
BioBuzzTeleOp.handleOperator()
 |     operator.isRightTriggerPressed()                 READ (once per pull)
 |     robot.shooter.launch(4)                          COMMAND (subsystems/ShooterSubsystem.java)
 |        launchStateMachine.start(4)                   the state machine: IDLE -> SPINNING_UP
 |-- robot.update()
 |     1. intake.update()       the roller
 |     2. shooter.update()      first its state machine, then its mechanisms:
 |          launchStateMachine.update()   SPINNING_UP: launcher.spinUp()  (to LAUNCH_RPM)
 |                                        ... up to speed -> FEEDING: feeder.feedOne()
 |                                        ... FEED_MS -> RECOVERING: feeder.hold() ... 4 times, then IDLE
 |          feeder, launcher runUpdate()  the launcher reads its speed
 |     3. vision.update()       the cameras read
 |     4. drive.update()
```

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
 press PLAY ──> start()         ONCE: start the clock and the match log; alliance and starting pose
                loop()          OVER AND OVER: this is the match (section 4)
 press STOP ──> stop()          ONCE: every mechanism to a safe state; close the match log
```

| Step | What BioBuzzTeleOp does |
| --- | --- |
| `init()` | Builds the `Robot`. |
| `init_loop()` | Gamepad 1 D-pad left/right switches the alliance (RED / BLUE). Shows the battery voltage ("LOW - swap the battery" under 12.0 V) and whether there is a position from Autonomous. |
| `start()` | `robot.startMatch(...)`: starts the 2:00 clock, the health checks and the match log. Tells Drive the alliance. With a position from Auto: S2 ready. Without: S1 only, and the robot is assumed to face away from the drivers. |
| `loop()` | `handleDriver()`, `handleOperator()`, `robot.update()`, `rumbleForEvents()`, telemetry. |
| `stop()` | `robot.endMatch()`: every mechanism safe, then the log's `# END` line. |

---

## 6. Driving: S1 and S2

**Field-centric** (the normal mode): push the stick away from you and the robot moves
away from you, whichever way it is facing. **Robot-centric**: "forward" is the robot's
own front. Used only when the driver presses X, or when the heading can't be trusted.

| Stage | Who drives |
| --- | --- |
| **S1 Manual** | The driver, with the sticks |
| **S2 Assisted** | The driver, plus A and B: the robot drives itself to a fixed place. **Any stick takes the robot back** |
| S3 Autonomous | The robot, in BioBuzz Auto (section 6a) |

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
| Back / Share | Mark this moment in the match log ("something odd just happened") |
| D-pad left/right (INIT only) | Switch alliance RED / BLUE |

| Gamepad 2 | Action |
| --- | --- |
| A | Intake on / off |
| B (hold) | Spit out (reverse the intake); also how opponent NECTAR goes back out |
| Right trigger | Launch 4 balls (they land best from our launch spot: driver's A) |
| Y | Spin the launcher up early |
| Left bumper | "Our HIVE just TIPPED": the other CELL faces up now |
| D-pad down | Stop all mechanisms |
| Back / Share | Mark this moment in the match log |

Gamepad 1 also has **right bumper (hold) = aim**: keep moving with the left stick while
the robot turns to face the upward CELL (needs S2: the position must be known).

**Rumbles:** a long buzz on both gamepads = an urgent ALERT (read the screen). Both buzz
at 1:00 left; gamepad 1 buzzes at 0:20 left: time to PARK.

All places are written **for RED** in `configurations/FieldConfig.java`. For BLUE,
`FieldPaths.place(...)` turns them half a turn around the middle of the field.

---

## 6a. Autonomous (S3)

`auto/BioBuzzAuto.java` is a list of steps, written like a state machine:

```
WAIT_DELAY -> LEAVE -> LAUNCH_PRELOADS -> PARK -> DONE                         (LAUNCH_PARK)
WAIT_DELAY -> LEAVE -> LAUNCH_PRELOADS -> COLLECT_GARDEN -> DRIVE_TO_LAUNCH
           -> LAUNCH_AGAIN -> PARK -> DONE                                     (LAUNCH_COLLECT_PARK)
```

Each step starts something on its first loop (a path, a launch), waits until it is done,
and gives up after its time limit in `AutoConfig` (a `TIMEOUT` line in the match log).
At `AutoConfig.PARK_BY_S` (26 s) the robot goes to PARK whatever it is doing.

**Before the match** set `AutoConfig.ROUTINE`, `START` and `START_DELAY_S` with your
alliance partner. **In INIT** pick the alliance with gamepad 1 D-pad left/right, and read
the screen: it shows the plan and a **PATH WARNING** if a path would leave our side or hit
the HIVE. The paths are drawn on the Dashboard field. Place the robot exactly at its start.
At the end, Auto saves its pose, so TeleOp starts in S2.

---

## 7. Subsystems, mechanisms and state machines

**A subsystem** is one file in `subsystems/` that `extends Subsystem`: one job of the
robot. In its constructor it says what it OWNS, with `addMechanism(...)` and
`addStateMachine(...)`. Its public methods are the **menu** OpModes call
(`robot.shooter.launch(4)`). `Subsystem` does the rest: every loop it updates its state
machines, then its mechanisms; `stop()` cancels and stops them all; it shows and logs
them. A subsystem has no states of its own, and never touches another subsystem's
mechanisms. If a menu command and a state machine use the same mechanism, the command
cancels the state machine first (only that one file has to know).

```
  ShooterSubsystem  (the menu: launch(4), cancelLaunch(), spinUp(), isLaunching() ...)
     owns  Feeder, Launcher                     mechanisms: action words + questions
     owns  LaunchStateMachine                   a job that takes time; uses Feeder + Launcher
```

**A mechanism** is one file in `mechanisms/` that `extends Mechanism`. It has action
words and questions, and fills in up to three methods: `update()` (every loop),
`stop()` (safe state) and `addDetails(t)` (telemetry). If it hits an error, it is marked
**FAULTED** and stops, and the rest of the robot keeps running.

**A state machine** is one file in `statemachines/` for a job that takes time. It
`extends StateMachine`, just as a mechanism `extends Mechanism`, and fills in `update()`,
`cancel()`, `isBusy()`, `addTelemetry()` and `addLogData()` with `@Override`, so its
subsystem can run any state machine the same way. Fill in all five (the template has them). A **state**
is what it is doing right now; it changes state only in `goTo(state, "reason")`. Every file
has the same four sections: 1. the list of states, 2. commands (`start()`, `cancel()`),
3. `update()` with `// DO`, `// CHECK`, `// GO` in each state, 4. bookkeeping (`isBusy()`,
telemetry). It waits with a `StateTimer` (`timer.isTimeUp(500)`), never by sleeping, and
every waiting state has a timeout.

**Today's robot:** three subsystems. `intake` owns Intake; `shooter` owns Feeder, Launcher
and `LaunchStateMachine` (the one state machine); `vision` owns HiveCamera and FrontCamera. Each has its own test OpMode in `testing/`.
Each mechanism's telemetry has an on/off switch in `TelemetryConfig` (its `isTelemetryOn()`).
**Which CELL is up:** `MatchInfo` remembers it; the operator's left bumper flips it at each
TIP. The aim button follows the upward CELL. The launcher has ONE speed
(`LauncherConfig.LAUNCH_RPM`), tuned from our launch spot.

**To add one**, copy the files in `template/` and follow `docs/HOW_TO_ADD_A_MECHANISM.md`.
A new mechanism or state machine usually goes into an EXISTING subsystem (one
`addMechanism(...)` line and a menu method there); `Robot.java` does not change. Only a new
job of the robot gets a new subsystem, at the `ADD A SUBSYSTEM HERE` spot in `Robot.java`.

**Every `if` and every method gets braces on their own lines.** Even when only one line runs,
write it with `{` and `}` and the body on its own line, like every file in this folder:

```java
if (isFaulted()) {
    return;
}

public boolean isOn() {
    return intake.isCollecting();
}
```

That way you can always see what the `if` controls, and adding a second line later cannot
break it.

---

## 8. The helpers

| Helper | What it is for |
| --- | --- |
| `StateTimer` | Stopwatch for "how long have we been in this state?" |
| `StateLog` | Remembers changes and warnings; shows them on telemetry |
| `GamepadInput` | Reads one gamepad. `isAPressed()` = once per press, `isAHeld()` = every loop while held, `wasBReleased()` = once when let go, `isDpadLeftHeldFor(1000)` = once after holding 1 s. Sticks have the deadzone applied |
| `AllianceColor` | The two alliances as an `enum`: `AllianceColor.RED`, `AllianceColor.BLUE` |
| `MatchInfo` | `MatchInfo.alliance` (RED until chosen), and the HIVE: which CELL is up, TIPS – kept from Auto into TeleOp |
| `PoseMemory` | Carries the robot's position from Autonomous into TeleOp |
| `DashboardTelemetry` | One telemetry that shows on the Driver Station AND FTC Dashboard |
| `FieldDrawing` | Draws the robot, its path and the upward CELL on the Dashboard field (BioBuzz field picture); `graph(...)` adds numbers to the Dashboard graph |
| `BatteryMonitor` | The battery's real voltage, measured by the hubs (the lowest of the two), read at most every 100 ms |
| `MatchClock` | Match time left. Robot owns one: `robot.clock.getTimeLeftText()` = "1:42" |
| `HealthMonitor` | Watches loop time, battery, the Pinpoint and the heading every loop, and raises ALERTs |
| `Alerts` | `Alerts.raise("text", urgent)`: shows under the header, goes in the log; urgent = rumble |
| `MatchLogger` | Writes every run to a CSV file (see below) |
| `LogSummary` | Reads one log file and sums it up (for Log Viewer) |
| `FieldCheckHelper` | What every Field Check shares: PASS / WARN / FAIL grades, results kept for the Summary, FIELDCHECK events in the log |

**The match log.** Every run of BioBuzz TeleOp writes a file to
`/sdcard/FIRST/data/biobuzz-logs/` on the Control Hub, named by time, OpMode and alliance,
for example `2026-10-18_14-32-05_BioBuzzTeleOp_RED.csv`. It has 20 DATA rows a second
(pose, heading, battery, loop time, sticks, Pinpoint status, ...) and an EVENT row for
everything that happens (changes, warnings, ALERTs, assists, MARK presses). A file that
does not end with `# END` means the OpMode crashed or the robot lost power. Copy the
files to a laptop with Android Studio's Device Explorer and open them in a spreadsheet.
To log a value from a new mechanism, fill in its `addLogValues()`.
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
| 4 | Match log, match clock, ALERTs; health checks: loop time, low battery, Pinpoint, heading jump (Requirements v2.6) | Done (not yet run on the robot) |
| 5 | Robot Check, Test: Parts, Log Viewer, templates | Done (not yet run on the robot) |
| 6 | Field Checks 01–13, FieldCheckHelper, FieldCheckConfig | Done (no home baseline recorded yet) |
| 7 | Autonomous (S3): BioBuzzAuto, AutoConfig, start positions, GARDEN and auto paths, own-side and HIVE checks at INIT, park-by guard | Done (places in FieldConfig are guesses from the field picture) |
| 8 | Mechanisms: Intake, Feeder, Launcher, HIVE camera, front camera; LaunchStateMachine; aim button; gamepad 2 map; one test each | Done (device names, ports and numbers are first guesses: TODO(BioBuzz)) |
| 9 | Guides (section 12), TROUBLESHOOTING.md, HLD/LLD updated, UPSTREAM.md, tools/plot_match.py | Done |
| 10 | Subsystems (Requirements v2.8): `Robot` split into `intake`, `shooter`, `vision` and `assists`; `Subsystem` and `StateMachine` base classes, `TemplateSubsystem` | Done (OpModes now call `robot.shooter.launch(4)`, ...) |
| 11 | Easier to read (Requirements v2.9): `Assists` renamed `DriveToSpot` (`robot.driveToSpot.launchSpot()`; "set pose here" is now `robot.drive.setPoseAtResetCorner()`); every `if` / `else` and every method has `{ }` on its own lines | Done |
| 12 | One launch speed (Requirements v2.11): `LauncherConfig.LAUNCH_RPM` replaces the speed-by-distance table, tuned from our launch spot; `launcher.spinUp()` | Done (the table is a future improvement: `docs/LLD.md`, section 14) |

**Releases** (tags on `master`; how they are made: Requirements, "Branches and releases")

| Tag | Date | What |
| --- | --- | --- |
| `v0.1` | 2026-10-24 | First student version: steps 1–11, Pedro 3.0.2. Not run on the robot yet |
| `v0.1.1` | 2026-10-24 | The repository's own front page (`.github/README.md`). Docs only |
| `v0.1.2` | 2026-10-24 | Every decision from Oct 23–24 written into the design docs (Requirements v2.10, design review section 10). Docs only |

> **Important:** Pedro's `pedro/Constants.java` is still the empty Quickstart file.
> Until it is filled in at drive tuning, every BioBuzz OpMode that drives stops at INIT
> with the message "DRIVE NOT SET UP". That is expected.
>
> **The robot is built? Start with `docs/NEW_ROBOT_CHECKLIST.md`.** It goes step by step
> from the wiring list to a match-ready robot, and says which step clears each `TODO(BioBuzz)`.
>
> **Also:** BioBuzz TeleOp (and every OpMode that builds `Robot`) now needs EVERY device
> in `HardwareNames`, including the mechanisms and both cameras. A missing one stops INIT
> with "Unable to find a hardware device...". Until the mechanisms are built and in the
> robot configuration, drive with "Test: Drive".

---

## 10. When something goes wrong

**On a new field** (the first practice slot at an event), run **Field Check 01** to **12**
in order (group "Field Checks"), then **Field Check 13: Summary**. Each one says on screen
what the crew does. PASS = fine, WARN = look at it, FAIL = fix it before matches. They
compare with our **home baseline** in `FieldCheckConfig`, so record one at home first.

**Before every match**, run **Robot Check** (group "Tests"): battery, Pinpoint, a 10 s
still-test of the gyro, each drive wheel, and the match log. It ends with
**READY FOR MATCH** only if every step passes. **Test: Parts** moves one motor or servo
at a time, to check a cable or a direction. **Log Viewer** shows the last 10 runs: how
long each ran, its ALERTs, the worst loop time, the lowest battery, and whether it ended
cleanly.

1. **Read the telemetry first.** The header line shows alliance, time left, loop time,
   battery voltage, drive mode, heading and "S1 only" or "S2 ready". Any ALERT is right
   under it. A full battery reads about 13 V at rest; it drops while the motors work hard.
   The Drive lines show who is driving, the Pinpoint status, the pose, any WARNING and
   the last change.
2. **Follow the loop** (section 4): was the button read? Was the subsystem method called?
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
8. **"ROBOT" suddenly in the header, with an ALERT:** the Pinpoint had a problem or the
   heading jumped. Check the Pinpoint cable. Once the Drive line says "Pinpoint READY",
   face the robot away from the drivers and press Y. For the assists, "set pose here".
9. **After the match:** open the log. Look at the EVENT rows around the time the drive
   team noticed the problem (`t_s` is seconds since PLAY), and at `battery_v` and
   `loop_ms` just before it. Pressing Back during the match leaves a MARK to find.

---

## 11. Words you will see

| Word | Meaning |
| --- | --- |
| OpMode | A program you pick on the Driver Station (TeleOp, Autonomous, a test) |
| Loop | One pass of read → command → update → show. About 50 per second |
| Subsystem | One job of the robot (intake, shooter, vision): owns mechanisms and state machines; its methods are the menu |
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

---

## 12. The guides in `docs/`

| Guide | Read it when you want to... |
| --- | --- |
| `GETTING_STARTED.md` | install Android Studio, build, deploy, run an OpMode, open the Dashboard |
| `NEW_ROBOT_CHECKLIST.md` | go from a newly built robot to a match-ready one, step by step (wiring, tests, tuning, cameras, field places, Auto, baseline) |
| `FIRST_CHANGES.md` | do five small exercises: a speed, a button, a telemetry line, a state, an auto step |
| `CONTROLS.md` | know what every button does (print it for the drive team) |
| `HOW_TO_ADD_A_MECHANISM.md` | add a mechanism or a state machine from the templates |
| `AUTO_GUIDE.md` | measure poses, change Autonomous, add a step or a start position |
| `TUNING.md` | tune the drive (with our Pedro tuning kit) and the mechanisms, in the right order |
| `MATCH_DAY.md` | check the robot on a new field, before and between matches; recovery buttons |
| `MATCH_LOGS.md` | copy and read match logs; use Log Viewer and `tools/plot_match.py` |
| `DEBUGGING.md` | find out what is wrong: the 6 questions, every ALERT, a symptom table |
| `TROUBLESHOOTING.md` | see the four troubleshooting stages on one page |
| `CAMERA_GUIDE.md` | mount, train and tune the HuskyLens and the webcam |
| `GLOSSARY.md` | look up a word (code words, design patterns, game words) |
| `UPDATING_PEDRO.md` | merge a new Pedro Pathing Quickstart release |
| `HLD.md`, `LLD.md` | the design: big picture, then every class |
| `BioBuzz-Requirements.md`, `DESIGN-REVIEW.md` | the rules the code follows, and why |

Changes to Quickstart files (outside `biobuzz/`) are listed in `UPSTREAM.md` at the
repository root. The repository's front page on GitHub is `.github/README.md`: it points
here. At each release, update its **Status** line.


---

## 13. Competition build

Android Studio normally builds a **debug** build. For competitions, use a **release**
build. It is the same code, but it runs a little faster, because Android can optimise
it more. Nothing in the project needs to change.

```
  pit (working on code)     ->  debug    (you can attach the debugger)
  competition matches       ->  release  (a little faster, no debugger)
```

**In Android Studio:**
1. Open **View → Tool Windows → Build Variants**.
2. Set **TeamCode** to `release` (FtcRobotController changes with it).
3. Press **Run** as usual (see `docs/GETTING_STARTED.md`, "Deploy to the robot").
4. Back in the pit, set it to `debug` again.

**In a terminal** (from the repository root):
```
./gradlew :TeamCode:assembleRelease   # makes TeamCode/build/outputs/apk/release/TeamCode-release.apk
./gradlew :TeamCode:installRelease    # makes it AND installs it on the connected Control Hub
```

**What stays the same:** FTC Dashboard, live Config changes, telemetry, the match logs
and Logcat. Every OpMode still shows on the Driver Station: the release build does not
shrink the code (`minifyEnabled` is off), so nothing is removed. Hiding the test
OpModes with one switch is a future improvement (`docs/LLD.md`, section 14).

**What you lose:** the debugger (breakpoints). Debug in the pit, not at the field.

**What you may see while it builds:** lines starting with `e:` about "an incompatible
version of Kotlin". They come from a check that only release builds run, and they do not
stop the build. The build worked if the APK file is there (about 51 MB, a little smaller
than debug).

**The signing key.** Android only installs an app over another one signed with the
same key. Our release build uses the shared FTC debug key (`libs/ftc.debug.keystore`),
the same as the debug build, so release and debug install over each other with no
uninstall. Keep it that way. A team key is possible (set the four `APK_SIGNING_*`
variables that `build.common.gradle` reads), but then:
- the first switch needs an uninstall of the app on the hub. The robot configuration
  files in `/sdcard/FIRST` are kept, but you may have to pick the active configuration
  again;
- every laptop that deploys needs the key file and the variables;
- the key file must never go into git.

**Check after installing:** the Driver Station connects, BioBuzz TeleOp and BioBuzz Auto
are in the lists, and **Robot Check** ends with READY FOR MATCH.
