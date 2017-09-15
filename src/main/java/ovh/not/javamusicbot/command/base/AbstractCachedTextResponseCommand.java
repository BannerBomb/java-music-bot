package ovh.not.javamusicbot.command.base;

import ovh.not.javamusicbot.CommandContext;

public abstract class AbstractCachedTextResponseCommand extends AbstractTextResponseCommand {
    private String cache = null;

    protected AbstractCachedTextResponseCommand(String name, String... names) {
        super(name, names);
    }

    @Override
    protected String textResponse(CommandContext context) {
        return cache != null ? cache : cachedTextResponse(context);
    }

    protected abstract String cachedTextResponse(CommandContext context);
}
