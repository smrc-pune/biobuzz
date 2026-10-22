package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * FeederConfig.java  -  numbers for the feeder (mechanisms/Feeder.java).       ROLE: config
 *
 * The feeder is a servo. 0.0 and 1.0 are the two ends of its travel. Find the two positions
 * with "Test: Feeder" (or "Test: Parts"), then type them in here.
 * TODO(BioBuzz): set both once the feeder is built.
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class FeederConfig {
    public static double HOLD_POSITION = 0.2;   // keeps the next ball back, in the hopper
    public static double FEED_POSITION = 0.6;   // pushes the next ball into the flywheel
}
