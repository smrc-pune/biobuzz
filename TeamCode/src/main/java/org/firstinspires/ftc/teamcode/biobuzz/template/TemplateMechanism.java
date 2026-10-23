package org.firstinspires.ftc.teamcode.biobuzz.template;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TemplateConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Mechanism;

/**
 * STUDENTS: copy this file to start a new mechanism.
 *
 * TemplateMechanism.java  -  one motor that can run or stop.                 ROLE: mechanism
 * PATTERN: Adapter (game words for SDK devices) + Template Method (fills in Mechanism's hooks).
 *
 * TO MAKE A NEW MECHANISM (the full checklist is in docs/HOW_TO_ADD_A_MECHANISM.md):
 *   1. Copy this file to mechanisms/Roller.java. Rename the class and the constructor,
 *      and the name in super("template").
 *   2. Add the device name to HardwareNames (with its hub and port, and in ALL_DEVICES).
 *   3. Copy configurations/TemplateConfig.java to RollerConfig.java.
 *   4. Give it to ONE subsystem: in that subsystem's constructor,
 *      roller = new Roller(hardwareMap); addMechanism(roller); then add menu methods that use it.
 *
 * RULES: verbs (run(), stop()) and questions (isRunning()). No states, no timers: a job that
 * takes time goes in a state machine. Nothing moves in the constructor (rule G304.H).
 */
public class TemplateMechanism extends Mechanism {

    private final DcMotorEx motor;
    private double power = 0;          // kept in a field so addLogValues() never reads hardware

    public TemplateMechanism(HardwareMap hardwareMap) {
        super("template");             // the name on telemetry and in the match log
        motor = hardwareMap.get(DcMotorEx.class, HardwareNames.MOTOR_TEMPLATE);
        // Nothing moves here: robots must stay still during INIT (rule G304.H).
    }

    // ---- What it can do (verbs). A FAULTED mechanism ignores them. ----
    public void run() {
        if (isFaulted()) {
            return;
        }
        power = TemplateConfig.RUN_POWER;
        motor.setPower(power);
    }

    // ---- What you can ask it (questions) ----
    public boolean isRunning() {
        return power != 0;
    }

    // ---- Filled in for Mechanism ----
    @Override
    protected void stop() {
        power = 0;
        motor.setPower(0);
    }

    @Override
    protected void addDetails(Telemetry t) {
        t.addData("Template power", "%.2f", power);
    }

    @Override
    protected void addLogValues() {
        MatchLogger.value("template_power", power);
    }
}
