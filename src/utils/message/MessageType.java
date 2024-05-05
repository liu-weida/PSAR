package utils.message;

public enum MessageType {
    DMA("dMalloc"),
    DAW("dAccessWrite"),
    DAR("dAccessRead"),
    DRE("dRelease"),
    DFR("dFree"),
    EXP("dException"), // Représente différents types de commandes, comme dMalloc, dAccessWrite, etc.
    HBM("HeartbeatMessage"); // Message de battement de coeur

    private final String description;

    // Constructeur pour associer les constantes avec des valeurs
    private MessageType(String description) {
        this.description = description;
    }

    // Obtenir la valeur associée à la constante
    public String getDescription() {
        return description;
    }
}
