package ovh.not.javamusicbot.command;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import ovh.not.javamusicbot.AbstractCommand;
import ovh.not.javamusicbot.CommandContext;
import ovh.not.javamusicbot.GuildManager;
import ovh.not.javamusicbot.MusicManager;

import static ovh.not.javamusicbot.Utils.formatDuration;
import static ovh.not.javamusicbot.Utils.formatTrackDuration;

public class NowPlayingCommand extends AbstractCommand {
    private static final String NOW_PLAYING_FORMAT = "Currently playing **%s** by **%s** `[%s/%s]`\nSong URL: %s";

    public NowPlayingCommand() {
        super("nowplaying", "current", "now", "np");
    }

    @Override
    public void on(CommandContext context) {
        MusicManager musicManager = GuildManager.getInstance().getMusicManager(context.getEvent().getGuild());
        if (!musicManager.isPlayingMusic()) {
            context.reply("No music is playing on this guild! To play a song use `{{prefix}}play`");
            return;
        }

        AudioTrack currentTrack = musicManager.getPlayer().getPlayingTrack();

        context.reply(NOW_PLAYING_FORMAT, currentTrack.getInfo().title, currentTrack.getInfo().author,
                formatDuration(currentTrack.getPosition()), formatTrackDuration(currentTrack),
                currentTrack.getInfo().uri);
    }
}
