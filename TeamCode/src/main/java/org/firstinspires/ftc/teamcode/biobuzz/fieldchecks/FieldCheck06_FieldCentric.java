package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.drive.Drive;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck06_FieldCentric.java  -  does "away" still mean away?            ROLE: OpMode (field check)
 *
 * 1. Before PLAY: pick the alliance (D-pad left/right) and place the robot FACING AWAY from the drivers.
 * 2. After PLAY: turn the robot about 90 deg with the right stick.
 * 3. Push the LEFT stick away from you. Did the robot move AWAY from you?   A = yes   B = no
 * FAIL: the driver heading in DriveConfig is wrong for this field, or the strafe sign in
 * Drive.driveWithSticks() is flipped. Check with "Test: Drive".
 */
@TeleOp(name = "Field Check 06: FieldCentric", group = "Field Checks")
public class FieldCheck06_FieldCentric extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;
    private Drive drive;
    private boolean answered = false;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        drive = new Drive(hardwareMap);
    }

    @Override
    public void init_loop() {
        if (pad.isDpadLeftPressed() || pad.isDpadRightPressed()) MatchInfo.switchAlliance();
        t.addData("Alliance (D-pad left/right)", MatchInfo.alliance);
        t.addLine("Place the robot FACING AWAY from the drivers, then press PLAY.");
        t.update();
    }

    @Override
    public void start() {
        FieldCheckHelper.start("FieldCheck06_FieldCentric", battery.getVoltage());
        drive.setAlliance(MatchInfo.isRed());
        drive.setStartingPoseFacingAway();
    }

    @Override
    public void loop() {
        boolean yes = pad.isAPressed();
        boolean no = pad.isBPressed();

        drive.driveWithSticks(pad.getLeftStickY(), pad.getLeftStickX(), pad.getRightStickX());
        drive.update();

        if (!answered && (yes || no)) {
            answered = true;
            if (yes) FieldCheckHelper.record(6, FieldCheckHelper.PASS, "driver: moved away");
            else FieldCheckHelper.record(6, FieldCheckHelper.FAIL, "driver: did NOT move away");
        }
        t.addLine("Turn about 90 deg (right stick), then push the LEFT stick away from you.");
        t.addLine("Did the robot move AWAY from you?   A = yes   B = no");
        t.addData("Heading", "%.0f deg", drive.getHeadingDegrees());
        FieldCheckHelper.show(t, 6);
        t.update();
    }

    @Override
    public void stop() {
        drive.stopDrivingItself();
        drive.update();
        FieldCheckHelper.stop();
    }
}
