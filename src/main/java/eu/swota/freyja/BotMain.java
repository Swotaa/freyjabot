package eu.swota.freyja;

import eu.swota.freyja.actions.EventReminderManager;
import eu.swota.freyja.actions.MyCommands;
import eu.swota.freyja.actions.TimelogListener;
import eu.swota.freyja.database.DatabaseManager;
import eu.swota.freyja.sheets.SheetConfig;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.JDALogger;

public class BotMain {

    public static DatabaseManager db;
    private static EventReminderManager reminderManager;

    public static void main(String[] args) throws Exception {
        SheetConfig.load();
        // Disable the fallback logger
        JDALogger.setFallbackLoggerEnabled(false);
        // Loading the .env file
        Dotenv dotenv = Dotenv.load();
        // Initialising BOT_TOKEN and DB_URL
        String BOT_TOKEN = dotenv.get("DISCORD_TOKEN");
        String DB_URL = dotenv.get("DB_URL");

        System.out.println("🔄 Initializing database...");
        // Initialising a DatabaseManager
        db = new DatabaseManager(DB_URL);

        System.out.println("🔄 Bot is connecting...");

        // Building the bot, initialising status and activity
        JDA bot = JDABuilder.createDefault(BOT_TOKEN)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Doom 64"))
                .addEventListeners(new TimelogListener(), new MyCommands(db))
                .build();

        bot.awaitReady();
        System.out.println("✅ Bot is up and ready!");

        System.out.println("🔄 Initializing reminder system...");
        // This initialises the reminder system
        reminderManager = new EventReminderManager(bot, db);
        reminderManager.loadExistingEvents();

        // Syncing slash commands
        bot.updateCommands().addCommands(
                Commands.slash("members", "Gives the number of members in the server"),
                Commands.slash("ping", "Pong!"),
                Commands.slash("event", "Create an event")
                        .addOption(OptionType.STRING, "name", "Event name", true)
                        .addOption(OptionType.STRING, "date", "Date (format: DD/MM/YYYY HH:mm)", true)
                        .addOption(OptionType.STRING, "description", "Description", false)
                        .addOption(OptionType.INTEGER, "duration", "Duration (hours)", false)
                        .addOption(OptionType.STRING, "location", "Event location", false),
                Commands.slash("register", "Register yourself into the database"),
                Commands.slash("cancelevent", "Cancel an event by giving its id")
                        .addOption(OptionType.STRING, "event_id", "Event id", true),
                Commands.slash("testsheets", "Test the connection with the google spreadsheet"),
                Commands.slash("addissue", "Add an Issue to the IssueBoard")
                        .addOption(OptionType.STRING, "title", "Title of the Issue", true)
                        .addOption(OptionType.STRING, "message", "Message of the issue", true)
                        .addOption(OptionType.STRING, "tags", "Tags séparés par des virgules (ex: bug,urgent), max 5 par post", false),
                Commands.slash("removeissue",  "Remove an issue from the issue board")
                        .addOption(OptionType.STRING, "issue_id", "Issue id", true)
        ).queue();

        System.out.println("\uD83D\uDCDD Commands registered!");

        // Shutdown hook to close the database properly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🔄 Shutting down bot...");
            reminderManager.shutdown();
            db.close();
        }));
    }

    // These are the getter if we need
    public static DatabaseManager getDatabase() {
        return db;
    }

    public static EventReminderManager getReminderManager() {
        return reminderManager;
    }
}
