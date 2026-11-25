package org.firstinspires.ftc.teamcode.trialbot.mechanisms;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class TrialBotConstants {

    public static MecanumConfig drivetrainConfig = new MecanumConfig(c -> {
        c.frontLeftName.set("LFMotor");
        c.frontRightName.set("RFMotor");
        c.backLeftName.set("LBMotor");
        c.backRightName.set("RBMotor");
        c.frontLeftDirection.set(DcMotorSimple.Direction.REVERSE);
        c.frontRightDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backLeftDirection.set(DcMotorSimple.Direction.REVERSE);
        c.backRightDirection.set(DcMotorSimple.Direction.FORWARD);
        c.powerThreshold.set(0.3);
    });


    public static PinpointConfig localizerConfig = new PinpointConfig(c -> {
        c.name.set("PinPointOdometry");
        c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        c.globalDistanceUnit.set(DistanceUnit.INCH);
        c.offsetUnits.set(DistanceUnit.MM);
        c.xPodOffset.set(-124.00);
        c.yPodOffset.set(-56.00);
        c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
    });


    public static ForesightConfig foresightConfig = new ForesightConfig(
            c -> {
                Controller primaryTranslationalForward = Controller.proportional(0.3337948140459428);
                Controller secondaryTranslationalForward = Controller.proportional(0.12332826462827799);
                Controller primaryTranslationalLateral = Controller.proportional(0.4602368411608494);
                Controller secondaryTranslationalLateral = Controller.proportional(0.17004521505404704);

                c.forwardTranslational.set(Controller.piecewise(secondaryTranslationalForward).put(2.5, primaryTranslationalForward));
                c.strafeTranslational.set(Controller.piecewise(secondaryTranslationalLateral).put(2.5, primaryTranslationalLateral));

                c.coast.set(Controller.proportionalFeedforward(0.01578921947621806));
                c.brake.set(Controller.proportionalFeedforward(0.013420836554785352));

                c.headingFeedback.set(Controller.proportional(3.560757681436778));
                c.headingBrakeCoefficients.set(Vector2D.cartesian(0.04733517648256952, 0.010077483658310588));

                c.linearBrakeCoefficients.set(Matrix.diag(0.09679560460225536, 0.01620660307098713));
                c.quadraticBrakeCoefficients.set(Matrix.diag(0.0023906559535445838, -5.788744824356805E-5));

                c.maxAchievableForwardVelocity.set(54.45150060882374);
                c.maxAchievableStrafeVelocity.set(46.741885332505845);
                c.naturalForwardDeceleration.set(22.28800474022436);
                c.naturalStrafeDeceleration.set(28.179704113900208);
            }
    );

    // Library defaults are a starting point; replace with Foresight AutoTune output.
    //   public static ForesightConfig foresightConfig = new ForesightConfig(c -> {});

    public static Follower create(HardwareMap h) {
        return new Follower(
                new PinpointLocalizer(h, localizerConfig),
                new Mecanum(h, drivetrainConfig),
                new Foresight(foresightConfig));
    }

    public static LiftConfig liftConfig = new LiftConfig(
            c -> {
                c.leftServoName.set("LeftLiftServo");
                c.rightServoName.set("RightLiftServo");
                c.leftServoDirection.set(Servo.Direction.REVERSE);
                c.rightServoDirection.set(Servo.Direction.FORWARD);
                c.leftUpPosn.set(0.5);
                c.rightUpPosn.set(0.5);
                c.leftDownPosn.set(0.0);
                c.rightDownPosn.set(0.0);
            }
    );


    public static LiftMechanism get_lift(HardwareMap h){
        return new LiftMechanism(h,liftConfig);
    }

    public static IntakeConfig intakeConfig = new IntakeConfig(
            c -> {
                c.intakeMotorName.set("IntakeMotor");
                c.intakeMotorDirection.set(DcMotorSimple.Direction.REVERSE);
            }
    );

    public static IntakeMechanism get_intake(HardwareMap h){
        return new IntakeMechanism(h,intakeConfig);
    }

    public static ShootingConfig shootConfig = new ShootingConfig(
            c -> {
                PIDFCoefficients pidf = new PIDFCoefficients(9.0, 0.8, 0.2, 12);
                c.leftMotorName.set("LShooter");
                c.leftMotorDirection.set(DcMotor.Direction.FORWARD);
                c.leftRunMode.set(DcMotor.RunMode.RUN_USING_ENCODER);
                c.leftTicksPerRev.set(28.0);

                c.rightMotorName.set("RShooter");
                c.rightMotorDirection.set(DcMotor.Direction.REVERSE);
                //          c.rightRunMode.set(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                c.rightRunMode.set(DcMotor.RunMode.RUN_USING_ENCODER);
                //            c.pidfCoefficients.set(pidf);
                c.rightTicksPerRev.set(28.0);
            }
    );

    public static ShootingMechanism get_shooter(HardwareMap h, Telemetry t){
        return new ShootingMechanism(h,shootConfig, t);
    }

}
