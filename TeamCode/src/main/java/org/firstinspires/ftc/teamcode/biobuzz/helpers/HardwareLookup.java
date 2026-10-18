package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import com.qualcomm.robotcore.hardware.HardwareMap;

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
 * TRACING A BUG: the message names the part and the device name. Fix the name in
 * configurations/HardwareNames.java, or in the Robot Controller app (Configure Robot).
 * Read this file once; you will not need to change it.
 */
public class HardwareLookup {

    public static <T> T get(HardwareMap hardwareMap, Class<T> type, String deviceName, String partName) {
        try {
            return hardwareMap.get(type, deviceName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("DEVICE NOT FOUND: " + partName + " needs a " + type.getSimpleName()
                    + " named '" + deviceName + "'. Fix: make the name in HardwareNames.java match the name in"
                    + " the Robot Controller app (Configure Robot), or plug in / configure the device.", e);
        }
    }
}
