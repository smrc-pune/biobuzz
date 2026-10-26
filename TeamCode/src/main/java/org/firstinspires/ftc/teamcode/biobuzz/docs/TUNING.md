# Tuning

Two kinds of tuning: **drive tuning** with Pedro Pathing's own tuners (once, then after big
changes), and **mechanism tuning** live in the Dashboard.

## Drive tuning (Pedro Pathing)

Drive tuning follows our **Pedro tuning kit**, kept in its own repository:
**[smrc-pune/pedropathing → pedro-tuning-kit](https://github.com/smrc-pune/pedropathing/tree/main/pedro-tuning-kit)**. It has:

| Kit file | What it is |
| --- | --- |
| [`Tuning_Checklist.md`](https://github.com/smrc-pune/pedropathing/blob/main/pedro-tuning-kit/docs/pedro-tuning/Tuning_Checklist.md) (+ PDF) | **The steps.** Stages 0–5, a gate after each stage, a sign-off log. Print it |
| [`Pedro_Pathing_Constants_Explained.md`](https://github.com/smrc-pune/pedropathing/blob/main/pedro-tuning-kit/docs/pedro-tuning/Pedro_Pathing_Constants_Explained.md) (+ PDF) | Why each value in `Constants.java` matters; what to do when a test fails |
| [`tuning-checker/index.html`](https://github.com/smrc-pune/pedropathing/blob/main/pedro-tuning-kit/docs/pedro-tuning/tuning-checker/index.html) | Download it (GitHub shows its source; use **Download raw file**), open it in any browser, paste 2–3 Foresight runs: it checks, compares and averages them. Works offline |
| [Staged `Tuning.java`](https://github.com/smrc-pune/pedropathing/blob/main/pedro-tuning-kit/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/Tuning.java) | Replaces our `pedro/Tuning.java`. One part per stage, switched on as you go |

Today our `pedro/Constants.java` and `pedro/Tuning.java` are the Quickstart's empty stubs, so
every BioBuzz OpMode that drives stops at INIT with "DRIVE NOT SET UP".

**Before Stage 0, once (a mentor):** give `Constants.java` its full structure, as in the
DecodeBot code: three config blocks with **exactly** these names, `drivetrainConfig`
(mecanum, motor names and directions from `HardwareNames`), `localizerConfig` (Pinpoint, name
`HardwareNames.PINPOINT`) and `foresightConfig`, and the factory `create(hardwareMap)`. The
kit's checklist needs those three names. Copy the kit's staged `Tuning.java` over ours (back
the old one up first: Stage 0 says how). Record both in `UPSTREAM.md`. From then on, only
VALUES change in `Constants.java`.

**Then work through the kit's checklist, top to bottom.** Do not start a stage until the
gate before it passes. If you change an earlier stage, redo every gate after it.

| Stage | What | Its gate (what good looks like) |
| --- | --- | --- |
| 0 | Before you start: battery, room, Pinpoint on an I2C port **other than 0**, sticker side up, pods in X and Y ports | All boxes ticked |
| 1 | Mecanum Procedure: each motor's direction → paste over `drivetrainConfig` | Sticks drive straight, strafe straight, turn in place; tape **FRONT** on the robot |
| 2 | Pinpoint Procedure: pod directions and offsets → paste over `localizerConfig` | Push FRONT → x up; push left → y up; offsets match a ruler; 48 in reads 48; spin test returns near 0 |
| 3 | Foresight Procedure (10 steps), run **2–3 times**, averaged in the tuning checker → paste over `foresightConfig` | Runs agree; no 0 or `NaN`; the checker's checks pass |
| 4 | Wire it in: `new Foresight(foresightConfig)` in `create()`, not `null` | Tests opens Hold and Line without "Algorithm is required" |
| 5 | Tests: Hold, Line, Curved, Interpolation | Holds its spot; 5 Line laps on the tape; smooth curve |

Fill in the kit's **sign-off log** as you pass each gate, and commit `pedro/Constants.java`
with the date. Keep `drivetrainConfig` reading the motor names and directions from
`HardwareNames` (the kit says this is fine) and put the directions Stage 1 found into
`HardwareNames.MOTOR_DRIVE_*_REVERSED`, so Pedro, Robot Check and Test: Parts all agree.

**Then check our side:** "DRIVE NOT SET UP" is gone, and **Test: Drive** drives
field-centric (`NEW_ROBOT_CHECKLIST.md`, step 7).

**Then record the home baseline:** run Field Checks 01–12 at home, and copy the values
Field Check 13 shows into the `BASELINE_` values in `FieldCheckConfig` with the date
(`BASELINE_FORWARD_DECEL` and `BASELINE_STRAFE_DECEL` are the same numbers as
`naturalForwardDeceleration` / `naturalStrafeDeceleration`). Re-record after every re-tune
or hardware change.

**A Field Check FAIL tells you which tuner to run:** 03–05 → the hardware first, then the
kit's Stage 2 (Pinpoint); 07 → use the measured deceleration for this event; 08–09 → the kit's Stage 3 (Foresight).

## Mechanism tuning (live in the Dashboard)

Open http://192.168.43.1:8080/dash → Configuration. Run the mechanism's test OpMode and change
numbers while it runs. **Then type the good values into the config file, and commit them to
git with the date and the field type** (for example "launch speed, home field, 2026-11-02").

| What | Test OpMode | Numbers | Good |
| --- | --- | --- | --- |
| Intake power | Test: Intake | `IntakeConfig.COLLECT_POWER`, `REVERSE_POWER` | Pulls a ball in reliably, without jamming |
| Feeder positions | Test: Feeder | `FeederConfig.HOLD_POSITION`, `FEED_POSITION` | Hold keeps a ball back; feed pushes it fully into the flywheel |
| Flywheel speed control | Test: Launcher (Graph view: `launcher_rpm`, `launcher_target_rpm`) | `LauncherConfig.SPEED_F` first (about right with P = 0), then `SPEED_P`; check `TICKS_PER_REV` | Reaches the target within 1 s, little overshoot, recovers quickly after a ball |
| Launch speed | Test: Launch, robot on our launch spot (`FieldConfig.LAUNCH_SPOT`) facing the CELL. Short: raise it 100 at a time; long: lower it | `LauncherConfig.LAUNCH_RPM` | All 4 balls land in the CELL from the launch spot |
| Launch timing | Test: Launch | `FEED_MS`, `RECOVER_MS`, `SPIN_UP_TIMEOUT_MS` | 4 balls in the least time, none short |
| Aim | BioBuzz TeleOp, hold RB | `DriveConfig.AIM_P`, `AIM_D`; `LauncherConfig.AIM_OFFSET_DEG` | Turns to face the CELL quickly, without wobbling |
| Cameras | Test: Camera | `CameraConfig` | See `CAMERA_GUIDE.md` |
