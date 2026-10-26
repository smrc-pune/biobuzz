# Camera guide – the HIVE camera

BIOBUZZ has AprilTags **only under the CELLS**, facing down; there are none on the walls. So
the robot has **one camera**, a HuskyLens looking up at those tags. It only answers questions
(what do I see?); it does not change the robot's position. Check it with **Test: Camera**
(it works in INIT) and, at each field, **Field Check 12**.

| Camera | File | Device | Sees |
| --- | --- | --- | --- |
| HIVE camera | `mechanisms/HiveCamera.java` | DFRobot HuskyLens, I2C | The AprilTags under the CELLS (looking up) |

**What it can and cannot tell us.** The HuskyLens reports each tag it learned as a box in its
picture: an id, where the box is (left or right of the middle) and how big it is. So it can say
**which CELL is up** and **how far left or right it is**. It does not measure distance, so it
cannot fix the robot's x/y; the box size gives only a rough "closer / farther".

## HIVE camera (HuskyLens)

**Mounting:** on the robot, pointing **up** at the underside of the HIVE (the CELL tags face
down). Plug it into a Control Hub I2C port and add it to the robot configuration as
"HuskyLens" with the name in `HardwareNames.HUSKYLENS_HIVE` (`hiveCamera`), on the bus in the
comment.

**The CELL tags** (Competition Manual TU06, section 9.9). Each CELL has one sticker with four
AprilTags (36h11 family, 3.25 in), facing down at the tiles:

| Our name | Where on the field | AprilTags | Train this one |
| --- | --- | --- | --- |
| RED CELL A | red CELL, audience side | 34, 35, 36, 37 | 34 → learned ID 1 |
| RED CELL B | red CELL, opposite the audience | 30, 31, 32, 33 | 30 → learned ID 2 |
| BLUE CELL A | blue CELL, opposite the audience | 42, 43, 44, 45 | 42 → learned ID 3 |
| BLUE CELL B | blue CELL, audience side | 38, 39, 40, 41 | 38 → learned ID 4 |

BLUE CELL A is the one **opposite** the audience: every BLUE place is the RED place turned half
a turn around the field centre (`FieldConfig`), and that turns the red audience-side CELL into
the blue far-side CELL. The same numbers are in `CameraConfig`.

**Training** (on the HuskyLens itself, once per HuskyLens). The HuskyLens does not read the
AprilTag number; it gives each tag it learns its own ID, 1, 2, 3... in the order you teach them.
Print the four tags from FIRST's AprilTag images (the Playing Field Resources page), or use a field.
1. Turn the function dial to **Tag Recognition**. (The code also selects it at INIT.)
2. Long-press the function button → enable "Learn Multiple".
3. Point it at AprilTag **34** and press the learn button: it becomes ID 1. Then **30** → 2,
   **42** → 3, **38** → 4 (the "Train this one" column).
4. Learned them in another order? Change `RED_CELL_A_TAG_ID` ... in `CameraConfig` to match.

**Check:** Test: Camera, hold a tag above the camera. "First tag" shows its learned id and
"Which CELL" names its CELL (for example "RED CELL A (AprilTags 34-37, audience side)"); the
offset is near 0 when the tag is in the middle of the picture (+ = right). Values are read
every `CameraConfig.HIVE_SAMPLE_MS` (100 ms).

**Field Check 12** (at each field, under its lights): robot on our launch spot facing the
upward CELL. PASS when the HuskyLens sees that CELL's tag on at least 80% of its reads
(`FieldCheckConfig.CAMERA_SEEN_FRACTION`). If not: the HuskyLens screen, its training, the
learned ids in `CameraConfig`, and the lighting.
