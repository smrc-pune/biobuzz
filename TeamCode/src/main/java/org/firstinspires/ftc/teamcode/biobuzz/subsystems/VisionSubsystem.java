package org.firstinspires.ftc.teamcode.biobuzz.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.HiveCamera;

/**
 * STUDENTS: you will change this file.
 *
 * VisionSubsystem.java  -  our one camera.                                    ROLE: subsystem
 *
 * Owns: HiveCamera (HuskyLens, looks UP at the AprilTags under the CELLS). BIOBUZZ has
 * AprilTags ONLY under the CELLS (none on the walls), so this is the only camera we need.
 * It only answers questions and never moves anything.
 * Today nothing outside asks it yet: Robot updates it, shows its telemetry and logs it.
 * ADD A QUESTION HERE when an OpMode needs one, for example
 *     public boolean seesTag(int learnedId) { return hiveCamera.seesTag(learnedId); }
 */
public class VisionSubsystem extends Subsystem {

    private final HiveCamera hiveCamera;

    public VisionSubsystem(HardwareMap hardwareMap) {
        super("vision");
        hiveCamera = new HiveCamera(hardwareMap);
        addMechanism(hiveCamera);
    }
}
