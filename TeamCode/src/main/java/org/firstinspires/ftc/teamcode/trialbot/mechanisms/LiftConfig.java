package org.firstinspires.ftc.teamcode.trialbot.mechanisms;


import com.pedropathing.config.ConfigVar;
import com.pedropathing.config.Configuration;
import com.qualcomm.robotcore.hardware.Servo;

public final class LiftConfig {
    public final ConfigVar<String> leftServoName = ConfigVar.required();
    public final ConfigVar<String> rightServoName  = ConfigVar.required();
    public final ConfigVar<Servo.Direction> leftServoDirection = ConfigVar.required();
    public final ConfigVar<Servo.Direction> rightServoDirection = ConfigVar.required();

    public final ConfigVar<Double> leftUpPosn = ConfigVar.required();
    public final ConfigVar<Double> rightUpPosn = ConfigVar.required();
    public final ConfigVar<Double> leftDownPosn = ConfigVar.required();
    public final ConfigVar<Double> rightDownPosn = ConfigVar.required();

    public LiftConfig(Configuration<LiftConfig> config) {
        config.configure(this);
    }

}