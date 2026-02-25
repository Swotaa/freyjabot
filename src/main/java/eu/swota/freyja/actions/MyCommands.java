package eu.swota.freyja.actions;

import eu.swota.freyja.BotMain;
import eu.swota.freyja.database.DatabaseManager;
import eu.swota.freyja.sheets.SheetConfig;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static eu.swota.freyja.sheets.SheetManager.testConnectionAndWrite;

public class MyCommands extends ListenerAdapter
{
    private final DatabaseManager db;
    public MyCommands(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // I am using a switch, but maybe that's not the best option
        switch (event.getName())
        {
            // ping to try if the bot is online and working
            case "ping" -> event.reply("\uD83C\uDFD3Pong!").queue();
            // useful if you want to know how many people are on your server
            case "members" -> event.reply("There are " + event.getGuild().getMemberCount() + " members in this server.").queue();
            case "event" -> eventCreator(event);
            case "register" -> registerUser(event);
            case "cancelevent" -> cancelEvent(event);

            // all commands that have to do with issues
            case "addissue" -> addIssue(event);
            case "removeissue" -> removeIssue(event);
            case "listissues" -> getIssuesId(event);
            case "editissuetags" -> editIssueTags(event);

            //test to see of the sheets can be written to
            case "testsheets" -> {
                event.deferReply(true).queue();
                testConnectionAndWrite("Test" + event.getMember());
                event.getHook().sendMessage("Test terminé").queue();
            }
            default -> event.reply("Cette commande n'existe pas !").setEphemeral(true).queue();
        }
    }

    // creates an event and initialises a reminder for it
    private void eventCreator(SlashCommandInteractionEvent event) {
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

    // register a user into the database
    public void registerUser(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        String username = event.getUser().getName();
        db.saveUser(userId, username);
    }

    // properly cancel an event from discord and delete the reminders
    public void cancelEvent(SlashCommandInteractionEvent event) {
        String eventId = event.getOption("event_id").getAsString();
        boolean res = db.deleteEvent(eventId);
        if(!res) {
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

    public void addIssue(SlashCommandInteractionEvent event) {
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
        OptionMapping tags = event.getOption("tags");

        sortTags(tags, forum, validTags, invalidTags);

        event.deferReply(true).queue();

        StringBuilder recap = new StringBuilder("📌 **Post créé**\n");

        forum.createForumPost(title, MessageCreateData.fromContent(message))
                .setTags(validTags)
                .queue(post -> {

                    long postId = post.getThreadChannel().getIdLong();

                    // DB update (on peut le faire ici aussi si tu veux être ultra safe)1476143932311077005
                    db.addIssue(
                            guild.getId(),
                            String.format("%d", postId),
                            LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                            )
                    );

                    recap.append("🔗 Post : <#").append(postId).append(">\n");

                    if (!validTags.isEmpty()) {
                        recap.append("✅ Tags ajoutés : ")
                            .append(validTags.stream()
                                .map(ForumTag::getName)
                                .collect(Collectors.joining(", ")))
                            .append("\n");
                    }

                    if (!invalidTags.isEmpty()) {
                        recap.append("❌ Tags inexistants : ")
                            .append(String.join(", ", invalidTags))
                            .append("\n");
                    }

                    event.getHook()
                        .sendMessage(recap.toString())
                        .queue();
                });
    }

    public void removeIssue(SlashCommandInteractionEvent event) {
        String issueId = event.getOption("issue_id").getAsString();
        Guild guild =  event.getGuild();
        if (guild == null) {
            event.reply("This command must be used in a server!").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();

        ThreadChannel thread = guild.getThreadChannelById(issueId);
        if (thread != null) {
            thread.delete().queue();
        }
        db.deleteIssue(guild.getId(), issueId);
        String msg = String.format("Issue %s has been deleted.", issueId);
        event.getHook()
                .sendMessage(msg)
                .queue();
        System.out.println(msg);
    }

    public void getIssuesId(SlashCommandInteractionEvent event){
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command must be used in a server!").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();
        StringBuilder msg = new StringBuilder("List of issues :\n");
        try {
            ResultSet rs = db.getAllIssues();
            var next = rs.next();
            while (next) {
                String issueId = rs.getString("issue_id");
                ThreadChannel thread = guild.getThreadChannelById(Long.parseLong(issueId));
                if (thread != null) {
                    msg.append(String.format("%s : %s\n", thread.getName(), issueId));
                }
                next = rs.next();
            }
        } catch (SQLException e) {
            event.getHook()
                    .sendMessage("The command ran into an error : " + e.getMessage())
                    .queue();
            return;
        }
        event.getHook()
                .sendMessage(msg.toString())
                .queue();
    }

    public void editIssueTags(SlashCommandInteractionEvent event){
        List<ForumTag> validTagsToAdd = new ArrayList<>();
        List<ForumTag> validTagsToRemove = new ArrayList<>();
        List<String> invalidTags = new ArrayList<>();
        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("This command must be used in a server!").setEphemeral(true).queue();
            return;
        }

        long forumId = SheetConfig.get().issueBoardId;
        ForumChannel forum = guild.getForumChannelById(forumId);
        if (forum == null) {
            event.reply("Forum introuvable ou inaccessible").setEphemeral(true).queue();
            return;
        }

        int postId = event.getOption("issue_id").getAsInt();
        ThreadChannel thread = guild.getThreadChannelById(postId);

        if (thread == null) {
            event.reply("Thread introuvable ou inaccessible").setEphemeral(true).queue();
            return;
        }

        OptionMapping tagsToAdd = event.getOption("tags_add");
        OptionMapping tagsToRemove = event.getOption("tags_remove");
        sortTags(tagsToAdd, forum, validTagsToAdd, invalidTags);
        sortTags(tagsToRemove, forum, validTagsToRemove, invalidTags);

        List<ForumTag> updatedTags = new ArrayList<>(thread.getAppliedTags());

        updatedTags.removeIf(tag ->
            validTagsToRemove.stream()
                .anyMatch(toRemove -> toRemove.getIdLong() == tag.getIdLong())
        );

        for (ForumTag tagToAdd : validTagsToAdd) {
            boolean alreadyPresent = updatedTags.stream()
                .anyMatch(existing -> existing.getIdLong() == tagToAdd.getIdLong());

            if (!alreadyPresent) {
                updatedTags.add(tagToAdd);
            }
        }

        thread.getManager()
            .setAppliedTags(updatedTags)
            .queue();
    }

    private static void sortTags(OptionMapping tags, ForumChannel forum, List<ForumTag> validTags, List<String> invalidTags) {
        if (tags != null) {
            String[] rawTags = Arrays.stream(tags.getAsString().split(",")).limit(5).toArray(String[]::new);
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
    }
}
