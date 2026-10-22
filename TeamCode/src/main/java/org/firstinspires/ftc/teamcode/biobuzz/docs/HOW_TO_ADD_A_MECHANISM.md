# How to add a mechanism (and a state machine)

Example: a **Gate** – one servo that opens and closes.

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
5. **Two edits in `Robot.java`** (look for `ADD A MECHANISM HERE`):
   a field `private final Gate gate;` and `gate = new Gate(hardwareMap);`, then its name in the list:
   `mechanisms = new Mechanism[] { intake, feeder, launcher, hiveCamera, frontCamera, gate };`
   Now update, stop-all, telemetry and the match log include it.
6. **Robot methods** (look for `ADD A ROBOT METHOD HERE`): `public void openGate() { gate.open(); }`.
   If a state machine also uses the gate, cancel it first in that method.
7. **A test.** Copy `template/TemplateMechanismTest.java` to `testing/GateTest.java`; change
   `@TeleOp(name = "Test: Gate", ...)`, **delete the `@Disabled` line**, build `new Gate(hardwareMap)`.
8. **Buttons.** In `teleop/BioBuzzTeleOp.java` `handleOperator()`: read the button once into a
   `boolean`, then call the Robot method. Update the three cheat sheets (`CONTROLS.md`).
9. **Docs.** `HLD.md` (robot table), `LLD.md` (section 5), the README file list.

## Part 2 – a state machine

Example: **"open the gate, wait, close it"** as `GatePulseStateMachine`.

1. Copy `template/TemplateStateMachine.java` to `statemachines/GatePulseStateMachine.java`.
   Keep the four sections in order: 1. STATES, 2. COMMANDS, 3. update(), 4. BOOKKEEPING.
2. List the states (`IDLE`, `OPEN`), one comment each. Every state that waits gets a timeout
   from a `...Config` (`GateConfig.OPEN_MS`), and goes to a safe state with a reason.
3. The constructor takes the mechanisms it uses: `public GatePulseStateMachine(Gate gate)`.
   It never calls `gate.update()` (Robot does), another state machine, or Robot.
4. In `Robot.java` (look for `ADD A STATE MACHINE HERE`): a field, build it after the
   mechanisms (`gatePulse = new GatePulseStateMachine(gate);`), and one line each in
   `update()` (section 1: `gatePulse.update();`), `stopMechanisms()` (`gatePulse.cancel();`),
   the log (`gatePulse.addLogData();`) and telemetry (with a `TelemetryConfig` switch).
5. A Robot start method: `public void pulseGate() { gatePulse.start(); }`. If another state
   machine uses the gate, cancel it first: Robot is the only place that knows about conflicts.
6. A test OpMode (`Test: Gate Pulse`) that builds `Robot`: A = start, B = cancel; show the
   state and history.
7. Docs: `LLD.md` section 7, with a state table (DO, CHECK → GO, timeout).

The full checklists are also in `LLD.md`, section 11.
