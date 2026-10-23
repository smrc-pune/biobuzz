package org.firstinspires.ftc.teamcode.biobuzz.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.FrontCamera;
import org.firstinspires.ftc.teamcode.biobuzz.mechanisms.HiveCamera;

/**
 * STUDENTS: you will change this file.
 *
 * VisionSubsystem.java  -  the two cameras.                                   ROLE: subsystem
 *
 * Owns: HiveCamera (HuskyLens, looks UP at the CELL tags) and FrontCamera (webcam: field
 * tags, POLLEN, NECTAR). They only answer questions and never move anything.
 * Today nothing outside asks them yet: Robot updates them, shows their telemetry and logs
 * them. ADD A QUESTION HERE when an OpMode needs one, for example
 *     public boolean seesPollen() { return frontCamera.seesPollen(); }
 */
public class VisionSubsystem extends Subsystem {

    private final HiveCamera hiveCamera;
    private final FrontCamera frontCamera;

    public VisionSubsystem(HardwareMap hardwareMap) {
        super("vision");
        hiveCamera = new HiveCamera(hardwareMap);
        frontCamera = new FrontCamera(hardwareMap);
        addMechanism(hiveCamera);
        addMechanism(frontCamera);
    }
}
