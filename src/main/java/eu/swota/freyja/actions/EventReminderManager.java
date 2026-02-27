package eu.swota.freyja.actions;

import eu.swota.freyja.database.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventReminderManager {
    private final ScheduledExecutorService scheduler;
    private final JDA jda;
    private final DatabaseManager db;

    public EventReminderManager(JDA jda, DatabaseManager db) {
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.jda = jda;
        this.db = db;
    }

    // Schedule all reminders for an event
    public void scheduleReminders(String eventId, String guildId,String eventName, String eventDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime eventDateTime = LocalDateTime.parse(eventDate, formatter);
            LocalDateTime now = LocalDateTime.now();

            // Schedule reminders: 7 days, 3 days, 2 days, 1 day before
            scheduleReminder(eventId, guildId, eventName, eventDateTime, now, 7);
            scheduleReminder(eventId, guildId, eventName, eventDateTime, now, 3);
            scheduleReminder(eventId, guildId, eventName, eventDateTime, now, 2);
            scheduleReminder(eventId, guildId, eventName, eventDateTime, now, 1);

            System.out.println("✅ Reminders scheduled for event: " + eventName);
        } catch (Exception e) {
            System.err.println("❌ Error scheduling reminders: " + e.getMessage());
        }
    }

    private void scheduleReminder(String eventId, String guildId, String eventName,
                                  LocalDateTime eventDateTime, LocalDateTime now, int daysBeforeEvent) {

        LocalDateTime reminderTime = eventDateTime.minusDays(daysBeforeEvent);

        // Only schedule if the reminder time is in the future
        if (reminderTime.isAfter(now)) {
            long delayInSeconds = Duration.between(now, reminderTime).getSeconds();

            scheduler.schedule(() -> sendReminder(eventName, guildId, eventId, daysBeforeEvent), delayInSeconds, TimeUnit.SECONDS);

            System.out.println("📅 Reminder scheduled for " + daysBeforeEvent + " days before: " + eventName);
        }
    }

    private void sendReminder(String eventName, String guildId ,String eventId, int daysBeforeEvent) {
        try {
            Guild guild = jda.getGuildById(guildId); // Could be replaced with a SQL query
            // Make sur that guild is defined
            assert guild != null;
            // Find the #events channel
            TextChannel eventsChannel = guild.getTextChannelsByName("events", true).stream()
                    .findFirst()
                    .orElse(null);

            if (eventsChannel == null) {
                System.err.println("❌ #events channel not found in guild: " + guild.getName());
                return;
            }

            // Create the event link (dunno why i did that)
            String eventLink = "https://discord.com/events/" + guild.getId() + "/" + eventId;

            // Choose emoji based on days
            String emoji = switch (daysBeforeEvent) {
                case 7 -> "📅";
                case 3 -> "⏰";
                case 2 -> "⚠️";
                case 1 -> "🔔";
                default -> "📢";
            };

            String message = emoji + " **Reminder: " + eventName + "**\n" +
                    "📍 Starting in **" + daysBeforeEvent + " day" + (daysBeforeEvent > 1 ? "s" : "") + "**!\n" +
                    "\uD83E\uDEAA Id : " + eventId;

            eventsChannel.sendMessage(message).queue(
                    _ -> System.out.println("✅ Reminder sent for: " + eventName),
                    error -> System.err.println("❌ Failed to send reminder: " + error.getMessage())
            );

        } catch (Exception e) {
            System.err.println("❌ Error sending reminder: " + e.getMessage());
        }
    }

    // Shutdown the scheduler properly
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            System.out.println("✅ Reminder scheduler shut down!");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    // Load existing events from the database and schedule their reminders
    public void loadExistingEvents() {
        try {
            var rs = db.getAllUpcomingEvents();
            while (rs != null && rs.next()) {
                String eventId = rs.getString("event_id");
                String guildId = rs.getString("guild_id");
                String name = rs.getString("name");
                String date = rs.getString("date");

                scheduleReminders(eventId, guildId, name, date);
            }
            System.out.println("✅ Existing events loaded and reminders scheduled!");
        } catch (Exception e) {
            System.err.println("❌ Error loading existing events: " + e.getMessage());
        }
    }
}
