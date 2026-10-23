package org.firstinspires.ftc.teamcode.biobuzz.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.GamepadInput;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.LogSummary;

/**
 * MENTORS: you do not need to change this file.
 *
 * LogViewer.java  -  read summaries of the last 10 match logs on the Driver Station.  ROLE: OpMode (test)
 *
 * It moves nothing and needs no robot parts, so it works with INIT alone (no PLAY needed).
 * For each run: start time, OpMode, alliance, how long it ran, ALERTs, worst loop time,
 * lowest battery, Auto TIMEOUTs, and whether it ended cleanly ("# END").
 * A run with NO END crashed or lost power: look at its last rows on a laptop
 * (docs/MATCH_LOGS.md). The file reading is in helpers/LogSummary.java.
 *
 * CONTROLS (gamepad 1):  D-pad up / down = pick a run    A = read the files again
 */
@TeleOp(name = "Log Viewer", group = "Tests")
public class LogViewer extends OpMode {

    private static final int RUNS_SHOWN = 10;

    private Telemetry t;
    private GamepadInput pad;
    private LogSummary[] runs;
    private int selected = 0;

    @Override
    public void init() {
        t = DashboardTelemetry.create(telemetry);
        pad = new GamepadInput(gamepad1);
        runs = LogSummary.newest(RUNS_SHOWN);
    }

    @Override
    public void init_loop() { show(); }

    @Override
    public void loop() { show(); }

    private void show() {
        if (pad.isAPressed()) {
            runs = LogSummary.newest(RUNS_SHOWN);
            selected = 0;
        }
        if (runs.length == 0) {
            t.addLine("No match logs yet. They are written when an OpMode runs with LogConfig.ENABLED.");
            t.update();
            return;
        }
        if (pad.isDpadDownPressed()) {
            selected = Math.min(selected + 1, runs.length - 1);
        }
        if (pad.isDpadUpPressed()) {
            selected = Math.max(selected - 1, 0);
        }

        LogSummary run = runs[selected];
        t.addLine("--- RUN " + (selected + 1) + " of " + runs.length + " (newest first) ---");
        t.addData("File", run.getFileName());
        t.addData("Started", run.getStartText());
        t.addData("OpMode / alliance", run.getOpModeName() + " / " + run.getAlliance());
        t.addData("Ran for", String.format("%.0f s (%d rows)", run.getDurationS(), run.getRowCount()));
        t.addData("ALERTs", run.getAlertCount());
        if (!run.getLastAlert().isEmpty()) {
            t.addData("Last ALERT", run.getLastAlert());
        }
        t.addData("Worst loop", String.format("%.0f ms", run.getWorstLoopMs()));
        t.addData("Battery", String.format("%.2f V at PLAY, lowest %.2f V",
                run.getBatteryAtPlayV(), run.getLowestBatteryV()));
        t.addData("Auto TIMEOUTs", run.getTimeoutCount());
        if (run.hasEndLine()) {
            t.addData("Ended", "cleanly (# END)");
        } else {
            t.addData("Ended", "NO END: crash or power loss");
        }

        t.addLine("--- all runs (D-pad up/down, A = reload) ---");
        for (int i = 0; i < runs.length; i++) {
            String marker = "   ";
            if (i == selected) {
                marker = ">> ";
            }
            t.addLine(marker + runs[i].getShortText());
        }
        t.update();
    }
}
