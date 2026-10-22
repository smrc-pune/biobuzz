package org.firstinspires.ftc.teamcode.biobuzz.mechanisms;

import android.util.Size;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.CameraConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagSingleDetection;
import org.firstinspires.ftc.vision.opencv.ColorBlobLocatorProcessor;
import org.firstinspires.ftc.vision.opencv.ColorRange;
import org.firstinspires.ftc.vision.opencv.ColorSpace;
import org.opencv.core.Scalar;

import java.util.concurrent.TimeUnit;

/**
 * MENTORS: you do not need to change this file. Its numbers are in CameraConfig.
 *
 * FrontCamera.java  -  the Logitech webcam that looks ahead.                   ROLE: mechanism
 * PATTERN: Adapter (the SDK's VisionPortal becomes plain answers) + Template Method.
 *
 * The SDK's VisionPortal runs four "processors" on every picture, on its OWN thread (not our loop):
 *   AprilTag  real field tag ids, and each tag's range, bearing and yaw from the camera
 *   POLLEN, red NECTAR, blue NECTAR   colour patches (the biggest of each is kept)
 * update() copies the newest answers once per loop, which is quick.
 * It only ANSWERS QUESTIONS; it never changes the pose (a camera pose fix is a future
 * improvement, docs/LLD.md section 14).
 *
 * SDK 12 NOTE: getDetections() returns AprilTagDetection; a plain tag is an
 * AprilTagSingleDetection (it has the id), so update() checks "instanceof" before reading it.
 *
 * TRACING A BUG: FAULTED or "camera ERROR" -> USB cable, or HardwareNames.WEBCAM_FRONT. Slow
 * loop with the camera on -> smaller WIDTH_PX/HEIGHT_PX, higher APRILTAG_DECIMATION, PREVIEW off.
 * Colours not seen under event lights -> docs/CAMERA_GUIDE.md.
 */
public class FrontCamera extends Mechanism {

    private static final int MAX_TAGS = 8;   // more tags than this in one picture are ignored

    private final VisionPortal portal;
    private final AprilTagProcessor aprilTags;
    private final ColorBlobLocatorProcessor pollenFinder;
    private final ColorBlobLocatorProcessor redNectarFinder;
    private final ColorBlobLocatorProcessor blueNectarFinder;
    private final ElapsedTime sinceUpdate = new ElapsedTime();
    private boolean exposureApplied = false;

    // The newest answers, copied in update().
    private final int[] tagIds = new int[MAX_TAGS];
    private final double[] tagRangeIn = new double[MAX_TAGS];
    private final double[] tagBearingDeg = new double[MAX_TAGS];
    private final double[] tagYawDeg = new double[MAX_TAGS];
    private int tagCount = 0;
    private double pollenBearingDeg = 0, redNectarBearingDeg = 0, blueNectarBearingDeg = 0;
    private boolean seesPollen = false, seesRedNectar = false, seesBlueNectar = false;

