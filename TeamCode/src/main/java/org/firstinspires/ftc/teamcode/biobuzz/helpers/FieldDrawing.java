package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldDrawing.java  -  draws the robot and its path on the FTC Dashboard field.  ROLE: helper
 *
 * Open http://192.168.43.1:8080/dash and pick the "Field" view.
 *
 * HOW IT IS USED (Drive does this every loop, then the OpMode sends it):
 *   FieldDrawing.drawRobot(x, y, headingDeg);
 *   FieldDrawing.drawPath(pathX, pathY);     // only while a path is followed
 *   FieldDrawing.send();                     // once per loop, after telemetry
 *
 * COORDINATES: Pedro's field frame, in inches. The Dashboard field is 0 to 144 inches
 * on each side, the same as Pedro's, so no conversion is done here.
 *
 * BACKGROUND: the Dashboard shows its own (current season) field picture by default.
 * With TelemetryConfig.FIELD_IMAGE on, it shows the BioBuzz field instead, from
 * TeamCode/src/main/assets/images/field-biobuzz.png.
 *
 * TRACING A BUG: robot drawn in the wrong place or turned the wrong way = check the
 * pose numbers on telemetry first. If those are right, the drawing is wrong (see LLD).
 * Read this file once; you will not need to change it.
 */
public class FieldDrawing {
    private static final double ROBOT_RADIUS_IN = 9;   // half of an 18 in robot
    private static final double FIELD_SIZE_IN = 144;
    private static final int GRID_LINES = 7;           // one line every 24 in tile

    private static TelemetryPacket packet = newPacket();
    private static final ElapsedTime sinceSent = new ElapsedTime();

    /** Draw the robot as a circle with a line pointing the way it faces. */
    public static void drawRobot(double xIn, double yIn, double headingDeg) {
        if (!TelemetryConfig.FIELD_DRAWING) return;
        Canvas canvas = packet.fieldOverlay();
        double headingRad = Math.toRadians(headingDeg);
        canvas.setStroke("#3F51B5");
        canvas.setStrokeWidth(1);
        canvas.strokeCircle(xIn, yIn, ROBOT_RADIUS_IN);
        canvas.strokeLine(xIn, yIn,
                xIn + ROBOT_RADIUS_IN * Math.cos(headingRad),
                yIn + ROBOT_RADIUS_IN * Math.sin(headingRad));
    }

    /** Draw a path as a line through these points (inches). Both arrays have the same length. */
    public static void drawPath(double[] xIn, double[] yIn) {
        if (!TelemetryConfig.FIELD_DRAWING) return;
        Canvas canvas = packet.fieldOverlay();
        canvas.setStroke("#4CAF50");
        canvas.setStrokeWidth(1);
        canvas.strokePolyline(xIn, yIn);
    }

    /** Send this loop's drawing, at most every TelemetryConfig.DS_TRANSMIT_INTERVAL_MS. */
    public static void send() {
        if (TelemetryConfig.FIELD_DRAWING && sinceSent.milliseconds() >= TelemetryConfig.DS_TRANSMIT_INTERVAL_MS) {
            FtcDashboard.getInstance().sendTelemetryPacket(packet);
            sinceSent.reset();
        }
        packet = newPacket();   // start a clean drawing for the next loop
    }

    /** A fresh packet, with the BioBuzz field behind it when FIELD_IMAGE is on. */
    private static TelemetryPacket newPacket() {
        if (!TelemetryConfig.FIELD_IMAGE) return new TelemetryPacket();   // Dashboard's own field
        TelemetryPacket fresh = new TelemetryPacket(false);
        Canvas field = fresh.field();
        field.drawImage(TelemetryConfig.FIELD_IMAGE_PATH, 0, 0, FIELD_SIZE_IN, FIELD_SIZE_IN);
        field.drawGrid(0, 0, FIELD_SIZE_IN, FIELD_SIZE_IN, GRID_LINES, GRID_LINES);
        return fresh;
    }
}
