# Tuning

Two kinds of tuning: **drive tuning** with Pedro Pathing's own tuners (once, then after big
changes), and **mechanism tuning** live in the Dashboard.

## Drive tuning (Pedro Pathing)

Today `pedro/Constants.java` and `pedro/Tuning.java` are the Quickstart's empty stubs, so every
BioBuzz OpMode that drives stops at INIT with "DRIVE NOT SET UP".

**Once, at the first tuning (a mentor):** fill in `Constants.java` with its full structure, as
in the DecodeBot code: the follower factory `create(hardwareMap)` with the drivetrain
(mecanum, motor names from `HardwareNames`), the Pinpoint localizer and the Foresight config.
Register the tuners we use in `Tuning.java`. Record this one change in `UPSTREAM.md`. From then
on, only VALUES change in `Constants.java`.

Then run the tuners in the Quickstart's `pedro/procedures/`, **in this order**. For each one:
what it measures → which number to change → what good looks like.

| # | Tuner | Measures | Change | Good |
| --- | --- | --- | --- | --- |
| 1 | Pinpoint tuner (`PinpointTuner`) | Pod offsets and pod directions | Pinpoint offsets and encoder directions in `Constants` | Pushing forward → x goes up; left → y goes up; turning in place → x and y hardly move |
| 2 | Localization test / push tests | Forward and lateral distance | Pod resolution (goBILDA pod type) | 48 in pushed reads 48 ± 0.5 in |
| 3 | Mecanum tuner (`MecanumTuner`) | Motor directions | Motor directions (`HardwareNames.MOTOR_DRIVE_*_REVERSED`) | Each wheel spins forward; the robot drives straight |
| 4 | Foresight tuner (`ForesightTuner`) | Max velocities, natural decelerations, braking, translational and heading constants | The values it prints into `Constants` (`naturalForwardDeceleration`, ...) | Paths end within an inch; no oscillation |
| 5 | Tests (`Tests`) | A line, a curve, a square | – | Ends close to the target, smooth |

**Then record the home baseline:** run Field Checks 01–12 at home, and copy the values
Field Check 13 shows into the `BASELINE_` values in `FieldCheckConfig` with the date
(`BASELINE_FORWARD_DECEL` and `BASELINE_STRAFE_DECEL` are the same numbers as
`naturalForwardDeceleration` / `naturalStrafeDeceleration`). Re-record after every re-tune
or hardware change.

**A Field Check FAIL tells you which tuner to run:** 03–05 → the hardware first, then the
Pinpoint tuner; 07 → use the measured deceleration for this event; 08–09 → the Foresight tuner.

## Mechanism tuning (live in the Dashboard)

Open http://192.168.43.1:8080/dash → Configuration. Run the mechanism's test OpMode and change
numbers while it runs. **Then type the good values into the config file, and commit them to
git with the date and the field type** (for example "launcher speeds, home field, 2026-11-02").

| What | Test OpMode | Numbers | Good |
| --- | --- | --- | --- |
| Intake power | Test: Intake | `IntakeConfig.COLLECT_POWER`, `REVERSE_POWER` | Pulls a ball in reliably, without jamming |
| Feeder positions | Test: Feeder | `FeederConfig.HOLD_POSITION`, `FEED_POSITION` | Hold keeps a ball back; feed pushes it fully into the flywheel |
| Flywheel speed control | Test: Launcher (Graph view: `launcher_rpm`, `launcher_target_rpm`) | `LauncherConfig.SPEED_F` first (about right with P = 0), then `SPEED_P`; check `TICKS_PER_REV` | Reaches the target within 1 s, little overshoot, recovers quickly after a ball |
| Speed table | Test: Launch, from each distance in `DISTANCE_IN` | `LauncherConfig.RPM` | Balls land in the CELL from every distance |
| Launch timing | Test: Launch | `FEED_MS`, `RECOVER_MS`, `SPIN_UP_TIMEOUT_MS` | 4 balls in the least time, none short |
| Aim | BioBuzz TeleOp, hold RB | `DriveConfig.AIM_P`, `AIM_D`; `LauncherConfig.AIM_OFFSET_DEG` | Turns to face the CELL quickly, without wobbling |
| Cameras | Test: Camera | `CameraConfig` | See `CAMERA_GUIDE.md` |
