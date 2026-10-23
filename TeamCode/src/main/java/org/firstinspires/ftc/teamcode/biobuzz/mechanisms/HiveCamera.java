package org.firstinspires.ftc.teamcode.biobuzz.mechanisms;

import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.CameraConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * MENTORS: you do not need to change this file. Its numbers are in CameraConfig.
 *
 * HiveCamera.java  -  the HuskyLens that looks UP at the AprilTags under the CELLS.   ROLE: mechanism
 * PATTERN: Adapter (the HuskyLens's boxes become plain answers) + Template Method.
 *
 * It only ANSWERS QUESTIONS; it never moves anything and never changes the pose:
 *   seesTag(id)          is a tag with this LEARNED id in view?
 *   getTagOffsetPx(id)   how far right (+) or left (-) of the picture's middle, in pixels
 *   getTagWidthPx(id)    how wide the tag looks (bigger = closer)
 *   getAgeMs()           how old these answers are
 * The HuskyLens learns its own ids (1, 2, 3...), not the real AprilTag numbers. Which id is
 * which CELL is written in CameraConfig. It is read every CameraConfig.HIVE_SAMPLE_MS,
 * because each read is a slow I2C command.
 *
 * TRACING A BUG: "sees nothing" -> the HuskyLens screen shows what it sees; is it in TAG
 * RECOGNITION mode with the tags learned (docs/CAMERA_GUIDE.md)? A FAULTED HiveCamera
 * means it stopped answering: check its I2C cable.
 */
public class HiveCamera extends Mechanism {

    private final HuskyLens huskyLens;
    private final ElapsedTime sinceRead = new ElapsedTime();
    private HuskyLens.Block[] blocks = new HuskyLens.Block[0];   // what it saw at the last read

    public HiveCamera(HardwareMap hardwareMap) {
        super("hiveCamera");
        huskyLens = hardwareMap.get(HuskyLens.class, HardwareNames.HUSKYLENS_HIVE);
        huskyLens.selectAlgorithm(HuskyLens.Algorithm.TAG_RECOGNITION);   // a camera setting, not a movement
    }

    // ---- What you can ask it ----
    public boolean seesTag(int learnedId) {
        return findBlock(learnedId) != null;
    }

    /** Pixels right (+) or left (-) of the picture's middle; 0 if the tag is not seen. */
    public double getTagOffsetPx(int learnedId) {
        HuskyLens.Block block = findBlock(learnedId);
        if (block == null) {
            return 0;
        }
        return block.x - CameraConfig.HIVE_IMAGE_WIDTH_PX / 2.0;
    }

    /** How wide the tag looks, in pixels (0 if not seen). Bigger = closer. */
    public double getTagWidthPx(int learnedId) {
        HuskyLens.Block block = findBlock(learnedId);
        if (block == null) {
            return 0;
        }
        return block.width;
    }

    /** How many tags are in view. */
    public int getTagCount() {
        return blocks.length;
    }

    /** The learned id of the first tag in view, or -1. */
    public int getFirstTagId() {
        if (blocks.length == 0) {
            return -1;
        }
        return blocks[0].id;
    }

    public double getAgeMs() {
        return sinceRead.milliseconds();
    }

    private HuskyLens.Block findBlock(int learnedId) {
        for (HuskyLens.Block block : blocks) {
            if (block.id == learnedId) {
                return block;
            }
        }
        return null;
    }

    // ---- Filled in for Mechanism ----
    @Override
    protected void update() {
        if (sinceRead.milliseconds() < CameraConfig.HIVE_SAMPLE_MS) {
            return;
        }
        HuskyLens.Block[] newBlocks = huskyLens.blocks();
        if (newBlocks == null) {
            newBlocks = new HuskyLens.Block[0];
        }
        blocks = newBlocks;
        sinceRead.reset();
    }

    @Override
    protected void addDetails(Telemetry t) {
        if (blocks.length == 0) {
            t.addData("HIVE camera", "no tags");
            return;
        }
        HuskyLens.Block first = blocks[0];
        t.addData("HIVE camera", String.format("%d tags; id %d at %+.0f px, %d px wide",
                blocks.length, first.id, first.x - CameraConfig.HIVE_IMAGE_WIDTH_PX / 2.0, first.width));
    }

    @Override
    protected void addLogValues() {
        int firstId = getFirstTagId();
        MatchLogger.value("hive_cam_tag_id", firstId);
        MatchLogger.value("hive_cam_offset_px", getTagOffsetPx(firstId));
        MatchLogger.value("hive_cam_age_ms", getAgeMs());
    }

    @Override
    protected boolean isTelemetryOn() {
        return TelemetryConfig.CAMERAS;
    }
}
