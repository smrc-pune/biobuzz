package org.firstinspires.ftc.teamcode.trialbot.mechanisms;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/** Remove @Disabled after confirming drivetrain directions and Pinpoint tuning. */


@TeleOp(name = "SMRC-Trialbot TeleOp", group = "BioBuzz")
public class TrialBotTeleOp extends OpMode {
    private Follower follower;
    private LiftMechanism lift;
    private IntakeMechanism intake;
    private ShootingMechanism shooter;
    private double rpm;

    @Override
    public void init() {
        follower = TrialBotConstants.create(hardwareMap)
                .withLogger(log -> telemetry.addData("Pedro", log.toString()));
        // Heading zero is the robot's forward direction at initialization.

        follower.setPose(Pose.zero());

        lift = TrialBotConstants.get_lift(hardwareMap);
        intake = TrialBotConstants.get_intake(hardwareMap);
        shooter = TrialBotConstants.get_shooter(hardwareMap,telemetry);

        rpm = 0;

        telemetry.addLine("Hold left bumper for robot-centric controls. Sticks drive/turn.");
    }

    @Override
    public void loop() {
        double forward = -gamepad1.left_stick_y;
        double lateral = -gamepad1.left_stick_x;
        double turn = -gamepad1.right_stick_x;
        if (gamepad1.left_bumper) {
            follower.manual(forward, lateral, turn);
        } else {
            follower.manual(ManualDrive.fieldCentric(
                    forward, lateral, turn, follower.pose().heading()));
        }

        if (gamepad1.left_trigger_pressed){
           lift.setLeftPosn(0.0);
           lift.setRightPosn(0.0);
        }

        if (gamepad1.right_trigger_pressed){
            lift.setLeftPosn(0.5);
            lift.setRightPosn(0.5);
        }

        if(gamepad1.dpad_up){
            intake.startIntakeMotor();
        }

        if(gamepad1.dpad_down){
            intake.stopIntakeMotor();
        }

        if (gamepad2.left_trigger_pressed){
            rpm-=60.0;
            if (rpm <=0)
                rpm = 0;
        }

        if (gamepad2.right_trigger_pressed){
            rpm+=60.0;
            if(rpm >=4500)
                rpm = 4500;
        }
        shooter.setrpm(rpm);
        if (gamepad2.dpad_up) {
            shooter.increment_p();
        }

        if (gamepad2.dpad_down) {
            shooter.decrement_p();
        }

        if (gamepad2.dpad_right) {
            shooter.increment_i();
        }

        if (gamepad2.dpad_left) {
            shooter.decrement_i();
        }
        if (gamepad2.left_bumper) {
            shooter.increment_f();
        }
        if (gamepad2.right_bumper) {
            shooter.decrement_f();
        }

        follower.update();
     //   telemetry.addData("Drive mode", gamepad1.left_bumper ? "Robot centric" : "Field centric");
    //    telemetry.addData("Pose", follower.pose());
     //   telemetry.update();
    }

    @Override
    public void stop() {
        if (follower != null) follower.stop();
    }
}
