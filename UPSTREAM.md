# UPSTREAM – our changes to Quickstart files, and the merge log

This repository is the [Pedro Pathing Quickstart](https://github.com/Pedro-Pathing/Quickstart)
plus our team code in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/biobuzz/`.
Quickstart files are never moved, renamed or deleted. Every change we make to a
Quickstart-owned file is listed here, so a Quickstart merge is easy to check.
How to merge: `TeamCode/.../biobuzz/docs/UPDATING_PEDRO.md`.

Git remote for the Quickstart: **`pedro`** → `https://github.com/Pedro-Pathing/Quickstart.git`.

## Changes to Quickstart-owned files

| File | What changed | Why |
| --- | --- | --- |
| `build.dependencies.gradle` | One block at the end, `// ---- BioBuzz additions ----`: the `https://maven.brott.dev/` repository and `com.acmerobotics.dashboard:dashboard:0.6.0` | FTC Dashboard (telemetry, live config, field view) |
| `.gitignore` | One block at the end, `# ---- BioBuzz additions ----`: `BIOBUZZ_Competition_Manual_*.pdf` | The game manual is downloaded from FIRST, not committed |
| `.github/README.md` | New file (the Quickstart has none). GitHub shows it at the top of the repository page in place of the SDK's `README.md` | Our own front page, without editing the SDK's `README.md` (FIRST rewrites it every SDK release, so edits would conflict). Update its **Status** line at each release |
| `TeamCode/src/main/assets/images/field-biobuzz.png` | New file (the Quickstart has no `assets/images/`) | BioBuzz field picture for the Dashboard field view |
| `TeamCode/.../pedro/Constants.java`, `Tuning.java` | **Not yet changed**: still the Quickstart stubs | At drive tuning they get a ONE-TIME fill-in: `Constants.java` gets `drivetrainConfig`, `localizerConfig`, `foresightConfig` and the follower factory; `Tuning.java` is replaced by the staged `Tuning.java` from our [Pedro tuning kit](https://github.com/smrc-pune/pedropathing/tree/main/pedro-tuning-kit). Record it here when it happens. `Constants.java` will import `biobuzz/configurations/HardwareNames` for device names: the only place a Quickstart file depends on team code |

No toolchain workaround (for example an Android Gradle Plugin downgrade in `build.gradle`) is committed.

## Merge log

| Date | Quickstart commit | Result | Tests |
| --- | --- | --- | --- |
| (start) | `b75637b` (Merge pull request #120, fix-tests-ignore-distance) | Base of this repository | – |
| 2026-10-24 | `9dc8978` (Merge pull request #112, foresight-tuner-safety) | Pedro `revhub` and `core` 3.0.1 → 3.0.2; `ForesightTuner` stops on NaN results and runaways, and its braking steps no longer push back after stopping. One conflict, in `build.dependencies.gradle` (end of file): kept our BioBuzz block. No team code changed: the 3.0.2 API only adds methods (checked with `javap`), and `PinpointLocalizer` still keeps its driver private | Debug build passes. Robot tests NOT run yet: the drive is not tuned and there is no Field Check baseline. Run the kit's Stage 5 tests and Field Checks 01–09 after tuning |
