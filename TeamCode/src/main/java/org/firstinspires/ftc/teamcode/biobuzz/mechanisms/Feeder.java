package org.firstinspires.ftc.teamcode.biobuzz.mechanisms;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FeederConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * STUDENTS: you will change this file.
 *
 * Feeder.java  -  the servo that pushes ONE ball from the hopper into the flywheel.   ROLE: mechanism
 * PATTERN: Adapter + Template Method.
 *
 *   hold()      keep the next ball back (the safe position)
 *   feedOne()   push the next ball into the flywheel
 * How LONG to push is not decided here: LaunchStateMachine times it (FEED_MS, RECOVER_MS).
 * TODO(BioBuzz): a servo is a first guess. If the feeder becomes a motor, keep these verbs.
 *
 * TRACING A BUG: the ball is not pushed far enough -> FeederConfig.FEED_POSITION, found with
 * "Test: Feeder".
 */
public class Feeder extends Mechanism {

    private final Servo pusher;
    private boolean feeding = false;
    private double position = -1;    // -1 = not commanded yet (a servo does not move until told)

    public Feeder(HardwareMap hardwareMap) {
        super("feeder");
        pusher = hardwareMap.get(Servo.class, HardwareNames.SERVO_FEEDER);
        // Nothing moves here: the servo stays where it is until the first command (rule G304.H).
    }

    // ---- What the feeder can do ----
    public void feedOne() {
        if (isFaulted()) return;
        feeding = true;
        moveTo(FeederConfig.FEED_POSITION);
    }

    public void hold() {
        if (isFaulted()) return;
        feeding = false;
        moveTo(FeederConfig.HOLD_POSITION);
    }

    // ---- What you can ask it ----
    public boolean isFeeding() { return feeding; }

    private void moveTo(double newPosition) {
        if (newPosition == position) return;   // a servo command is a hub write: only send changes
        position = newPosition;
        pusher.setPosition(position);
    }

    // ---- Filled in for Mechanism ----
    @Override
    protected void stop() {
        feeding = false;
        moveTo(FeederConfig.HOLD_POSITION);
    }

    @Override
    protected void addDetails(Telemetry t) {
        String what = "HOLDING";
        if (feeding) what = "FEEDING";
        t.addData("Feeder", what + String.format("  (servo %.2f)", position));
    }

    @Override
    protected void addLogValues() { MatchLogger.value("feeder_feeding", feeding); }

    @Override
    protected boolean isTelemetryOn() { return TelemetryConfig.FEEDER; }
}
