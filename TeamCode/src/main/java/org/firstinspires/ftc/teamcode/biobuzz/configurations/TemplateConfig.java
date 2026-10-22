package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file (by copying it).
 *
 * TemplateConfig.java  -  the numbers for template/TemplateMechanism and TemplateStateMachine.  ROLE: config
 *
 * Copy this file when you copy the templates, for example to RollerConfig.java, and rename
 * the values. Every number a mechanism or state machine uses lives in a file like this.
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type good values back in here.
 */
@Config
public class TemplateConfig {
    public static double RUN_POWER = 0.5;      // -1.0 to 1.0: motor power while running
    public static double RUN_TIME_MS = 2000;   // TemplateStateMachine runs the motor this long
    public static double TIMEOUT_MS = 5000;    // ... and gives up after this long, whatever happens
}
