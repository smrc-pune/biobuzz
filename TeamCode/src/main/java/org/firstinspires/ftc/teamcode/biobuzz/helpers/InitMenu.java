package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * MENTORS: you do not need to change this file.
 *
 * InitMenu.java  -  a small menu on the Driver Station screen during INIT.     ROLE: helper
 *
 * WHY IT EXISTS:
 * Before a match the drive team picks the alliance (and, in Autonomous, the start,
 * routine and start delay). Giving each one its own button would run out of
 * buttons, so every choice is a ROW in one menu, and four buttons do everything:
 *
 *      D-pad up / down      move the cursor to another row
 *      D-pad left / right   change the value in that row
 *
 *   Example screen:          > Alliance     RED
 *                              Routine      LAUNCH_PARK
 *                              Delay (s)    0
 *
 * HOW AN OPMODE USES IT:
 *   int allianceRow = menu.addChoiceRow("Alliance", Alliance.values(), MatchInfo.alliance);
 *   int delayRow    = menu.addNumberRow("Delay (s)", 0, 10, 0);
 *   // Alliance.values() is the enum's whole list: { RED, BLUE }. The last value is the start choice.
 *   // in init_loop(): read the D-pad ONCE, then pass the presses in
 *   menu.update(driver.isDpadUpPressed(), driver.isDpadDownPressed(),
 *               driver.isDpadLeftPressed(), driver.isDpadRightPressed());
 *   menu.addTo(t);
 *   // in start(): read the answers
 *   MatchInfo.alliance = (Alliance) menu.getChoice(allianceRow);   // "(Alliance)": it IS an Alliance
 *   int delaySeconds   = menu.getNumber(delayRow);
 *
 * The menu never reads a gamepad itself: only OpModes read gamepads.
 * Values wrap around: pressing right on the last choice goes back to the first.
 *
 * TRACING A BUG: a row that will not change usually means the OpMode read that
 * D-pad button somewhere else in the same loop (see GamepadInput's RULE).
 */
public class InitMenu {
    private static final int MAX_ROWS = 12;

    private final String[] labels = new String[MAX_ROWS];
    private final Object[][] choices = new Object[MAX_ROWS][];  // the list of choices; null for a number row
    private final int[] minimums = new int[MAX_ROWS];
    private final int[] maximums = new int[MAX_ROWS];
    private final int[] values = new int[MAX_ROWS];   // choice index, or the number itself
    private int rowCount = 0;
    private int cursor = 0;

    /**
     * Add a row that picks one value from a list, for example an enum's values().
     * @param startChoice the value shown first (the first in the list if it is not found).
     * @return the row number, to read the choice later with getChoice(row).
     */
    public int addChoiceRow(String label, Object[] rowChoices, Object startChoice) {
        labels[rowCount] = label;
        choices[rowCount] = rowChoices;
        minimums[rowCount] = 0;
        maximums[rowCount] = rowChoices.length - 1;
        values[rowCount] = 0;
        for (int i = 0; i < rowChoices.length; i++) {
            if (rowChoices[i].equals(startChoice)) values[rowCount] = i;
        }
        rowCount = rowCount + 1;
        return rowCount - 1;
    }

    /** Add a row that picks a whole number from min to max. @return the row number. */
    public int addNumberRow(String label, int min, int max, int startValue) {
        labels[rowCount] = label;
        choices[rowCount] = null;
        minimums[rowCount] = min;
        maximums[rowCount] = max;
        values[rowCount] = startValue;
        rowCount = rowCount + 1;
        return rowCount - 1;
    }

    /** Call once per init_loop() with this loop's D-pad presses. */
    public void update(boolean upPressed, boolean downPressed, boolean leftPressed, boolean rightPressed) {
        if (rowCount == 0) return;
        if (upPressed) cursor = cursor - 1;
        if (downPressed) cursor = cursor + 1;
        if (cursor < 0) cursor = rowCount - 1;
        if (cursor >= rowCount) cursor = 0;

        if (leftPressed) values[cursor] = values[cursor] - 1;
        if (rightPressed) values[cursor] = values[cursor] + 1;
        if (values[cursor] < minimums[cursor]) values[cursor] = maximums[cursor];
        if (values[cursor] > maximums[cursor]) values[cursor] = minimums[cursor];
    }

    /** @return the chosen value of a choice row. Put the type in front: (Alliance) menu.getChoice(row). */
    public Object getChoice(int row) { return choices[row][values[row]]; }

    /** @return the chosen number of a number row. */
    public int getNumber(int row) { return values[row]; }

    /** @return the chosen value as text, for telemetry and the log header. */
    public String getText(int row) {
        if (choices[row] == null) return String.valueOf(values[row]);
        return choices[row][values[row]].toString();
    }

    /** Show every row, with ">" next to the one the cursor is on. */
    public void addTo(Telemetry telemetry) {
        telemetry.addLine("INIT MENU  (D-pad up/down = row, left/right = change)");
        for (int i = 0; i < rowCount; i++) {
            String marker = "   ";
            if (i == cursor) marker = ">  ";
            telemetry.addData(marker + labels[i], getText(i));
        }
    }
}
