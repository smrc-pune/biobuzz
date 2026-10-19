# BioBuzz Robot Code – Start Here

This guide explains how the BioBuzz code is organised and how one loop of the robot
flows through it. Read it before you open any Java file. You do not need to know Java
yet: every file starts with a comment that says what it does in plain English.

> **Keep this guide up to date.** When you add or change a file, update the matching
> section here in the same commit. The "What is built so far" table at the end says
> what exists today.

---

## 1. The big idea in 60 seconds

The robot runs a **loop**: the same few steps, about 50 times a second, until the
match ends. Nothing in the code ever waits or sleeps. Each pass is a few milliseconds.

Every pass of the loop does four things, always in this order:

```
  1. READ     the gamepads                     (only the OpMode does this)
  2. COMMAND  tell a brain what you want       (robot.drive.resetHeading())
  3. UPDATE   every brain takes ONE step       (robot.update())
  4. SHOW     telemetry on the Driver Station  (t.update())
```

A **command** never makes the robot move by itself. It only says what you want.
The brain's `update()` decides what to do about it, on this loop or a later one.

---

## 2. The four layers

The code is stacked in four layers. **Each layer only talks to the layer below it.**

```
  +-----------------------------------------------------------+
  |  OpModes          BioBuzzTeleOp, Test: Drive, ...         |  read gamepads, give commands
  +-----------------------------------------------------------+
                              |  commands
                              v
  +-----------------------------------------------------------+
  |  Robot            Robot.java                              |  builds everything, update() once
  |                                                           |  per loop, links brains together
  +-----------------------------------------------------------+
                              |
                              v
  +-----------------------------------------------------------+
  |  Brains           subsystems/Drive.java  (+ mechanisms)   |  state machines: decide WHAT to do
  +-----------------------------------------------------------+
                              |
                              v
  +-----------------------------------------------------------+
  |  Parts            parts/DriveBase.java   (+ mechanisms)   |  hardware: motors, servos, sensors
  +-----------------------------------------------------------+
                              |
                              v
                Control Hub / Expansion Hub devices
```

Two more folders are used by every layer:

- **configurations/** – every number you might want to change (speeds, timeouts,
  device names). No other file has "magic numbers".
- **helpers/** – tools: timers, gamepad reading, telemetry, menus. You rarely change these.

| Layer | Job | Never does |
| --- | --- | --- |
| OpMode | Reads gamepads, calls commands, then `robot.update()` and telemetry | Touch motors. Hold mechanism logic. |
| Robot | Builds parts and brains. Calls each brain's `update()`. The only place brains talk to each other | Read gamepads. Set motor powers. |
| Brain (subsystem) | One state machine. Its `update()` does one step | Call another brain. Wait or sleep. |
| Part | Wraps one group of devices with plain verbs (`setPower()`, `open()`) | Make decisions or use timers. |

**Why so strict?** Because then a bug can only be in one place. If a motor does the
wrong thing, the motor command came from exactly one part, which was told by exactly
one brain, which was told by exactly one OpMode.

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
      DriveConfig.java        drive speeds, deadzone, "away from drivers" headings
      ControlsConfig.java     trigger threshold, rumble lengths, button list
      TelemetryConfig.java    what shows on screen
    parts/
      DriveBase.java          the drivetrain, through Pedro Pathing
    subsystems/
      Drive.java              the driving brain
    teleop/
      BioBuzzTeleOp.java      the match TeleOp: both gamepad maps
    testing/
      DriveTest.java          "Test: Drive" – drive only, to check the drivetrain
    helpers/                tools (see section 7)
    docs/                   requirements, design documents, guides
```

Coming later (see section 9): `auto/`, `fieldchecks/`, `template/`, and more helpers.

**How a part finds its devices.** The robot has two hubs: the **Control Hub** (drive
motors, Pinpoint) and the **Expansion Hub** (most mechanism motors). Each part gets its
devices in its constructor with the normal SDK call and a name from `HardwareNames`:

```java
intakeMotor = hardwareMap.get(DcMotorEx.class, HardwareNames.MOTOR_INTAKE);
```

The code does not care which hub a device is on: the name finds it on either hub. The
comment next to each name in `HardwareNames.java` says which hub and port it uses, so
that file is also our wiring sheet. (`DriveBase` is special: Pedro's `Constants.java`
finds the drive motors and Pinpoint, using the same `HardwareNames`.)

---

## 4. One loop, step by step: "the driver pushes the stick"

This is the most useful thing in this guide. When something goes wrong, follow the
same path and look at each step.

```
BioBuzzTeleOp.loop()
 |
 |-- handleDriver()
 |     driver.getLeftStickY() ...                      1. READ the sticks (GamepadInput)
 |     robot.drive.setDriverInput(fwd, right, turn)    2. COMMAND: Drive stores the values
 |                                                        (tiny values become 0: the deadzone)
 |-- handleOperator()                                  (gamepad 2: mechanisms)
 |
 |-- robot.update()                                    3. UPDATE
 |     drive.update()
 |       state is DRIVER_CONTROL, so:
 |       driveBase.driveManual(...)                       scale by speed, then Pedro's
 |                                                        ManualDrive.fieldCentric() turns the
 |                                                        stick direction to match the field
 |       driveBase.update()                               Pedro reads the Pinpoint and
 |                                                        sets the four motor powers
 |
 |-- showTelemetry()                                   4. SHOW
       robot.addTelemetry(t)                              header line, Drive state, pose,
                                                          field drawing on the Dashboard
       t.update()                                         actually sends it
```

### Another example: "the driver presses Y"

Y means "the robot is facing away from the drivers right now".

1. `handleDriver()` reads `driver.isYPressed()` once and keeps the answer.
2. It calls the command `robot.drive.resetHeading()`. That only sets a flag.
3. In `robot.update()`, `drive.update()` sees the flag, sets the heading to the
   "away from drivers" angle in `DriveConfig`, and turns field-centric back on.
4. Telemetry shows the new heading and `drive FIELD` in the header.

---

## 5. The life of an OpMode

An OpMode is a program you pick on the Driver Station. The FTC SDK calls its
methods for us, at these moments:

```
 press INIT ──> init()          ONCE: build the Robot, set up the INIT menu
                init_loop()     OVER AND OVER until PLAY: the INIT menu runs here
 press PLAY ──> start()         ONCE: save the menu choices, set the starting pose
                loop()          OVER AND OVER: this is the match (section 4)
 press STOP ──> stop()          ONCE: put every mechanism in a safe state
```

**What BioBuzzTeleOp does at each step**

| Step | What happens |
| --- | --- |
| `init()` | Builds the `Robot`. Adds the INIT menu rows: Alliance, Match type, Match number, Battery. |
| `init_loop()` | Gamepad 1 D-pad runs the menu. Shows whether there is a pose from Autonomous. |
| `start()` | Copies the menu choices into `MatchInfo`. Starts from the Autonomous pose if there is one; otherwise assumes the robot faces away from the drivers. |
| `loop()` | `handleDriver()`, `handleOperator()`, `robot.update()`, telemetry. |
| `stop()` | `robot.stopMechanisms()`. |

---

## 6. How a brain (state machine) works

A **state** is what the brain is doing right now. A brain is always in exactly one
state, and it changes state only inside `goTo()`, always with a reason.

`Drive` has three states:

```
                     followPath(path)                path finished / timeout
   DRIVER_CONTROL  ------------------>  FOLLOWING_PATH  ------------------>  HOLDING
        ^   ^                                 |                                 |
        |   +------ stick moved --------------+                                 |
        +---------- stick moved (or takeDriverControl()) -----------------------+
```

Every brain file has the same four sections, in this order:

1. **States** – the `enum` list of states, one comment each.
2. **Commands** – public methods the OpMode calls. They only remember the request
   or call `goTo(...)`. They never touch hardware.
3. **`update()`** – a `switch` with one `case` per state. Each case is marked:
   - `// DO`    – set the parts (motors, servos)
   - `// CHECK` – look at a timer or a sensor
   - `// GO`    – `goTo(nextState, "reason")` when the check says so
4. **Bookkeeping** – `goTo()`, `isBusy()`, `getState()`, `addTelemetry()`.

**Waiting without sleeping.** Each brain has a `StateTimer` that restarts in every
`goTo()`. So `timer.isTimeUp(500)` means "we have been in this state for 500 ms".
Every state that waits for something has a timeout, so a jam can never freeze the robot.

**The black box.** Each brain has a `StateLog`. It remembers the last 5 state changes
and why they happened, and shows them on telemetry. When a brain refuses a command,
it calls `log.warn("...")`, which shows a WARNING line for 4 seconds.

---

## 7. The helpers

| Helper | What it is for |
| --- | --- |
| `StateTimer` | Stopwatch for "how long have we been in this state?" |
| `StateLog` | Remembers state changes and warnings; shows them on telemetry |
| `GamepadInput` | Reads one gamepad. `isAPressed()` = once per press, `isAHeld()` = every loop while held |
| `InitMenu` | The INIT menu: D-pad up/down picks a row, left/right changes it |
| `MatchInfo` | Alliance, match id (`Q12`), battery label – kept from Auto INIT into TeleOp |
| `PoseMemory` | Carries the robot's pose from Autonomous into TeleOp |
| `DashboardTelemetry` | One telemetry that shows on the Driver Station AND FTC Dashboard |
| `FieldDrawing` | Draws the robot and its path on the Dashboard field |

**Edge detection (important!).** The loop is much faster than your finger. If the
code asked "is A down?", one press would count dozens of times. `isAPressed()` is
true only on the one loop when the button goes from up to down. Read each button
**once per loop** into a local `boolean`, then use that variable.

---

## 8. Driving: field-centric

**Field-centric** (the normal mode): push the stick away from you and the robot
moves away from you, whichever way it is facing. The code uses the robot's heading
(from the Pinpoint) to turn the stick direction.

**Robot-centric**: "forward" is the robot's own front, like a remote-control car.
Used only when the driver presses X, or when the heading can't be trusted.

| Gamepad 1 | Action |
| --- | --- |
| Left stick | Move |
| Right stick X | Turn |
| Left trigger (hold) | Slow mode |
| Y | "Robot faces away from the drivers now" – resets the heading, field-centric on |
| X | Switch field-centric / robot-centric |
| D-pad (INIT only) | INIT menu |

| Gamepad 2 | Action |
| --- | --- |
| D-pad down | Stop all mechanisms |
| (mechanisms) | Coming once the mechanisms are designed |

Gamepad 1 only drives. Gamepad 2 only runs mechanisms. Both maps are in
`teleop/BioBuzzTeleOp.java`: `handleDriver()` and `handleOperator()`.

---

## 9. What is built so far

| Step | What | Status |
| --- | --- | --- |
| 1 | Repository, Gradle (FTC Dashboard) | Done |
| 2 | Base helpers: StateTimer, StateLog, GamepadInput, InitMenu, MatchInfo, PoseMemory, DashboardTelemetry | Done |
| 3 | Robot, DriveBase, Drive, BioBuzzTeleOp, Test: Drive, FieldDrawing, drive configs | Done (not yet run on the robot) |
| 4 | Battery and loop monitors, health alerts, match logging, heading safeguards, recovery buttons | Next |
| 5 | Robot Check, Test: Parts, Log Viewer, mechanism template | Planned |
| 6 | Field Checks 01–13 | Planned |
| 7 | Autonomous, BioBuzz field image | Planned |
| 8 | Mechanisms | After the game reveal |
| 9 | Design documents and the other student guides | Planned |

> **Important:** Pedro's `pedro/Constants.java` is still the empty Quickstart file.
> Until it is filled in at drive tuning, every BioBuzz OpMode stops at INIT with the
> message "DRIVE NOT SET UP". That is expected.

---

## 10. When something goes wrong

1. **Read the telemetry first.** The header line shows alliance, match, drive mode
   and heading. Each brain shows its state, how long it has been there, and the
   reason for the last change.
2. **Follow the loop** (section 4): was the button read? Was the command called?
   What state is the brain in? What did the part get told?
3. **`Unable to find a hardware device with name "..."`** at INIT: a name in
   `configurations/HardwareNames.java` doesn't match the Robot Controller
   configuration. Check the hub and port in the comment next to that name. If every
   Expansion Hub device is missing, check that hub's power and its RS-485 cable.
4. **Field-centric points the wrong way:** face the robot away from the drivers and
   press Y. Use "Test: Drive" to check.
5. **A button does nothing:** check it is read only once per loop.

---

## 11. Words you will see

| Word | Meaning |
| --- | --- |
| OpMode | A program you pick on the Driver Station (TeleOp, Autonomous, a test) |
| Loop | One pass of read → command → update → show. About 50 per second |
| Brain / subsystem | A state machine that decides what one mechanism does |
| Part | A class that wraps hardware and does exactly what it is told |
| State | What a brain is doing right now (for example `DRIVER_CONTROL`) |
| Command | A method that asks a brain to do something (`resetHeading()`) |
| Pose | Where the robot is: x and y in inches, heading in degrees |
| Heading | Which way the robot faces |
| Field-centric | Stick directions follow the field, not the robot |
| Deadzone | Tiny stick values treated as 0, so the robot doesn't creep |
| Telemetry | Text the robot sends to the Driver Station and Dashboard |
| Dashboard | FTC Dashboard, a web page at `http://192.168.43.1:8080/dash` |
| Config | A file of numbers you can change live in the Dashboard |
| Pedro Pathing | The library that follows paths and knows where the robot is |
| Pinpoint | The goBILDA odometry computer that tracks the robot's position |
| Edge detection | Counting a button press once, not every loop it is held |
