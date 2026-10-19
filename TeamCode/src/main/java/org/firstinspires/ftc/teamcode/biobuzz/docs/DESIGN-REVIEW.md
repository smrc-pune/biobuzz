# BioBuzz Software – Design Review

Oct 19, 2026 · for mentor review before any code changes

This review compares three things: the requirements (v1.4), the new design the team agreed on in discussion, and the BIOBUZZ Competition Manual (TU05). It lists every conflict, misalignment and gap found, with the fix applied in Requirements v2.0, `HLD.md` and `LLD.md`, and the questions that only the team can answer.

**How to read it.** Section 1 is the design in one page, as it stands now (Requirements v2.3). Section 2 lists problems inside the old requirements. Section 3 lists what the game adds. Section 4 lists the design patterns used. Section 5 lists the questions and the team's answers. Sections 6 and 7 list the changes from the second and third review rounds; where a fix in sections 2–3 names `game/`, BLUE poses or NEAR/FAR, sections 6 and 7 replace it. Nothing in the code has changed yet.

---

## 1. The agreed design in one page

```
OpModes        TeleOp: buttons -> Robot methods     Auto: list of steps -> Robot methods
   |           (S1 manual, S2 assisted)             (S3 autonomous)
   v
Robot          the robot's "menu": one method per action. Builds everything.
   |           update(): 1. state machines  2. mechanisms  3. Drive (Pedro)  4. always-on rules
   v
statemachines/ tasks that take time and use several mechanisms (Launch). Standalone classes.
   |
   v
drive/         Drive (sticks, paths, pose) + FieldPaths (every path)      -> Pedro Pathing (off the shelf)
mechanisms/    Intake, Feeder, Launcher, HiveCamera, FrontCamera: operations only, no states.
               Robot updates every mechanism once per loop; state machines only command them.
               Every mechanism extends one plain base class, Mechanism.     -> FTC SDK (off the shelf)
```

Owners: rookie 7th graders with no Java. Student zone: configurations, mechanisms, state machines, TeleOp, Auto, `FieldPaths`, Robot's marked spots, tests. Mentor zone: `Mechanism`, `Drive`, helpers, Field Checks, `pedro/`.

**Where does my code go?**

| If it... | It goes in |
| --- | --- |
| happens when a button is pressed | `teleop/BioBuzzTeleOp.java` |
| moves or reads one device group | that mechanism's class in `mechanisms/` |
| takes several steps over time, or needs two mechanisms together | a class in `statemachines/`, started through a `Robot` method |
| must always happen, in every OpMode | `Robot.update()` |
| is a place or a path on the field | `configurations/FieldConfig.java` (poses, RED side) and `drive/FieldPaths.java` (paths) |
| is a number someone will tune | a `*Config` class in `configurations/` |

**The three stages of driving.** The layers below the OpMode are the same in every stage.

| Stage | Who drives | Where |
| --- | --- | --- |
| S1 Manual | Driver only | TeleOp, sticks only |
| S2 Assisted | Driver, plus a few buttons that drive a short fixed path (to our favourite launch spot, to the LOADING ZONE). Any stick takes the robot back at once | Same TeleOp |
| S3 Autonomous | Robot only | `auto/BioBuzzAuto.java` step list |

