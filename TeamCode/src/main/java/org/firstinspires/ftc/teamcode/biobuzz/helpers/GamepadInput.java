package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.biobuzz.configurations.ControlsConfig;

/**
 * GamepadInput.java  -  clean access to ONE gamepad's sticks and buttons.      ROLE: helper
 *
 * It only reads inputs. It does not know what they are used for: buttons are wired
 * to actions in the OpMode (BioBuzzTeleOp.handleDriver() / handleOperator()).
 *
 *   GamepadInput driver   = new GamepadInput(gamepad1);
 *   GamepadInput operator = new GamepadInput(gamepad2);
 *
 * TWO KINDS OF BUTTON READ:
 *   isAPressed()  EDGE-DETECTED: true on the ONE loop where A goes from up to down.
 *                 Holding A does not keep returning true. Use it for "do this once".
 *   isAHeld()     true on every loop while A is down. Use it for "while held".
 * The loop runs far faster than a person can let go of a button, so without edge
 * detection one press would run an action dozens of times.
 *
 * RULE: call each isXPressed() ONCE per loop, and keep the answer in a local boolean.
 * A second call in the same loop returns false, because the press was already used.
 *
 * STICKS: -1.0 to 1.0. The SDK reports "up" as negative; we flip it so up = positive.
 * Triggers are 0.0 to 1.0, and count as "pressed" past ControlsConfig.TRIGGER_THRESHOLD.
 *
 * PS4/PS5 gamepads: Back = Share, Start = Options, A = cross, B = circle,
 * X = square, Y = triangle.
 *
 * TRACING A BUG: if a button does nothing, check it is read only once per loop.
 */
public class GamepadInput {

    // One slot per button, holding whether it was down LAST loop (for edge detection).
    private static final int A = 0;
    private static final int B = 1;
    private static final int X = 2;
    private static final int Y = 3;
    private static final int DPAD_UP = 4;
    private static final int DPAD_DOWN = 5;
    private static final int DPAD_LEFT = 6;
    private static final int DPAD_RIGHT = 7;
    private static final int LEFT_BUMPER = 8;
    private static final int RIGHT_BUMPER = 9;
    private static final int LEFT_TRIGGER = 10;
    private static final int RIGHT_TRIGGER = 11;
    private static final int START = 12;
    private static final int BACK = 13;
    private static final int LEFT_STICK_BUTTON = 14;
    private static final int RIGHT_STICK_BUTTON = 15;
    private final boolean[] wasDown = new boolean[16];

    private final Gamepad gamepad;

    public GamepadInput(Gamepad gamepad) {
        this.gamepad = gamepad;
    }

    /** Edge detection: true only if the button is down now AND was up last time we asked. */
    private boolean isNewPress(int button, boolean isDownNow) {
        boolean newPress = isDownNow && !wasDown[button];
        wasDown[button] = isDownNow;
        return newPress;
    }

    // ========== STICKS (-1.0 to 1.0) ==========
    public double getLeftStickY()  { return -gamepad.left_stick_y; }   // up = positive
    public double getLeftStickX()  { return gamepad.left_stick_x; }    // right = positive
    public double getRightStickY() { return -gamepad.right_stick_y; }  // up = positive
    public double getRightStickX() { return gamepad.right_stick_x; }   // right = positive

    // ========== TRIGGERS (0.0 to 1.0) ==========
    public double getLeftTrigger()  { return gamepad.left_trigger; }
    public double getRightTrigger() { return gamepad.right_trigger; }

    // ========== PRESSED: true once per press ==========
    public boolean isAPressed() { return isNewPress(A, gamepad.a); }
    public boolean isBPressed() { return isNewPress(B, gamepad.b); }
    public boolean isXPressed() { return isNewPress(X, gamepad.x); }
    public boolean isYPressed() { return isNewPress(Y, gamepad.y); }
    public boolean isDpadUpPressed()    { return isNewPress(DPAD_UP, gamepad.dpad_up); }
    public boolean isDpadDownPressed()  { return isNewPress(DPAD_DOWN, gamepad.dpad_down); }
    public boolean isDpadLeftPressed()  { return isNewPress(DPAD_LEFT, gamepad.dpad_left); }
    public boolean isDpadRightPressed() { return isNewPress(DPAD_RIGHT, gamepad.dpad_right); }
    public boolean isLeftBumperPressed()  { return isNewPress(LEFT_BUMPER, gamepad.left_bumper); }
    public boolean isRightBumperPressed() { return isNewPress(RIGHT_BUMPER, gamepad.right_bumper); }
    public boolean isLeftTriggerPressed()  { return isNewPress(LEFT_TRIGGER, isLeftTriggerHeld()); }
    public boolean isRightTriggerPressed() { return isNewPress(RIGHT_TRIGGER, isRightTriggerHeld()); }
    public boolean isStartPressed() { return isNewPress(START, gamepad.start); }
    public boolean isBackPressed()  { return isNewPress(BACK, gamepad.back); }
    public boolean isLeftStickButtonPressed()  { return isNewPress(LEFT_STICK_BUTTON, gamepad.left_stick_button); }
    public boolean isRightStickButtonPressed() { return isNewPress(RIGHT_STICK_BUTTON, gamepad.right_stick_button); }

    // ========== HELD: true every loop while down ==========
    public boolean isAHeld() { return gamepad.a; }
    public boolean isBHeld() { return gamepad.b; }
    public boolean isXHeld() { return gamepad.x; }
    public boolean isYHeld() { return gamepad.y; }
    public boolean isLeftBumperHeld()  { return gamepad.left_bumper; }
    public boolean isRightBumperHeld() { return gamepad.right_bumper; }
    public boolean isLeftTriggerHeld()  { return gamepad.left_trigger > ControlsConfig.TRIGGER_THRESHOLD; }
    public boolean isRightTriggerHeld() { return gamepad.right_trigger > ControlsConfig.TRIGGER_THRESHOLD; }
    public boolean isLeftStickButtonHeld()  { return gamepad.left_stick_button; }
    public boolean isRightStickButtonHeld() { return gamepad.right_stick_button; }

    // ========== FEEDBACK to the person holding the gamepad ==========

    /** Buzz the gamepad for this many milliseconds. Durations are named in ControlsConfig. */
    public void rumble(int milliseconds) { gamepad.rumble(milliseconds); }

    /** Set the light bar colour on PS4/PS5 gamepads (does nothing on others). Values 0.0 to 1.0. */
    public void setLedColor(double red, double green, double blue) {
        gamepad.setLedColor(red, green, blue, Gamepad.LED_DURATION_CONTINUOUS);
    }

    /** @return the time (ms) the Driver Station last sent new input for this gamepad. */
    public long getLastUpdateMs() { return gamepad.timestamp; }
}
