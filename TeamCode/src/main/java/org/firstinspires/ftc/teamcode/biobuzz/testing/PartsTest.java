package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.ControlsConfig;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.HardwareNames;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.BatteryMonitor;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * MENTORS: you do not need to change this file.
 *
 * PartsTest.java  -  move ONE motor or servo at a time, to check wiring and direction.   ROLE: OpMode (test)
 *
 * It builds nothing but the devices in HardwareNames.ALL_DEVICES, found with
 * hardwareMap.tryGet(...), so a missing device is listed instead of stopping the OpMode.
 * Nothing moves until you push a stick, and only the device you picked moves.
 *
 * CONTROLS (gamepad 1):
 *   D-pad up / down   pick the device (the one that was moving stops)
 *   Left stick Y      MOTOR or CONTINUOUS SERVO: power, up to ControlsConfig.PARTS_TEST_MAX_POWER
 *   Right stick Y     SERVO: move the position slowly (ControlsConfig.PARTS_TEST_SERVO_STEP per loop)
 *   X                 SERVO: go to the middle (0.5)
 *
 * A continuous servo (configured as "Continuous Rotation Servo") spins like a small
 * motor: our windmill and the two intake side wheels. A plain servo moves to a position.
 *
 * WHAT TO WATCH: push the left stick UP. A motor or continuous servo should turn the way
 * that moves the mechanism "forward": intake roller and side wheels pull balls IN, the
 * windmill pushes balls TOWARD the flywheel, the launcher throws OUT. If it turns the
 * other way while its REVERSED value in HardwareNames is false, set it to true (and the
 * other way round). This test does not use those values: it shows how each device turns
 * as wired. A motor's encoder count should go UP. "MISSING" means the
 * name in HardwareNames does not match the Robot Controller configuration.
 */
@TeleOp(name = "Test: Parts", group = "Tests")
public class PartsTest extends OpMode {

    private Telemetry t;
    private GamepadInput pad;
    private BatteryMonitor battery;

    // One slot per name in HardwareNames.ALL_DEVICES. A slot is null when it is not that kind.
    private final String[] names = HardwareNames.ALL_DEVICES;
    private final DcMotorEx[] motors = new DcMotorEx[names.length];
    private final Servo[] servos = new Servo[names.length];
    private final CRServo[] spinners = new CRServo[names.length];   // continuous servos
    private final boolean[] found = new boolean[names.length];
    private int selected = 0;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        battery = new BatteryMonitor(hardwareMap);
        for (int i = 0; i < names.length; i++) {
            motors[i] = hardwareMap.tryGet(DcMotorEx.class, names[i]);
            servos[i] = hardwareMap.tryGet(Servo.class, names[i]);
            spinners[i] = hardwareMap.tryGet(CRServo.class, names[i]);
            found[i] = hardwareMap.tryGet(HardwareDevice.class, names[i]) != null;
            // Nothing moves here: we only found the devices (rule G304.H).
        }
    }

    @Override
    public void init_loop() {
        showDeviceList();
        t.update();
    }

    @Override
    public void start() {
        MatchLogger.start("PartsTest", battery.getVoltage());
    }

    @Override
    public void loop() {
        boolean next = pad.isDpadDownPressed();
        boolean previous = pad.isDpadUpPressed();
        boolean servoMiddle = pad.isXPressed();

        if (next || previous) {
            stopSelected();
            if (next) {
                selected = (selected + 1) % names.length;
            } else {
                selected = (selected - 1 + names.length) % names.length;
            }
            MatchLogger.event("PARTS", "selected " + names[selected]);
        }

        DcMotorEx motor = motors[selected];
        Servo servo = servos[selected];
        CRServo spinner = spinners[selected];
        if (motor != null) {
            motor.setPower(pad.getLeftStickY() * ControlsConfig.PARTS_TEST_MAX_POWER);
        }
        if (spinner != null) {
            spinner.setPower(pad.getLeftStickY() * ControlsConfig.PARTS_TEST_MAX_POWER);
        }
        if (servo != null) {
            double position = servo.getPosition() + pad.getRightStickY() * ControlsConfig.PARTS_TEST_SERVO_STEP;
            if (servoMiddle) {
                position = 0.5;
            }
            servo.setPosition(Math.max(0, Math.min(1, position)));
        }

        showSelected();
        showDeviceList();
        t.update();
    }

    @Override
    public void stop() {
        stopSelected();
        MatchLogger.stop("");
    }

    /** The motor or continuous servo that was moving stops when another device is picked, and at STOP. */
    private void stopSelected() {
        if (motors[selected] != null) {
            motors[selected].setPower(0);
        }
        if (spinners[selected] != null) {
            spinners[selected].setPower(0);
        }
    }

    private void showSelected() {
        t.addData("SELECTED", names[selected]);
        DcMotorEx motor = motors[selected];
        if (motor != null) {
            String direction = "forward";
            if (motor.getDirection() == DcMotor.Direction.REVERSE) {
                direction = "REVERSED";
            }
            t.addData("Motor power", String.format("%.2f  (%s)", motor.getPower(), direction));
            t.addData("Encoder", String.format("%d ticks, %.0f ticks/s", motor.getCurrentPosition(), motor.getVelocity()));
            t.addData("Current", String.format("%.2f A", motor.getCurrent(CurrentUnit.AMPS)));
            t.addLine("Left stick Y = power");
        } else if (spinners[selected] != null) {
            CRServo spinner = spinners[selected];
            String direction = "forward";
            if (spinner.getDirection() == DcMotorSimple.Direction.REVERSE) {
                direction = "REVERSED";
            }
            t.addData("Continuous servo power", String.format("%.2f  (%s)", spinner.getPower(), direction));
            t.addLine("Left stick Y = power. No encoder: watch which way it turns.");
        } else if (servos[selected] != null) {
            t.addData("Servo position", String.format("%.3f", servos[selected].getPosition()));
            t.addLine("Right stick Y = move slowly, X = middle (0.5)");
            t.addLine("Spins nonstop? It is a continuous servo: configure it as Continuous Rotation Servo.");
        } else if (found[selected]) {
            t.addLine("Found, but not a motor or servo: nothing to move.");
        } else {
            t.addLine("MISSING: check the name in HardwareNames and the robot configuration.");
        }
        t.addData("Battery", String.format("%.1f V", battery.getVoltage()));
    }

    private void showDeviceList() {
        t.addLine("--- D-pad up/down picks a device ---");
        for (int i = 0; i < names.length; i++) {
            String kind = "MISSING";
            if (motors[i] != null) {
                kind = "motor";
            } else if (spinners[i] != null) {
                kind = "continuous servo";
            } else if (servos[i] != null) {
                kind = "servo";
            } else if (found[i]) {
                kind = "sensor";
            }
            String marker = "   ";
            if (i == selected) {
                marker = ">> ";
            }
            t.addLine(marker + names[i] + "  (" + kind + ")");
        }
    }
}