Build order: S1 (exists) → S3 (autonomous, proves paths and pose) → S2 (reuses S3's paths).

---

## 2. Problems in Requirements v1.4

### 2.1 Conflicts with the agreed design

| # | v1.4 says | Problem | Fixed in v2.0 by |
| --- | --- | --- | --- |
| C1 | Four layers: parts (hardware) + subsystems (state machine "brains"), one brain per mechanism | Most mechanisms have no states (an intake is on, off or reversed). States belong to **tasks** that take time and often use several mechanisms (launching uses Storage + Launcher). One-brain-per-mechanism forced fake states and scattered one task over several files and Robot "links" | Mechanisms are operation classes; tasks are state machines in `statemachines/` |
| C2 | `Drive` has three states (`DRIVER_CONTROL`, `FOLLOWING_PATH`, `HOLDING`) | These are "who is driving", which the OpMode already knows. Pedro already switches FOLLOW / HOLD / MANUAL itself | `Drive` is an operation class: `driveWithSticks`, `followPath`, `isDrivingItself`, `holdHere` |
| C3 | `Robot` "links" subsystems; `Robot.isBusy()` is true if any subsystem is busy | Links were hidden cross-talk. `isBusy()` mixes unrelated jobs, so an auto step could wait on the wrong thing | Links become state machines. Robot's start methods cancel any state machine that shares a mechanism (Mediator). Auto steps check the one thing they started |
| C4 | R3: no abstract base classes; `OpMode` is the only superclass | The team chose one plain base class, `Mechanism`, so adding a mechanism is 2 edits in Robot instead of ~6 | R3 amended: exactly one team base class, `Mechanism`, one level deep, not abstract |
| C5 | R3: `try`/`catch` only in `helpers/`. Troubleshooting: "after PLAY nothing MUST throw" | Mechanisms call the SDK directly, so an SDK exception would crash the OpMode, and nothing outside helpers may catch it | The one `try`/`catch` outside helpers is in `Mechanism.runUpdate()`: an exception marks that mechanism FAULTED and the rest keep running |
| C6 | R3: `enum` for state lists only | Auto also needs `enum Step` and `enum Routine` | `enum` allowed for fixed lists: states, steps, routines |
| C7 | R10: layout MUST follow DecodeBot | The new layout differs (`mechanisms/`, `statemachines/`, `drive/`, `game/`) | R10 amended: this document defines the layout; DecodeBot is the style reference where it is silent |
| C8 | Device disconnected → "that subsystem goes to a FAULT state" | Mechanisms no longer have states | `Mechanism` has a FAULTED flag with a reason; Robot skips a faulted mechanism and raises an ALERT |
| C9 | Vision: `subsystems/Vision.java` reads AprilTags and corrects the pose | The team uses a **HuskyLens**. It returns 2D boxes (x, y, width, height) with IDs it learned itself, not AprilTag IDs and not a 3D pose. It runs one algorithm at a time and is read over I2C (a few ms per read) | `mechanisms/Camera.java` answers questions (is a tag in view, its bearing and size). Pose fixes are optional and conservative. See section 3 |
| C10 | "Pedro's API appears only in `auto/AutoPaths.java`" | TeleOp assists need paths too, and they start from wherever the robot is | `game/FieldPaths.java` builds every path (auto and assists). Pedro's API appears only there and in `drive/Drive.java` |
| C11 | "Each subsystem MUST have `Robot.buildX(hardwareMap)`" | Mechanisms build themselves: `new Intake(hardwareMap)`. A test OpMode builds just the one it tests | `buildX` methods removed |
| C12 | "Every button calls one subsystem command" | Gamepad 2 buttons could start a state machine and a mechanism verb that fight over the same motor | Every gamepad-2 button calls one `Robot` method. Robot cancels the state machine that owns that mechanism before a manual command |
| C13 | Helpers raise ALERTs "as a gamepad rumble" | Only OpModes may touch gamepads (rule from v1.3), so a helper cannot rumble | Robot keeps the newest urgent ALERT. The OpMode asks `robot.hasNewUrgentAlert()` and rumbles |

### 2.2 Misalignments inside v1.4 (or between v1.4 and the code)

| # | Where | Problem | Fix |
| --- | --- | --- | --- |
| M1 | Driving: default alliance is `AutoConfig.ALLIANCE_RED` | `MatchInfo` defaults to BLUE, and the INIT menu is now the only alliance choice | Default is `MatchInfo.alliance`, BLUE until chosen. `AutoConfig.ALLIANCE_RED` removed |
| M2 | Driving: with no auto pose, TeleOp "uses the alliance's default start pose" | The code starts at (0, 0, driver heading), and a guessed x/y would send assist paths to the wrong place | No auto pose → TeleOp starts as **S1**: field-centric works (it needs the heading only), and assist buttons are refused until the pose is known. See question Q1 |
| M3 | Autonomous: `setStartingPose(...)` in `init()` | Alliance and start position are chosen in `init_loop()`, after `init()` | The start pose is set in `start()`, from the menu choices |
| M4 | Autonomous: RED poses are mirrored (`mirrorX` or `mirrorY`) | The BIOBUZZ field is **rotationally symmetric**: red's GARDEN, LOADING ZONE, FLOWERS and CELLS are blue's turned 180° about the field centre. A single mirror would put red paths in the wrong places | RED = BLUE rotated 180° about (72, 72): both reflections chained, `mirrorX(72.0).mirrorY(72.0)` (agreed, Q7) |
| M5 | `DriveConfig.BLUE_DRIVER_HEADING_DEG = 0`, `RED = 180` | With our field frame (image as drawn, origin bottom-left, red ALLIANCE AREA on the left wall), red drivers face +x (0°) and blue drivers face −x (180°): the values look swapped | Still TODO, now with the frame written down. Confirm on the field (Q8) |
| M6 | Field image | Committed at `TeamCode/src/assets/images/field-biobuzz.png`. The Dashboard serves only `TeamCode/src/main/assets/images/` | **Done:** moved to `TeamCode/src/main/assets/images/field-biobuzz.png` |
| M7 | Telemetry header shows "match time left" | Nothing in the design keeps match time | New `helpers/MatchClock.java` (also needed for game timing, section 3) |
| M8 | Field Check 01: "a missing device names the part" | Plain `hardwareMap.get` crashes on the first missing device | Field Check 01 and Robot Check try each name in `HardwareNames.ALL_DEVICES` (a fixed array) with `hardwareMap.tryGet` and list every missing one |
| M9 | Robot Check step 4 spins each drive motor | Pedro owns the drive motors | Robot Check gets the four motors from `hardwareMap` itself and does not build the follower |
| M10 | Log columns `<subsystem>_state` | No subsystems any more | `<statemachine>_state` plus each mechanism's `addLogData()` values and fault flag |
| M11 | Gamepad disconnected → "drive powers set to zero" | In S2 a path may be running | Also `drive.stopDrivingItself()` |
| M12 | Purpose: "game details are not known yet" | The manual is now available | Game section added, with the remaining unknowns as TODO and questions |

### 2.3 A requirement conflict to keep in mind (no change needed)

**R2 (gamepad 1 drives, gamepad 2 runs mechanisms) vs. a combined "drive to the spot and launch" button.** A single button that both drives and launches would break R2 whichever gamepad has it. Rule in v2.0: no button may command both driving and mechanisms. The driver drives to the spot (gamepad 1), and the operator launches (gamepad 2).

---

## 3. What the BIOBUZZ game adds (gaps found)

Points from Table 10-2: LEAVE 3 (auto), PARK 5 (auto and teleop), HIVE TIP 20 (auto and teleop), element left in an upward CELL 2, FLOWER points (last 60 s), GARDEN 1. RP: SWARM (LEAVE + PARK ≥ 16 per alliance), POLLINATOR 1 and 2 (4 and 7 TIPS).

| # | Game fact (manual) | What the software must do | Where |
| --- | --- | --- | --- |
| G1 | AUTO 30 s, then an 8 s transition, then TELEOP 2:00. LEAVE and PARK are scored at the end of AUTO; PARK again at the end of the MATCH (10.4, 10.5) | `MatchClock` gives time left. Auto has a **park-by** guard: whatever step it is on, it goes to PARK at `AutoConfig.PARK_BY_S`. TeleOp rumbles at 0:20 left ("park") | Auto, TeleOp, `MatchClock` |
| G2 | Robots must be motionless after INIT completes (G304.H) and during the transition (G403): "if INIT causes the ROBOT to move or twitch, wait until TELEOP begins before pressing INIT" | Mechanism constructors never move anything. Only Auto INIT may move actuators (`robot.holdPreloads()`, to grip the 4 POLLEN), and only at the start of INIT. TeleOp INIT never commands an actuator; the first command is in `start()` | `Mechanism` rule, Auto, TeleOp |
| G3 | A robot may CONTROL at most 4 elements (G407); 5 or more held repeatedly is likely STRATEGIC | Storage counts what it holds. `CollectStateMachine` stops the intake at 4. If a 5th is seen, the intake reverses briefly, with an ALERT and a rumble | Storage, `CollectStateMachine`, `StorageConfig.MAX_HELD = 4` |
| G4 | A robot may not CONTROL opponent NECTAR (G408), even with an auto-discard sorter if repeated | An element's colour is checked at the intake. Opponent NECTAR (red vs blue, from `MatchInfo`) is pushed straight back out (reverse `REJECT_MS`) and logged | Storage colour sensor, `CollectStateMachine` |
| G5 | Each TIP flips the HIVE: the other CELL faces up (9.6, 10.5.1). Launching into the downward CELL during a tip may spoil the TIP | Software must know **which CELL is up**: the launch spot and aim depend on it. `game/HiveState` starts from the setup position, flips on each TIP, and pauses launching for `LauncherConfig.TIP_PAUSE_MS` after a TIP | `HiveState`, `LaunchStateMachine`, operator button (Q2) |
| G6 | POLLEN is 2.8 in, NECTAR 3.6 in (9.8) | The launcher speed depends on the element type and the distance to the upward CELL | `LauncherConfig` speed tables (fixed arrays), `Launcher.setTargetFor(type, distance)` |
| G7 | During AUTO each alliance stays on its own side: columns A–C red, D–F blue (G402) | Every auto path is checked at INIT against the alliance's half of the field; a path that crosses shows a red warning on the INIT screen | `FieldPaths.isOnOwnSide(...)` |
| G8 | Robots may not catch elements falling from a tipped HIVE (G409), or touch the HIVE (G417) | Paths keep out of the HIVE footprint (49.5 × 39 in at the centre) plus a margin | `FieldConfig.HIVE_KEEP_OUT_IN`; paths checked when built |
| G9 | Start: touching the wall, own side, not in the LOADING ZONE, touching 4 POLLEN (G304) | Start positions are a fixed list in `FieldConfig`, picked in the INIT menu | `FieldConfig`, `InitMenu` Start row |
| G10 | AprilTags are only on the **bottom** of each CELL, facing down (9.9). Tags 38–41 are the blue audience-side CELL; the other IDs are hard to read in our copy of the manual | The HuskyLens must face upward toward the HIVE. Because it learns its own IDs, it must be trained once per tag (a procedure in `CAMERA_GUIDE.md`). The first use is "which CELL is up, and how far left or right is it"; a pose fix comes later, if at all | `Camera`, `CameraConfig`, Q2, Q9 |
| G11 | 8 motors and 8 servos at most (R503); Control Hub plus at most one Expansion Hub (R701) | `HardwareNames` keeps a motor and servo count in its header. The drive uses 4 motors, leaving 4 | `HardwareNames` |
| G12 | NECTAR enters the field only after a TIP of your HIVE, or with 60 s left (G426). The FLOWER opens with 60 s left (G410) | Rumble both gamepads at 1:00 left. The human player must know when NECTAR can be entered: the header shows `NECTAR: +n` (one per TIP, from `HiveState`) | `MatchClock`, `HiveState`, TeleOp |
| G13 | FLOWER scoring (Bottom NECTAR Bonus 5, owned FLOWER 2 per element) needs NECTAR placed into a 4 in opening 21.5 in high | The planned robot has no FLOWER placer, so these points are not planned. Recorded as a strategy gap, not a software one | Q3 |
| G14 | SWARM RP needs LEAVE + PARK ≥ 16 per alliance | Auto always ends with PARK (guard G1). TeleOp has a one-button assist to the LOADING ZONE (S2) | Auto, TeleOp |
| G15 | 4 FLOWERS hold POLLEN that can be pulled from a Retrieval Opening at the bottom (9.7), and each GARDEN holds 4 POLLEN in a line | Collect positions for auto (own GARDEN, own-side FLOWERS) are poses in `FieldConfig` | `FieldConfig`, Auto |

---

## 4. Design patterns used (and not used)

Each pattern below is a standard one. The HLD names it where it is used, so students learn the real names.

| Pattern | Where | Why it helps students |
| --- | --- | --- |
| **Layered architecture** | OpMode → Robot → state machines → mechanisms/Drive | A bug can only be in one layer; each layer only calls the one below |
| **Game Loop + Update Method** | Every OpMode's `loop()` calls `robot.update()` once; every mechanism and state machine has `update()` | Nothing waits or sleeps; everything moves one small step per loop |
| **Facade** | `Robot` (one menu of robot actions); `Drive` (simple verbs over Pedro's follower) | OpModes never need to know how many parts are behind an action |
| **Adapter** (wrapper) | Each mechanism wraps SDK devices with game verbs (`intake.collect()`); `HiveCamera` wraps the HuskyLens, `FrontCamera` the webcam's `VisionPortal`; `GamepadInput` wraps `Gamepad` and adds edge detection | Game words in OpModes, device details in one file |
| **Template Method** | `Mechanism.runUpdate()` (fixed: skip if faulted, catch errors) calls the hook `update()` that each mechanism overrides; the same for `stop()`, `addTelemetry()`, `addLogData()` | Students write only the hooks; safety is built in once |
| **State pattern (enum + switch form)** | Each class in `statemachines/` and Auto's step list | The classic "class per state" State pattern is replaced by its simplest form, an `enum` and a `switch`, because R3 forbids interfaces |
| **Mediator** | `Robot.startLaunching()` cancels `CollectStateMachine` first, because both use Storage | State machines never call each other; the one place that knows about conflicts is Robot |
| **Dependency injection** (constructor injection) + **Composition root** | Robot builds the mechanisms, then passes them into each state machine's constructor | A state machine can be tested with just the mechanisms it needs; nothing finds its own dependencies |
| **Factory method** | `FieldPaths.toLaunchSpot(from, ...)`, `FieldPaths.toLoadingZone(from, ...)` | One place builds paths; TeleOp and Auto get the same spot |
| **Monostate / shared static state** | `MatchInfo`, `PoseMemory` (static fields that survive from Auto to TeleOp) | Used only where the SDK gives no other way to pass data between OpModes; named as a known risk |

**Not used, on purpose:** the Command pattern and command schedulers (FTCLib, NextFTC), because they need lambdas and interfaces and hide the flow (R3); Observer/event buses, because direct calls are easier to trace; Strategy with interfaces, because auto routines are an `enum` plus a `switch`.

---

## 5. Questions for the team

Team decisions were entered in the shared review doc on Oct 19, 2026 and applied in Requirements v2.1.

| # | Question | Recommendation | Team decision |
| --- | --- | --- | --- |
| Q1 | "Always start with S1": did you mean (a) with no auto pose, TeleOp starts as S1 and assists are refused until the pose is known, or (b) TeleOp always starts as S1, even after Auto, and assists are turned on some other way? | (a), written that way in v2.0 | Agreed: (a) |
| Q2 | Which CELL is up: how should the robot know? (a) the operator presses a button at each TIP, (b) the HuskyLens, (c) both | (a) first: simple and reliable. The camera can confirm later. Written as (a) with the camera as optional | Agreed: (a) |
| Q3 | FLOWER scoring (Bottom NECTAR Bonus, owned FLOWER) is not possible without a placer. Should the software plan for it at all? | No, for now. Leave a TODO | Agreed: not planned |
| Q4 | Without a camera pose fix, the only way to recover x/y in TeleOp is a "set pose here" button pressed with the robot backed into a known corner (for example the LOADING ZONE corner). Add it? | Yes: one gamepad-1 button and one `FieldConfig` pose | Agreed |
| Q5 | Storage sensing: which sensors count the elements held and read their colour (one colour sensor at the intake mouth, a distance sensor per slot, …)? | Needed before Storage can be designed; TODO in the LLD | **Deferred:** "this will come later once the design is evolved" |
| Q6 | Confirm: no button may command both driving and mechanisms (section 2.3) | Yes | Agreed: gamepad 1 for drive, gamepad 2 for operator |
| Q7 | Confirm the field is rotationally symmetric (red = blue turned 180° about the centre), from Figure 9-2 or the CAD | Our field image shows rotation, not a mirror | Agreed: chain both reflections, `mirrorX(72.0).mirrorY(72.0)` |
| Q8 | Confirm the field frame: the field image as drawn, origin at the bottom-left corner (red GARDEN corner), x to the right, y up, audience at the bottom. Then red drivers face 0° and blue drivers face 180° | Yes | Agreed |
| Q9 | HuskyLens: one unit looking up at the HIVE (tags), or also one looking forward for element colour? It runs one algorithm at a time | One, looking up, tag mode only | **Changed:** add a 2nd camera for field AprilTags, POLLEN and NECTAR; it is a Logitech USB webcam |
| Q10 | Which auto routines for the first event? For example "near: launch 4, collect GARDEN, launch, park" and "far: launch 4, park" | Start with these two | Agreed |

**Follow-up from Q9.** A second HuskyLens would have shared the first one's fixed I2C address and had to switch between tag and colour mode. The team chose a Logitech USB webcam instead: the SDK's `VisionPortal` runs AprilTag and colour processing at the same time, with real AprilTag IDs and a tag pose. Its cost is Control Hub CPU, so the loop time is measured with the webcam streaming.

---

## 6. Review round 2 (Oct 19, 2026)

| # | Team input | Change in v2.2 |
| --- | --- | --- |
| R2-1 | Show Pedro Pathing in the design diagram as off-the-shelf | Diagrams in the HLD, Requirements and the shared review doc show Pedro Pathing, the FTC SDK and FTC Dashboard as libraries we use as they come; only `Drive` and `FieldPaths` call Pedro |
| R2-2 | Key-value pairs instead of an array, so students don't remember index numbers | The list stays, but it is written by name (`{ intake, storage, launcher, ... }`) and walked with a "for each" loop, so no index numbers appear anywhere; mechanisms are always used by their field name. A `Map<String, Mechanism>` was considered: it adds generics and string keys that fail only while the robot runs, and Robot never needs to look one up. R3 now allows "for each" loops |
| R2-3 | Standardize on `isBusy` | State machines use `isBusy()` (Robot's `isCollecting()` / `isLaunching()` call it) |
| R2-4 | Remove `game/` if it adds little | Removed: `FieldPaths` moved to `drive/`, `Element` to `mechanisms/`, the HIVE state (`HiveState`) folded into `MatchInfo` with the other match facts |
| R2-5 | Rookie 7th graders with no Java will own the code | Requirements Audience and R3 rewritten around them; student zone and mentor zone, named in each file's first line; plain-English names; the HLD rewritten in simpler words with the patterns moved to a "for mentors" section; a student-level Intake sketch in the LLD |
| R2-6 | No near or far zones: launch from anywhere | Routines renamed `LAUNCH_PARK` and `LAUNCH_COLLECT_PARK`; Auto launches its preloads right after LEAVE; one favourite `LAUNCH_SPOT` (was one per CELL) for the assist; the launcher speed follows the distance to the upward CELL |
| R2-7 | Default poses for RED | All poses are written for RED, the default alliance (`MatchInfo` starts as RED); BLUE poses are turned 180° by `FieldPaths.forAlliance` |

---

## 7. Review round 3 (Oct 19, 2026)

| # | Team input | Change in v2.3 |
| --- | --- | --- |
| R3-1 | What is `Element.java` for? Rename to `GameElement` if really needed | Not needed: it told the launcher which ball was next and spotted opponent NECTAR, and both need Storage sensors we don't have. Removed; the front camera uses plain questions (`seesPollen()`, `seesRedNectar()`, `seesBlueNectar()`) |
| R3-2 | Should Robot call the mechanisms' `update()` when state machines sit between them in the diagram? | Yes. A layer may call any layer below it. Every mechanism needs its `update()` once per loop even when no state machine uses it (cameras, launcher speed), and a shared mechanism must be updated exactly once. State machines only command mechanisms. The diagrams now show the Robot → mechanisms arrow and say why |
| R3-3 | No sensors to count balls: keep Storage for the future | No Storage mechanism now (future improvement, with `CollectStateMachine`). A separate `Feeder` mechanism pushes balls into the launcher. The intake is manual (gamepad 2 A on/off, B hold reverse). G407 is met by the hopper holding 4; G408 is the drive team's job. `LaunchStateMachine` feeds `count` times; launcher speed by distance only |
