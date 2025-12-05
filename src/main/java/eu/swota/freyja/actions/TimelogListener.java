package eu.swota.freyja.actions;

import eu.swota.freyja.sheets.SheetManager;
import eu.swota.freyja.sheets.LogCategory;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimelogListener extends ListenerAdapter {

    // So, because I'm French, the name of the channel I use is "activité", but you can change it easily right here dw
    private static final String CHANNEL_NAME = "activité";



    // This is the regex that is used to recognise the "command", cover most of french way to write the day
    // Note that this is meant for productivity, so it aims at being fast to write.
    private static final Pattern TIMELOG_PATTERN = Pattern.compile(
            "(?:(?:[Ll]undi|[Mm]ardi|[Mm]ercredi|[Jj]eudi|[Vv]endredi|[Ss]amedi|[Dd]imanche)\\s+)?" +
                    "(?:" +
                    "(\\d{1,2})\\s*[/-\\\\.\\s]\\s*(\\d{1,2})(?:\\s*[/-\\\\.\\s]\\s*(\\d{2,4})\\s*)?" +
                    "|" +
                    "(\\d{1,2})\\s*(?:er|ème)?\\s+" +
                    "(janv\\.?|févr?\\.?|mars|avr\\.?|mai|juin|juil\\.?|ao[ûu]t|sept?\\.?|oct\\.?|nov\\.?|déc\\.?" +
                    "|Janvier|Février|Mars|Avril|Mai|Juin|Juillet|Août|Septembre|Octobre|Novembre|Décembre)" +
                    "\\s*(?:\\.?\\s*(\\d{2,4})\\s*)?" +
                    ")?" +
                    "\\s*" +
                    "(\\d+[hH]\\d{0,2}|\\d+[hH]|\\d+\\s*min)\\s+" +
                    "(.+)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return; // Just in case, bots cannot trigger this bot (Freyja)
        if (!event.getChannel().getName().equalsIgnoreCase(CHANNEL_NAME)) return; // Make sur the good channel is used

        String content = event.getMessage().getContentRaw().trim(); // Get the raw message
        Matcher m = TIMELOG_PATTERN.matcher(content); // Uses the regex to see if there is a match

        if (!m.find()) return; // Break the function if it doesn't match

        try {
            LocalDate date = LocalDate.now(); // Recovering the date

            String jourNum = m.group(1); // Day (30/11) format
            String moisNum = m.group(2); // Month (30/11) format
            String anneeNum = m.group(3); // Year (30/11/2025) format (Optional)
            String jourTexte = m.group(4); // Day Text Format (29 Novembre) -> Novembre = November in French btw
            String moisTexte = m.group(5); // Month Text Format (29 Novembre) -> Novembre = November in French btw
            String anneeTexte = m.group(6); // Year Text Format (29 Novembre 2025) -> Still the same

            if (jourNum != null && moisNum != null) {
                // Format : 30/11 or 30/11/2025
                int j = Integer.parseInt(jourNum);
                int mo = Integer.parseInt(moisNum);
                int a = LocalDate.now().getYear();
                if (anneeNum != null && !anneeNum.isBlank()) {
                    a = anneeNum.length() == 2 ? 2000 + Integer.parseInt(anneeNum) : Integer.parseInt(anneeNum);
                }
                date = LocalDate.of(a, mo, j);

            } else if (jourTexte != null && moisTexte != null) {
                // Format : 29 Novembre ou 1er décembre 2025
                int j = Integer.parseInt(jourTexte);
                int mo = moisEnNombre(moisTexte);
                int a = LocalDate.now().getYear();
                if (anneeTexte != null && !anneeTexte.isBlank()) {
                    a = anneeTexte.length() == 2 ? 2000 + Integer.parseInt(anneeTexte) : Integer.parseInt(anneeTexte);
                }
                date = LocalDate.of(a, mo, j);
            }
            // Else → date = today (if none provided)

            // === Duration ===
            String dureeRaw = m.group(7).trim().toLowerCase().replaceAll("\\s+", "");
            int minutesTotales;

            if (dureeRaw.contains("min")) {
                minutesTotales = Integer.parseInt(dureeRaw.replaceAll("\\D+", ""));
            } else {
                String[] parts = dureeRaw.split("h");
                int heures = Integer.parseInt(parts[0]);
                int minutes = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : 0;
                minutesTotales = heures * 60 + minutes;
            }

            String duree = String.format("%dh%02d", minutesTotales / 60, minutesTotales % 60);
            String description = m.group(8).trim();

            LogCategory category = detectCategory(description);
            // === Google Sheets ===
            SheetManager.logWork(
                    event.getMember().getIdLong(),
                    event.getMember() != null ? event.getMember().getNickname() : event.getAuthor().getName(),
                    date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                    duree,
                    description,
                    category.getColour()
            );

            // === Confirmation ===
            event.getMessage().addReaction(Emoji.fromUnicode("U+2705")).queue();
            event.getChannel().sendMessage(
                    "Temps enregistré : **" + duree + "** le *" +
                            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                            "* → " + description
            ).queue();

        } catch (Exception e) {
            event.getMessage().addReaction(Emoji.fromUnicode("U+274C")).queue();
            event.getChannel().sendMessage("Erreur de format, désolé !").queue();
            e.printStackTrace();
        }
    }

    // Detect the category of the activity
    private LogCategory detectCategory(String description) {
        String descLower = description.toLowerCase();

        if (descLower.contains("php")) {
            return LogCategory.PHP;
        }
        if (descLower.contains("api")) {
            return LogCategory.API;
        }
        if (descLower.contains("ef")) {
            return LogCategory.EF;
        }
        if (descLower.contains("gestion")) {
            return LogCategory.GESTION;
        }

        return LogCategory.DEFAULT;
    }

    private int moisEnNombre(String mois) {
        mois = mois.toLowerCase().replaceAll("\\.", "");
        return switch (mois) {
            case "janvier", "janv" -> 1;
            case "février", "fevrier", "févr", "fevr" -> 2;
            case "mars" -> 3;
            case "avril", "avr" -> 4;
            case "mai" -> 5;
            case "juin" -> 6;
            case "juillet", "juil" -> 7;
            case "août", "aout", "aou" -> 8;
            case "septembre", "sept" -> 9;
            case "octobre", "oct" -> 10;
            case "novembre", "nov" -> 11;
            case "décembre", "decembre", "déc", "dec" -> 12;
            default -> LocalDate.now().getMonthValue();
        };
    }
}