    public FrontCamera(HardwareMap hardwareMap) {
        super("frontCamera");
        aprilTags = new AprilTagProcessor.Builder()
                .setOutputUnits(DistanceUnit.INCH, AngleUnit.DEGREES)
                .setCameraPose(
                        new Position(DistanceUnit.INCH, -CameraConfig.CAMERA_LEFT_IN, CameraConfig.CAMERA_FORWARD_IN,
                                CameraConfig.CAMERA_UP_IN, 0),
                        new YawPitchRollAngles(AngleUnit.DEGREES, CameraConfig.CAMERA_YAW_DEG,
                                CameraConfig.CAMERA_PITCH_DEG, CameraConfig.CAMERA_ROLL_DEG, 0))
                .build();
        aprilTags.setDecimation((float) CameraConfig.APRILTAG_DECIMATION);
        pollenFinder = colourFinder(CameraConfig.POLLEN_MIN, CameraConfig.POLLEN_MAX);
        redNectarFinder = colourFinder(CameraConfig.RED_NECTAR_MIN, CameraConfig.RED_NECTAR_MAX);
        blueNectarFinder = colourFinder(CameraConfig.BLUE_NECTAR_MIN, CameraConfig.BLUE_NECTAR_MAX);
        portal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, HardwareNames.WEBCAM_FRONT))
                .setCameraResolution(new Size(CameraConfig.WIDTH_PX, CameraConfig.HEIGHT_PX))
                .enableLiveView(CameraConfig.PREVIEW)
                .addProcessors(aprilTags, pollenFinder, redNectarFinder, blueNectarFinder)
                .build();
        // A camera is not an actuator: streaming during INIT is allowed.
    }

    private static ColorBlobLocatorProcessor colourFinder(double[] min, double[] max) {
        ColorBlobLocatorProcessor finder = new ColorBlobLocatorProcessor.Builder()
                .setTargetColorRange(new ColorRange(ColorSpace.YCrCb,
                        new Scalar(min[0], min[1], min[2]), new Scalar(max[0], max[1], max[2])))
                .setContourMode(ColorBlobLocatorProcessor.ContourMode.EXTERNAL_ONLY)
                .setBlurSize(5)
                .build();
        finder.addFilter(new ColorBlobLocatorProcessor.BlobFilter(
                ColorBlobLocatorProcessor.BlobCriteria.BY_CONTOUR_AREA, CameraConfig.MIN_BLOB_AREA_PX, 1e9));
        return finder;
    }

    // ---- What it can do ----

    /** Stop sending pictures to the processors (saves the Control Hub's CPU). */
    public void pause() { portal.stopStreaming(); }

    public void resume() { portal.resumeStreaming(); }

    public void setPreview(boolean on) {
        if (on) portal.resumeLiveView();
        else portal.stopLiveView();
    }

    // ---- What you can ask it ----
    public boolean seesTag(int id) { return findTag(id) >= 0; }

    /** Distance from the camera to the tag, in inches (0 if not seen). */
    public double getTagRangeIn(int id) {
        int slot = findTag(id);
        if (slot < 0) return 0;
        return tagRangeIn[slot];
    }

    /** Degrees the tag is to the LEFT (+) of straight ahead (0 if not seen). */
    public double getTagBearingDeg(int id) {
        int slot = findTag(id);
        if (slot < 0) return 0;
        return tagBearingDeg[slot];
    }

    /** How much the tag is turned, in degrees (0 = facing the camera). */
    public double getTagYawDeg(int id) {
        int slot = findTag(id);
        if (slot < 0) return 0;
        return tagYawDeg[slot];
    }

    public int getTagCount() { return tagCount; }

    /** The id of the first tag in view, or -1. */
    public int getFirstTagId() {
        if (tagCount == 0) return -1;
        return tagIds[0];
    }

    public boolean seesPollen() { return seesPollen; }
    public boolean seesRedNectar() { return seesRedNectar; }
    public boolean seesBlueNectar() { return seesBlueNectar; }

    /** Degrees the biggest POLLEN is to the RIGHT (+) of the picture's middle (0 if none). */
    public double getPollenBearingDeg() { return pollenBearingDeg; }
    public double getRedNectarBearingDeg() { return redNectarBearingDeg; }
    public double getBlueNectarBearingDeg() { return blueNectarBearingDeg; }

    public double getFps() { return portal.getFps(); }

    public String getCameraState() { return portal.getCameraState().toString(); }

    public double getAgeMs() { return sinceUpdate.milliseconds(); }

    private int findTag(int id) {
        for (int slot = 0; slot < tagCount; slot++) {
            if (tagIds[slot] == id) return slot;
        }
        return -1;
    }

    // ---- Filled in for Mechanism ----
    @Override
    protected void update() {
        applyExposureOnce();
        tagCount = 0;
        for (AprilTagDetection detection : aprilTags.getDetections()) {
            if (tagCount >= MAX_TAGS || !(detection instanceof AprilTagSingleDetection)) continue;
            tagIds[tagCount] = ((AprilTagSingleDetection) detection).id;
            tagRangeIn[tagCount] = 0;
            tagBearingDeg[tagCount] = 0;
            tagYawDeg[tagCount] = 0;
            // A tag that is not in the SDK's tag library has no size, so no pose: only its id.
            // TODO(BioBuzz): add the BIOBUZZ tags (ids and sizes) with setTagLibrary(...) if the SDK lacks them.
            if (detection.ftcPose != null) {
                tagRangeIn[tagCount] = detection.ftcPose.range;
                tagBearingDeg[tagCount] = detection.ftcPose.bearing;
                tagYawDeg[tagCount] = detection.ftcPose.yaw;
            }
            tagCount++;
        }
        seesPollen = false;
        for (ColorBlobLocatorProcessor.Blob blob : pollenFinder.getBlobs()) {
            if (!seesPollen) pollenBearingDeg = bearingDeg(blob);
            seesPollen = true;
        }
        seesRedNectar = false;
        for (ColorBlobLocatorProcessor.Blob blob : redNectarFinder.getBlobs()) {
            if (!seesRedNectar) redNectarBearingDeg = bearingDeg(blob);
            seesRedNectar = true;
        }
        seesBlueNectar = false;
        for (ColorBlobLocatorProcessor.Blob blob : blueNectarFinder.getBlobs()) {
            if (!seesBlueNectar) blueNectarBearingDeg = bearingDeg(blob);
            seesBlueNectar = true;
        }
        sinceUpdate.reset();
    }

    /** The blobs come biggest first. Picture x -> degrees right of the middle. */
    private double bearingDeg(ColorBlobLocatorProcessor.Blob blob) {
        double middle = CameraConfig.WIDTH_PX / 2.0;
        return (blob.getBoxFit().center.x - middle) / middle * CameraConfig.HORIZONTAL_FOV_DEG / 2.0;
    }

    /** Exposure can only be set once the camera is streaming. */
    private void applyExposureOnce() {
        if (exposureApplied || CameraConfig.EXPOSURE_MS <= 0) return;
        if (portal.getCameraState() != VisionPortal.CameraState.STREAMING) return;
        ExposureControl exposure = portal.getCameraControl(ExposureControl.class);
        exposure.setMode(ExposureControl.Mode.Manual);
        exposure.setExposure((long) CameraConfig.EXPOSURE_MS, TimeUnit.MILLISECONDS);
        GainControl gain = portal.getCameraControl(GainControl.class);
        gain.setGain(CameraConfig.GAIN);
        exposureApplied = true;
    }

    @Override
    protected void addDetails(Telemetry t) {
        String tags = "no tags";
        if (tagCount > 0) tags = String.format("tag %d at %.0f in, %.0f deg", tagIds[0], tagRangeIn[0], tagBearingDeg[0]);
        String colours = "";
        if (seesPollen) colours += " POLLEN";
        if (seesRedNectar) colours += " red-NECTAR";
        if (seesBlueNectar) colours += " blue-NECTAR";
        if (colours.isEmpty()) colours = " nothing";
        t.addData("Front camera", tags + "; sees" + colours);
        if (TelemetryConfig.DETAILS) t.addData("Front camera state", getCameraState() + String.format(", %.0f fps", getFps()));
    }

    @Override
    protected void addLogValues() {
        MatchLogger.value("front_cam_tag_id", getFirstTagId());
        MatchLogger.value("front_cam_tag_range_in", getTagRangeIn(getFirstTagId()));
        MatchLogger.value("front_cam_pollen", seesPollen);
        MatchLogger.value("front_cam_red_nectar", seesRedNectar);
        MatchLogger.value("front_cam_blue_nectar", seesBlueNectar);
    }

    @Override
    protected boolean isTelemetryOn() { return TelemetryConfig.CAMERAS; }
}
