# Match logs

Every OpMode run (matches, tests, Field Checks) writes one CSV file on the Control Hub, when
`LogConfig.ENABLED` is on (it is, by default). `helpers/MatchLogger.java` writes it.

## Where the files are

- Folder on the Control Hub: `/sdcard/FIRST/data/biobuzz-logs/` (not the SDK's own `matchlogs/`).
- Name: `<yyyy-MM-dd_HH-mm-ss>_<OpMode>_<ALLIANCE>.csv`, for example
  `2026-10-18_14-32-05_BioBuzzAuto_RED.csv`. Time first, so they sort in the order they ran.
- The newest `LogConfig.KEEP_FILES` (100) files are kept.
- **One match = an Auto file and the TeleOp file that started within 180 s after it, same alliance.**

## Copying them to a laptop

1. Connect the laptop to the robot's Wi-Fi (or USB).
2. Either: `adb pull /sdcard/FIRST/data/biobuzz-logs/ logs/`
   Or: Android Studio → View → Tool Windows → **Device Explorer** → `sdcard/FIRST/data/biobuzz-logs` → right click → Save As.

## What is inside

```
# opmode=BioBuzzTeleOp
# alliance=RED
# field=home
# battery_at_play_v=12.84
# started_epoch_ms=1760798525123
type,t_s,event,match_s_left,loop_ms,worst_loop_ms,battery_v,x_in,y_in,heading_deg,...
EVENT,0.0,PLAY:BioBuzzTeleOp
DATA,0.05,,119.95,17.2,21.0,12.80,9.0,36.0,0.0,...
EVENT,3.41,ASSIST:launch spot
EVENT,11.9,ALERT:LOW BATTERY 8.9 V
# END rows=2398 min_battery_v=8.90 worst_loop_ms=45
```

- `t_s` = seconds since PLAY of this OpMode.
- `DATA` rows, 20 a second: time left, loop time, battery, pose, Pinpoint status, drive mode,
  sticks, every mechanism's values and `_faulted` flag, `launch_state`, `up_cell_a`, `tip_count`.
  The full column list is in `LLD.md`, section 12.
- `EVENT` rows: `CHANGE` (state and mode changes, with the reason), `WARN`, `ALERT`, `PINPOINT`,
  `ASSIST`, `MARK`, `HIVE_TIP`, `INTAKE`, `LAUNCH`, `TIMEOUT`, `PARK_BY`, `FIELDCHECK_<nn>`.
- **No `# END` line = the OpMode crashed or the robot lost power.** Look at the last rows.

Open a file in a spreadsheet (Excel, Google Sheets): filter the `type` column to `EVENT` to
read the story of the run.

## In the pits: Log Viewer

`Log Viewer` (group "Tests") shows the 10 newest runs on the Driver Station: start time,
OpMode, alliance, duration, ALERTs, worst loop time, lowest battery, TIMEOUTs, clean end.
No laptop needed.

## On a laptop: plot_match.py

`tools/plot_match.py` (repository root; needs Python 3 with pandas and matplotlib:
`pip install pandas matplotlib`).

| Command | Shows |
| --- | --- |
| `python tools/plot_match.py logs/<file>.csv` | Stacked graphs on one time axis (heading, battery, drive effort, loop time, Pinpoint, launch state, launcher speed), EVENTs as vertical lines, the path coloured by battery, and a printed summary |
| `python tools/plot_match.py --match logs/<Auto file>.csv` | That Auto joined with its TeleOp on match time; Auto, transition and TeleOp shaded |
| `... --match <Auto file> --at 2.0` (or `--at 1:45`) | The rows and events from 1 s before to 1 s after that match time |
| `python tools/plot_match.py --compare a.csv b.csv ...` | Battery voltage of several runs on one graph |
| `python tools/plot_match.py --fieldcheck logs/*.csv` | The Field Check table: one row per field, one column per check |

The summary prints, for each ALERT, the battery voltage at that moment and the lowest in the
2 s before it: the quickest answer to "was it the battery?".

## The 5-minute debrief

1. Copy the files.
2. `plot_match.py --match <Auto file>`.
3. Ask the drive team what felt wrong, and when; find it on the plot.
4. Fill one row of the match sheet: match number (from the event schedule), log files, field, resting and lowest voltage, clean end, symptom (in the drive team's words), evidence (time, plot), cause, fix.
5. Anything new goes into `DEBUGGING.md`.

Phone or stream video lines up with the log at the `PLAY` event.
