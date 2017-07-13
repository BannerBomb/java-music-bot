package ovh.not.javamusicbot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import net.dv8tion.jda.core.entities.Member;
import ovh.not.javamusicbot.command.*;
import ovh.not.javamusicbot.manager.ShardManager;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    public final Map<String, Command> commands = new HashMap<>();
    public final Map<Member, Selection<AudioTrack, String>> selectors = new HashMap<>();

    public CommandManager(Config config) {
        Constants constants = config.constants;
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        CommandManager.register(commands,
                new AboutCommand(config),
                new AdminCommand(config, playerManager),
                new ChooseCommand(this),
                new DiscordFMCommand(this, playerManager),
                new DumpCommand(playerManager, config),
                new HelpCommand(this, constants),
                new InviteCommand(config),
                new JumpCommand(),
                new LoadCommand(playerManager),
                new LoopCommand(),
                new MoveCommand(),
                new NowPlayingCommand(),
                new PauseCommand(),
                new PlayCommand(this, playerManager),
                new QueueCommand(config),
                new RadioCommand(this, playerManager, constants),
                new RemoveCommand(),
                new ReorderCommand(),
                new RepeatCommand(),
                new ResetCommand(),
                new RestartCommand(),
                new SearchCommand(this),
                new ShuffleCommand(),
                new SkipCommand(),
                new StopCommand(),
                new VolumeCommand(config)
        );
    }

    public static void register(Map<String, Command> commands, Command... cmds) {
        for (Command command : cmds) {
            for (String name : command.names) {
                if (commands.containsKey(name)) {
                    throw new RuntimeException(String.format("Command name collision %s in %s!", name,
                            command.getClass().getName()));
                }
                commands.put(name, command);
            }
        }
    }

    Command getCommand(String name) {
        return commands.get(name);
    }
}
