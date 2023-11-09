package dev.kmfg.musicbot.core.commands.executors;

import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.Optional;

public class SeekCommand extends Command {
    public static final String COMMAND_NAME = "seek";
    private static final String DESCRIPTION = "Seek to a specific position in the current playing song. Just one of three options is required.";
    public SeekCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent) {
        super(sessionManager, slashCommandEvent);
    }

    public SeekCommand() {
        super();
    }

    @Override
    public void register(DiscordApi discordApi) {
        ArrayList<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption
                .create(SlashCommandOptionType.LONG, "seconds", "The amount of seconds"));
        options.add(SlashCommandOption
                .create(SlashCommandOptionType.LONG, "minutes", "The amount of minutes"));
        options.add(SlashCommandOption
                .create(SlashCommandOptionType.LONG, "hours", "The amount of hours"));

        SlashCommand.with(COMMAND_NAME, DESCRIPTION, options).createGlobal(discordApi);
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String getCommandDescription() {
        return DESCRIPTION;
    }

    @Override
    public void execute() {
        EnsuredSlashCommandInteraction ensuredInteraction = this.getEnsuredInteraction(null);
        if(ensuredInteraction == null) return;

        Optional<String> seconds = this.slashCommandEvent
            .getSlashCommandInteraction()
            .getArgumentStringRepresentationValueByName("seconds");

        Optional<String> minutes = this.slashCommandEvent
            .getSlashCommandInteraction()
            .getArgumentStringRepresentationValueByName("minutes");

        Optional<String> hours = this.slashCommandEvent
            .getSlashCommandInteraction()
            .getArgumentStringRepresentationValueByName("hours");

        // ensure at least one value is present
        if(seconds.isEmpty() && minutes.isEmpty() && hours.isEmpty()) {
            this.messageSender.sendEmptyParameterEmbed("HMS");
        }

        // ensure we can parse present values
        if(presentOptionalsArentParseable(seconds, minutes, hours)) {
            this.messageSender.sendBadParameterEmbed("HMS");
            return;
        }

        AudioTrack relevantAudioTrack = ensuredInteraction.getAudioSession().getLavaSource().getCurrentPlayingAudioTrack();

        long seekMs = seekToPositionInNowPlayingTrack(relevantAudioTrack,seconds, minutes, hours);

        if(seekMs < 0) {
            this.messageSender.sendBadParameterEmbed("HMS");
            return;
        }
        else {
            this.messageSender.sendSeekEmbed(relevantAudioTrack, seekMs);
        }
    }

    private boolean presentOptionalsArentParseable(Optional<String> seconds, Optional<String> minutes, Optional<String> hours) {
       try {
           long temp = 0;

           if(seconds.isPresent()) {
               temp = Long.parseLong(seconds.get());
               if(temp < 0) throw new NumberFormatException();
           }

           if(minutes.isPresent()) {
               temp = Long.parseLong(minutes.get());
               if(temp < 0) throw new NumberFormatException();
           }
           if(hours.isPresent()) {
               temp = Long.parseLong(hours.get());
               if(temp < 0) throw new NumberFormatException();
           }

           return false;
       }
       catch(NumberFormatException numberFormatException) {
           org.tinylog.Logger.error(numberFormatException, "Bad parameter in /seek command interaction");
           return true;
       }
    }

    private long seekToPositionInNowPlayingTrack(AudioTrack audioTrack, Optional<String> seconds, Optional<String> minutes, Optional<String> hours) {
        long totalSeekMs = 0;
        long trackDuration = audioTrack.getDuration();

        if(seconds.isPresent()) {
            totalSeekMs += Long.parseLong(seconds.get()) * 1000; 
        }
        if(minutes.isPresent()) {
            totalSeekMs += Long.parseLong(minutes.get()) * 1000 * 60;
        }
        if(hours.isPresent()) {
            totalSeekMs += Long.parseLong(hours.get()) * 1000 * 60 * 60;
        }

        if(totalSeekMs < trackDuration) {
            audioTrack.setPosition(totalSeekMs);
            return totalSeekMs;
        }
        else {
            return -1;
        }
    }
}
