package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.biobuzz.configurations.TelemetryConfig;

/**
 * MENTORS: you do not need to change this file.
 *
 * DashboardTelemetry.java  -  one telemetry for the Driver Station AND FTC Dashboard.   ROLE: helper
 *
 * Every OpMode starts with:   Telemetry t = DashboardTelemetry.create(telemetry);
 * Remember to call t.update() once per loop - that is what actually sends the text.
 *
 * Dashboard: join the robot's Wi-Fi and open http://192.168.43.1:8080/dash
 *
 * WHY THE SEND INTERVAL: sending text to the Driver Station takes time. Sending it
 * at most every TelemetryConfig.DS_TRANSMIT_INTERVAL_MS (100 ms) keeps the loop fast.
 *
 * TRACING A BUG: text on the Driver Station but not the Dashboard (or the other way
 * round) means the OpMode used "telemetry" instead of the "t" made here.
 */
public class DashboardTelemetry {

    public static Telemetry create(Telemetry driverStationTelemetry) {
        driverStationTelemetry.setMsTransmissionInterval(TelemetryConfig.DS_TRANSMIT_INTERVAL_MS);
        return new MultipleTelemetry(driverStationTelemetry, FtcDashboard.getInstance().getTelemetry());
    }
}
