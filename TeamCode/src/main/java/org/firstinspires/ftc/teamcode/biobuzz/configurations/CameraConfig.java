package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * CameraConfig.java  -  numbers for both cameras.                              ROLE: config
 * Used by mechanisms/HiveCamera.java (HuskyLens) and mechanisms/FrontCamera.java (webcam).
 * How to set them: docs/CAMERA_GUIDE.md. Check them with "Test: Camera".
 *
 * The webcam values marked (restart) are used when the camera starts, so a Dashboard edit
 * only counts after the OpMode is started again.
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class CameraConfig {
    // ==================== HIVE camera (HuskyLens, looks UP at the CELL tags) ====================
    public static double HIVE_SAMPLE_MS = 100;    // read the HuskyLens at most this often (each read takes a few ms)
    public static int HIVE_IMAGE_WIDTH_PX = 320;  // HuskyLens picture width; the middle is at half of it
    // The HuskyLens gives each tag it LEARNED its own ID (1, 2, 3...). Write here which learned
    // ID belongs to which CELL. TODO(BioBuzz): train the HuskyLens (docs/CAMERA_GUIDE.md), then fill in.
    public static int RED_CELL_A_TAG_ID = 1;
    public static int RED_CELL_B_TAG_ID = 2;
    public static int BLUE_CELL_A_TAG_ID = 3;
    public static int BLUE_CELL_B_TAG_ID = 4;

    // ==================== FRONT camera (Logitech webcam, looks ahead) ====================
    public static boolean PREVIEW = false;        // (restart) live picture on the Robot Controller; OFF in matches
    public static int WIDTH_PX = 640;             // (restart) picture size: smaller = faster loop
    public static int HEIGHT_PX = 480;            // (restart)
    public static double HORIZONTAL_FOV_DEG = 70; // how wide the camera sees, edge to edge (Logitech C920: about 70)
    public static double APRILTAG_DECIMATION = 2; // 1 = sees tags farther away but slower; 3 = faster, closer only
    public static double EXPOSURE_MS = 0;         // 0 = automatic. Short (5-10 ms) stops motion blur under event lights
    public static int GAIN = 0;                   // used only when EXPOSURE_MS is not 0
    public static double MIN_BLOB_AREA_PX = 200;  // (restart) colour patches smaller than this are ignored

    // Where the webcam sits on the robot, for the tag pose (inches from the robot's centre, degrees).
    // TODO(BioBuzz): measure once the camera is mounted.
    public static double CAMERA_FORWARD_IN = 8;   // (restart) + = toward the robot's front
    public static double CAMERA_LEFT_IN = 0;      // (restart) + = toward the robot's left
    public static double CAMERA_UP_IN = 6;        // (restart) height above the floor
    public static double CAMERA_YAW_DEG = 0;      // (restart) 0 = facing the robot's front
    public static double CAMERA_PITCH_DEG = -90;  // (restart) the SDK's "level" is -90
    public static double CAMERA_ROLL_DEG = 0;     // (restart)

    // Colour ranges, YCrCb: { Y, Cr, Cb } minimum and maximum. (restart)
    // Starting values are the SDK's YELLOW, RED and BLUE. TODO(BioBuzz): tune under event lights.
    public static double[] POLLEN_MIN = { 32, 128, 0 };         // POLLEN is yellow
    public static double[] POLLEN_MAX = { 255, 170, 120 };
    public static double[] RED_NECTAR_MIN = { 32, 176, 0 };
    public static double[] RED_NECTAR_MAX = { 255, 255, 132 };
    public static double[] BLUE_NECTAR_MIN = { 16, 0, 155 };
    public static double[] BLUE_NECTAR_MAX = { 255, 127, 255 };
}
