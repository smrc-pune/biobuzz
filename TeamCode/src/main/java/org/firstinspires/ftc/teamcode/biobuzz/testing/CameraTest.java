package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.CameraConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.HiveCamera;

/**
 * STUDENTS: you will change this file.
 *
 * CameraTest.java  -  see what the HIVE camera (HuskyLens) sees.               ROLE: OpMode (test)
 *
 * It works in INIT too (cameras are not actuators), so you can check it before PLAY.
 * WHAT TO WATCH: hold a CELL tag above the camera. "Which CELL" should name the CELL that
 * tag belongs to (the AprilTag numbers are in CameraConfig). "not one of our CELL ids" means
 * it was learned under another id: fix the four ..._TAG_ID numbers in CameraConfig.
 * The offset should be near 0 when the tag is in the middle; the width grows as it comes closer.
 * Setup and training: docs/CAMERA_GUIDE.md.
 */
@TeleOp(name = "Test: Camera", group = "Tests")
public class CameraTest extends OpMode {

    private Telemetry t;
    private HiveCamera hiveCamera;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        hiveCamera = new HiveCamera(hardwareMap);
    }

    @Override
    public void init_loop() {
        showCamera();
    }

    @Override
    public void loop() {
        showCamera();
    }

    private void showCamera() {
        hiveCamera.runUpdate();

        t.addData("Tags in view", hiveCamera.getTagCount());
        int id = hiveCamera.getFirstTagId();
        t.addData("First tag", "id %d, offset %+.0f px, width %.0f px", id,
                hiveCamera.getTagOffsetPx(id), hiveCamera.getTagWidthPx(id));
        t.addData("Which CELL", cellNameFor(id));
        t.addData("isFaulted()", hiveCamera.isFaulted());
        hiveCamera.addTelemetry(t);
        t.update();
    }

    /** Which CELL a learned id belongs to, from CameraConfig. */
    private String cellNameFor(int learnedId) {
        if (learnedId < 0) {
            return "no tag in view";
        }
        if (learnedId == CameraConfig.RED_CELL_A_TAG_ID) {
            return "RED CELL A (AprilTags 34-37, audience side)";
        }
        if (learnedId == CameraConfig.RED_CELL_B_TAG_ID) {
            return "RED CELL B (AprilTags 30-33, opposite the audience)";
        }
        if (learnedId == CameraConfig.BLUE_CELL_A_TAG_ID) {
            return "BLUE CELL A (AprilTags 42-45, opposite the audience)";
        }
        if (learnedId == CameraConfig.BLUE_CELL_B_TAG_ID) {
            return "BLUE CELL B (AprilTags 38-41, audience side)";
        }
        return "not one of our CELL ids (CameraConfig)";
    }
}
