package org.firstinspires.ftc.teamcode.biobuzz.helpers;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * MENTORS: you do not need to change this file.
 *
 * InitMenu.java  -  a small menu on the Driver Station screen during INIT.     ROLE: helper
 *
 * WHY IT EXISTS:
 * Before a match the drive team picks the alliance, start, routine, start delay,
 * match id and battery label. Giving each one its own button would run out of
 * buttons, so every choice is a ROW in one menu, and four buttons do everything:
 *
 *      D-pad up / down      move the cursor to another row
 *      D-pad left / right   change the value in that row
 *
 *   Example screen:          > Alliance     BLUE
 *                              Routine      NEAR
 *                              Delay (s)    0
 *
 * HOW AN OPMODE USES IT:
 *   int allianceRow = menu.addChoiceRow("Alliance", MatchInfo.ALLIANCES, MatchInfo.alliance);
 *   int delayRow    = menu.addNumberRow("Delay (s)", 0, 10, 0);
 *   // in init_loop(): read the D-pad ONCE, then pass the presses in
 *   menu.update(driver.isDpadUpPressed(), driver.isDpadDownPressed(),
 *               driver.isDpadLeftPressed(), driver.isDpadRightPressed());
 *   menu.addTo(t);
 *   // in start(): read the answers
 *   MatchInfo.alliance = menu.getIndex(allianceRow);
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
    private final String[][] choices = new String[MAX_ROWS][];  // null for a number row
    private final int[] minimums = new int[MAX_ROWS];
    private final int[] maximums = new int[MAX_ROWS];
    private final int[] values = new int[MAX_ROWS];   // choice index, or the number itself
    private int rowCount = 0;
    private int cursor = 0;

    /** Add a row that picks one word from a list. @return the row number, to read it later. */
    public int addChoiceRow(String label, String[] rowChoices, int startIndex) {
        labels[rowCount] = label;
        choices[rowCount] = rowChoices;
        minimums[rowCount] = 0;
        maximums[rowCount] = rowChoices.length - 1;
        values[rowCount] = startIndex;
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

    /** @return the chosen index for a choice row, or the number for a number row. */
    public int getIndex(int row) { return values[row]; }

    /** @return the chosen value as text, for telemetry and the log header. */
    public String getText(int row) {
        if (choices[row] == null) return String.valueOf(values[row]);
        return choices[row][values[row]];
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
