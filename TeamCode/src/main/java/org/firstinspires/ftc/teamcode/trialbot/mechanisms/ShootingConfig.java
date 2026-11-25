package org.firstinspires.ftc.teamcode.trialbot.mechanisms;
import com.pedropathing.config.ConfigVar;
import com.pedropathing.config.Configuration;
import com.qualcomm.robotcore.hardware.DcMotor;


public final class ShootingConfig {
    public final ConfigVar<String> leftMotorName = ConfigVar.required();
    public final ConfigVar<String> rightMotorName  = ConfigVar.required();
    public final ConfigVar<DcMotor.Direction> leftMotorDirection = ConfigVar.required();
    public final ConfigVar<DcMotor.Direction> rightMotorDirection = ConfigVar.required();
 //   public final ConfigVar<PIDFCoefficients> pidfCoefficients = ConfigVar.required();
    public final ConfigVar<DcMotor.RunMode> leftRunMode = ConfigVar.required();
    public final ConfigVar<DcMotor.RunMode> rightRunMode = ConfigVar.required();
    public final ConfigVar<Double> leftTicksPerRev = ConfigVar.required();
    public final ConfigVar<Double> rightTicksPerRev = ConfigVar.required();




    public ShootingConfig(Configuration<ShootingConfig> config) {
        config.configure(this);
    }

}