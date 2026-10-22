package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * MENTORS: you do not need to change this file.
 *
 * MatchClock.java  -  how much match time is left.                             ROLE: helper
 *
 * Robot owns one clock. Robot.startMatch() starts it at PLAY; Robot.update() ticks it.
 *   robot.clock.getTimeLeftText()           "1:42", for the header line
 *   robot.clock.justReachedSecondsLeft(20)  true on the ONE loop when 0:20 is reached (rumble)
 *
 * The clock only counts from our own PLAY. It does not know the field timer, so it can be
 * a fraction of a second off; that is fine for warnings.
 */
public class MatchClock {
    public static final double AUTO_S = 30;     // game manual: AUTO is 30 s
    public static final double TELEOP_S = 120;  // game manual: TELEOP is 2:00

    private final ElapsedTime sinceStart = new ElapsedTime();
    private double lengthS = 0;      // 0 = not started (test OpModes)
    private double leftS = 0;
    private double leftLastLoopS = 0;

    /** PLAY was pressed. lengthS = AUTO_S or TELEOP_S. */
    public void start(double lengthS) {
        this.lengthS = lengthS;
        sinceStart.reset();
        leftS = lengthS;
        leftLastLoopS = lengthS;
    }

    /** Once per loop (Robot.update() does it). */
    public void update() {
        leftLastLoopS = leftS;
        leftS = Math.max(0, lengthS - sinceStart.seconds());
    }

    public boolean hasStarted() { return lengthS > 0; }

    /** true when this clock was started for TeleOp (2:00). */
    public boolean isTeleOp() { return lengthS == TELEOP_S; }

    public double secondsSinceStart() { return sinceStart.seconds(); }

    public double secondsLeft() { return leftS; }

    /** @return true on the one loop where the time left goes down to "seconds". */
    public boolean justReachedSecondsLeft(double seconds) {
        return leftLastLoopS > seconds && leftS <= seconds;
    }

    /** @return the time left as minutes:seconds, for example "1:42". */
    public String getTimeLeftText() {
        int seconds = (int) Math.ceil(leftS);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
