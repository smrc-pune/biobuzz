# BioBuzz – our robot code for FTC BIOBUZZ (2026–27)

The code for our *FIRST* Tech Challenge robot in the BIOBUZZ season. It is built on the
[FTC SDK 12.0](https://github.com/FIRST-Tech-Challenge/FtcRobotController) and the
[Pedro Pathing 3.0.2 Quickstart](https://github.com/Pedro-Pathing/Quickstart), and written so
that students who are new to Java can read it, change it and own it.

> **Status: `v0.1.4`, not run on the robot yet.** Every part is written, but the drive is not
> tuned yet: until then, every OpMode that drives stops at INIT with "DRIVE NOT SET UP".
> Device names, field places and speeds are first guesses, marked `TODO(BioBuzz)` in the code.
> **Test: Parts** and **Robot Check** work now.

## Start here

| You are… | Open |
| --- | --- |
| New to the code | [The student guide](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/README.md), then [Getting started](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/GETTING_STARTED.md) (install, build, deploy) |
| Setting up a newly built robot | [New robot checklist](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/NEW_ROBOT_CHECKLIST.md): 14 steps from the wiring list to match-ready |
| Tuning the drive | [Tuning](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/TUNING.md) and our [Pedro tuning kit](https://github.com/smrc-pune/pedropathing/tree/main/pedro-tuning-kit) |
| Learning to change code | [First changes](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/FIRST_CHANGES.md) (five small exercises), [How to add a mechanism](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/HOW_TO_ADD_A_MECHANISM.md) |
| On the drive team | [Controls](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/CONTROLS.md) (print it), [Match day](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/MATCH_DAY.md) |
| Chasing a problem | [Debugging](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/DEBUGGING.md), [Match logs](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/MATCH_LOGS.md) |
| A mentor, or curious about the design | [HLD](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/HLD.md) (big picture), [LLD](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/LLD.md) (every class), [Requirements](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/BioBuzz-Requirements.md) |

Every guide is in [`biobuzz/docs/`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs);
the student guide's section 12 lists them all.

## Where things are

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
  biobuzz/          OUR CODE: everything we write is in here
    README.md         the student guide: start here
    docs/             the guides
    teleop/  auto/    the match OpModes: BioBuzz TeleOp, BioBuzz Auto
    subsystems/       intake, shooter, vision: what OpModes ask the robot to do
    mechanisms/       one class per group of motors and servos
    configurations/   every number and device name (tuned live in FTC Dashboard)
    testing/  fieldchecks/   test OpModes and the Field Checks
  pedro/            Pedro Pathing's Quickstart files: drive constants and tuners
tools/plot_match.py graphs a match log on a laptop
UPSTREAM.md         every change we made to a file we did not write, and the merge log
```

Everything outside `biobuzz/` comes from the FTC SDK or the Pedro Quickstart. We keep those
files as they come, so updates merge cleanly. The SDK's own README is
[`README.md`](../README.md) at the top of the repository.

## How we work

- **`master` is the students' version.** It always builds, and it is what runs on the robot
  at practice.
- **One branch per task** (`maya-intake-power`), then a **pull request** to `master`. A mentor
  reviews it. Nobody commits to `master` directly.
- **`biobuzz-dev`** is where mentors build bigger changes. They reach `master` in tested
  batches, tagged as releases (`v0.1`, `v0.2`, ...).
- **Every code change updates the guides in the same commit**, so the docs never fall behind.
- Practice uses `master`. If you deploy anything else to try it, deploy `master` again before
  you hand the robot back.

## Build and deploy

```
git clone https://github.com/smrc-pune/biobuzz
```

Open the folder in Android Studio, wait for Gradle sync, then **Build → Make Project**.
Connect to the Control Hub and press **Run**. The full steps are
in [Getting started](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/docs/GETTING_STARTED.md).
For a competition, use a release build: the student guide, section 13.

## Built on

- [FTC SDK](https://github.com/FIRST-Tech-Challenge/FtcRobotController) by *FIRST*
- [Pedro Pathing Quickstart](https://github.com/Pedro-Pathing/Quickstart) (path following)
- [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard) (live telemetry, tuning, field view)
- Our [Pedro tuning kit](https://github.com/smrc-pune/pedropathing/tree/main/pedro-tuning-kit)

The SDK's license is in [`LICENSE`](../LICENSE).
