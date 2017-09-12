package ovh.not.javamusicbot;

import com.google.gson.Gson;
import com.moandjiezana.toml.Toml;
import lavalink.client.io.Lavalink;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.bot.sharding.ShardManagerBuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public final class MusicBot {
    private static final Logger logger = LoggerFactory.getLogger(MusicBot.class);

    public static final String CONFIG_PATH = "config.toml";
    public static final String CONSTANTS_PATH = "constants.toml";
    public static final String USER_AGENT = "JavaMusicBot (https://github.com/sponges/JavaMusicBot)";
    public static final Gson GSON = new Gson();
    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    public static volatile boolean running = true;

    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                // copying the request to a builder
                Request.Builder builder = chain.request().newBuilder();

                // adding user agent header
                builder.addHeader("User-Agent", MusicBot.USER_AGENT);

                // building the new request
                Request request = builder.build();

                // logging
                String method = request.method();
                String uri = request.url().uri().toString();
                logger.info("OkHttpClient: {} {}", method, uri);

                return chain.proceed(request);
            }).build();

    private static ConfigLoadResult configs = null;

    public static void main(String[] args) {
        Config config = getConfigs().config;

        Listener listener = new Listener();

        ShardManagerBuilder builder = new ShardManagerBuilder()
                .addEventListener(listener)
                .setToken(config.token)
                .setGame(Game.of(config.game));

        if (args.length < 3) {
            builder.setShardTotal(1).setShards(0);
        } else {
            try {
                int shardTotal = Integer.parseInt(args[0]);
                int minShardId = Integer.parseInt(args[1]);
                int maxShardId = Integer.parseInt(args[2]);

                builder.setShardTotal(shardTotal).setShards(minShardId, maxShardId);
            } catch (Exception ex) {
                logger.warn("Could not instantiate with given args! Usage: <shard total> <min shard> <max shard>");
                return;
            }
        }

        // todo set reconnect ipc queue (when alpaca adds support for it)

        ShardManager manager;
        try {
            manager = builder.buildBlocking();
        } catch (LoginException | InterruptedException | RateLimitedException e) {
            logger.error("error on call to ShardManager#buildBlocking", e);
            return;
        }

        Lavalink lavalink = new Lavalink(config.lavalinkUserId, manager.getShardsTotal(), manager::getShard);
        listener.setLavalink(lavalink);
        GuildManager.getInstance().setLavalink(lavalink);

        for (Map.Entry<String, String> entry : config.lavalinkNodes.entrySet()) {
            try {
                lavalink.addNode(new URI(entry.getKey().substring(1, entry.getKey().length() - 1)), entry.getValue());
            } catch (URISyntaxException e) {
                logger.error("error parsing lavalink node server uri", e);
                return;
            }
        }

        while (running) try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Shutting down lavalink");
        lavalink.shutdown();
    }

    public static ConfigLoadResult getConfigs() {
        if (configs == null) {
            Config config = new Toml().read(new File(CONFIG_PATH)).to(Config.class);
            Constants constants = new Toml().read(new File(CONSTANTS_PATH)).to(Constants.class);
            configs = new ConfigLoadResult(config, constants);
        }
        return configs;
    }

    public static ConfigLoadResult reloadConfigs() {
        configs = null;
        return getConfigs();
    }

    public static class ConfigLoadResult {
        public Config config;
        public Constants constants;

        ConfigLoadResult(Config config, Constants constants) {
            this.config = config;
            this.constants = constants;
        }
    }
}
