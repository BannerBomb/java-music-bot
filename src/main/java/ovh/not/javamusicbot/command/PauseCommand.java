package ovh.not.javamusicbot.command;

import ovh.not.javamusicbot.AbstractCommand;
import ovh.not.javamusicbot.CommandContext;
import ovh.not.javamusicbot.GuildManager;
import ovh.not.javamusicbot.MusicManager;

public class PauseCommand extends AbstractCommand {
    public PauseCommand() {
        super("pause", "resume");
    }

    @Override
    public void on(CommandContext context) {
        MusicManager musicManager = GuildManager.getInstance().getMusicManager(context.getEvent().getGuild());
        if (!musicManager.isPlayingMusic()) {
            context.reply("No music is playing on this guild! To play a song use `{{prefix}}play`");
            return;
        }

        boolean action = !musicManager.getPlayer().isPaused();
        musicManager.getPlayer().setPaused(action);

        if (action) {
            context.reply("Paused music playback! Use `{{prefix}}resume` to resume.");
        } else {
            context.reply("Resumed music playback!");
        }
    }
}
