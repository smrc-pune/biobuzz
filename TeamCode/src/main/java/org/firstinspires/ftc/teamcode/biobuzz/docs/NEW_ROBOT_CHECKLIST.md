# New robot checklist – from a built robot to a match-ready robot

Use this when the mechanical build is done and the wiring is in. Go **in order**: each step
needs the one before it. Tick the boxes as you go, and write the date next to each step.
The other guides explain the details; this page tells you which one to open, and when.

**Who:** most steps are for students. The steps marked **(mentor)** need a mentor there.
**Time:** about 4 to 6 sessions, and drive tuning is the longest step.

**Safety, every time the robot is powered:**
- Wheels off the floor (on blocks) unless the step says "on the floor".
- One person holds the Driver Station and can press **STOP** at once.
- Hands, hair and cables away from the wheels, the intake and the flywheel.

---

## Step 0 – The laptop and the code

- [ ] Android Studio installed, project opened, **Build → Make Project** ends with
      "BUILD SUCCESSFUL" (`GETTING_STARTED.md`, sections 1–2).
- [ ] You have read `README.md` sections 1–4 (the big idea, the parts, where things live, one loop).
- [ ] A charged battery (about 13 V or more), and a spare.

**Done when:** the project builds on the laptop that will deploy to the robot.

## Step 1 – Write down the wiring

Open `configurations/HardwareNames.java`. It is the wiring sheet.

- [ ] For every motor, servo and sensor: the **name**, the **hub** (Control Hub or Expansion
      Hub) and the **port**, in the comment next to it. Drive motors on the Control Hub.
- [ ] A new device? Add its name here AND to `ALL_DEVICES`.
- [ ] Count: at most 8 motors and 8 servos (game rules R503, R701). Update the count in the
      file's header.

**Done when:** the file matches the real robot, cable by cable.

## Step 2 – Robot configuration on the Driver Station

- [ ] Driver Station: ⋮ → **Configure Robot** → a new configuration named `BioBuzz`.
- [ ] Every device on the hub and port from step 1, with **exactly** the same name
      (capital letters count). The Pinpoint (goBILDA Pinpoint) on its I2C bus; the HuskyLens
      as "HuskyLens"; the webcam under the Control Hub's USB devices.
- [ ] Save, and make it the **active** configuration.

**Done when:** the Driver Station shows `BioBuzz` as the active configuration.

## Step 3 – Put the code on the robot

- [ ] Deploy (`GETTING_STARTED.md`, section 3): Wi-Fi or USB, pick "REV Control Hub", **Run**.
- [ ] The Driver Station connects, and the lists show BioBuzz TeleOp, the Tests and the
      Field Checks.

**Done when:** you can pick an OpMode on the Driver Station.

## Step 4 – Every motor and servo, one at a time (on blocks)

Run **Test: Parts** (group "Tests"). D-pad up/down picks a device; only that one moves.

- [ ] Nothing says **MISSING**. If something does, the name in step 1 and step 2 differ.
- [ ] Each motor: push the left stick UP gently. It turns the "forward" way (wheels forward,
      intake pulls in, launcher shoots out), and its encoder count goes UP. If not, flip its
      `..._REVERSED` value in `HardwareNames` (true ↔ false), deploy, check again.
- [ ] Each servo: move it SLOWLY with the right stick. Write down the positions where it is
      safe; do not push it into a hard stop.

**Done when:** every device moves the right way and none is missing.

## Step 5 – Robot Check (on blocks)

Run **Robot Check** (group "Tests"). A = next step, B = repeat.

- [ ] BATTERY, PINPOINT, GYRO_STILL, LOGGER: all PASS.
- [ ] DRIVE_MOTORS: each wheel turns forward (A). X = wrong way: fix it in `HardwareNames`.
- [ ] It ends with **READY FOR MATCH**.

**Done when:** READY FOR MATCH. (Drive tuning in step 6 may change wheel directions again.)

## Step 6 – Drive tuning (mentor)

Until this step, every OpMode that drives stops at INIT with **"DRIVE NOT SET UP"**. That is
expected.

- [ ] **(mentor)** Fill in `pedro/Constants.java` once (drivetrain, Pinpoint, Foresight) and
      write the change in `UPSTREAM.md` (`TUNING.md`, "Once, at the first tuning").
- [ ] Run the tuners **in order**: Pinpoint tuner → localization push tests → Mecanum tuner
      → Foresight tuner → Tests (`TUNING.md`, the table). Each row says what good looks like.
- [ ] Commit `pedro/Constants.java` with the date.

**Done when:** "DRIVE NOT SET UP" is gone, and Pedro's square test ends close to its start.

## Step 7 – Driving by hand (on the floor)

Run **Test: Drive**.

- [ ] Robot facing away from the drivers, press **Y**: the heading reads the driver heading
      (`DriveConfig.RED_DRIVER_HEADING_DEG`).
- [ ] Turn the robot 90°, push the stick away from you: it still drives away from you
      (field-centric). X switches to robot-centric and back.
