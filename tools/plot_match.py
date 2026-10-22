#!/usr/bin/env python3
"""
plot_match.py - plots and summaries of BioBuzz match logs (laptop only, mentor-side).

The robot writes one CSV per OpMode run (TeamCode .../biobuzz/helpers/MatchLogger.java):
    # key=value header lines, a "type,t_s,event,..." column line, DATA and EVENT rows,
    and "# END ..." after a clean stop (no END = crash or power loss).

Usage (needs: pip install pandas matplotlib):
    plot_match.py LOG.csv                          stacked graphs + field view + summary
    plot_match.py --match AUTO.csv                 Auto joined with its TeleOp, on match time
    plot_match.py --match AUTO.csv --at 2.0        rows and events 1 s either side of match time 2.0 s
    plot_match.py --match AUTO.csv --at 1:45       ... at 1 min 45 s of match time
    plot_match.py --compare A.csv B.csv ...        battery voltage of several runs, overlaid
    plot_match.py --fieldcheck LOGS...             Field Check table: one row per field, one column per check
    add --save out.png to write the figure instead of showing it.

Match time: match_s = t_s + (file start - Auto start) / 1000, from each file's started_epoch_ms,
so match_s is 0 at Auto PLAY and TeleOp continues from the moment it really started.
A TeleOp file belongs to the Auto file just before it when it started within MATCH_JOIN_S
of that Auto file, on the same alliance.
"""
import argparse
import os
import sys

import pandas as pd

MATCH_JOIN_S = 180          # a TeleOp started within this long after an Auto is the same match
AUTO_S = 30
TRANSITION_S = 8
PLAY_LOW_BATTERY_V = 9.0    # HealthConfig.PLAY_LOW_BATTERY_V
PINPOINT_NAMES = ["NOT_READY", "READY", "CALIBRATING", "FAULT_X_POD", "FAULT_Y_POD",
                  "FAULT_NO_PODS", "FAULT_IMU_RUNAWAY", "FAULT_BAD_READ"]


class Log:
    """One match-log file: header dict, DATA rows (DataFrame), EVENT rows (DataFrame)."""

    def __init__(self, path):
        self.path = path
        self.name = os.path.basename(path)
        self.header = {}
        self.end_line = None
        columns = None
        data_rows, events = [], []
        with open(path, encoding="utf-8", errors="replace") as f:
            for line in f:
                line = line.rstrip("\n")
                if line.startswith("# END"):
                    self.end_line = line
                elif line.startswith("# "):
                    for part in line[2:].split(" "):
                        if "=" in part:
                            key, value = part.split("=", 1)
                            self.header[key] = value
                elif line.startswith("type,"):
                    columns = line.split(",")
                elif line.startswith("DATA,") and columns:
                    cells = line.split(",")
                    data_rows.append([to_number(c) for c in cells[1:]])
                elif line.startswith("EVENT,"):
                    cells = line.split(",", 2)
                    if len(cells) == 3:
                        events.append((to_number(cells[1]), cells[2]))
        names = columns[1:] if columns else ["t_s", "event"]
        width = len(names)
        data_rows = [(row + [None] * width)[:width] for row in data_rows]
        self.data = pd.DataFrame(data_rows, columns=names)
        if "event" in self.data:
            self.data = self.data.drop(columns=["event"])
        self.events = pd.DataFrame(events, columns=["t_s", "text"])
        self.start_ms = float(self.header.get("started_epoch_ms", "0") or 0)
        self.opmode = self.header.get("opmode", "?")
        self.alliance = self.header.get("alliance", "?")
        self.offset_s = 0.0   # added to t_s to get match time (set by join_match)

    @property
    def clean_end(self):
        return self.end_line is not None

    def match_time(self, frame):
        return frame["t_s"] + self.offset_s


def to_number(text):
    try:
        return float(text)
    except ValueError:
        return text if text != "" else None


def parse_time(text):
    """'2.0' -> 2.0 s; '1:45' -> 105 s."""
    if ":" in text:
        minutes, seconds = text.split(":", 1)
        return int(minutes) * 60 + float(seconds)
    return float(text)


def find_teleop(auto):
    """The TeleOp file that started within MATCH_JOIN_S after this Auto, same alliance."""
    folder = os.path.dirname(os.path.abspath(auto.path))
    best = None
    for name in sorted(os.listdir(folder)):
        if not name.endswith(".csv") or "TeleOp" not in name:
            continue
        candidate = Log(os.path.join(folder, name))
        gap_s = (candidate.start_ms - auto.start_ms) / 1000.0
        if candidate.alliance == auto.alliance and 0 < gap_s <= MATCH_JOIN_S:
            if best is None or candidate.start_ms < best.start_ms:
                best = candidate
    return best


def join_match(auto):
    logs = [auto]
    teleop = find_teleop(auto)
    if teleop is not None:
        teleop.offset_s = (teleop.start_ms - auto.start_ms) / 1000.0
        logs.append(teleop)
    return logs


