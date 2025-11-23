package basecode;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.LocalDateTime;

public class MyCommands extends ListenerAdapter
{
    private final DatabaseManager db;
    public MyCommands(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        switch (event.getName())
        {
            case "ping":
                event.reply("Pong!").queue();
                break;
            case "members":
                event.reply("There are " + event.getGuild().getMemberCount() + " members in this server.").queue();
                break;
            case "event":
                eventCreator(event);
                break;
            case "register":
                registerUser(event);
                break;
        }
    }

    private void eventCreator(SlashCommandInteractionEvent event)
    {
        String name = event.getOption("name").getAsString();

        String dateStr = event.getOption("date").getAsString();

        String description = event.getOption("description") != null ?
                event.getOption("description").getAsString() : "No description!";

        int duration = event.getOption("duration") != null ?
                event.getOption("duration").getAsInt() : 1;

        String location = event.getOption("location") != null ?
                event.getOption("location").getAsString() : "IUT Clermont Auvergne";

        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("This command must be used in a server!").setEphemeral(true).queue();
            return;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            LocalDateTime localDateTime = LocalDateTime.parse(dateStr, formatter);
            OffsetDateTime startTime = localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();

            guild.createScheduledEvent(name, location, startTime, startTime.plusHours(2))
                    .setDescription(description)
                    .queue(
                            scheduledEvent -> {
                                    event.reply("🎉 **New Event Created !**\n" +
                                            "➡\uFE0F\u200B " + name + "\n" +
                                            "📅 " + dateStr + "\n" +
                                            "📍 " + location).queue();
                                    TextChannel eventsChannel = guild.getTextChannelsByName("events", true).stream().findFirst().orElse(null);
                                    if (eventsChannel != null) {
                                        String eventLink = "https://discord.com/events/" + guild.getId() + "/" + scheduledEvent.getId();
                                        eventsChannel.sendMessage(eventLink).queue();
                                    } else {
                                        event.getChannel().sendMessage("⚠\uFE0F Events channel not found !").queue();
                                    }
                                    db.saveEvent(scheduledEvent.getId(), name, description, dateStr, location, duration);
                                    BotMain.getReminderManager().scheduleReminders(scheduledEvent.getId(), name, dateStr);
                                },
                            error -> {
                                event.reply("❌ Error encountered : " + error.getMessage()).setEphemeral(true).queue();
                            }
                    );

        } catch (Exception e) {
            event.reply("❌ If you see this message, there are two options : \n- You entered an invalid date format -> Please use : DD/MM/YYYY HH:mm (e.g : 25/12/2024 20:00)\n- You tried to create an event in the past").setEphemeral(true).queue();
        }
    }

    public void registerUser(SlashCommandInteractionEvent event)
    {
        String userId = event.getUser().getId();
        String username = event.getUser().getName();
        db.saveUser(userId, username);
    }


}