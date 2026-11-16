package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * FeederConfig.java  -  numbers for the feeder (mechanisms/Feeder.java).       ROLE: config
 *
 * The feeder is a windmill on a CONTINUOUS servo: these are powers (-1.0 to 1.0), not
 * positions. Try them with "Test: Feeder", then type good values in here.
 * How LONG it feeds, and how fast the flywheel must be, are in LauncherConfig.
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class FeederConfig {
    public static double FEED_POWER = 1.0;    // windmill power while feeding (goBILDA's example: 1.0)
    public static double UNJAM_POWER = -0.5;  // windmill power backwards, to free a stuck ball
}