# ==================== printed summary ====================

def summarize(log):
    data, events = log.data, log.events
    duration = max(data["t_s"].max() if len(data) else 0, events["t_s"].max() if len(events) else 0)
    print(f"\n=== {log.name}")
    print(f"  opmode {log.opmode}, alliance {log.alliance}, field {log.header.get('field', '?')}")
    for key, value in log.header.items():
        if key not in ("opmode", "alliance", "field", "battery_at_play_v", "started_epoch_ms"):
            print(f"  {key} = {value}")
    print(f"  duration {duration:.1f} s, {len(data)} DATA rows, {len(events)} EVENTs")
    print(f"  clean end: {'yes' if log.clean_end else 'NO (crash or power loss)'}")
    if "battery_v" in data and len(data):
        print(f"  battery at PLAY {log.header.get('battery_at_play_v', '?')} V, lowest {data['battery_v'].min():.2f} V")
    if "worst_loop_ms" in data and len(data):
        print(f"  worst loop {data['worst_loop_ms'].max():.0f} ms, average {data['loop_ms'].mean():.1f} ms")
    alerts = events[events["text"].str.startswith("ALERT:")]
    for _, alert in alerts.iterrows():
        line = f"  ALERT at {alert.t_s:.2f} s: {alert.text[6:]}"
        if "battery_v" in data and len(data):
            before = data[(data["t_s"] <= alert.t_s) & (data["t_s"] >= alert.t_s - 2)]
            now = data.iloc[(data["t_s"] - alert.t_s).abs().argsort()[:1]]
            if len(now):
                line += f"   battery {now['battery_v'].iloc[0]:.2f} V"
            if len(before):
                line += f", lowest in the 2 s before {before['battery_v'].min():.2f} V"
        print(line)
    timeouts = events[events["text"].str.startswith("TIMEOUT")]
    if len(timeouts):
        print(f"  TIMEOUTs: {', '.join(timeouts['text'])}")


def print_window(logs, at_s):
    print(f"\n=== match time {at_s:.2f} s (from {at_s - 1:.2f} to {at_s + 1:.2f})")
    for log in logs:
        rows = log.data[(log.match_time(log.data) >= at_s - 1) & (log.match_time(log.data) <= at_s + 1)]
        events = log.events[(log.match_time(log.events) >= at_s - 1) & (log.match_time(log.events) <= at_s + 1)]
        if len(rows) == 0 and len(events) == 0:
            continue
        print(f"--- {log.name} (offset {log.offset_s:.2f} s)")
        for _, event in events.iterrows():
            print(f"  EVENT match {event.t_s + log.offset_s:7.2f} s: {event.text}")
        if len(rows):
            shown = rows.copy()
            shown.insert(0, "match_s", log.match_time(rows))
            keep = [c for c in ["match_s", "t_s", "battery_v", "loop_ms", "x_in", "y_in", "heading_deg",
                                "pinpoint_status", "field_centric", "launch_state", "launcher_rpm",
                                "launcher_target_rpm"] if c in shown]
            print(shown[keep].to_string(index=False))


# ==================== graphs ====================

