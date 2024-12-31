package dev.kmfg.musicbot.core.commands.intermediates;

import dev.kmfg.musicbot.core.commands.executors.*;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.tinylog.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Holds the registry of available commands to execute
 */
public class CommandsRegistry {
    private final ConcurrentHashMap<String, Class<? extends Command>> commandsMap;
    private final ExecutorService statisticsExecutorService;

    /**
     * @param statisticsExecutorService should mirror the number of available
     *                                  threads in the command executor pool.
     */
    public CommandsRegistry(ExecutorService statisticsExecutorService) {
        this.commandsMap = new ConcurrentHashMap<>();
        this.statisticsExecutorService = statisticsExecutorService;
        // register all the commands
        this.commandsMap.put(PlayCommand.COMMAND_NAME, PlayCommand.class);
        this.commandsMap.put(PlayNextCommand.COMMAND_NAME, PlayNextCommand.class);
        this.commandsMap.put(InviteCommand.COMMAND_NAME, InviteCommand.class);
        this.commandsMap.put(NowPlayingCommand.COMMAND_NAME, NowPlayingCommand.class);
        this.commandsMap.put(SearchCommand.COMMAND_NAME, SearchCommand.class);
        this.commandsMap.put(SkipCommand.COMMAND_NAME, SkipCommand.class);
        this.commandsMap.put(StopCommand.COMMAND_NAME, StopCommand.class);
        this.commandsMap.put(ViewQueueCommand.COMMAND_NAME, ViewQueueCommand.class);
        this.commandsMap.put(SeekCommand.COMMAND_NAME, SeekCommand.class);
        this.commandsMap.put(ShuffleCommand.COMMAND_NAME, ShuffleCommand.class);
        this.commandsMap.put(RemoveCommand.COMMAND_NAME, RemoveCommand.class);
    }

    public void registerCommands(DiscordApi discordApi)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (Class<? extends Command> commandClass : commandsMap.values()) {
            Constructor<? extends Command> constructor = commandClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Command command = constructor.newInstance();
            command.register(discordApi);
        }
    }

    public Command getCommand(String commandName, SessionManager sessionManager, SlashCommandCreateEvent event) {
        Class<? extends Command> commandClass = commandsMap.get(commandName);
        if (commandClass != null) {
            try {
                Constructor<? extends Command> constructor = commandClass.getConstructor(SessionManager.class,
                        SlashCommandCreateEvent.class, ExecutorService.class);
                return constructor.newInstance(sessionManager, event, statisticsExecutorService);
            } catch (NoSuchMethodException noSuchMethodException) {
                Logger.error(noSuchMethodException,
                        "This shouldn't happen! NoSuchMethod exception while constructing a command!");
            } catch (InstantiationException instantiationException) {
                Logger.error(instantiationException,
                        "This shouldn't happen! Instantiation exception while constructing a command!");
            } catch (IllegalAccessException illegalAccessException) {
                Logger.error(illegalAccessException,
                        "This shouldn't happen! IllegalAccess exception while constructing a command!");
            } catch (IllegalArgumentException illegalArgumentException) {
                Logger.error(illegalArgumentException,
                        "This shouldn't happen! IllegalArgument exception while constructing a command!");
            } catch (InvocationTargetException invocationTargetException) {
                Logger.error(invocationTargetException,
                        "This shouldn't happen! InvocationTarget exception while constructing a command!");
            }
        }
        return null;
    }
}
