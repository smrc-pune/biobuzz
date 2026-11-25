package org.firstinspires.ftc.teamcode.trialbot.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class ShootingMechanism {
    public final ShootingConfig config;
    DcMotorEx lShootMotor;
    DcMotorEx rShootMotor;
    double leftTicksPerRev;
    double rightTicksPerRev;
    double velocity;
    Telemetry telemetry;
    PIDFCoefficients pidf;
    double p,i,d,f;

    public ShootingMechanism(HardwareMap map, ShootingConfig config, Telemetry telemetry) {
        this.config = config;
        this.telemetry = telemetry;
        lShootMotor = map.get(DcMotorEx.class,config.leftMotorName.get());
        lShootMotor.setDirection(config.leftMotorDirection.get());
        lShootMotor.setMode(config.leftRunMode.get());
   //     lShootMotor.setPIDFCoefficients(config.leftRunMode.get(),config.pidfCoefficients.get());
        lShootMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        rShootMotor = map.get(DcMotorEx.class,config.rightMotorName.get());
        rShootMotor.setDirection(config.rightMotorDirection.get());
     //   rShootMotor.setPIDFCoefficients(config.rightRunMode.get(),config.pidfCoefficients.get());
        rShootMotor.setMode(config.rightRunMode.get());
        rShootMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        leftTicksPerRev = config.leftTicksPerRev.get();
        rightTicksPerRev = config.rightTicksPerRev.get();

        pidf = lShootMotor.getPIDFCoefficients(config.leftRunMode.get());

        f = (32767.0*60.0)/(4500.0 * leftTicksPerRev);
        p= 0.1 *f;
        i= 0.1 *f;
        d= 0.01*f;
    }

    public void setrpm(double rpm){
            velocity = (rpm * leftTicksPerRev)/60.0;

            if(rpm == 0){
                stopMotors();
            }
            else{
                SetMotors (velocity);
            }
        telemetry.addData("Shooter RPM: ", rpm);
        telemetry.addData("PIDF",  pidf.toString());
        telemetry.update();
    }

    public void increment_p(){
        pidf.p += 0.1*p;
        rShootMotor.setPIDFCoefficients(config.rightRunMode.get(),pidf);
        lShootMotor.setPIDFCoefficients(config.leftRunMode.get(),pidf);
    }

    public void decrement_p(){
        pidf.p -= 0.1*p;
        if(pidf.p <=0 )
            pidf.p =0.0;
        rShootMotor.setPIDFCoefficients(config.rightRunMode.get(),pidf);
        lShootMotor.setPIDFCoefficients(config.leftRunMode.get(),pidf);
    }

    public void increment_i(){
        pidf.i += 0.1*i;
        rShootMotor.setPIDFCoefficients(config.rightRunMode.get(),pidf);
        lShootMotor.setPIDFCoefficients(config.leftRunMode.get(),pidf);
    }

    public void decrement_i(){
        pidf.i -= 0.1*i;
        if(pidf.i <=0)
            pidf.i = 0.0;
        rShootMotor.setPIDFCoefficients(config.rightRunMode.get(),pidf);
        lShootMotor.setPIDFCoefficients(config.leftRunMode.get(),pidf);
    }

    public void increment_f(){
        pidf.f += 0.1*f;
        rShootMotor.setPIDFCoefficients(config.rightRunMode.get(),pidf);
        lShootMotor.setPIDFCoefficients(config.leftRunMode.get(),pidf);
    }

    public void decrement_f(){
        pidf.f -= 0.1*f;
        if(pidf.f <=0)
            pidf.f = 0.0;
        rShootMotor.setPIDFCoefficients(config.rightRunMode.get(),pidf);
        lShootMotor.setPIDFCoefficients(config.leftRunMode.get(),pidf);
    }

    public void increment_d(){
        pidf.d += 0.1*d;
        rShootMotor.setPIDFCoefficients(config.rightRunMode.get(),pidf);
        lShootMotor.setPIDFCoefficients(config.leftRunMode.get(),pidf);
    }

    public void decrement_d(){
        pidf.d -= 0.1*d;
        if(pidf.d <=0)
            pidf.d = 0.0;
        rShootMotor.setPIDFCoefficients(config.rightRunMode.get(),pidf);
        lShootMotor.setPIDFCoefficients(config.leftRunMode.get(),pidf);
    }

    private void stopMotors(){
        lShootMotor.setPower(0);
        rShootMotor.setPower(0);
    }

    private void SetMotors(double velocity){
        double curr_power;
        lShootMotor.setVelocity(velocity);
        rShootMotor.setVelocity(velocity);
      /*  boolean target_reached= false;

        while(!target_reached) {
            curr_power = lShootMotor.getPower();
            rShootMotor.setPower(curr_power);
            if(Math.abs(lShootMotor.getVelocity() -velocity)<2)
                target_reached = true;
        }*/

    }

}