def plot_logs(logs, title, save):
    import matplotlib.pyplot as plt
    panels = [("heading_deg", "heading (deg)"), ("battery_v", "battery (V)"), ("effort", "drive effort"),
              ("loop_ms", "loop (ms)"), ("pinpoint_status", "Pinpoint"), ("launch_state", "launch state"),
              ("launcher_rpm", "launcher RPM")]
    fig = plt.figure(figsize=(14, 11))
    grid = fig.add_gridspec(len(panels), 2, width_ratios=[3, 1.3])
    axes = [fig.add_subplot(grid[i, 0]) for i in range(len(panels))]
    for ax in axes[1:]:
        ax.sharex(axes[0])
    field_ax = fig.add_subplot(grid[:, 1])
    volts = pd.concat([log.data["battery_v"] for log in logs if "battery_v" in log.data])
    low_v, high_v = (volts.min(), volts.max()) if len(volts) else (PLAY_LOW_BATTERY_V, 13)
    points = None
    for log in logs:
        data = log.data
        if len(data) == 0:
            continue
        t = log.match_time(data)
        label = log.opmode
        for ax, (column, name) in zip(axes, panels):
            ax.set_ylabel(name, fontsize=8)
            if column == "effort" and "stick_forward" in data:
                effort = (data["stick_forward"] ** 2 + data["stick_right"] ** 2) ** 0.5
                ax.plot(t, effort, lw=0.8, label=label + " move")
                ax.plot(t, data["stick_turn"].abs(), lw=0.8, label=label + " turn")
            elif column in data:
                ax.plot(t, data[column], lw=0.8, label=label)
            if column == "heading_deg" and "field_centric" in data:
                robot = data["field_centric"] == 0
                ax.fill_between(t, -180, 180, where=robot, color="orange", alpha=0.2, step="post")
            if column == "launcher_rpm" and "launcher_target_rpm" in data:
                ax.plot(t, data["launcher_target_rpm"], lw=0.8, ls="--", label=label + " target")
            if column == "loop_ms" and "worst_loop_ms" in data:
                ax.plot(t, data["worst_loop_ms"], lw=0.5, alpha=0.6, label=label + " worst")
        if "battery_at_play_v" in log.header:
            axes[1].axhline(float(log.header["battery_at_play_v"]), color="green", lw=0.6, ls=":")
        for _, event in log.events.iterrows():
            colour = "red" if str(event.text).startswith("ALERT") else "grey"
            for ax in axes:
                ax.axvline(event.t_s + log.offset_s, color=colour, lw=0.4, alpha=0.5)
        if "x_in" in data and "battery_v" in data:
            points = field_ax.scatter(data["x_in"], data["y_in"], c=data["battery_v"], s=3, cmap="viridis",
                                      vmin=low_v, vmax=high_v)
    if points is not None:
        fig.colorbar(points, ax=field_ax, label="battery (V)", shrink=0.5)
    axes[1].axhline(PLAY_LOW_BATTERY_V, color="red", lw=0.6, ls=":")
    axes[4].set_yticks(range(len(PINPOINT_NAMES)))
    axes[4].set_yticklabels(PINPOINT_NAMES, fontsize=6)
    if len(logs) > 1:   # shade AUTO, transition, TELEOP
        for ax in axes:
            ax.axvspan(0, AUTO_S, color="blue", alpha=0.05)
            ax.axvspan(AUTO_S, logs[1].offset_s, color="grey", alpha=0.15)
            ax.axvspan(logs[1].offset_s, logs[1].offset_s + logs[1].data["t_s"].max(), color="green", alpha=0.04)
    axes[0].legend(fontsize=7, loc="upper right")
    axes[-1].set_xlabel("match time (s)" if len(logs) > 1 else "t_s (s since PLAY)")
    field_ax.set_xlim(0, 144)
    field_ax.set_ylim(0, 144)
    field_ax.set_aspect("equal")
    field_ax.set_title("path (colour = battery)", fontsize=9)
    fig.suptitle(title, fontsize=10)
    fig.tight_layout()
    show_or_save(plt, save)


def plot_compare(logs, save):
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(figsize=(12, 5))
    for log in logs:
        if "battery_v" in log.data:
            ax.plot(log.data["t_s"], log.data["battery_v"], lw=0.8, label=log.name)
    ax.axhline(PLAY_LOW_BATTERY_V, color="red", lw=0.6, ls=":")
    ax.set_xlabel("t_s (s since PLAY)")
    ax.set_ylabel("battery (V)")
    ax.legend(fontsize=7)
    show_or_save(plt, save)


def show_or_save(plt, save):
    if save:
        plt.savefig(save, dpi=120)
        print(f"saved {save}")
    else:
        plt.show()


def fieldcheck_table(logs):
    rows = {}
    for log in logs:
        field = log.header.get("field", "?")
        for _, event in log.events.iterrows():
            text = str(event.text)
            if text.startswith("FIELDCHECK_"):
                check, result = text.split(":", 1)
                rows.setdefault(field, {})[check.replace("FIELDCHECK_", "")] = result.strip()
    table = pd.DataFrame(rows).T.sort_index(axis=1)
    pd.set_option("display.max_colwidth", 40)
    print(table.to_string())


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("files", nargs="*", help="match-log CSV files")
    parser.add_argument("--match", help="an Auto file: join it with its TeleOp on match time")
    parser.add_argument("--at", help="print rows and events around this match time (2.0 or 1:45)")
    parser.add_argument("--compare", action="store_true", help="overlay the battery of several files")
    parser.add_argument("--fieldcheck", action="store_true", help="the Field Check table")
    parser.add_argument("--save", help="write the figure to this file instead of showing it")
    parser.add_argument("--no-plot", action="store_true", help="print only")
    args = parser.parse_args()

    if args.fieldcheck:
        fieldcheck_table([Log(p) for p in args.files])
        return
    if args.compare:
        logs = [Log(p) for p in args.files]
        for log in logs:
            summarize(log)
        if not args.no_plot:
            plot_compare(logs, args.save)
        return
    if args.match:
        logs = join_match(Log(args.match))
        if len(logs) == 1:
            print(f"(no TeleOp file within {MATCH_JOIN_S} s after {logs[0].name}, same alliance)")
        title = " + ".join(log.name for log in logs)
    elif len(args.files) == 1:
        logs = [Log(args.files[0])]
        title = logs[0].name
    else:
        parser.print_help()
        sys.exit(1)
    for log in logs:
        summarize(log)
    if args.at:
        print_window(logs, parse_time(args.at))
    elif not args.no_plot:
        plot_logs(logs, title, args.save)


if __name__ == "__main__":
    main()
