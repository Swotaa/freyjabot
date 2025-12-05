package eu.swota.freyja.actions;

import eu.swota.freyja.BotMain;
import eu.swota.freyja.database.DatabaseManager;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.LocalDateTime;

import static eu.swota.freyja.sheets.SheetManager.testConnectionAndWrite;

public class MyCommands extends ListenerAdapter
{
    private final DatabaseManager db;
    public MyCommands(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        switch (event.getName()) // I am using a switch, but maybe that's not the best option
        {
            case "ping": // ping to try if the bot is online and working
                event.reply("\uD83C\uDFD3Pong!").queue();
                break;
            case "members": // useful if you want to know how many people are on your server
                event.reply("There are " + event.getGuild().getMemberCount() + " members in this server.").queue();
                break;
            case "event": // creates an event and initialises a reminder for it
                eventCreator(event);
                break;
            case "register": // register a user into the database
                registerUser(event);
                break;
            case "cancelevent": // properly cancel an event from discord and delete the reminders
                cancelEvent(event);
                break;
            case "testsheets": // This command is bad, like doesn't even answer, and you get a discord error message or something
                // But at least, it is working, writing shit into a sheets.
                testConnectionAndWrite("Test" + event.getMember().toString());
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
                                            "📍 " + location + "\n" +
                                            "\uD83E\uDEAA " + scheduledEvent.getId() + "\n@everyone").queue();
                                    TextChannel eventsChannel = guild.getTextChannelsByName("events", true).stream().findFirst().orElse(null);
                                    if (eventsChannel != null) {
                                        String eventLink = "https://discord.com/events/" + guild.getId() + "/" + scheduledEvent.getId();
                                        eventsChannel.sendMessage(eventLink).queue();
                                    } else {
                                        event.getChannel().sendMessage("⚠\uFE0F Events channel not found !").queue();
                                    }
                                    db.saveEvent(scheduledEvent.getId(), event.getGuild().getId(), name, description, dateStr, location, duration);
                                    BotMain.getReminderManager().scheduleReminders(scheduledEvent.getId(), event.getGuild().getId() ,name, dateStr);
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

    public void cancelEvent(SlashCommandInteractionEvent event)
    {
        String eventId = event.getOption("event_id").getAsString();
        boolean res = db.deleteEvent(eventId);
        if(!res)
        {
            event.reply("Cannot find any event with id : " + eventId).queue();
        }
        else {
            event.reply("Event \"" + eventId + "\" has been deleted.").queue();
            Guild guild = event.getGuild();
            ScheduledEvent scheduledEvent = guild.getScheduledEventById(eventId);
            if(scheduledEvent != null) {
                scheduledEvent.delete().queue();
            }
        }
    }

}