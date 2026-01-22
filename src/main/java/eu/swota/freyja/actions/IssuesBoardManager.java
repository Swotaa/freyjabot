package eu.swota.freyja.actions;

import eu.swota.freyja.database.DatabaseManager;
import net.dv8tion.jda.api.JDA;

//TODO: Unfinished, not even sure if this class is necessary
public class IssuesBoardManager {
    private final JDA jda;
    private final DatabaseManager db;

    public IssuesBoardManager(JDA jda, DatabaseManager db) {
        this.jda = jda;
        this.db = db;
    }


}
