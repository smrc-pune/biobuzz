# Troubleshooting – the four stages

Troubleshooting is one loop: **check** the robot, **detect and recover** during the match,
**record** every run, and **analyse** it afterwards so the next check is better.
Each tool reports in plain words on the Driver Station and writes to the match log.

```
  1a. New field          1b. Before every match     2. During the match         3. Always          4. After the match
  Field Check 01-13  ->  Robot Check            ->  HealthMonitor + ALERTs  ->  MatchLogger    ->  Log Viewer, plot_match.py
  (pit crew + driver)    (pit crew)                 (driver + operator)         (automatic)        (data lead + drive team)
        ^                                                                                                |
        +-------------------- a new check, alert or fix ---------------------------------------------------+
```

| Stage | Tool (file) | Guide |
| --- | --- | --- |
| 1a. New field | `fieldchecks/FieldCheck01_Hardware` ... `FieldCheck13_Summary` | `MATCH_DAY.md` (field check table) |
| 1b. Before every match | `testing/RobotCheck.java` + the paper checklist | `MATCH_DAY.md` |
| 2. Detect and recover | `helpers/HealthMonitor.java`, `helpers/Alerts.java`, gamepad 1 Y / X / D-pad left | `MATCH_DAY.md` (recovery buttons), `DEBUGGING.md` |
| 3. Record | `helpers/MatchLogger.java`, `helpers/BatteryMonitor.java` | `MATCH_LOGS.md` |
| 4. Analyse | `testing/LogViewer.java`, `tools/plot_match.py` | `MATCH_LOGS.md` |

## What the robot watches during a match

| Problem | How it is noticed | What happens | What the drive team does |
| --- | --- | --- | --- |
| Slow loop | average over 1 s > `HealthConfig.SLOW_LOOP_AVERAGE_MS`, or one loop > `SLOW_LOOP_WORST_MS` | ALERT `SLOW LOOP` (no rumble) | Nothing in the match; after it, see `DEBUGGING.md` |
| Low battery | below `PLAY_LOW_BATTERY_V` for `PLAY_LOW_BATTERY_MS` | urgent ALERT, rumble | Drive gently; fresh battery next match |
| Pinpoint not READY | status changes | robot-centric, position unknown, urgent ALERT | When the Drive line says READY: face away, **Y**; for assists, **set pose here** |
| Heading jump | turned faster than `MAX_TURN_DEG_PER_S` | robot-centric, urgent ALERT | Face away, **Y** |
| Mechanism error (cable out) | an error inside the mechanism | that mechanism FAULTED and stops, urgent ALERT | Keep playing without it; fix the cable after |
| Launcher can't reach speed | `LaunchStateMachine` spin-up timeout | launching stops, urgent ALERT | Try again; check the battery |
| Robot restarted | no pose from Auto | TeleOp is S1 only | **Y** facing away; **set pose here** in our reset corner |

All thresholds are starting values in `configurations/HealthConfig.java`. Set them from our own
logs after about 10 runs.
