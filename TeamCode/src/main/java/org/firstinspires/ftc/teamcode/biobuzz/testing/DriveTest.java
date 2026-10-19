package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.Robot;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldDrawing;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;
import org.firstinspires.ftc.teamcode.biobuzz.subsystems.Drive;

/**
 * DriveTest.java  -  test DRIVING by itself (no mechanisms).                   ROLE: OpMode (test)
 *
 * WHEN TO USE: the robot drives strangely (strafe or turn backwards, field-centric
 * points the wrong way). Check the pose numbers here first.
 *
 * CONTROLS (gamepad 1):   left stick = move   right stick X = turn
 *                         Y = "facing away from drivers now"   X = switch field / robot-centric
 *                         B (INIT only) = switch alliance BLUE / RED
 *
 * WHAT TO WATCH: the pose (x, y, heading). Push forward: does x or y change the way you
 * expect? With the robot facing away from the drivers, press Y: the heading must read
 * the driver heading in DriveConfig. Then turn the robot 90 degrees and push the stick
 * away from you: the robot must still move away from you.
 * If strafe or turn is backwards, the fix is the sign flip in parts/DriveBase.driveManual().
 */
@TeleOp(name = "Test: Drive", group = "Tests")
public class DriveTest extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private Drive drive;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        drive = Robot.buildDrive(hardwareMap);   // ONLY the drivetrain
        MatchInfo.matchIdOverride = "TEST";
    }

    @Override
    public void init_loop() {
        if (pad.isBPressed()) MatchInfo.alliance = 1 - MatchInfo.alliance;   // 0 <-> 1
        t.addData("Alliance (B to switch)", MatchInfo.getAllianceName());
        showControls();
        t.update();
    }

    @Override
    public void start() {
        drive.setAlliance(MatchInfo.isRed());
        drive.setStartingPoseFacingAway();
    }

    @Override
    public void loop() {
        boolean faceAway = pad.isYPressed();
        boolean switchMode = pad.isXPressed();

        drive.setDriverInput(pad.getLeftStickY(), pad.getLeftStickX(), pad.getRightStickX());
        if (faceAway) drive.resetHeading();
        if (switchMode) drive.switchDriveMode();

        drive.update();

        showControls();
        drive.addTelemetry(t);
        drive.addDrawing();
        FieldDrawing.send();
        t.update();
    }

    @Override
    public void stop() {
        MatchInfo.matchIdOverride = "";   // back to normal match ids for the next OpMode
    }

    /** Which gamepad buttons this test uses. Shown before PLAY and every loop. */
    private void showControls() {
        t.addLine("--- CONTROLS (gamepad 1) ---");
        t.addData("Left stick", "MOVE (field-centric: away = away from you)");
        t.addData("Right stick X", "TURN");
        t.addData("Y", "robot faces AWAY from drivers now (heading reset)");
        t.addData("X", "switch FIELD-centric / ROBOT-centric");
    }
}
