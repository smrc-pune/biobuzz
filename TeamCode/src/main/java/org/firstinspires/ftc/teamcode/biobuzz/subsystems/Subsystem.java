package org.firstinspires.ftc.teamcode.biobuzz.subsystems;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Mechanism;
import org.firstinspires.ftc.teamcode.biobuzz.statemachines.StateMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * MENTORS: you do not need to change this file.
 *
 * Subsystem.java  -  what every subsystem has in common.                      ROLE: base class
 * PATTERN: Composition (a subsystem HAS mechanisms and state machines) + Template Method
 * (this class runs them; your subsystem only adds them and writes its menu).
 *
 *   public class GateSubsystem extends Subsystem {
 *       private final Gate gate;
 *       public GateSubsystem(HardwareMap hardwareMap) {
 *           super("gate");
 *           gate = new Gate(hardwareMap);
 *           addMechanism(gate);                             // this subsystem OWNS the gate
 *       }
 *       public void open() { gate.open(); }                 // the menu: what OpModes may ask for
 *   }
 *
 * A SUBSYSTEM is one job of the robot (collect, shoot, see). It owns its mechanisms: a
 * mechanism belongs to ONE subsystem, and every state machine that uses it lives in that
 * same subsystem. So two commands can only fight over a motor inside one file, and that
 * file is where you stop the fight (cancel the state machine first).
 *
 * A subsystem has NO states of its own: states belong to state machines (a job that takes
 * time). Robot calls update(), addTelemetry() and addLogData() on every subsystem; OpModes
 * call only the menu methods (and stop()).
 *
 * One of the three team base classes, with Mechanism and StateMachine (Requirements v2.8, R3).
 * The lists are ArrayLists so a subsystem can add any number of parts; students never
 * see them, they only call addMechanism(...) and addStateMachine(...).
 */
public class Subsystem {

    private final String name;
    private final List<StateMachine> stateMachines = new ArrayList<>();
    private final List<Mechanism> mechanisms = new ArrayList<>();

    public Subsystem(String name) {
        this.name = name;
    }

    // ==================== FOR YOUR SUBSYSTEM'S CONSTRUCTOR ====================

    /** This subsystem owns this mechanism from now on: it updates, stops, shows and logs it. */
    protected final void addMechanism(Mechanism mechanism) {
        mechanisms.add(mechanism);
    }

    /** This subsystem runs this state machine from now on. Add it AFTER the mechanisms it uses. */
    protected final void addStateMachine(StateMachine stateMachine) {
        stateMachines.add(stateMachine);
    }

    // ==================== HOOK: your subsystem may fill this in ====================

    /** A few words for the telemetry header, for example "intake ON". "" = nothing. */
    public String getStatusText() {
        return "";
    }

    // ==================== FIXED: Robot calls these ====================

    /** One loop: state machines decide first, then every mechanism updates once. */
    public final void update() {
        for (StateMachine stateMachine : stateMachines) {
            stateMachine.update();
        }
        for (Mechanism mechanism : mechanisms) {
            mechanism.runUpdate();
        }
    }

    /** Everything in this subsystem to its safe state: state machines cancelled, mechanisms stopped. */
    public final void stop() {
        for (StateMachine stateMachine : stateMachines) {
            stateMachine.cancel();
        }
        for (Mechanism mechanism : mechanisms) {
            mechanism.runStop();
        }
    }

    public final void addTelemetry(Telemetry t) {
        for (StateMachine stateMachine : stateMachines) {
            stateMachine.addTelemetry(t);
        }
        for (Mechanism mechanism : mechanisms) {
            mechanism.addTelemetry(t);   // each one checks its own TelemetryConfig switch
        }
    }

    /** Match-log columns: every mechanism's, then every state machine's. */
    public final void addLogData() {
        for (Mechanism mechanism : mechanisms) {
            mechanism.addLogData();
        }
        for (StateMachine stateMachine : stateMachines) {
            stateMachine.addLogData();
        }
    }

    /** The names of this subsystem's FAULTED mechanisms, for example "feeder launcher", or "". */
    public final String getFaultedNames() {
        String names = "";
        for (Mechanism mechanism : mechanisms) {
            if (mechanism.isFaulted()) {
                names += mechanism.getName() + " ";
            }
        }
        return names.trim();
    }

    public final String getName() {
        return name;
    }
}
