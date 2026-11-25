package org.firstinspires.ftc.teamcode.trialbot.mechanisms;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;


public class LiftMechanism {
    public final LiftConfig config;
    Servo leftLift;
    Servo rightLift;
    double leftUpPosn;
    double rightUpPosn;
    double leftDownPosn;
    double rightDownPosn;

    public LiftMechanism(HardwareMap map, LiftConfig config) {
        this.config = config;
        leftLift = map.get(Servo.class,config.leftServoName.get());
        rightLift = map.get(Servo.class,config.rightServoName.get());
        leftLift.setDirection(config.leftServoDirection.get());
        rightLift.setDirection(config.rightServoDirection.get());
        leftUpPosn = config.leftUpPosn.get();
        leftDownPosn = config.leftDownPosn.get();
        rightUpPosn = config.rightUpPosn.get();
        rightDownPosn = config.rightDownPosn.get();

    }

    public void setLeftPosn(double posn){
        leftLift.setPosition(posn);
    }

    public void setRightPosn(double posn) {
        rightLift.setPosition(posn);
    }

    public void setLiftDown() {
        leftLift.setPosition(leftDownPosn);
        rightLift.setPosition(rightDownPosn);
    }

    public void setLiftUp() {
        leftLift.setPosition(leftUpPosn);
        rightLift.setPosition(rightUpPosn);
    }
}










