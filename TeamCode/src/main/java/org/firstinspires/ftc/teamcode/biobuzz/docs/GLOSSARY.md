# Glossary

## Code words

| Word | Meaning |
| --- | --- |
| OpMode | A program you pick on the Driver Station (TeleOp, Autonomous, a test) |
| Loop | One pass of read → command → update → show; about 50 a second. Nothing waits |
| Robot | `Robot.java`: the one object that holds the drive and the subsystems (`robot.intake`, `robot.shooter`, `robot.vision`, `robot.driveToSpot`) and updates them every loop |
| Subsystem | One job of the robot (intake, shooter, vision). Owns its mechanisms and the state machines that use them; its methods are the menu OpModes call. No states |
| Menu method | A subsystem's public method that an OpMode calls, for example `robot.shooter.launch(4)` |
| Mechanism | One group of devices (intake, launcher) with action words (verbs) and questions. No states |
| Hook | A method a mechanism fills in for `Mechanism`: `update()`, `stop()`, `addDetails()`, `addLogValues()`, `isTelemetryOn()` |
| FAULTED | A mechanism that hit an error (often a cable) and stopped; the rest of the robot keeps going |
| State machine | A job that takes time, done one small step per loop (`LaunchStateMachine`) |
| State | What a state machine is doing right now (`SPINNING_UP`) |
| DO / CHECK / GO | Inside each state: what it does, what it checks, where it goes next |
| Timeout | The longest a state (or Auto step) may wait before giving up safely |
| Pose | Where the robot is: x and y in inches, heading in degrees |
| Pose known | We trust x and y (after Auto, or "set pose here"), so S2 works |
| Heading | Which way the robot faces (0° = toward the blue wall, +x) |
| Field-centric | Stick directions follow the field: away = away from the drivers |
| Robot-centric | Stick directions follow the robot's own front |
| S1 / S2 / S3 | Manual driving / assisted driving (a button drives a short path) / Autonomous |
| Assist | A button that drives the robot along a short path by itself; any stick takes it back |
| Path | A line or curve Pedro Pathing drives along (`FieldPaths`) |
| Deadband / deadzone | Tiny stick values treated as 0, so the robot doesn't creep |
| Edge detection | "Pressed" is true only on the one loop the button goes down |
| PIDF | The four numbers that make a motor hold a speed (P, I, D, F) |
| RPM | Revolutions (turns) per minute |
| Config | A file in `configurations/` with numbers you can change live in the Dashboard |
| ALERT | A warning to the drive team on the screen; urgent ones rumble the gamepads |
| Match log | The CSV file every run writes (`MATCH_LOGS.md`) |
| Baseline | Our home-field Field Check results, to compare an event field with |
| enum | A fixed list of names, like `AllianceColor { RED, BLUE }` |
| Off the shelf | Code written by others that we use as it comes (Pedro Pathing, FTC SDK, FTC Dashboard) |

## Design patterns (for curious students)

| Pattern | In our code |
| --- | --- |
| Layered architecture | OpMode → subsystems → state machines → mechanisms; each layer only calls the ones below |
| Game Loop / Update Method | `loop()` → `robot.update()` → every `update()` |
| Facade | Each subsystem and `Drive`: one simple front for many parts |
| Composition | Building a big object out of smaller ones it HAS: Robot has subsystems, a subsystem has mechanisms |
| Adapter | Mechanisms and `GamepadInput`: device calls become game words |
| Template Method | `Mechanism.runUpdate()` calls your `update()` safely |
| State pattern (enum + switch) | `LaunchStateMachine`, Auto's steps |
| Mediator | A subsystem's menu methods cancel its state machine when they share a mechanism |
| Dependency injection | `new LaunchStateMachine(feeder, launcher, drive)` |
| Factory method | `FieldPaths.toLaunchSpot(...)` |

## Game words (BIOBUZZ)

| Word | Meaning |
| --- | --- |
| POLLEN | Yellow ball, 2.8 in |
| NECTAR | Red or blue ball, 3.6 in; never control the opponent's (G408) |
| HIVE | The structure in the middle of the field, with our two CELLS |
| CELL | Where balls are launched; one of our two faces up at a time |
| TIP | The HIVE tips over when enough balls land: 20 points, the other CELL faces up |
| GARDEN | Our corner line of 4 POLLEN |
| LOADING ZONE | Our box on our wall: PARK here; NECTAR is picked up here |
| FLOWER | Scoring places on the walls in the last 60 s (not planned for our robot) |
| LEAVE / PARK | Auto points for moving off the wall / ending in the LOADING ZONE |
| SWARM, POLLINATOR | Ranking points (LEAVE + PARK; 4 and 7 TIPS) |
| AUTO / TELEOP | The 30 s autonomous period / the 2:00 driver period, 8 s apart |
