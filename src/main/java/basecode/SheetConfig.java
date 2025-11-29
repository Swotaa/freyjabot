package basecode;

import com.google.gson.Gson;

import java.io.FileReader;
import java.util.Map;

public class SheetConfig {
    public Map<String, UserConfig> users;

    public record UserConfig(String name, String startColumn) {}

    private static SheetConfig INSTANCE;

    public static void load() {
        try (FileReader reader = new FileReader("config.json")) {
            INSTANCE = new Gson().fromJson(reader, SheetConfig.class);
            System.out.println("Config chargée : " + INSTANCE.users.size() + " utilisateurs trouvés");
        } catch (Exception e) {
            System.err.println("ERREUR : impossible de lire config.json ! Vérifie qu'il est à la racine du projet.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static SheetConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }
}
