package org.firstinspires.ftc.teamcode.trialbot.mechanisms;

import static com.pedropathing.ivy.commands.Commands.instant;
import static com.pedropathing.ivy.commands.Commands.waitMs;
import static com.pedropathing.ivy.groups.Groups.sequential;
import static com.pedropathing.ivy.pedro.PedroCommands.follow;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.pedro.examples.PracticePaths;

@Autonomous(name = "SMRC-Trialbot Auto", group = "BioBuzz")
public class TrialBotAuto extends OpMode {
    private Follower follower;
    private Command routine;
    private String stage = "Ready";

    @Override
    public void init() {
        Scheduler.reset();
        follower = TrialBotConstants.create(hardwareMap)
                .withLogger(log -> telemetry.addData("Pedro", log.toString()));
        PracticePaths paths = new PracticePaths();

        follower.setPose(paths.start);
        routine = sequential(
                instant(() -> stage = "Outbound"),
                follow(follower, paths.moveaheadturnleft()),
                waitMs(2000),
                instant(() -> stage = "FlowerMove"),
                follow(follower, paths.movetoflower()),
                waitMs(2000),
                instant(() -> stage = "Return"),
                follow(follower, paths.backtoBase()),
                instant(() -> {
                    stage = "Complete";
                    follower.stop();
                }));
        telemetry.addLine("Demo route starts Tune first.");
    }

    @Override
    public void start() {
        Scheduler.schedule(routine);
    }

    @Override
    public void loop() {
        Scheduler.execute();
        follower.update();
        telemetry.addData("Stage", stage);
        telemetry.addData("Following", follower.following());
        telemetry.addData("Completion", follower.completion());
        telemetry.update();
    }

    @Override
    public void stop() {
        try {
            Scheduler.reset();
        } finally {
            if (follower != null) follower.stop();
        }
    }
}