- [ ] Strafe and turn are the right way round. If not: the sign flip in
      `drive/Drive.driveWithSticks()` (`DriveTest.java`'s header explains).

**Done when:** a driver can drive it around comfortably.

## Step 8 – Mechanisms, one at a time (on blocks first)

For each one: run its test, change numbers live in the **Dashboard**
(http://192.168.43.1:8080/dash → Configuration), then **type the good values into the config
file and commit them** (`TUNING.md`, "Mechanism tuning").

- [ ] **Test: Intake**: `IntakeConfig.COLLECT_POWER`, `REVERSE_POWER`.
- [ ] **Test: Feeder**: `FeederConfig.HOLD_POSITION`, `FEED_POSITION` (use the safe
      positions from step 4).
- [ ] **Test: Launcher**: `LauncherConfig.SPEED_F`, then `SPEED_P`; check `TICKS_PER_REV`.
- [ ] **Test: Launch** (needs step 6), from each distance in `DISTANCE_IN`: the `RPM` table,
      then `FEED_MS`, `RECOVER_MS`.
- [ ] A new mechanism that is not in the code yet? Follow `HOW_TO_ADD_A_MECHANISM.md`.

**Done when:** 4 balls go into the CELL from every distance in the table.

## Step 9 – Cameras

Follow `CAMERA_GUIDE.md`, then check with **Test: Camera** (it works in INIT).

- [ ] HuskyLens mounted pointing up, trained on the four CELL tags, IDs in `CameraConfig`.
- [ ] Webcam mounted; its position and angle measured into `CameraConfig`.
- [ ] POLLEN and NECTAR colour ranges tuned (and again under the event lights).

**Done when:** Test: Camera shows the tags and the game pieces you hold in front of it.

## Step 10 – The buttons

- [ ] Drive team tries **BioBuzz TeleOp**. Agree on the gamepad 2 map; change it in
      `teleop/BioBuzzTeleOp.java` (`handleOperator()`) and `ControlsConfig` if needed.
- [ ] Update and print the cheat sheets in `CONTROLS.md`.

**Done when:** the drivers know every button, and the printed sheet matches the code.

## Step 11 – Field places (on a real field, or a taped copy)

Follow `AUTO_GUIDE.md`, "Measuring a pose". Measure against **tile seams** (every 24 in).

- [ ] Start positions, our launch spot, LOADING ZONE approach and park, the GARDEN line and
      our reset corner, into `configurations/FieldConfig.java` (RED side only; BLUE is turned
      for you).
- [ ] BioBuzz Auto in INIT shows **no PATH WARNING**, and the Dashboard field draws the paths
      where you expect.

**Done when:** every place in `FieldConfig` is measured, not guessed.

## Step 12 – Autonomous and the drive-to-spot buttons

`AUTO_GUIDE.md`, "Testing safely".

- [ ] **On blocks:** BioBuzz Auto. The Step line goes through every step; the Dashboard
      robot follows the path.
- [ ] **On the floor, slowly:** lower `maxPathSpeed` in `pedro/Constants.java`, run Auto, then
      put it back. The match log has no `TIMEOUT` lines.
- [ ] Right after Auto, BioBuzz TeleOp says **"Pose from auto ... S2 ready"**. Driver A drives
      to the launch spot, B to the LOADING ZONE; moving a stick takes over at once.
- [ ] Without Auto: in the reset corner, hold **D-pad left** 1 s (long buzz), then A and B work.

**Done when:** Auto scores and parks, and A/B drive to their spots.

## Step 13 – The home baseline

`TUNING.md`, "Then record the home baseline", and `MATCH_DAY.md` for what each check does.

- [ ] In `FieldCheckConfig`: `FIELD_NAME` = `home`; the seam crossing and tag for Field Check 12.
- [ ] A check in `FieldCheck11_Mechanisms` for every mechanism the robot has.
- [ ] Run **Field Check 01 … 12**, then **13 Summary**. Copy its numbers into the `BASELINE_`
      values in `FieldCheckConfig`, with `BASELINE_DATE`. Commit.

**Done when:** Field Check 13 shows all PASS against the baseline.

## Step 14 – Ready for competition

- [ ] No `TODO(BioBuzz)` left (see the table below).
- [ ] Tick the **Open items** in `BioBuzz-Requirements.md` that this checklist answered.
- [ ] Practise a full match: Auto, then TeleOp; read the match log afterwards (`MATCH_LOGS.md`).
- [ ] Deploy a **release** build (`README.md`, section 13). Robot Check: READY FOR MATCH.
- [ ] Print `MATCH_DAY.md` and `CONTROLS.md` for the pit box.

**Done when:** you could hand the robot to the drive team at an event.

---

## Which step clears each `TODO(BioBuzz)`

Search the code for `TODO(BioBuzz)` (Android Studio: Edit → Find → Find in Files). When a step
is done, delete the TODO line it answers.

| File | The TODO | Step |
| --- | --- | --- |
| `configurations/HardwareNames.java` | names, ports, directions, the localizer | 1, 4, 6 |
| `configurations/FeederConfig.java`, `mechanisms/Feeder.java` | feeder positions; servo or motor | 4, 8 |
| `configurations/LauncherConfig.java` | the speed table | 8 |
| `configurations/CameraConfig.java`, `mechanisms/FrontCamera.java` | tag IDs, mounting, colours, the BIOBUZZ tag library | 9 |
| `configurations/ControlsConfig.java`, `teleop/BioBuzzTeleOp.java` | the gamepad 2 map | 10 |
| `configurations/FieldConfig.java` | every field place | 11 |
| `configurations/FieldCheckConfig.java`, `fieldchecks/FieldCheck11_Mechanisms.java` | the baseline; the camera check place; a check per mechanism | 13 |

## When a step fails

- An OpMode stops at INIT with a red message: read it, then `DEBUGGING.md`.
- "Unable to find a hardware device": go back to steps 1 and 2.
- Something moves the wrong way: step 4 (motors), step 7 (stick directions).
- Still stuck: `TROUBLESHOOTING.md`, then ask a mentor. Write what you found in the team log.
