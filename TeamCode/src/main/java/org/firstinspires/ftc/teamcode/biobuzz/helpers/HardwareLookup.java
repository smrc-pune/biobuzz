package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.ArrayList;
import java.util.List;

/**
 * HardwareLookup.java  -  finds a device by name, with a HELPFUL error.          ROLE: helper
 *
 * The plain hardwareMap.get(...) crashes with a long, confusing message when a
 * name is wrong. This wrapper says exactly what went wrong and where to fix it.
 * Every part uses it:
 *
 *   motor = HardwareLookup.get(hardwareMap, DcMotorEx.class, HardwareNames.INTAKE_MOTOR, "Intake");
 *
 * WHY IT CRASHES: a missing device MUST stop the OpMode at INIT, so the problem is
 * found in the pits and not during a match. Parts are only built in init().
 *
 * BULK READS: asking a hub for one sensor value takes about 2 ms, and a loop asks many.
 * In MANUAL bulk-read mode the hub sends ALL its values in one answer, and we reuse that
 * answer until clearBulkReadCache() is called. Robot turns this on in its constructor and
 * clears the cache once at the start of every Robot.update(). Test OpModes that build one
 * subsystem alone leave it off (slower, but always fresh values).
 *
 * TRACING A BUG: the message names the part and the device name. Fix the name in
 * configurations/HardwareNames.java, or in the Robot Controller app (Configure Robot).
 * Sensor values that never change = bulk reads on, but the cache is never cleared.
 * Read this file once; you will not need to change it.
 */
public class HardwareLookup {
    private static List<LynxModule> hubs = new ArrayList<>();

    public static <T> T get(HardwareMap hardwareMap, Class<T> type, String deviceName, String partName) {
        try {
            return hardwareMap.get(type, deviceName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("DEVICE NOT FOUND: " + partName + " needs a " + type.getSimpleName()
                    + " named '" + deviceName + "'. Fix: make the name in HardwareNames.java match the name in"
                    + " the Robot Controller app (Configure Robot), or plug in / configure the device.", e);
        }
    }

    /** Turn on MANUAL bulk reads on every hub. Call once, in init. */
    public static void useManualBulkReads(HardwareMap hardwareMap) {
        hubs = hardwareMap.getAll(LynxModule.class);
        for (int i = 0; i < hubs.size(); i++) {
            hubs.get(i).setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }
    }

    /** Throw away the saved hub values, so this loop reads fresh ones. Call once per loop. */
    public static void clearBulkReadCache() {
        for (int i = 0; i < hubs.size(); i++) {
            hubs.get(i).clearBulkCache();
        }
    }
}
