# Your first changes – five exercises

Do them in order. Each one says which files to open and how to check the result. Make a git
branch first (`git checkout -b my-first-changes`), so you can throw it away afterwards.
After each exercise: **Build → Make Project**, deploy, and test.

## Exercise 1 – change a speed

**Goal:** the intake pulls balls in more gently.

1. Open `configurations/IntakeConfig.java`.
2. Change `COLLECT_POWER = 1.0` to `0.7`.
3. Check: run **Test: Intake**, press A. The roller turns slower. Telemetry shows `power 0.70`.

*Try it live too:* in the Dashboard (Configuration → IntakeConfig) change it while Test: Intake
runs. It changes at once, but is lost when the app restarts. That is why you typed it into the file.

## Exercise 2 – change a button

**Goal:** the operator spins the launcher up with X instead of Y.

1. Open `teleop/BioBuzzTeleOp.java`, find `handleOperator()`.
2. Change `boolean spinUp = operator.isYPressed();` to `boolean spinUp = operator.isXPressed();`.
3. Update the three cheat sheets: the comment at the top of the same file,
   `TelemetryConfig.GAMEPAD2_HELP`, and `docs/CONTROLS.md`.
4. Check: BioBuzz TeleOp, gamepad 2 X → the header says "launcher spinning".

## Exercise 3 – add a telemetry line

**Goal:** show how many TIPS we have under the intake line.

1. Open `mechanisms/Intake.java`, find `addDetails(Telemetry t)`.
2. Add a line: `t.addData("Tips so far", MatchInfo.getTipCount());`
   and add `import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchInfo;` at the top
   (Android Studio offers it: Alt+Enter on the red word).
3. Check: BioBuzz TeleOp, gamepad 2 left bumper → the number goes up.
4. Turn the intake's lines off with `TelemetryConfig.INTAKE = false`: your line disappears too.

## Exercise 4 – add a state with a timeout

**Goal:** in `LaunchStateMachine`, wait a moment before the first spin-up ("SETTLING").

1. Open `statemachines/LaunchStateMachine.java`.
2. **1. STATES:** add `SETTLING,   // wait SETTLE_MS before spinning up` to the enum.
3. **Config:** in `configurations/LauncherConfig.java` add `public static double SETTLE_MS = 200;`.
4. **2. COMMANDS:** in `start(...)`, change `goTo(State.SPINNING_UP, ...)` to `goTo(State.SETTLING, "launch " + count);`.
5. **3. UPDATE:** add a case:
   ```java
   case SETTLING:
       // DO: nothing, just wait
       // CHECK + GO (this wait IS its timeout: it always ends)
       if (timer.isTimeUp(LauncherConfig.SETTLE_MS)) goTo(State.SPINNING_UP, "settled");
       break;
   ```
6. Check: **Test: Launch**, A → the Launch line shows `SETTLING`, then `SPINNING_UP`. The
   history line says why each change happened. (The `launch_state` numbers in the match log
   shift by one: write that down in `LLD.md`, section 12.)

## Exercise 5 – add an auto step

**Goal:** wait 1 s after LEAVE before launching (to let the robot settle).

1. Open `auto/BioBuzzAuto.java`.
2. **1. STEPS:** add `SETTLE,   // wait AutoConfig.SETTLE_S after LEAVE` after `LEAVE`.
3. **Config:** in `configurations/AutoConfig.java` add `public static double SETTLE_S = 1;`.
4. In `case LEAVE:` change both `Step.LAUNCH_PRELOADS` to `Step.SETTLE`.
5. Add a case:
   ```java
   case SETTLE:
       if (stepTimer.seconds() >= AutoConfig.SETTLE_S) goToStep(Step.LAUNCH_PRELOADS, "settled");
       break;
   ```
6. Check: run BioBuzz Auto with the robot on blocks (wheels off the floor). The Step line shows
   SETTLE for 1 s. Update the step table in `LLD.md`, section 9.2.
