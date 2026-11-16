package org.firstinspires.ftc.teamcode.biobuzz.mechanisms;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FeederConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * STUDENTS: you will change this file.
 *
 * Feeder.java  -  the WINDMILL that pushes balls from the hopper into the flywheel.  ROLE: mechanism
 * PATTERN: Adapter + Template Method.
 *
 * THE HARDWARE (goBILDA StarterBot): a 4-paddle windmill turned by a CONTINUOUS servo.
 * While it turns, each paddle pushes the next ball into the flywheel. When it stops, the
 * paddles keep the balls back.
 *
 *   feed()      turn the windmill: balls go into the flywheel, one after another
 *   hold()      stop the windmill: balls stay in the hopper (the safe state)
 *   unjam()     turn it backwards a little, to free a stuck ball
 * WHEN to feed is not decided here: LaunchStateMachine feeds only while the flywheel is
 * fast enough, and for how long (LauncherConfig.FEED_TOTAL_MS).
 *
 * TRACING A BUG: the windmill turns the wrong way -> flip SERVO_FEEDER_REVERSED in
 * HardwareNames. Balls jam or fly weakly -> FeederConfig.FEED_POWER, with "Test: Feeder".
 */
public class Feeder extends Mechanism {

    private final CRServo windmill;
    private double power = 0;    // what we asked for; kept so the log never reads hardware

    public Feeder(HardwareMap hardwareMap) {
        super("feeder");
        windmill = hardwareMap.get(CRServo.class, HardwareNames.SERVO_FEEDER);
        if (HardwareNames.SERVO_FEEDER_REVERSED) {
            windmill.setDirection(DcMotorSimple.Direction.REVERSE);
        } else {
            windmill.setDirection(DcMotorSimple.Direction.FORWARD);
        }
        // Nothing moves here: robots must stay still during INIT (rule G304.H).
    }

    // ---- What the feeder can do ----
    public void feed() {
        setPower(FeederConfig.FEED_POWER);
    }

    public void hold() {
        setPower(0);
    }

    public void unjam() {
        setPower(FeederConfig.UNJAM_POWER);
    }

    // ---- What you can ask it ----
    public boolean isFeeding() {
        return power > 0;
    }

    public double getPower() {
        return power;
    }

    private void setPower(double newPower) {
        if (isFaulted()) {
            return;
        }
        if (newPower == power) {
            return;   // a servo command is a hub write: only send changes
        }
        MatchLogger.event("FEEDER", String.format("power %.2f", newPower));
        power = newPower;
        windmill.setPower(power);
    }

    // ---- Filled in for Mechanism ----
    @Override
    protected void stop() {
        if (power != 0) {
            MatchLogger.event("FEEDER", "off");
        }
        power = 0;
        windmill.setPower(0);
    }

    @Override
    protected void addDetails(Telemetry t) {
        String what = "HOLDING";
        if (power > 0) {
            what = "FEEDING";
        }
        if (power < 0) {
            what = "UNJAMMING";
        }
        t.addData("Feeder", what + String.format("  (windmill power %.2f)", power));
    }

    @Override
    protected void addLogValues() {
        MatchLogger.value("feeder_power", power);
    }

    @Override
    protected boolean isTelemetryOn() {
        return TelemetryConfig.FEEDER;
    }
}
