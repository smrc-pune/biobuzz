package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * IntakeConfig.java  -  numbers for the intake (mechanisms/Intake.java).       ROLE: config
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class IntakeConfig {
    public static double COLLECT_POWER = 1.0;    // -1.0 to 1.0: roller power to pull balls in
    public static double REVERSE_POWER = -0.6;   // roller power to spit balls out (also opponent NECTAR, G408)
}
