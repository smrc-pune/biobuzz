package org.firstinspires.ftc.teamcode.biobuzz.helpers;

/**
 * MENTORS: you do not need to change this file.
 *
 * AllianceColor.java  -  the two alliance colours.                           ROLE: helper
 *
 * An "enum" is a fixed list of names. A variable of type AllianceColor can only ever be
 * AllianceColor.RED or AllianceColor.BLUE, so a typo like "RDE" is caught when the code is built,
 * not during a match. RED comes first because it is our default.
 *
 *   MatchInfo.alliance = AllianceColor.BLUE;
 *   if (MatchInfo.alliance == AllianceColor.RED) { ... }
 */
public enum AllianceColor {
    RED,
    BLUE
}
