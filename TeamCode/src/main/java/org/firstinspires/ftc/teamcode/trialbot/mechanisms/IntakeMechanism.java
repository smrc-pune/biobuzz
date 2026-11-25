package org.firstinspires.ftc.teamcode.trialbot.mechanisms;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class IntakeMechanism {
    public final IntakeConfig config;
    DcMotor intakeMotor;

    public IntakeMechanism(HardwareMap map, IntakeConfig config) {
        this.config = config;
        intakeMotor = map.get(DcMotor.class,config.intakeMotorName.get());
        intakeMotor.setDirection(config.intakeMotorDirection.get());
    }

    public void startIntakeMotor(){
        intakeMotor.setPower(1.0);
    }

    public void stopIntakeMotor() {
        intakeMotor.setPower(0);
    }

}

