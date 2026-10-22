package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.CameraConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.FrontCamera;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.HiveCamera;

/**
 * STUDENTS: you will change this file.
 *
 * CameraTest.java  -  see what BOTH cameras see.                               ROLE: OpMode (test)
 *
 * It works in INIT too (cameras are not actuators), so you can check them before PLAY.
 * CONTROLS (gamepad 1):   X = webcam preview on/off (shows on the Robot Controller screen)
 * WHAT TO WATCH:
 *   HIVE camera: hold a CELL tag above it. The learned id should match CameraConfig
 *   (RED_CELL_A_TAG_ID ...), and the offset should be near 0 when the tag is in the middle.
 *   Front camera: the frame rate (fps) should stay above about 15. A field AprilTag shows
 *   its id, range and bearing; POLLEN and NECTAR show "yes" when one is in front.
 * Setup and colour tuning: docs/CAMERA_GUIDE.md.
 */
@TeleOp(name = "Test: Camera", group = "Tests")
public class CameraTest extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private HiveCamera hiveCamera;
    private FrontCamera frontCamera;
    private boolean preview = CameraConfig.PREVIEW;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        hiveCamera = new HiveCamera(hardwareMap);
        frontCamera = new FrontCamera(hardwareMap);
    }

    @Override
    public void init_loop() { showCameras(); }

    @Override
    public void loop() { showCameras(); }

    private void showCameras() {
        if (pad.isXPressed()) {
            preview = !preview;
            frontCamera.setPreview(preview);
        }
        hiveCamera.runUpdate();
        frontCamera.runUpdate();

        t.addLine("X = webcam preview on/off");
        t.addLine("--- HIVE camera (HuskyLens) ---");
        t.addData("Tags in view", hiveCamera.getTagCount());
        int hiveId = hiveCamera.getFirstTagId();
        t.addData("First tag", "id %d, offset %+.0f px, width %.0f px", hiveId,
                hiveCamera.getTagOffsetPx(hiveId), hiveCamera.getTagWidthPx(hiveId));
        t.addData("isFaulted()", hiveCamera.isFaulted());
        hiveCamera.addTelemetry(t);

        t.addLine("--- Front camera (webcam) ---");
        t.addData("State / fps", "%s, %.0f fps", frontCamera.getCameraState(), frontCamera.getFps());
        int frontId = frontCamera.getFirstTagId();
        t.addData("First tag", "id %d, %.1f in, bearing %.1f deg, yaw %.1f deg", frontId,
                frontCamera.getTagRangeIn(frontId), frontCamera.getTagBearingDeg(frontId), frontCamera.getTagYawDeg(frontId));
        t.addData("POLLEN", "%s, %.0f deg", frontCamera.seesPollen(), frontCamera.getPollenBearingDeg());
        t.addData("Red NECTAR", "%s, %.0f deg", frontCamera.seesRedNectar(), frontCamera.getRedNectarBearingDeg());
        t.addData("Blue NECTAR", "%s, %.0f deg", frontCamera.seesBlueNectar(), frontCamera.getBlueNectarBearingDeg());
        t.addData("isFaulted()", frontCamera.isFaulted());
        frontCamera.addTelemetry(t);
        t.update();
    }
}
