package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * HealthConfig.java  -  numbers for watching the robot's health.              ROLE: config
 *
 * Today: how often the battery voltage is read. TODO(BioBuzz) step 4: alert thresholds
 * (low battery, slow loop, heading safeguards).
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class HealthConfig {
    public static double BATTERY_SAMPLE_MS = 100;   // read the battery voltage at most this often
}
