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

**Training on the CELL tags** (on the HuskyLens itself, once per HuskyLens):
1. Turn the function dial to **Tag Recognition**. (The code also selects it at INIT.)
2. Long-press the function button → enable "Learn Multiple".
3. Point it at our RED CELL A tag, press the learn button: it becomes ID 1. Then RED CELL B → 2,
   BLUE CELL A → 3, BLUE CELL B → 4.
4. Write the IDs into `CameraConfig` (`RED_CELL_A_TAG_ID` ...) if you learned them in another order.

**Check:** Test: Camera, hold a tag above the camera. "First tag" shows its learned id; the
offset is near 0 when the tag is in the middle of the picture (+ = right). Values are read
every `CameraConfig.HIVE_SAMPLE_MS` (100 ms).

**Field Check 12** (at each field, under its lights): robot on our launch spot facing the
upward CELL. PASS when the HuskyLens sees that CELL's tag on at least 80% of its reads
(`FieldCheckConfig.CAMERA_SEEN_FRACTION`). If not: the HuskyLens screen, its training, the
learned ids in `CameraConfig`, and the lighting.
