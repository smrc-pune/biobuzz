package org.firstinspires.ftc.teamcode.biobuzz;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.HardwareLookup;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.parts.DriveBase;
import org.firstinspires.ftc.teamcode.biobuzz.subsystems.Drive;

/**
 * Robot.java  -  the whole robot in one object. TeleOp AND Autonomous both use it.   ROLE: robot
 *
 * WHAT ROBOT DOES (and ONLY this):
 *   1. BUILDS every part and brain. This is the only place that connects device NAMES
 *      (HardwareNames) to parts, so you never hunt for names anywhere else.
 *   2. UPDATES every brain, once per loop:   robot.update()
 *   3. LINKS brains that must cooperate (the ONE place brains talk to each other).
 *   4. Shows telemetry, honoring the switches in TelemetryConfig.
 *
 * WHAT ROBOT NEVER DOES: read gamepads (that is the OpMode's job) or set motor powers
 * (parts do that).
 *
 * THE BIG PICTURE:
 *
 *   OpMode (gamepad or auto steps)
 *        | commands
 *        v
 *      Robot  ---->  Drive  (+ one brain per mechanism, TODO BioBuzz)   brains: states + timing
 *                      |
 *                      v
 *                  DriveBase  (+ mechanism parts)                       parts: hardware
 *
 * TRACING A BUG: start at the OpMode's loop(), then update() below, then the brain.
 */
public class Robot {

    // Public so OpModes can command them:  robot.drive.resetHeading();
    public final Drive drive;
    // TODO(BioBuzz): one public final field per mechanism brain.

    public Robot(HardwareMap hardwareMap) {
        HardwareLookup.useManualBulkReads(hardwareMap);   // one hub read per loop (see HardwareLookup)
        drive = buildDrive(hardwareMap);
        // TODO(BioBuzz): build each mechanism here with its buildX() method.
    }

    // The builders are separate (and public) so a test OpMode can build ONE subsystem alone.

    public static Drive buildDrive(HardwareMap hardwareMap) {
        return new Drive(new DriveBase(hardwareMap));   // DriveBase gets its motors from Pedro's Constants
    }

    /**
     * Run one step of everything. Call once per loop, and ONLY once per loop.
     */
    public void update() {
        HardwareLookup.clearBulkReadCache();   // fresh sensor values for this loop

        // ---- LINKS between brains (the only place brains talk to each other) ----
        // TODO(BioBuzz): for example "while the shooter fires, the intake feeds".

        // TODO(BioBuzz): each mechanism's update() goes here, in a fixed order.
        drive.update();
    }

    /** true if ANY brain is in the middle of a job. Autonomous uses this to know when to move on. */
    public boolean isBusy() {
        return drive.isBusy();   // TODO(BioBuzz): || each mechanism's isBusy()
    }

    /** Put every mechanism in its safe state. OpModes call this in stop(), and D-pad down calls it. */
    public void stopMechanisms() {
        // TODO(BioBuzz): each mechanism's stop() command. Drive is left to the driver and Pedro.
    }

    /** The first telemetry line, always shown: alliance, match and drive mode. */
    public void addHeader(Telemetry t) {
        String driveMode = "FIELD";
        if (!drive.isFieldCentric()) driveMode = "ROBOT";
        t.addData("Match", MatchInfo.getAllianceName() + "  " + MatchInfo.getMatchId()
                + "  battery " + MatchInfo.getBatteryLabel() + "  drive " + driveMode
                + String.format("  heading %.0f deg", drive.getHeadingDegrees()));
        // TODO(BioBuzz) step 4: routine, match time left, loop ms, battery volts, ALERT.
    }

    /** Header, then every brain that is switched on in TelemetryConfig, then the field drawing. */
    public void addTelemetry(Telemetry t) {
        addHeader(t);
        if (TelemetryConfig.DRIVE) drive.addTelemetry(t);
        // TODO(BioBuzz): if (TelemetryConfig.<MECHANISM>) <mechanism>.addTelemetry(t);
        drive.addDrawing();
        FieldDrawing.send();
    }
}
