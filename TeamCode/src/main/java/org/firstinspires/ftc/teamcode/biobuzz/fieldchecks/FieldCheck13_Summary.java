package org.firstinspires.ftc.teamcode.biobuzz.fieldchecks;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.FieldCheckConfig;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.DashboardTelemetry;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.FieldCheckHelper;
import org.firstinspires.ftc.teamcode.biobuzz.helpers.MatchLogger;

/**
 * MENTORS: you do not need to change this file.
 *
 * FieldCheck13_Summary.java  -  every Field Check result on one screen.       ROLE: OpMode (field check)
 *
 * The robot does nothing; INIT is enough. It shows each result from this app session, the
 * battery sag (resting from 02, lowest while driving in 07-09), and what to do next.
 * If the app restarted, older results are only in the match logs (FIELDCHECK_<nn> events:
 * "Log Viewer", or tools/plot_match.py --fieldcheck on a laptop).
 * AT HOME: copy the measured values into the BASELINE_ values in FieldCheckConfig, with the date.
 */
@TeleOp(name = "Field Check 13: Summary", group = "Field Checks")
public class FieldCheck13_Summary extends OpMode {

    private Telemetry t;
    private boolean logged = false;

    @Override
    public void init() { t = DashboardTelemetry.create(telemetry); }

    @Override
    public void init_loop() { show(); }

    @Override
    public void start() {
        MatchLogger.start("FieldCheck13_Summary", FieldCheckHelper.getRestingBatteryV());
    }

    @Override
    public void loop() {
        if (!logged) {   // one copy of every result in this file too
            logged = true;
            for (int number = 1; number <= FieldCheckHelper.CHECK_COUNT - 1; number++) {
                MatchLogger.event(String.format("SUMMARY_%02d", number),
                        FieldCheckHelper.getGrade(number) + " " + FieldCheckHelper.getText(number));
            }
        }
        show();
    }

    private void show() {
        t.addData("Field", FieldCheckConfig.FIELD_NAME + "   (baseline: " + FieldCheckConfig.BASELINE_DATE + ")");
        int fails = 0;
        int warns = 0;
        for (int number = 1; number <= FieldCheckHelper.CHECK_COUNT - 1; number++) {
            FieldCheckHelper.show(t, number);
            String grade = FieldCheckHelper.getGrade(number);
            if (grade.equals(FieldCheckHelper.FAIL)) {
                fails++;
            }
            if (grade.equals(FieldCheckHelper.WARN)) {
                warns++;
            }
        }
        double resting = FieldCheckHelper.getRestingBatteryV();
        double lowest = FieldCheckHelper.getLowestDrivingBatteryV();
        if (resting > 0 && lowest > 0) {
            t.addData("Battery sag", "%.2f V resting, %.2f V lowest while driving: %.2f V", resting, lowest, resting - lowest);
        }
        if (fails > 0) {
            t.addLine(fails + " FAIL: fix before matches (docs/MATCH_DAY.md, Field Checks table)");
        } else if (warns > 0) {
            t.addLine(warns + " WARN: look at them; at home, record a baseline");
        } else {
            t.addLine("ALL PASS");
        }
        t.update();
    }

    @Override
    public void stop() { MatchLogger.stop("field=" + FieldCheckConfig.FIELD_NAME); }
}
