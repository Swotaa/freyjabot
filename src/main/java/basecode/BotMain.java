package basecode;

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

        // Disable the fallback logger
        JDALogger.setFallbackLoggerEnabled(false);
        Dotenv dotenv = Dotenv.load();
        String BOT_TOKEN = dotenv.get("DISCORD_TOKEN");
        String DB_URL = dotenv.get("DB_URL");

        System.out.println("🔄 Initializing database...");
        db = new DatabaseManager(DB_URL);

        System.out.println("🔄 Bot is connecting...");

        JDA bot = JDABuilder.createDefault(BOT_TOKEN)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Doom 64"))
                .addEventListeners(new MyListener(), new MyCommands(db))
                .build();

        bot.awaitReady();
        System.out.println("✅ Bot is up and ready!");

        System.out.println("🔄 Initializing reminder system...");
        reminderManager = new EventReminderManager(bot, db);
        reminderManager.loadExistingEvents();

        bot.updateCommands().addCommands(
                Commands.slash("members", "Gives the number of members in the server"),
                Commands.slash("ping", "Pong!"),
                Commands.slash("event", "Create an event")
                        .addOption(OptionType.STRING, "name", "Event name", true)
                        .addOption(OptionType.STRING, "date", "Date (format: DD/MM/YYYY HH:mm)", true)
                        .addOption(OptionType.STRING, "description", "Description", false)
                        .addOption(OptionType.INTEGER, "duration", "Duration (hours)", false)
                        .addOption(OptionType.STRING, "location", "Event location", false),
                Commands.slash("register", "Register yourself into the database")
        ).queue();

        System.out.println("\uD83D\uDCDD Commands registered!");

        // Shutdown hook to close the database properly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🔄 Shutting down bot...");
            reminderManager.shutdown();
            db.close();
        }));
    }

    // utils for some commands
    public static DatabaseManager getDatabase() {
        return db;
    }

    public static EventReminderManager getReminderManager() {
        return reminderManager;
    }
}
