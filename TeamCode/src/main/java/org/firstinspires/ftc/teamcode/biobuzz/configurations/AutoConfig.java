package org.firstinspires.ftc.teamcode.biobuzz.configurations;

import com.acmerobotics.dashboard.config.Config;

/**
 * STUDENTS: you will change this file.
 *
 * AutoConfig.java  -  the Autonomous plan for the next match, and its time limits.   ROLE: config
 *
 * DECIDE BEFORE THE MATCH, with the alliance partner: which ROUTINE, which START position,
 * and how long to wait first (START_DELAY_S, to stay out of the partner's way). Set them
 * here (or in the Dashboard: http://192.168.43.1:8080/dash -> AutoConfig) in the pits.
 * Only the alliance (RED / BLUE) is picked at the field, with gamepad 1 D-pad in INIT.
 * The INIT screen shows all of them, so the drive team can check before PLAY.
 *
 * Live edits in FTC Dashboard are LOST when the app restarts; type values meant to last
 * back in here and deploy.
 */
@Config
public class AutoConfig {

    /** What the robot does in Autonomous. */
    public enum Routine {
        LAUNCH_PARK,           // leave the wall, launch the 4 preloads, park in the LOADING ZONE
        LAUNCH_COLLECT_PARK    // leave, launch 4, collect our GARDEN, launch again, park
    }

    /** Where the robot is placed before the match (the places are in FieldConfig, RED side). */
    public enum Start {
        WALL_NEAR_GARDEN,      // back on our wall, near our GARDEN (audience end)
        WALL_MIDDLE            // back on our wall, between the GARDEN and the LOADING ZONE
    }

    // ---- The plan (decided before the match) ----
    public static Routine ROUTINE = Routine.LAUNCH_PARK;
    public static Start START = Start.WALL_NEAR_GARDEN;
    public static double START_DELAY_S = 0;          // 0 to 10 s: wait this long after PLAY

    // ---- Time limits for each step. A step that runs out of time logs "TIMEOUT" and moves on. ----
    public static double DRIVE_TIMEOUT_MS = 4000;    // LEAVE
    public static double LAUNCH_TIMEOUT_MS = 6000;   // LAUNCH_PRELOADS, LAUNCH_AGAIN
    public static double COLLECT_TIMEOUT_MS = 5000;  // COLLECT_GARDEN
    public static double PARK_TIMEOUT_MS = 4000;     // PARK

    // ---- Always PARK in time (rule 10.5, SWARM RP): whatever step is running, go to PARK at ----
    public static double PARK_BY_S = 26;             // seconds after PLAY (AUTO is 30 s)
}
