package machine;

import rmi.ForcedServerShutdown;
import utils.channel.Channel;
import utils.channel.ChannelWithBuffer;
import utils.enums.HeartSource;
import utils.enums.HeartState;
import utils.message.HeartbeatMessage;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MirrorInitiator extends Machine {
    private Channel channel;
    // Identifier l'état du serveur
    private boolean continueRunning = true;
    // Éviter les appels répétés de la méthode over
    private boolean isOverCalled = false;
    private String serverHost = "localhost";
    private int serverport = 8080;

    // Constructeur
    public MirrorInitiator(String id, int port) throws IOException {
        super(id, port);
        this.channel = new ChannelWithBuffer(new Socket(serverHost, serverport + 1));
        startHeartbeat();
    }

    // Boucle d'envoi et de réception des messages de battement de cœur
    private void startHeartbeat() {
        new Thread(() -> {
            while (continueRunning) {
                try {
                    heartbeatSend();
                    heartbeatRecv();
                    Thread.sleep(5000); // À changer pour 30s plus tard
                } catch (Exception e) {
                    handleError(e);
                }
            }
        }).start();
    }

    // Fonction exécutée en cas d'erreur pour tenter un redémarrage, un seul redémarrage tenté
    private void handleError(Exception e) {
        if (!isOverCalled) {
            System.out.println("An error occurred: " + e.getMessage());
            try {
                reconnect();
            } catch (IOException | ClassNotFoundException ex) {
                // Handle the exception
            }
            isOverCalled = true; // Marquer comme appelé
        }
    }

    // Envoyer un signal de battement de cœur
    private void heartbeatSend() throws IOException {
        HeartbeatMessage hbm = new HeartbeatMessage(HeartSource.MIRROR, HeartState.HEART);
        channel.send(hbm);
    }

    // Recevoir un signal de battement de cœur
    private void heartbeatRecv() throws IOException, ClassNotFoundException {
        HeartbeatMessage heartbeatMessage = null;

        try {
            heartbeatMessage = (HeartbeatMessage) channel.recvWithTimeout(5000); // timeout de 5 secondes
        } catch (SocketTimeoutException e) {
            handleError(e);
        }

        if (heartbeatMessage == null || heartbeatMessage.getOperationStatus() != HeartState.HEARTNORMAL || heartbeatMessage.getSource() != HeartSource.SERVER) {
            throw new SocketException("Pas de réponse reçue du serveur.");
        }
        //System.out.println(heartbeatMessage.toString());
    }

    // Reconnecter
    public void reconnect() throws IOException, ClassNotFoundException {
        if (!continueRunning) {    // Si déjà traité, retourner directement
            return;
        }

        System.out.println("La connexion du battement de cœur a échoué/connexion socket interrompue !!!");
        continueRunning = true;
        killServer();

        System.out.println("Démarrer le serveur miroir");
        restartServer(8080, "server");
    }

    // Fermer le serveur
    public static void killServer() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            ForcedServerShutdown stub = (ForcedServerShutdown) registry.lookup("RemoteShutdownService");
            stub.forcedserverShutdown();
            System.out.println("Méthode distante invoquée");
        } catch (Exception e) {
            // System.err.println("Client exception: " + e.toString());
            // e.printStackTrace();
        }
    }

    // Redémarrer le serveur
    public static void restartServer(int port, String serverName) throws IOException, ClassNotFoundException {
        Server server = new Server(port, serverName);
        server.start();
    }
}