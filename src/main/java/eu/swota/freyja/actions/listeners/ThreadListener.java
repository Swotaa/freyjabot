package eu.swota.freyja.actions.listeners;

import eu.swota.freyja.database.DatabaseManager;
import eu.swota.freyja.sheets.SheetConfig;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ThreadListener extends ListenerAdapter {

    private final DatabaseManager db;
    public ThreadListener(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        long forumId = SheetConfig.get().issueBoardId;

        if (!(event.getChannel() instanceof ThreadChannel thread)) {
            return;
        }

        if (thread.getParentChannel().getIdLong() != forumId) {
            return;
        }

        long deletedThreadId = thread.getIdLong();

        System.out.println("Thread supprimé : " + deletedThreadId);
        db.deleteIssue(event.getGuild().getId(), thread.getId());
    }
}
