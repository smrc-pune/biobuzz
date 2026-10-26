package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * CameraConfig.java  -  numbers for the HIVE camera (HuskyLens, our only camera).   ROLE: config
 * Used by mechanisms/HiveCamera.java.
 * How to set them: docs/CAMERA_GUIDE.md. Check them with "Test: Camera".
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class CameraConfig {
    // ==================== HIVE camera (HuskyLens, looks UP at the CELL tags) ====================
    public static double HIVE_SAMPLE_MS = 100;    // read the HuskyLens at most this often (each read takes a few ms)
    public static int HIVE_IMAGE_WIDTH_PX = 320;  // HuskyLens picture width; the middle is at half of it

    // ---- The AprilTags under the CELLS (Competition Manual TU06, section 9.9) ----
    // Each CELL has one sticker with FOUR AprilTags (36h11 family, 3.25 in), facing down:
    //   RED  CELL on the audience side:        34, 35, 36, 37   = our RED CELL A
    //   RED  CELL opposite the audience:       30, 31, 32, 33   = our RED CELL B
    //   BLUE CELL opposite the audience:       42, 43, 44, 45   = our BLUE CELL A
    //   BLUE CELL on the audience side:        38, 39, 40, 41   = our BLUE CELL B
    // BLUE CELL A is the one OPPOSITE the audience because BLUE places are RED places turned
    // half a turn around the field centre (FieldConfig). These numbers come from the manual:
    // do not change them, unless a Team Update changes the manual.
    public static final int RED_CELL_A_FIRST_APRILTAG = 34;
    public static final int RED_CELL_B_FIRST_APRILTAG = 30;
    public static final int BLUE_CELL_A_FIRST_APRILTAG = 42;
    public static final int BLUE_CELL_B_FIRST_APRILTAG = 38;

    // ---- What the HuskyLens calls them ----
    // The HuskyLens does NOT report the AprilTag number: it gives each tag it LEARNED its own ID
    // (1, 2, 3...). Teach it the FIRST AprilTag of each CELL, in this order (docs/CAMERA_GUIDE.md):
    //   learned 1 = AprilTag 34,  2 = AprilTag 30,  3 = AprilTag 42,  4 = AprilTag 38.
    // Learned them in another order? Change these four numbers to match.
    // TODO(BioBuzz): train the HuskyLens, then check each CELL with "Test: Camera".
    public static int RED_CELL_A_TAG_ID = 1;    // AprilTag 34 (RED, audience side)
    public static int RED_CELL_B_TAG_ID = 2;    // AprilTag 30 (RED, opposite the audience)
    public static int BLUE_CELL_A_TAG_ID = 3;   // AprilTag 42 (BLUE, opposite the audience)
    public static int BLUE_CELL_B_TAG_ID = 4;   // AprilTag 38 (BLUE, audience side)
}
