# Debugging – what is wrong?

## The 6 questions (ask them in this order)

1. **What does the screen say?** The header lines (alliance, time, loop ms, battery, FIELD/ROBOT,
   heading, S1/S2; intake, launcher, up CELL, tips), any **ALERT**, any **WARNING** line.
2. **What state is it in, and why?** Each state machine shows its state, seconds in it, and its
   "last change (reason)". `TelemetryConfig.HISTORY` shows the 4 before it.
3. **Was the command given?** Follow the loop: the button is read once in `handleDriver()` /
   `handleOperator()` → the subsystem method (`robot.shooter.launch(4)`) → the state machine or mechanism.
4. **Was it refused?** A WARNING says why (position unknown, already launching, FAULTED).
5. **Does the hardware do what it is told?** Test the one part alone: `Test: Parts`, then
   `Test: Intake` / `Feeder` / `Launcher` / `Camera` / `Launch` / `Drive`.
6. **Is it a number?** Every tunable number is in `configurations/`. Change it live in the
   Dashboard (http://192.168.43.1:8080/dash); type good values back into the file.

## ALERTs

| ALERT | Means | Fix |
| --- | --- | --- |
| `SLOW LOOP: average .. ms, worst .. ms` | The loop is slow; driving feels laggy | Fewer telemetry lines (`TelemetryConfig`), smaller webcam picture, then bulk reads (`LLD.md` section 14) |
| `LOW BATTERY .. V` | The battery sagged under 9 V | Fresh battery; retire one that sags every time |
| `PINPOINT ...: robot-centric. Y when READY` | Pinpoint fault or restart | Cable and mount; then Y; set pose here for assists |
| `HEADING JUMP .. deg: robot-centric` | The heading changed impossibly fast | Pinpoint cable / power dip; Y facing away |
| `<mechanism> FAULTED: <reason>` | An error inside that mechanism (often a cable) | Check its cable and name; `Test: Parts` |
| `LAUNCHER TIMEOUT: .. of .. RPM` | The flywheel never reached its speed | Battery; `LauncherConfig` PIDF and `TICKS_PER_REV` (`Test: Launcher`) |
| `LOG OFF: ...` | The match log could not be written | Free storage; it never stops the robot |

## Symptoms

| Symptom | Likely causes | In the log | Fix |
| --- | --- | --- | --- |
| Field-centric suddenly rotated | Pinpoint reset, an accidental Y, heading jump | `PINPOINT:` event, `ALERT:HEADING JUMP`, a `CHANGE:Drive` heading event | Secure the Pinpoint cable; check for a voltage dip then; Y facing away |
| Field-centric slowly rotated | Gyro calibration at INIT, mounting | Heading creeps while still | Hands off in INIT; Y; remount; Field Check 03 |
| "Unable to find a hardware device with name ..." at INIT | A name in `HardwareNames` doesn't match the robot configuration | – | Fix the name or the configuration; check the hub and port in the comment |
| "DRIVE NOT SET UP" at INIT | `pedro/Constants.java` is still the Quickstart stub | – | Drive tuning (`TUNING.md`) |
| A or B (assists) only buzz | Position unknown (S1), or the path would hit the HIVE | `WARN:Drive: ... refused` | Set pose here in our reset corner |
| Auto fine at home, off at the event | Different tile grip | Field Check 07–09 vs baseline | Use the measured deceleration; adjust end poses |
| Auto ends early or skips a step | A step timed out | `TIMEOUT:<step>` events | Longer timeout in `AutoConfig`, or fix what was slow |
| Launches short or long | Not on our launch spot, wrong upward CELL (aim), low battery | `up_cell_a`, pose, `launcher_rpm` vs target, `battery_v` at each `LAUNCH` | Launch from the launch spot (driver's A); press TIP at every tip; retune `LAUNCH_RPM`; fresh battery |
| Laggy, jerky driving | Slow loop | `worst_loop_ms` spikes | See SLOW LOOP above |
| Log ends early (no `# END`) | Crash, power loss | The last voltage reading | Robot Controller log for the crash; power connectors |
| A button does nothing | Read twice in one loop | – | Read each button once into a `boolean` |

When you find a new symptom, add a row here.
