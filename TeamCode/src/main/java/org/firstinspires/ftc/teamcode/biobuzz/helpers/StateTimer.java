package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * MENTORS: you do not need to change this file.
 *
 * StateTimer.java  -  a stopwatch for state machines.          ROLE: helper
 *
 * Every state machine restarts this timer each time it changes state (inside goTo()),
 * so "timer.isTimeUp(500)" always means "we have been in THIS state for 500 ms".
 * This is how a state machine WAITS without freezing the robot: instead of
 * sleep(500), it checks the timer every loop and does something else meanwhile.
 *
 * TRACING A BUG: if a state never ends, check that goTo() calls timer.reset()
 * and that the state's CHECK uses isTimeUp() with a value from its *Config file.
 * Read this file once; you will not need to change it.
 */
public class StateTimer {
    private final ElapsedTime timer = new ElapsedTime();

    /** Start counting from zero again. */
    public void reset() {
        timer.reset();
    }

    public double seconds() {
        return timer.seconds();
    }

    public double milliseconds() {
        return timer.milliseconds();
    }

    /** @return true once at least this many milliseconds have passed since the last reset(). */
    public boolean isTimeUp(double milliseconds) {
        return timer.milliseconds() >= milliseconds;
    }
}
