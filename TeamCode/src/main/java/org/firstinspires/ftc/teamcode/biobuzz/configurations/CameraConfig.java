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
    // The HuskyLens gives each tag it LEARNED its own ID (1, 2, 3...). Write here which learned
    // ID belongs to which CELL. TODO(BioBuzz): train the HuskyLens (docs/CAMERA_GUIDE.md), then fill in.
    public static int RED_CELL_A_TAG_ID = 1;
    public static int RED_CELL_B_TAG_ID = 2;
    public static int BLUE_CELL_A_TAG_ID = 3;
    public static int BLUE_CELL_B_TAG_ID = 4;
}
