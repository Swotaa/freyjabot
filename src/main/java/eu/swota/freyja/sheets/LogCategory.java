package eu.swota.freyja.sheets;

public enum LogCategory {
    PHP("#D9EAD3"),      // Light Green
    API("#D0E0E3"),      // Blue
    EF("#FFF2CC"),       // Light Orange // Yellow
    GESTION("#FCE5CD"),  // Orange
    DEFAULT("#FFFFFF");  // White

    private final String colour;

    LogCategory(String colour) {
        this.colour = colour;
    }

    public String getColour() {
        return colour;
    }
}
