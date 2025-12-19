package eu.swota.freyja.sheets;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
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

    public static boolean logWork(long userId, String username, String date, String duration, String description, String colour) {
        var config = SheetConfig.get();
        var userConfig = config.users.get(String.valueOf(userId));

        if (userConfig == null) {
            System.out.println("User not configured into config.json : " + username + " (" + userId + ")");
            return false;
        }

        String startCol = userConfig.startColumn();
        String endCol = columnLetter(letterToColumn(startCol) + 2); // A→C, E→G, I→K, etc.
        String range = SHEET_NAME + "!" + startCol + ":" + endCol;

        List<List<Object>> values = List.of(List.of(
                date,
                duration,
                description
        ));

        try {
            String userColumnRange = SHEET_NAME + "!" + startCol + ":" + startCol;
            ValueRange userColumnData = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, userColumnRange)
                    .execute();
            
            int nextRow = (userColumnData.getValues() != null ? userColumnData.getValues().size() : 0) + 1;
            String specificRange = SHEET_NAME + "!" + startCol + nextRow + ":" + endCol + nextRow;
            
            sheetsService.spreadsheets().values()
                    .update(SPREADSHEET_ID, specificRange, new ValueRange().setValues(values))
                    .setValueInputOption("RAW")
                    .execute();

            int rowIndex = nextRow - 1;
            int colIndex = letterToColumn(startCol);

            Request colorRequest = new Request()
                    .setRepeatCell(new RepeatCellRequest()
                            .setRange(new GridRange()
                                    .setSheetId(0)
                                    .setStartRowIndex(rowIndex)
                                    .setEndRowIndex(rowIndex + 1)
                                    .setStartColumnIndex(colIndex)
                                    .setEndColumnIndex(colIndex + 3))
                            .setCell(new CellData()
                                    .setUserEnteredFormat(new CellFormat()
                                            .setBackgroundColor(hexToColor(colour))))
                            .setFields("userEnteredFormat.backgroundColor"));

            sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID,
                    new BatchUpdateSpreadsheetRequest().setRequests(List.of(colorRequest))).execute();

            System.out.println(userConfig.name() + " → +" + duration + " – " + description);
        } catch (Exception e) {
            System.err.println("Cannot write in Sheets for " + userConfig.name());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Convert String into Color
    private static Color hexToColor(String hex) {
        hex = hex.replace("#", "");
        return new Color()
                .setRed(Integer.parseInt(hex.substring(0, 2), 16) / 255f)
                .setGreen(Integer.parseInt(hex.substring(2, 4), 16) / 255f)
                .setBlue(Integer.parseInt(hex.substring(4, 6), 16) / 255f);
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
