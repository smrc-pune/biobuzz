package org.firstinspires.ftc.teamcode.biobuzz.template;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.biobuzz.subsystems.Subsystem;

/**
 * STUDENTS: copy this file to start a new subsystem.
 *
 * TemplateSubsystem.java  -  one job of the robot, and its menu for OpModes.  ROLE: subsystem
 * PATTERN: Composition (it HAS mechanisms and state machines) + Template Method (Subsystem
 * updates, stops, shows and logs them for you).
 *
 * TO MAKE A NEW SUBSYSTEM (the full checklist is in docs/HOW_TO_ADD_A_MECHANISM.md):
 *   1. Copy this file to subsystems/GateSubsystem.java. Rename the class, the constructor
 *      and the name in super("template").
 *   2. In the constructor, build each part, then addMechanism(...) for each mechanism it
 *      OWNS and addStateMachine(...) for each state machine that uses them.
 *   3. Write the menu: one method for each thing an OpMode may ask for.
 *   4. In Robot.java (look for ADD A SUBSYSTEM HERE): a public field, build it, add it to the list.
 *
 * RULES: a mechanism belongs to ONE subsystem. If a menu command and a state machine both
 * use a mechanism, the command cancels the state machine first (see run() below).
 * Most new mechanisms go in an EXISTING subsystem instead: only make a new one for a new job.
 */
public class TemplateSubsystem extends Subsystem {

    private final TemplateMechanism mechanism;
    private final TemplateStateMachine stateMachine;

    public TemplateSubsystem(HardwareMap hardwareMap) {
        super("template");
        mechanism = new TemplateMechanism(hardwareMap);
        stateMachine = new TemplateStateMachine(mechanism);
        addMechanism(mechanism);
        addStateMachine(stateMachine);
    }

    // ==================== THE MENU ====================

    /** Run the mechanism for TemplateConfig.RUN_TIME_MS, then stop (the state machine times it). */
    public void runForAWhile() { stateMachine.start(); }

    /** Run the mechanism until told to stop. The state machine must not fight it: cancel it first. */
    public void run() {
        stateMachine.cancel();
        mechanism.run();
    }

    // stop() comes from Subsystem: the state machine cancelled, the mechanism stopped.

    public boolean isRunning() { return mechanism.isRunning(); }

    @Override
    public String getStatusText() {
        if (mechanism.isRunning()) return "template ON";
        return "";
    }
}
