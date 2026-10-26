# Controls – both gamepads

Print this page for the drive team. **Gamepad 1 only drives. Gamepad 2 only runs mechanisms.**
The buttons are wired in `teleop/BioBuzzTeleOp.java` (`handleDriver()`, `handleOperator()`).
Keep this page, the header of that file and `TelemetryConfig.GAMEPAD1_HELP` / `GAMEPAD2_HELP`
the same by hand.

```
 GAMEPAD 1 - DRIVER                             GAMEPAD 2 - OPERATOR
 +------------------------------------------+   +------------------------------------------+
 | LB  -                RB  AIM (hold)      |   | LB  HIVE TIPPED      RB  -               |
 | LT  -                RT  -               |   | LT  -                RT  LAUNCH 4        |
 |                                          |   |                                          |
 | D-pad <  SET POSE (hold 1 s)             |   | D-pad v  STOP ALL                        |
 |   (INIT: < > = alliance)                 |   |                                          |
 |                                          |   |                                          |
 | Y  RESET HEADING                         |   | Y  SPIN UP                               |
 | X  FIELD / ROBOT-CENTRIC                 |   | X  -                                     |
 | B  LOADING ZONE (S2)                     |   | B  SPIT OUT (hold)                       |
 | A  LAUNCH SPOT (S2)                      |   | A  INTAKE ON / OFF                       |
 |                                          |   |                                          |
 | Left stick MOVE  Right stick X TURN      |   |                                          |
 | Back  MARK                               |   | Back  MARK                               |
 +------------------------------------------+   +------------------------------------------+
```

## Gamepad 1 – driver

| Control | Action |
| --- | --- |
| Left stick | Move, field-centric: push away from you = the robot moves away from you |
| Right stick X | Turn |
| Y | "The robot faces away from us now": heading reset, field-centric back on |
| X | Switch field-centric / robot-centric (a buzz says it switched) |
| A | S2: drive to our launch spot (refused with a short buzz if the position is unknown) |
| B | S2: drive into our LOADING ZONE |
| Right bumper (hold) | Aim: keep facing the upward CELL while you move (needs S2) |
| D-pad left (hold 1 s) | Set pose here: robot backed into our reset corner (long buzz = S2 ready) |
| Back / Share | MARK: "something odd just happened" (find it in the match log) |
| D-pad left / right (INIT only) | Switch alliance |

Moving **any** stick while the robot drives itself takes it back at once.

## Gamepad 2 – operator

| Control | Action |
| --- | --- |
| A | Intake on / off |
| B (hold) | Spit out (reverse the intake); also how opponent NECTAR goes back out |
| Right trigger | Launch 4 balls (they land best from our launch spot: driver's A) |
| Y | Spin the launcher up early |
| Left bumper | "Our HIVE just TIPPED": press it EVERY time, so the robot aims at the right CELL |
| D-pad down | **STOP ALL mechanisms** |
| Back / Share | MARK in the match log |

TODO(BioBuzz): confirm the gamepad 2 map once the robot is built.

## Rumbles

| Buzz | Meaning |
| --- | --- |
| Long, both gamepads | Urgent ALERT: read the screen (low battery, Pinpoint, heading jump, FAULTED, launcher timeout) |
| Both gamepads at 1:00 left | NECTAR may be entered, FLOWERS open |
| Gamepad 1 at 0:20 left | Time to PARK (B drives to the LOADING ZONE) |
| Short, gamepad 1 | An assist was refused (screen says why) |
| Long, gamepad 1 after D-pad left | Pose set, assists ready |
