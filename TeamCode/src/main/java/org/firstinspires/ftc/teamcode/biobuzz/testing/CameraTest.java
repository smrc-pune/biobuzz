package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.HiveCamera;

/**
 * STUDENTS: you will change this file.
 *
 * CameraTest.java  -  see what the HIVE camera (HuskyLens) sees.               ROLE: OpMode (test)
 *
 * It works in INIT too (cameras are not actuators), so you can check it before PLAY.
 * WHAT TO WATCH: hold a CELL tag above the camera. The learned id should match CameraConfig
 * (RED_CELL_A_TAG_ID ...), and the offset should be near 0 when the tag is in the middle.
 * The width grows as the tag comes closer.
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
        t.addData("isFaulted()", hiveCamera.isFaulted());
        hiveCamera.addTelemetry(t);
        t.update();
    }
}
