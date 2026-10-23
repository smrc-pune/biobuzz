package org.firstinspires.ftc.teamcode.biobuzz.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.Intake;

/**
 * STUDENTS: you will change this file.
 *
 * IntakeSubsystem.java  -  picking balls up: everything OpModes may ask the intake to do.   ROLE: subsystem
 *
 *   OpMode:  robot.intake.toggle()   robot.intake.collect()   robot.intake.reverse()
 *            robot.intake.stop()     robot.intake.isOn()
 *
 * Owns: Intake (the roller). No state machine yet.
 * ADD A MENU METHOD HERE when an OpMode needs something new from the intake.
 */
public class IntakeSubsystem extends Subsystem {

    private final Intake intake;

    public IntakeSubsystem(HardwareMap hardwareMap) {
        super("intake");
        intake = new Intake(hardwareMap);
        addMechanism(intake);
    }

    // ==================== THE MENU ====================

    /** On if it was off, off if it was on (operator's A). */
    public void toggle() {
        if (intake.isCollecting()) {
            stop();
        } else {
            intake.collect();
        }
    }

    /** Pull balls in. */
    public void collect() {
        intake.collect();
    }

    /** Spit balls out while the operator holds B (also how opponent NECTAR goes back out, G408). */
    public void reverse() {
        intake.reverse();
    }

    // stop() comes from Subsystem: the roller off.

    public boolean isOn() {
        return intake.isCollecting();
    }

    @Override
    public String getStatusText() {
        if (intake.isCollecting()) {
            return "intake ON";
        }
        return "intake off";
    }
}
