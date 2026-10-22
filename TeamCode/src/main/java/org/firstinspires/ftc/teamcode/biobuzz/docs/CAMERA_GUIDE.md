# Camera guide – the HIVE camera and the front camera

The robot has two cameras. Both only answer questions (what do I see?); neither changes the
robot's position in this version. Check both with **Test: Camera** (it works in INIT).

| Camera | File | Device | Sees |
| --- | --- | --- | --- |
| HIVE camera | `mechanisms/HiveCamera.java` | DFRobot HuskyLens, I2C | The AprilTags under the CELLS (looking up) |
| Front camera | `mechanisms/FrontCamera.java` | Logitech USB webcam, the SDK's `VisionPortal` | Field AprilTags, POLLEN, red and blue NECTAR |

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

## Front camera (Logitech webcam)

**Mounting:** facing forward over the intake. Plug into a Control Hub USB port; add a
"Webcam" in the robot configuration named as `HardwareNames.WEBCAM_FRONT` ("Webcam 1").
Measure where it sits and type it into `CameraConfig`: `CAMERA_FORWARD_IN`, `CAMERA_LEFT_IN`,
`CAMERA_UP_IN` (from the robot's centre and the floor), `CAMERA_YAW_DEG`, `CAMERA_PITCH_DEG`
(the SDK's "level" is −90), `CAMERA_ROLL_DEG`.

**Picture size and speed:** `WIDTH_PX` × `HEIGHT_PX` (640 × 480). Smaller is faster. Vision runs
on its own thread, but it still loads the Control Hub: watch the loop time in the header with
the camera on. `APRILTAG_DECIMATION` 2 (higher = faster, sees tags only closer). `PREVIEW`
stays **false** in matches (X in Test: Camera turns it on for setup).

**Exposure:** `EXPOSURE_MS = 0` is automatic. Under bright event lights, set 5–10 ms and a
`GAIN` to stop motion blur and colour washing out. It is applied once the camera is streaming.

**Colours (POLLEN yellow, red NECTAR, blue NECTAR):** each has a minimum and maximum
`{ Y, Cr, Cb }` (YCrCb colour space): `POLLEN_MIN` / `POLLEN_MAX`, ... They start as the SDK's
YELLOW, RED and BLUE ranges. To tune:
1. Test: Camera with PREVIEW on; put one ball in front.
2. If it is not seen, widen the range (lower MIN, raise MAX) one number at a time; if other
   things are seen, narrow it. Cr is "how red", Cb is "how blue", Y is brightness.
3. Values marked "(restart)" in `CameraConfig` count only after the OpMode starts again.
4. Re-check at every event: lighting is the most common cause of failure.

**AprilTags:** the SDK knows the tags in its own library. If the BIOBUZZ field tags are not in
it, the camera still reads their ids, but not their range (TODO(BioBuzz) in `FrontCamera`:
add them with `setTagLibrary`). **Field Check 12** checks the camera's range against a tag at a
known place.
