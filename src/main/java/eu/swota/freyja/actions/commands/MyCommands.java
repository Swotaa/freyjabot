package eu.swota.freyja.actions.commands;

import eu.swota.freyja.database.DatabaseManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import static eu.swota.freyja.actions.commands.EventCommands.cancelEvent;
import static eu.swota.freyja.actions.commands.EventCommands.eventCreator;
import static eu.swota.freyja.actions.commands.IssueBoardCommands.*;
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
            case "event" -> eventCreator(event, db);
            case "register" -> registerUser(event);
            case "cancelevent" -> cancelEvent(event, db);

            // all commands that have to do with issues
            case "addissue" -> addIssue(event);
            case "removeissue" -> removeIssue(event);
            case "listissues" -> getIssuesId(event, db);
            case "editissuetags" -> editIssueTags(event);

            //test to see of the sheets can be written to
            case "testsheets" -> {
                event.deferReply(true).queue();
                testConnectionAndWrite("Test" + event.getMember());
                event.getHook().sendMessage("Test executed").queue();
            }
            default -> event.reply("This command does not exist !").setEphemeral(true).queue();
        }
    }

    // register a user into the database
    public void registerUser(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        String username = event.getUser().getName();
        db.saveUser(userId, username);
    }
}
