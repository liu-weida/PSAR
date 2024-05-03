package utils.tools;

/**
 * Minuteur de décompte
 * Possède une barre de progression en pourcentage
 * Utilisé pour attendre une reconnexion
 */
public class CountdownTimer {
    private int timeInSeconds;

    public CountdownTimer(int timeInSeconds) {
        this.timeInSeconds = timeInSeconds;
    }

    public void start() {
        int totalSteps = 100;  // Le nombre total d'étapes est de 100, correspondant au pourcentage
        int delay = timeInSeconds * 10;  // Le délai total est le temps du compte à rebours multiplié par 10 millisecondes

        try {
            for (int i = 0; i <= totalSteps; i++) {
                System.out.print("\r");
                System.out.print("Progression: " + i + "% " + progressBar(i, totalSteps));
                Thread.sleep(delay);  // Petit délai pour une mise à jour rapide de l'affichage
            }
            System.out.print("\r");
            System.out.println("Compte à rebours terminé !");
        } catch (InterruptedException e) {
            System.err.println("Compte à rebours interrompu !");
            Thread.currentThread().interrupt();
        }
    }

    private String progressBar(int current, int total) {
        int percentage = (int) ((double) current / total * 50);  // Calcul de la longueur de la barre de progression
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 50; i++) {
            if (i < percentage) {
                bar.append("=");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");
        return bar.toString();
    }

//    public static void main(String[] args) {
//        CountdownTimer timer = new CountdownTimer(5);  // Crée un compte à rebours de 5 secondes
//        timer.start();
//    }
}
