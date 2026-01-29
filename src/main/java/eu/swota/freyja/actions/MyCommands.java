package eu.swota.freyja.actions;

import eu.swota.freyja.BotMain;
import eu.swota.freyja.database.DatabaseManager;
import eu.swota.freyja.sheets.SheetConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.attribute.IGuildChannelContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
            case "addissue":
                addIssue(event);
                break;
            case "removeissue":
                removeIssue(event);
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

    public void addIssue(SlashCommandInteractionEvent event)
    {
        List<ForumTag> validTags = new ArrayList<>();
        List<String> invalidTags = new ArrayList<>();
        long forumId = SheetConfig.get().issueBoardId;

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command must be used in a server!").setEphemeral(true).queue();
            return;
        }

        ForumChannel forum = guild.getForumChannelById(forumId);
        if (forum == null) {
            event.reply("Forum introuvable ou inaccessible").setEphemeral(true).queue();
            return;
        }

        String title = event.getOption("title").getAsString();
        String message = event.getOption("message").getAsString();


        OptionMapping opt = event.getOption("tags");
        if (opt != null) {
            String[] rawTags = Arrays.stream(opt.getAsString().split(",")).limit(5).toArray(String[]::new);

            for (String raw : rawTags) {
                String name = raw.trim();

                forum.getAvailableTags().stream()
                        .filter(t -> t.getName().equalsIgnoreCase(name))
                        .findFirst()
                        .ifPresentOrElse(
                                validTags::add,
                                () -> invalidTags.add(name)
                        );
            }
        }

        db.addIssue(guild.getId(), title, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

        forum.createForumPost(title, MessageCreateData.fromContent(message))
                .setTags(validTags)
                .queue();

        StringBuilder recap = new StringBuilder("📌 **Post créé**\n");

        if (!validTags.isEmpty()) {
            recap.append("✅ Tags ajoutés : ")
                    .append(
                            validTags.stream()
                                    .map(ForumTag::getName)
                                    .collect(Collectors.joining(", "))
                    )
                    .append("\n");
        }

        if (!invalidTags.isEmpty()) {
            recap.append("❌ Tags inexistants : ")
                    .append(String.join(", ", invalidTags))
                    .append("\n");
        }

        event.reply(recap.toString())
                .setEphemeral(true)
                .queue();

        System.out.printf(
                "Forum post créé | tags=%s | invalid=%s%n",
                validTags.stream().map(ForumTag::getName).toList(),
                invalidTags
        );
    }

    public void removeIssue(SlashCommandInteractionEvent event)
    {
        String issueId = event.getOption("issue_id").getAsString();
        Guild guild =  event.getGuild();

        if (guild == null) {
            event.reply("This command must be used in a server!").setEphemeral(true).queue();
            return;
        }

        ThreadChannel thread = guild.getThreadChannelById(issueId);
        if (thread != null) {
            thread.delete().queue();
        }

        db.deleteIssue(guild.getId(), issueId);
        String msg = String.format("Issue %s has been deleted.", issueId);
        event.reply(msg).queue();
        System.out.println(msg);
    }
}
