package ovh.not.javamusicbot.command;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.VoiceChannel;
import ovh.not.javamusicbot.*;
import ovh.not.javamusicbot.manager.ShardManager;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AdminCommand extends Command {
    private final Map<String, Command> subCommands = new HashMap<>();
    private final Config config;
    private final String subCommandsString;

    public AdminCommand(Config config, ShardManager.Shard shard, AudioPlayerManager playerManager) {
        super("admin", "a");
        hide = true;
        this.config = config;
        CommandManager.register(subCommands,
                new EvalCommand(shard),
                new StopCommand(),
                new ShardRestartCommand(shard),
                new EncodeCommand(playerManager),
                new DecodeCommand(playerManager)/*,
                new PatreonCommand(shard.manager.patreonManager),
                new PremiumCommand(shard.manager.premiumManager)*/
        );
        StringBuilder builder = new StringBuilder("Subcommands:");
        subCommands.values().forEach(command -> builder.append(" ").append(command.names[0]));
        subCommandsString = builder.toString();
    }

    @Override
    public void on(Context context) {
        if (!context.event.getAuthor().getId().equals(config.owner)) {
            return;
        }
        if (context.args.length == 0) {
            context.reply(subCommandsString);
            return;
        }
        if (!subCommands.containsKey(context.args[0])) {
            context.reply("Invalid subcommand!");
            return;
        }
        Command command = subCommands.get(context.args[0]);
        context.args = Arrays.copyOfRange(context.args, 1, context.args.length);
        command.on(context);
    }

    private class StopCommand extends Command {
        private StopCommand() {
            super("stop");
        }

        @Override
        public void on(Context context) {
            context.event.getJDA().shutdown();
            System.exit(0);
        }
    }

    private class EvalCommand extends Command {
        private final ScriptEngineManager engineManager = new ScriptEngineManager();
        private final ShardManager.Shard shard;

        private EvalCommand(ShardManager.Shard shard) {
            super("eval", "js");
            this.shard = shard;
        }

        @Override
        public void on(Context context) {
            ScriptEngine engine = engineManager.getEngineByName("nashorn");
            engine.put("event", context.event);
            engine.put("args", context.args);
            engine.put("shard", shard);
            try {
                Object result = engine.eval(String.join(" ", context.args));
                if (result != null) context.reply(result.toString());
            } catch (ScriptException e) {
                e.printStackTrace();
                context.reply(e.getMessage());
            }
        }
    }

    private class ShardRestartCommand extends Command {
        private final ShardManager.Shard shard;

        private ShardRestartCommand(ShardManager.Shard shard) {
            super("shardrestart", "sr");
            this.shard = shard;
        }

        @Override
        public void on(Context context) {
            if (context.args.length == 0) {
                context.reply("Restarting shard " + shard.id + "...");
                shard.restart();
            } else {
                int id = Integer.parseInt(context.args[0]);
                for (ShardManager.Shard s : shard.manager.shards) {
                    if (s.id == id) {
                        context.reply("Restarting shard " + s.id + "...");
                        s.restart();
                        return;
                    }
                }
                context.reply("Invalid shard " + id + ".");
            }
        }
    }

    private class EncodeCommand extends Command {
        private final AudioPlayerManager playerManager;

        private EncodeCommand(AudioPlayerManager playerManager) {
            super("encode");
            this.playerManager = playerManager;
        }

        @Override
        public void on(Context context) {
            GuildMusicManager musicManager = GuildMusicManager.get(context.event.getGuild());
            if (musicManager == null || !musicManager.open || musicManager.player.getPlayingTrack() == null) {
                context.reply("Not playing music!");
                return;
            }
            try {
                context.reply(Utils.encode(playerManager, musicManager.player.getPlayingTrack()));
            } catch (IOException e) {
                e.printStackTrace();
                context.reply("An error occurred!");
            }
        }
    }

    private class DecodeCommand extends Command {
        private final AudioPlayerManager playerManager;

        private DecodeCommand(AudioPlayerManager playerManager) {
            super("decode");
            this.playerManager = playerManager;
        }

        @Override
        public void on(Context context) {
            GuildMusicManager musicManager = GuildMusicManager.getOrCreate(context.event.getGuild(),
                    context.event.getTextChannel(), playerManager);
            if (context.args.length == 0) {
                context.reply("Usage: %prefix%a decode <base64 string>");
                return;
            }
            VoiceChannel channel = context.event.getMember().getVoiceState().getChannel();
            if (channel == null) {
                context.reply("Must be in a voice channel!");
                return;
            }
            String base64 = context.args[0];
            AudioTrack track;
            try {
                track = Utils.decode(playerManager, base64);
            } catch (IOException e) {
                e.printStackTrace();
                context.reply("An error occurred!");
                return;
            }
            if (!musicManager.open) {
                musicManager.open(channel, context.event.getAuthor());
            }
            musicManager.player.playTrack(track);
        }
    }

    /*private class PatreonCommand extends Command {
        private final PatreonManager manager;

        private PatreonCommand(PatreonManager manager) {
            super("patreon", "patron");
            this.manager = manager;
        }

        @Override
        public void on(Context context) {
            if (context.args.length < 2) {
                context.reply("%prefix%a patreon <add|remove> <user id>");
                return;
            }
            String reply = "";
            switch (context.args[0].toLowerCase()) {
                case "add": {
                    reply = "e";
                    manager.addPatreon(context.args[1]);
                    break;
                }
                case "remove": {
                    manager.removePatreon(context.args[1]);
                    break;
                }
                default:
                    context.reply("Invalid args!");
                    return;
            }
            context.reply(context.args[0].toLowerCase() + reply + "d patreon!");
        }
    }

    private class PremiumCommand extends Command {
        private final PremiumManager manager;

        private PremiumCommand(PremiumManager manager) {
            super("premium");
            this.manager = manager;
        }

        @Override
        public void on(Context context) {
            if (context.args.length < 2) {
                context.reply("%prefix%a premium <add|remove> <user id>");
                return;
            }
            String reply = "";
            switch (context.args[0].toLowerCase()) {
                case "add": {
                    reply = "e";
                    manager.addPremium(context.args[1]);
                    break;
                }
                case "remove": {
                    manager.removePremium(context.args[1]);
                    break;
                }
                default:
                    context.reply("Invalid args!");
                    return;
            }
            context.reply(context.args[0].toLowerCase() + reply + "d premium!");
        }
    }*/
}
