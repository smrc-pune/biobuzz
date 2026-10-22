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
| `TeamCode/src/main/assets/images/field-biobuzz.png` | New file (the Quickstart has no `assets/images/`) | BioBuzz field picture for the Dashboard field view |
| `TeamCode/.../pedro/Constants.java`, `Tuning.java` | **Not yet changed**: still the Quickstart stubs | At drive tuning they get a ONE-TIME fill-in (follower factory, drivetrain, Pinpoint localizer, Foresight config; our tuners). Record it here when it happens. `Constants.java` will import `biobuzz/configurations/HardwareNames` for device names: the only place a Quickstart file depends on team code |

No toolchain workaround (for example an Android Gradle Plugin downgrade in `build.gradle`) is committed.

## Merge log

| Date | Quickstart commit | Result | Tests |
| --- | --- | --- | --- |
| (start) | `b75637b` (Merge pull request #120, fix-tests-ignore-distance) | Base of this repository | – |
