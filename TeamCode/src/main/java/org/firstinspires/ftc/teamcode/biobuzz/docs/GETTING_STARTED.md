# Getting started

## 1. Install

1. Install **Android Studio** (the newest version) from developer.android.com/studio.
2. Install **git** and get access to the team repository `https://github.com/smrc-pune/biobuzz`.
3. Clone it: `git clone https://github.com/smrc-pune/biobuzz` (or in Android Studio:
   File → New → Project from Version Control).

## 2. Open and build

1. Android Studio → **Open** → the `biobuzz` folder (the one with `build.gradle` in it).
2. Wait for "Gradle sync" to finish (the first time takes several minutes: it downloads the
   FTC SDK, Pedro Pathing and FTC Dashboard).
3. **Build → Make Project**. It must end with "BUILD SUCCESSFUL".

Our code is in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/`. Read its
`README.md` first.

## 3. Deploy to the robot

1. Turn the robot on. Connect the laptop to the Control Hub's Wi-Fi (or a USB cable).
2. If over Wi-Fi the first time: in a terminal, `adb connect 192.168.43.1:5555`.
3. At the top of Android Studio pick the device "REV Control Hub" and the **TeamCode** run
   configuration, then press **Run** (the green triangle).
4. The Robot Controller app restarts with the new code.

For a competition, deploy a **release** build instead: see `README.md`, section 13,
"Competition build".

## 4. Run an OpMode

On the Driver Station: the **TeleOp** list (or **Autonomous** list) → pick an OpMode → **INIT**
→ (check the screen) → **PLAY** → **STOP** when done.

| Group | OpModes |
| --- | --- |
| BioBuzz | BioBuzz TeleOp, BioBuzz Auto (the match OpModes) |
| Tests | Test: Drive, Test: Parts, Test: Intake, Feeder, Launcher, Camera, Launch, Robot Check, Log Viewer |
| Field Checks | Field Check 01 ... 13 |

First time on a new robot: **Test: Parts** (each motor and servo), then **Test: Drive**.

## 5. FTC Dashboard

Join the robot's Wi-Fi and open **http://192.168.43.1:8080/dash** in a browser:
telemetry, the field view (robot, path, upward CELL on the BioBuzz field), graphs, and the
**Configuration** tab where every `...Config` number can be changed live.
**Live changes are lost when the app restarts**: type good values back into the file.

## 6. Robot configuration

On the Driver Station: ⋮ → Configure Robot. Every device name must match
`configurations/HardwareNames.java` exactly (the hub and port are in the comment next to each
name). BioBuzz TeleOp and Auto need every device in that file.
