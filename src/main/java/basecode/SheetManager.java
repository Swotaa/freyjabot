package basecode;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class SheetManager {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String SPREADSHEET_ID = dotenv.get("SPREADSHEET_ID"); // ← Sheet ID (from the dotenv)
    private static final String SHEET_NAME = "Suivi";
    private static final Sheets sheetsService;

    static {
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new FileInputStream("credentials.json"))
                    .createScoped(SheetsScopes.SPREADSHEETS);

            sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Discord Bot Test")
                    .build();

            System.out.println("Google Sheets initialisé avec succès !");
        } catch (Exception e) {
            System.err.println("ÉCHEC initialisation Google Sheets :");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static boolean logManualWork(long userId, String username, int minutes, String description) {
        var config = SheetConfig.get();
        var userConfig = config.users.get(String.valueOf(userId));

        if (userConfig == null) {
            System.out.println("User not configured into config.json : " + username + " (" + userId + ")");
            return false;
        }

        String startCol = userConfig.startColumn();
        String endCol = columnLetter(letterToColumn(startCol) + 3); // A→D, F→I, K→N, etc.
        String range = SHEET_NAME + "!" + startCol + ":" + endCol;

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String prettyTime = formatMinutes(minutes);

        List<List<Object>> values = List.of(List.of(
                date,
                prettyTime,
                description,
                minutes
        ));

        try {
            sheetsService.spreadsheets().values()
                    .append(SPREADSHEET_ID, range, new ValueRange().setValues(values))
                    .setValueInputOption("RAW")
                    .execute();

            System.out.println(userConfig.name() + " → +" + prettyTime + " – " + description);
        } catch (Exception e) {
            System.err.println("Cannot write in Sheets for " + userConfig.name());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // === Convert column A=0, B=1, ..., Z=25, AA=26 ===
    private static int letterToColumn(String col) {
        int column = 0;
        for (char c : col.toUpperCase().toCharArray()) {
            column = column * 26 + (c - 'A' + 1);
        }
        return column - 1;
    }

    private static String columnLetter(int index) {
        StringBuilder col = new StringBuilder();
        while (index >= 0) {
            col.insert(0, (char) ('A' + (index % 26)));
            index = (index / 26) - 1;
        }
        return col.toString();
    }

    private static String formatMinutes(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return h > 0 ? h + "h" + (m > 0 ? m : "") : m + "min";
    }

    public static void testConnectionAndWrite(String testerName) {
        try {
            List<List<Object>> values = Arrays.asList(
                    Arrays.asList(
                            "CONNECTION TEST",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            testerName != null ? testerName : "Unknown",
                            "BOT ONLINE !"
                    )
            );

            ValueRange body = new ValueRange().setValues(values);

            sheetsService.spreadsheets().values()
                    .append(SPREADSHEET_ID, "Suivi!A:D", body)  // change "Suivi" if your tab is called something else
                    .setValueInputOption("RAW")
                    .execute();

            System.out.println("Google Sheets : Line successfully added !");
        } catch (Exception e) {
            System.err.println("Google Sheets failed :");
            e.printStackTrace();
        }
    }
}
