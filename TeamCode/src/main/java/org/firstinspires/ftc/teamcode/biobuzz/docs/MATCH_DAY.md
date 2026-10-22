# Match day

## At a new field (first practice slot, and after a big repair)

Run the Field Checks in order (Driver Station group **"Field Checks"**), then the Summary.
Set `FieldCheckConfig.FIELD_NAME` first (for example `event-practice-1`). Two short slots of
about 5 minutes are enough. Check positions against **tile seams** (every 24 in), never tape.

| # | The crew does | If it fails |
| --- | --- | --- |
| 01 Hardware | Nothing | Fix the missing device or cable it names |
| 02 Battery | Nothing (2 s) | Charge or swap the battery, start again |
| 03 Still | Hands off (10 s) | Pinpoint or its mount damaged: fix first |
| 04 Push | Push 2 tiles forward, then 2 sideways (A to start and stop each) | Pod, mount or cable damaged: fix first |
| 05 Spin | Turn the robot 4 full turns by hand, back to the seam | Pinpoint loose: fix the mount |
| 06 FieldCentric | Turn 90°, push the stick away: A = moved away, B = not | Check `DriveConfig` driver headings with Test: Drive |
| 07 Brake | Nothing: the robot drives itself fast (4 clear tiles ahead and to the left) | Use the printed deceleration in `pedro/Constants.java` for this event, then re-run 08 |
| 08 Line | Place on a seam; the robot drives 48 in | Adjust Auto end poses, or re-run Pedro's tuners |
| 09 Square | Place on a seam crossing; the robot drives a square | Re-run Pedro's tuners (`TUNING.md`) |
| 10 Wall | Flat on one wall (A), then flat on another (A) | The walls may not be square: don't trust wall resets here |
| 11 Mechanisms | No balls, hands clear | Fix the FAULTED mechanism it names |
| 12 Camera | Robot on the seam crossing in `FieldCheckConfig`, tag in view | Re-check the camera mount numbers and exposure (`CAMERA_GUIDE.md`) |
| 13 Summary | Read it | Every FAIL fixed before qualification matches |

"Battery differs" on 07–09: re-run with a battery closer to the baseline voltage before
changing any tuning.

## Before every match (in the pits, before queuing)

1. Run **Robot Check** (group "Tests"). It must end with **READY FOR MATCH**.
2. Paper checklist (things code cannot see):
   - [ ] Charged battery in, strapped down; spare charged
   - [ ] Pinpoint light on; Pinpoint mounted firmly
   - [ ] Every connector seated (drive motors, Expansion Hub RS-485 cable, cameras, servo)
   - [ ] `AutoConfig.ROUTINE`, `START`, `START_DELAY_S` agreed with our partner and set
   - [ ] Nothing loose; bumpers on with the right alliance colour
3. At the field, in **INIT** of BioBuzz Auto: pick the alliance (D-pad left/right), read the
   plan on screen, no PATH WARNING. **Place the robot exactly on its start position.**
   Hands off during INIT.
4. After Auto, in TeleOp INIT: check the alliance, "Pose from auto: ... S2 ready".

## Between matches

- [ ] Swap the battery; note anything odd on the match sheet
- [ ] Log Viewer: did the last run end cleanly? Any ALERTs?
- [ ] Copy the logs to the laptop when there is time (`MATCH_LOGS.md`)

## Recovery buttons (driver, gamepad 1)

| Problem on screen | Do this |
| --- | --- |
| "ROBOT" in the header, or field-centric feels rotated | Face the robot away from the drivers, press **Y** |
| Field-centric still wrong | **X**: robot-centric, and drive that way |
| "S1 only" / assists refused | Back into our reset corner (back on our wall, side on the far wall), **hold D-pad left 1 s** |
| An ALERT names a FAULTED mechanism | Keep driving; the operator stops using it |
| Something odd happened | **Back**: MARK it for the debrief |
