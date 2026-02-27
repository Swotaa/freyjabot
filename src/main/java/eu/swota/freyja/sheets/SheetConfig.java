package eu.swota.freyja.sheets;

import com.google.gson.Gson;

import java.io.FileReader;
import java.util.Map;

public class SheetConfig {
    public Map<String, UserConfig> users;
    public long issueBoardId;

    public record UserConfig(String name, String startColumn) {}

    private static SheetConfig INSTANCE;

    public static void load() {
        try (FileReader reader = new FileReader("config.json")) {
            INSTANCE = new Gson().fromJson(reader, SheetConfig.class);

            if (INSTANCE == null) {
                throw new IllegalStateException("Config vide ou invalide");
            }

            if (INSTANCE.issueBoardId == 0L) {
                System.err.println("issueBoard non défini dans config.json");
            }

            System.out.printf(
                    "Config chargée : %d utilisateurs trouvés, forum=%d%n",
                    INSTANCE.users.size(),
                    INSTANCE.issueBoardId
            );

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public static SheetConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }
}
