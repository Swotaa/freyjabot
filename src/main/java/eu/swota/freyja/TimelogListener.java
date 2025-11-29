package eu.swota.freyja;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimelogListener extends ListenerAdapter {

    private static final String CHANNEL_NAME = "activité";

    // Regex finale ultra-robuste (corrigée et blindée contre les décalages de groupes)
    private static final Pattern TIMELOG_PATTERN = Pattern.compile(
            // Jour de la semaine (optionnel)
            "(?:[Ll]undi|[Mm]ardi|[Mm]ercredi|[Jj]eudi|[Vv]endredi|[Ss]amedi|[Dd]imanche)\\s+" +

                    // Bloc date complet (optionnel)
                    "(?:" +
                    // Jour du mois (capturé)
                    "(\\d{1,2})\\s*(?:er|ème)?\\s*[/\\-\\.\\s]\\s*" +

                    // Mois : soit numérique, soit en lettres
                    "(?:" +
                    // Variante 1 : mois numérique (ex: 29/11 ou 29-11-2025)
                    "(\\d{1,2})\\s*(?:[/\\-\\.\\s]\\s*(\\d{2,4})\\s*)?" +
                    "|" +
                    // Variante 2 : mois en lettres (ex: Novembre 2025)
                    "(janv\\.?|févr?\\.?|mars|avr\\.?|mai|juin|juil\\.?|ao[ûu]t|sept?\\.?|oct\\.?|nov\\.?|déc\\.?" +
                    "|Janvier|Février|Mars|Avril|Mai|Juin|Juillet|Août|Septembre|Octobre|Novembre|Décembre)" +
                    "\\s*(?:\\.?\\s*(\\d{2,4})\\s*)?" +
                    ")\\s*" +  // espace final optionnel
                    ")?" +

                    // Durée obligatoire (2h, 2h30, 90min, etc.)
                    "(\\d+[hH]\\d{0,2}|\\d+[hH]|\\d+\\s*min)\\s+" +

                    // Description (tout le reste)
                    "(.+)",

            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.getChannel().getName().equalsIgnoreCase(CHANNEL_NAME)) return;

        String content = event.getMessage().getContentRaw().trim();
        Matcher m = TIMELOG_PATTERN.matcher(content);

        if (!m.find()) return;

        try {
            LocalDate date = LocalDate.now();

            // === Extraction sécurisée de la date ===
            String jourStr = m.group(1);           // jour du mois (ex: 29)
            String moisNumStr = m.group(2);        // mois numérique (ou null)
            String anneeNumStr = m.group(3);       // année après mois numérique (ou null)
            String moisLettre = m.group(4);        // mois en lettres (ou null)
            String anneeLettreStr = m.group(5);    // année après mois en lettres (ou null)

            if (jourStr != null) {
                int jour = Integer.parseInt(jourStr);
                int mois = date.getMonthValue();
                int annee = date.getYear();

                if (moisNumStr != null) {
                    // Cas : format numérique (ex: 29/11)
                    mois = Integer.parseInt(moisNumStr);
                    if (anneeNumStr != null && !anneeNumStr.trim().isEmpty()) {
                        annee = anneeNumStr.length() == 2 ? 2000 + Integer.parseInt(anneeNumStr)
                                : Integer.parseInt(anneeNumStr);
                    }
                } else if (moisLettre != null) {
                    // Cas : mois en lettres (ex: Novembre)
                    mois = moisEnNombre(moisLettre);
                    if (anneeLettreStr != null && !anneeLettreStr.trim().isEmpty()) {
                        annee = anneeLettreStr.length() == 2 ? 2000 + Integer.parseInt(anneeLettreStr)
                                : Integer.parseInt(anneeLettreStr);
                    }
                }
                date = LocalDate.of(annee, mois, jour);
            }

            // === Durée ===
            String dureeRaw = m.group(6).trim().toLowerCase().replaceAll("\\s+", "");
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
            String description = m.group(7).trim();

            // === Envoi dans Google Sheets ===
            SheetManager.logWork(
                    event.getMember().getIdLong(),
                    event.getMember().getNickname(),
                    date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                    duree,
                    description
            );

            // === Confirmation ===
            event.getMessage().addReaction(Emoji.fromUnicode("U+2705")).queue();
            event.getChannel().sendMessage(
                    "Temps enregistré : **" + duree + "** sur *" + description + "*"
            ).queue();

        } catch (Exception e) {
            event.getMessage().addReaction(Emoji.fromUnicode("U+274C")).queue();
            event.getChannel().sendMessage("Erreur de format, désolé !").queue();
            e.printStackTrace();
        }
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