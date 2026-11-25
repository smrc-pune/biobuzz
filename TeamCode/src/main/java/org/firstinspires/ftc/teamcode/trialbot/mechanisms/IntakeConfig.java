package org.firstinspires.ftc.teamcode.trialbot.mechanisms;

import com.pedropathing.config.ConfigVar;
import com.pedropathing.config.Configuration;
import com.qualcomm.robotcore.hardware.DcMotor;


public class IntakeConfig {
    public final ConfigVar<String> intakeMotorName = ConfigVar.required();
    public final ConfigVar<DcMotor.Direction> intakeMotorDirection = ConfigVar.required();

    public IntakeConfig(Configuration<IntakeConfig> config) {
        config.configure(this);
    }

}

