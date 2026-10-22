package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.CameraConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldCheckConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.FrontCamera;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck12_Camera.java  -  does the webcam measure a tag's distance right, under THESE lights?
 *                                                                              ROLE: OpMode (field check)
 *
 * Place the robot's centre on the seam crossing in FieldCheckConfig (CAMERA_CHECK_X_IN, _Y_IN),
 * facing CAMERA_CHECK_HEADING_DEG, with tag CAMERA_CHECK_TAG_ID in view. Press PLAY.
 * The check works out how far the tag SHOULD be (from where the robot is placed, where the
 * camera sits on it in CameraConfig, and where the tag is), and compares it with what the
 * webcam measures, averaged over 1 s.
 * PASS: within CAMERA_TOLERANCE_IN (2 in). FAIL: re-check the camera mount numbers in
 * CameraConfig, and the exposure (event lighting is a common cause), docs/CAMERA_GUIDE.md.
 * Only needed if the camera is used for driving. It needs the BIOBUZZ tags in the SDK's tag
 * library (FrontCamera's TODO), otherwise the tag has no range.
 */
@TeleOp(name = "Field Check 12: Camera", group = "Field Checks")
public class FieldCheck12_Camera extends OpMode {

    private static final double MEASURE_S = 1;
    private static final double GIVE_UP_S = 10;

    private Telemetry t;
    private BatteryMonitor battery;
    private FrontCamera camera;
    private final StateTimer timer = new StateTimer();
    private final StateTimer seenTimer = new StateTimer();
    private boolean seenOnce = false;
    private double rangeTotal = 0;
    private int rangeCount = 0;
    private boolean done = false;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        battery = new BatteryMonitor(hardwareMap);
        camera = new FrontCamera(hardwareMap);
    }

    @Override
    public void init_loop() {
        camera.runUpdate();
        t.addLine("Robot on the seam crossing in FieldCheckConfig, tag " + FieldCheckConfig.CAMERA_CHECK_TAG_ID + " in view.");
        t.addData("Tag seen now", camera.seesTag(FieldCheckConfig.CAMERA_CHECK_TAG_ID));
        t.update();
    }

    @Override
    public void start() {
        FieldCheckHelper.start("FieldCheck12_Camera", battery.getVoltage());
        timer.reset();
    }

    @Override
    public void loop() {
        camera.runUpdate();
        int tagId = FieldCheckConfig.CAMERA_CHECK_TAG_ID;
        if (!done && camera.seesTag(tagId) && camera.getTagRangeIn(tagId) > 0) {
            if (!seenOnce) seenTimer.reset();
            seenOnce = true;
            rangeTotal += camera.getTagRangeIn(tagId);
            rangeCount++;
        }
        if (!done && seenOnce && seenTimer.seconds() >= MEASURE_S) finish();
        if (!done && timer.seconds() >= GIVE_UP_S) {
            done = true;
            FieldCheckHelper.record(12, FieldCheckHelper.FAIL, "tag " + tagId + " not seen (or has no range) in "
                    + GIVE_UP_S + " s");
        }
        t.addData("Expected", "%.1f in", expectedRangeIn());
        t.addData("Measured now", "%.1f in", camera.getTagRangeIn(tagId));
        t.addData("Camera", "%s, %.0f fps", camera.getCameraState(), camera.getFps());
        FieldCheckHelper.show(t, 12);
        t.update();
    }

    /** The straight-line distance from the camera to the tag, from where everything is placed. */
    private double expectedRangeIn() {
        double headingRad = Math.toRadians(FieldCheckConfig.CAMERA_CHECK_HEADING_DEG);
        double cameraX = FieldCheckConfig.CAMERA_CHECK_X_IN + CameraConfig.CAMERA_FORWARD_IN * Math.cos(headingRad)
                - CameraConfig.CAMERA_LEFT_IN * Math.sin(headingRad);
        double cameraY = FieldCheckConfig.CAMERA_CHECK_Y_IN + CameraConfig.CAMERA_FORWARD_IN * Math.sin(headingRad)
                + CameraConfig.CAMERA_LEFT_IN * Math.cos(headingRad);
        double dx = FieldCheckConfig.CAMERA_CHECK_TAG_X_IN - cameraX;
        double dy = FieldCheckConfig.CAMERA_CHECK_TAG_Y_IN - cameraY;
        double dz = FieldCheckConfig.CAMERA_CHECK_TAG_Z_IN - CameraConfig.CAMERA_UP_IN;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void finish() {
        done = true;
        double measured = rangeTotal / rangeCount;
        double errorIn = measured - expectedRangeIn();
        FieldCheckHelper.record(12, FieldCheckHelper.grade(errorIn, FieldCheckConfig.CAMERA_TOLERANCE_IN),
                String.format("measured %.1f in, expected %.1f in (%+.1f)", measured, expectedRangeIn(), errorIn));
    }

    @Override
    public void stop() { FieldCheckHelper.stop(); }
}
