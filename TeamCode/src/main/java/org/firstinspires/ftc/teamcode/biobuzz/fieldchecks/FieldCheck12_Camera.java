package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.CameraConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldCheckConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.StateTimer;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.HiveCamera;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck12_Camera.java  -  does the HIVE camera see the upward CELL's tag, under THESE lights?
 *                                                                              ROLE: OpMode (field check)
 *
 * Place the robot on our launch spot (FieldConfig.LAUNCH_SPOT), facing the upward CELL, with
 * the alliance and the upward CELL as they will be in the match (the INIT screen shows both;
 * gamepad 1 D-pad left/right switches the alliance).
 * Press PLAY. The check watches the HuskyLens for 1 s after it first sees the tag.
 * PASS: the tag was seen on at least CAMERA_SEEN_FRACTION of the reads. WARN: seen, but less
 * often. FAIL: not seen in 10 s. Then check the HuskyLens screen, its training and the
 * learned ids in CameraConfig, and the event lighting (docs/CAMERA_GUIDE.md).
 */
@TeleOp(name = "Field Check 12: Camera", group = "Field Checks")
public class FieldCheck12_Camera extends OpMode {

    private static final double MEASURE_S = 1;
    private static final double GIVE_UP_S = 10;

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private HiveCamera camera;
    private final StateTimer timer = new StateTimer();
    private final StateTimer seenTimer = new StateTimer();
    private boolean seenOnce = false;
    private int reads = 0;
    private int seenReads = 0;
    private boolean done = false;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        camera = new HiveCamera(hardwareMap);
    }

    @Override
    public void init_loop() {
        if (pad.isDpadLeftPressed() || pad.isDpadRightPressed()) {
            MatchInfo.switchAlliance();
        }
        camera.runUpdate();
        t.addLine("Robot on our launch spot, facing the upward CELL.");
        t.addData("Looking for", "%s CELL %s, learned id %d", MatchInfo.alliance, MatchInfo.getUpCellName(), upCellTagId());
        t.addData("Tag seen now", camera.seesTag(upCellTagId()));
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
        boolean seen = camera.seesTag(upCellTagId());
        if (!done && seen && !seenOnce) {
            seenOnce = true;
            seenTimer.reset();
        }
        if (!done && seenOnce) {
            reads++;
            if (seen) {
                seenReads++;
            }
            if (seenTimer.seconds() >= MEASURE_S) {
                finish();
            }
        }
        if (!done && !seenOnce && timer.seconds() >= GIVE_UP_S) {
            done = true;
            FieldCheckHelper.record(12, FieldCheckHelper.FAIL, "tag " + upCellTagId() + " not seen in " + GIVE_UP_S + " s");
        }
        t.addData("Tag seen now", seen);
        t.addData("Offset / width", "%+.0f px / %.0f px", camera.getTagOffsetPx(upCellTagId()), camera.getTagWidthPx(upCellTagId()));
        FieldCheckHelper.show(t, 12);
        t.update();
    }

    /** The learned id of our upward CELL's tag (CameraConfig). */
    private int upCellTagId() {
        if (MatchInfo.isRed()) {
            if (MatchInfo.isCellAUp()) {
                return CameraConfig.RED_CELL_A_TAG_ID;
            }
            return CameraConfig.RED_CELL_B_TAG_ID;
        }
        if (MatchInfo.isCellAUp()) {
            return CameraConfig.BLUE_CELL_A_TAG_ID;
        }
        return CameraConfig.BLUE_CELL_B_TAG_ID;
    }

    private void finish() {
        done = true;
        double fraction = (double) seenReads / reads;
        String grade = FieldCheckHelper.PASS;
        if (fraction < FieldCheckConfig.CAMERA_SEEN_FRACTION) {
            grade = FieldCheckHelper.WARN;
        }
        FieldCheckHelper.record(12, grade, String.format("tag %d seen on %.0f%% of reads, offset %+.0f px",
                upCellTagId(), fraction * 100, camera.getTagOffsetPx(upCellTagId())));
    }

    @Override
    public void stop() {
        FieldCheckHelper.stop();
    }
}
