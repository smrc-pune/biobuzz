# How to add a mechanism (and a state machine, and a subsystem)

Example: a **Gate** – one servo that opens and closes. It keeps balls in the hopper, so
it belongs to the **shooter** job: it goes in `ShooterSubsystem`.

```
  OpMode  --robot.shooter.openGate()-->  ShooterSubsystem  --gate.open()-->  Gate (mechanism)
```

**First decide: which subsystem owns it?** A mechanism belongs to ONE subsystem: the one
whose job it helps (`intake`, `shooter`, `vision`). Only a new job of the robot (for
example climbing) gets a new subsystem: see Part 3.

## Part 1 – a mechanism

1. **Copy the template.** Copy `template/TemplateMechanism.java` to `mechanisms/Gate.java`.
   Change the package line to `...biobuzz.mechanisms`, rename the class and the constructor to
   `Gate`, and `super("template")` to `super("gate")`. Keep `extends Mechanism`.
2. **The device name.** In `configurations/HardwareNames.java`, add
   `public static final String SERVO_GATE = "gate";   // Expansion Hub, servo port 1`,
   add `SERVO_GATE` to `ALL_DEVICES`, and update the motor/servo count in the header (at most 8 each).
   Add the same name in the robot configuration on the Driver Station.
3. **The numbers.** Copy `configurations/TemplateConfig.java` to `GateConfig.java`:
   `OPEN_POSITION`, `CLOSED_POSITION`. Keep `@Config` and `public static`.
4. **Write the verbs and questions.** `open()`, `close()`, `isOpen()`. Each verb starts with
   `if (isFaulted()) return;`. Get the servo in the constructor with
   `hardwareMap.get(Servo.class, HardwareNames.SERVO_GATE)` and **move nothing there**.
   Fill in the hooks you need: `stop()` (the safe state), `addDetails(t)`, `addLogValues()`
   (`MatchLogger.value("gate_open", open)`), and `isTelemetryOn()` (add `GATE` to `TelemetryConfig`).
5. **Give it to its subsystem.** In `subsystems/ShooterSubsystem.java`: a field
   `private final Gate gate;` and, in the constructor, `gate = new Gate(hardwareMap);` and
   `addMechanism(gate);`. That is all: now update, stop-all, telemetry and the match log include it.
   `Robot.java` does not change.
6. **Menu methods** in the same subsystem (look for `ADD A MENU METHOD HERE`):
   `public void openGate() { gate.open(); }`. If a state machine also uses the gate, cancel
   it first in that method (`launchStateMachine.cancel();`).
7. **A test.** Copy `template/TemplateMechanismTest.java` to `testing/GateTest.java`; change
   `@TeleOp(name = "Test: Gate", ...)`, **delete the `@Disabled` line**, build `new Gate(hardwareMap)`.
8. **Buttons.** In `teleop/BioBuzzTeleOp.java` `handleOperator()`: read the button once into a
   `boolean`, then call the menu method: `if (openGate) robot.shooter.openGate();`.
   Update the three cheat sheets (`CONTROLS.md`).
9. **Docs.** `HLD.md` (robot table), `LLD.md` (sections 3 and 5), the README file list.

## Part 2 – a state machine

Example: **"open the gate, wait, close it"** as `GatePulseStateMachine`.

1. Copy `template/TemplateStateMachine.java` to `statemachines/GatePulseStateMachine.java`.
   Keep `extends StateMachine` and all five `@Override` methods (`update()`, `cancel()`,
   `isBusy()`, `addTelemetry()`, `addLogData()`): a missing `cancel()` is not caught by the
   build, and stop-all would not stop it. Keep the four sections in order:
   1. STATES, 2. COMMANDS, 3. update(), 4. BOOKKEEPING.
2. List the states (`IDLE`, `OPEN`), one comment each. Every state that waits gets a timeout
   from a `...Config` (`GateConfig.OPEN_MS`), and goes to a safe state with a reason.
3. The constructor takes the mechanisms it uses: `public GatePulseStateMachine(Gate gate)`.
   It never calls `gate.update()` (its subsystem does), another state machine, or Robot.
   Its `addTelemetry()` starts with its own switch: `if (!TelemetryConfig.GATE_PULSE) return;`.
4. In the subsystem that owns the gate (`ShooterSubsystem`): a field, and in the constructor
   AFTER the mechanisms: `gatePulse = new GatePulseStateMachine(gate);` and
   `addStateMachine(gatePulse);`. That puts it in update (before the mechanisms), stop-all, telemetry and the log.
5. A menu method: `public void pulseGate() { gatePulse.start(); }`. If another state machine
   uses the gate, cancel it first: the subsystem is the only place that knows about conflicts.
   A state machine may only use mechanisms of ITS subsystem. If it needs two subsystems'
   mechanisms, those two jobs belong together: ask a mentor before merging them.
6. A test OpMode (`Test: Gate Pulse`) that builds `Robot`: A = start, B = cancel; show the
   state and history.
7. Docs: `LLD.md` section 7, with a state table (DO, CHECK → GO, timeout).

## Part 3 – a subsystem (a new job of the robot)

Example: a **Climber** with one motor.

1. Make the mechanism (Part 1, steps 1–4): `mechanisms/ClimbMotor.java`.
2. Copy `template/TemplateSubsystem.java` to `subsystems/ClimberSubsystem.java`. Rename the
   class, the constructor and `super("template")` to `super("climber")`.
3. In its constructor: `addMechanism(...)` for what it owns, then `addStateMachine(...)`.
4. The menu: `climb()`, `lower()`, `isClimbing()`. Optional: `getStatusText()` for the header
   (`"climber UP"`).
5. In `Robot.java` (look for `ADD A SUBSYSTEM HERE`): `public final ClimberSubsystem climber;`,
   `climber = new ClimberSubsystem(hardwareMap);`, and add `climber` to the `subsystems` list.
6. OpModes call `robot.climber.climb()`.

The full checklists are also in `LLD.md`, section 11.
