# Autonomous guide

## How Auto works

`auto/BioBuzzAuto.java` is a list of steps (`enum Step`) and a `switch` in `runStep()`.
Each step starts something on its first loop, checks if it is done, and checks its own
timeout from `AutoConfig`. A timeout writes `TIMEOUT:<step>` to the log and moves on.
At `AutoConfig.PARK_BY_S` the robot goes to PARK, whatever it is doing.

| Routine (`AutoConfig.ROUTINE`) | Steps |
| --- | --- |
| `LAUNCH_PARK` | WAIT_DELAY → LEAVE → LAUNCH_PRELOADS → PARK → DONE |
| `LAUNCH_COLLECT_PARK` | WAIT_DELAY → LEAVE → LAUNCH_PRELOADS → COLLECT_GARDEN → DRIVE_TO_LAUNCH → LAUNCH_AGAIN → PARK → DONE |

## Field frame and RED poses

```
  y = 144 +--------------------------------------+
          | reset corner                         |
          | LOADING ZONE (x 0-12, y 96-120)      |    origin = bottom-left corner (our GARDEN)
          |              +----------+            |    x to the right, y up, inches
          |  launch      | CELL B   |            |    heading 0 deg = facing +x (the blue wall)
          |  spot        |   HIVE   |            |    90 deg = facing the far wall
          |  (30, 72)    | CELL A   |            |
          |              +----------+            |    RED drivers along the left wall
          | start (9, 36)                        |
          | GARDEN (along the bottom wall)       |
  y = 0   +--------------------------------------+
         x = 0                                 x = 144
```

Every place in `configurations/FieldConfig.java` is written **for RED**. For BLUE,
`FieldPaths.place(...)` turns it half a turn about the field centre (72, 72): x → 144 − x,
y → 144 − y, heading + 180°. You never write BLUE numbers.

## Measuring a pose

1. Put the robot exactly where you want it (its centre, the way it should face).
2. Measure from the **left wall** (x) and the **audience wall** (y) to the robot's **centre**,
   in inches. Tile seams are every 24 in: count tiles, then measure the rest.
3. Heading: 0° = facing the blue wall, 90° = facing the far wall, 180° = facing our wall.
4. Type the numbers into `FieldConfig` (for example `START_NEAR_GARDEN_X_IN`). Or check
   with the Dashboard: drive there in BioBuzz TeleOp after Auto and read the pose line.

TODO(BioBuzz): every place in `FieldConfig` is a first guess from the field picture.

## Before the match

Set in `AutoConfig` (in the pits, file or Dashboard): `ROUTINE`, `START`
(`WALL_NEAR_GARDEN` or `WALL_MIDDLE`), `START_DELAY_S`. In INIT the screen shows them, and a
**PATH WARNING** if a path leaves our side (G402) or hits the HIVE (G409, G417). The paths are
drawn on the Dashboard field.

## Adding a step

1. A value in `enum Step` (with a comment). 2. A `case` in `runStep()`: first loop / done /
timeout. 3. A timeout in `AutoConfig`. 4. The step table in `LLD.md`, section 9.2.
Exercise 5 in `FIRST_CHANGES.md` walks through one.

## Adding a start position

1. Places in `FieldConfig` (`START_..._X_IN`, `_Y_IN`, `_HEADING_DEG`). Rule G304: touching
the wall, our side, not in the LOADING ZONE. 2. A value in `AutoConfig.Start`. 3. An `if` in
`FieldPaths.startPose(...)`.

## Testing safely

1. **On blocks** (wheels off the floor): run BioBuzz Auto. Watch the Step line and the
   Dashboard field: the robot drawing should follow the path.
2. On the floor, at slow speed: lower `maxPathSpeed` in `pedro/Constants.java` while testing
   (Pedro's speed limit for paths), then put it back.
3. Check the log: every step change is a `CHANGE:Auto` event; no `TIMEOUT` lines.
4. At the end of Auto, start BioBuzz TeleOp: it should say "Pose from auto ... S2 ready".
