package eu.swota.freyja.actions.commands;

import eu.swota.freyja.database.DatabaseManager;
import eu.swota.freyja.sheets.SheetConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class IssueBoardCommands {
    // creates a post
    public static void addIssue(SlashCommandInteractionEvent event) {
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
            event.reply("Forum not found or inaccessible").setEphemeral(true).queue();
            return;
        }
        String title = event.getOption("title").getAsString();
        String message = event.getOption("message").getAsString();
        OptionMapping tags = event.getOption("tags");

        sortTags(tags, forum, validTags, invalidTags);

        event.deferReply(true).queue();

        StringBuilder recap = new StringBuilder("📌 **Post created**\n");

        forum.createForumPost(title, MessageCreateData.fromContent(message))
            .setTags(validTags)
            .queue(post -> {

                long postId = post.getThreadChannel().getIdLong();

                String link = "<https://discord.com/channels/%s/%d>"
                        .formatted(guild.getId(), postId);
                recap.append("🔗 Post : ").append(link).append("\n");

                if (!validTags.isEmpty()) {
                    recap.append("✅ Tags added : ")
                        .append(validTags.stream()
                            .map(ForumTag::getName)
                            .collect(Collectors.joining(", ")))
                        .append("\n");
                }

                if (!invalidTags.isEmpty()) {
                    recap.append("❌ Non-existent tags : ")
                        .append(String.join(", ", invalidTags))
                        .append("\n");
                }

                event.getHook()
                    .sendMessage(recap.toString())
                    .queue();
            });
    }

    // removes the post
    public static void removeIssue(SlashCommandInteractionEvent event) {
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
        String msg = String.format("Issue %s has been deleted.", issueId);
        event.getHook()
            .sendMessage(msg)
            .queue();
        System.out.println(msg);
    }

    public static void getIssuesId(SlashCommandInteractionEvent event, DatabaseManager db){
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

    // edits a post tags
    public static void editIssueTags(SlashCommandInteractionEvent event){
        List<ForumTag> validTagsToAdd = new ArrayList<>();
        List<ForumTag> validTagsToRemove = new ArrayList<>();
        List<String> invalidTags = new ArrayList<>();
        List<ForumTag> actuallyAdded = new ArrayList<>();
        List<ForumTag> actuallyRemoved = new ArrayList<>();
        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("This command must be used in a server!")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        long forumId = SheetConfig.get().issueBoardId;
        ForumChannel forum = guild.getForumChannelById(forumId);
        if (forum == null) {
            event.reply("Forum not found or inaccessible")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        int postId = event.getOption("issue_id").getAsInt();
        ThreadChannel thread = guild.getThreadChannelById(postId);

        if (thread == null) {
            event.reply("Thread not found or inaccessible")
                    .setEphemeral(true)
                    .queue();
            return;
        }
        event.deferReply(true).queue();

        OptionMapping tagsToAdd = event.getOption("tags_add");
        OptionMapping tagsToRemove = event.getOption("tags_remove");
        System.out.printf("tagsToAdd: %s%n", tagsToAdd);
        System.out.printf("tagsToRemove: %s%n", tagsToRemove);

        sortTags(tagsToAdd, forum, validTagsToAdd, invalidTags);
        sortTags(tagsToRemove, forum, validTagsToRemove, invalidTags);

        List<ForumTag> ThreadTags = new ArrayList<>(thread.getAppliedTags());
        System.out.printf("updatedTags: %s%n", ThreadTags);

        ThreadTags.removeIf(tag -> {
            boolean shouldRemove = validTagsToRemove.stream()
                    .anyMatch(toRemove -> toRemove.getIdLong() == tag.getIdLong());

            if (shouldRemove) {
                actuallyRemoved.add(tag);
            }
            return shouldRemove;
        });
        System.out.printf("removedTags: %s%n", actuallyRemoved);

        for (ForumTag tagToAdd : validTagsToAdd) {
            boolean alreadyPresent = ThreadTags.stream()
                    .anyMatch(existing -> existing.getIdLong() == tagToAdd.getIdLong());

            if (!alreadyPresent) {
                ThreadTags.add(tagToAdd);
                actuallyAdded.add(tagToAdd);
            }
        }
        System.out.printf("addedTags: %s%n", actuallyAdded);

        thread.getManager()
            .setAppliedTags(ThreadTags)
            .queue(success -> {
                StringBuilder response = new StringBuilder();
                if (!actuallyAdded.isEmpty()) {
                    response.append("Added: ")
                        .append(actuallyAdded.stream()
                            .map(ForumTag::getName)
                            .toList())
                        .append("\n");
                }
                if (!actuallyRemoved.isEmpty()) {
                    response.append("Removed: ")
                        .append(actuallyRemoved.stream()
                            .map(ForumTag::getName)
                            .toList())
                        .append("\n");
                }
                if (!invalidTags.isEmpty()) {
                    response.append("Invalid: ")
                        .append(invalidTags)
                        .append("\n");
                }
                if (response.isEmpty()) {
                    response.append("No changes made.");
                }
                event.getHook()
                    .sendMessage(response.toString())
                    .queue();
            },
            error -> event.getHook()
                .sendMessage("Failed to update tags, error : %s".formatted(error.getMessage()))
                .queue()
            );
    }

    private static void sortTags(OptionMapping tags, ForumChannel forum, List<ForumTag> validTags, List<String> invalidTags) {
        if (tags != null) {
            String[] rawTags = Arrays.stream(tags.getAsString().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toMap(
                    String::toLowerCase,
                    s -> s,
                    (existing, replacement) -> existing,
                    LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(5)
                .toArray(String[]::new);
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
