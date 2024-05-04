package machine;

import annotations.CommandMethod;
import rmi.ClientErrorSet;
import utils.channel.ChannelWithBuffer;
import utils.enums.ClientState;
import utils.enums.HeartSource;
import utils.enums.HeartState;
import utils.message.*;
import utils.channel.Channel;
import utils.processor.ClientProcessor;
import utils.tools.CountdownTimer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client extends Machine implements ClientErrorSet {
    // Tableau des données internes
    private HashMap<String, Object> localHeap = new HashMap<>();
    // Processeur des tâches du client
    private ClientProcessor processor = new ClientProcessor();
    // Canal de communication dédié au client
    private Channel channel;
    // Canal pour la confirmation de la connexion
    private Channel channelHeart;
    // Port du serveur
    private final int serverPort = 8080;
    // Adresse du serveur
    private final String serverHost = "localhost";
    // État du client
    private ClientState clientState = ClientState.normal;
    // Port d'interface de connexion
    int localPort = -1;
    // Port d'interface pour le cœur
    int localPortHeart = -1;

    // Constructeur
    public Client(int port, String clientId) throws IOException {
        super(clientId, port);
        this.channel = createChannel(true);  // true pour le tunnel de messages normaux
        this.channelHeart = createChannel(false); // false pour le tunnel de messages de battement de cœur
        processor.setClient(this);
        listenForClientMessages(); // Écouter les tâches de lecture d'autres clients
        heartBeat();
        registerRmiClient();
    }

    // Envoi périodique de signaux de battement de cœur
    private void heartBeat() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(); // Créer une tâche programmée
        scheduler.scheduleAtFixedRate(() -> {

            InetAddress localHost = channelHeart.getLocalHost();
            int localPort = super.getPort(); // Obtenir l'adresse et le port locaux

            HeartbeatMessage heartbeatMessage = null;

            switch (clientState) { // Construire un message selon l'état du serveur
                case timeout -> { // Dormir 100s sans envoyer de signal
                    try {
                        Thread.sleep(100000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    heartbeatMessage = null;
                }
                case errorSource -> { // Envoyer une source d'erreur (choix aléatoire entre trois)
                    Random random = new Random(3);
                    int index = random.nextInt();

                    switch (index) {
                        case 0 -> heartbeatMessage = new HeartbeatMessage(HeartSource.CLIENT, HeartState.HEART, localHost, localPort);
                        case 1 -> heartbeatMessage = new HeartbeatMessage(HeartSource.MIRROR, HeartState.HEART, localHost, localPort);
                        case 2 -> heartbeatMessage = new HeartbeatMessage(null, HeartState.HEART, localHost, localPort);
                    }
                }
                case errorState -> { // Envoyer un état d'erreur (choix aléatoire entre deux)
                    Random random = new Random(2);
                    int index = random.nextInt();

                    switch (index) {
                        case 0 -> heartbeatMessage = new HeartbeatMessage(HeartSource.CLIENT, HeartState.HEARTNORMAL, localHost, localPort);
                        case 1 -> heartbeatMessage = new HeartbeatMessage(HeartSource.MIRROR, null, localHost, localPort);
                    }
                }
                case errorNull -> { // Ne pas envoyer de signal
                    heartbeatMessage = null;
                }
                case normal -> { // Envoyer un signal normal
                    heartbeatMessage = new HeartbeatMessage(HeartSource.CLIENT, HeartState.HEART, localHost, localPort);
                }
            }

            try {
                channelHeart.send(heartbeatMessage); // Essayer d'envoyer le message
            } catch (IOException e) {
                reconnectToServer(); // Si échec, essayer de reconnecter
                try {
                    channelHeart.send(heartbeatMessage); // Si problème persistant, lever une exception
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        }, 3, 3, TimeUnit.SECONDS); // Exécuter toutes les trois secondes
    }

    // Créer une connexion de canal
    private Channel createChannel(boolean generalMessageOrNo) throws IOException {
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        int targetPort = generalMessageOrNo ? serverPort : serverPort + 1;  // Choisir le port selon le booléen
        int port = generalMessageOrNo ? this.localPort : this.localPortHeart;

        if (port == -1) {
            socket.connect(new InetSocketAddress(serverHost, targetPort));
            if (generalMessageOrNo) {
                this.localPort = socket.getLocalPort();
            } else {
                this.localPortHeart = socket.getLocalPort();
            }
        } else {
            socket.bind(new InetSocketAddress((InetAddress) null, port));
            socket.connect(new InetSocketAddress(serverHost, targetPort));
        }
        return new ChannelWithBuffer(socket);
    }

    // Essayer de se reconnecter au serveur
    private void reconnectToServer() {
        try {
            System.out.println("Connexion interrompue détectée, tentative de reconnexion");
            channel.close();
            channelHeart.close();
            CountdownTimer timer = new CountdownTimer(2);  // Créer un compte à rebours de 2 secondes
            timer.start();
            this.channel = createChannel(true); // Reconstruire la connexion
            this.channelHeart = createChannel(false);
            System.out.println("Reconnexion réussie !");
        } catch (IOException e) {
            System.out.println("Impossible de se connecter au serveur : " + e.getMessage());
        }
    }

    // Vérifier si le tableau contient les données recherchées
    public boolean heapHaveData(String variableId) {
        return localHeap.containsKey(variableId);
    }

    // Retourner le tableau de recherche
    public HashMap<String, Object> getLocalHeap() {
        return localHeap;
    }

    // Obtenir tous les noms dans le tableau de données
    public List<String> getAllStringsFromLocalHeap() {
        List<String> stringsList = new ArrayList<>();

        for (Map.Entry<String, Object> entry : localHeap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                stringsList.add((String) value);
            }
        }

        return stringsList;
    }

    // Ajouter une donnée au tableau
    public void setObject(String variableId, Object o) {
        localHeap.put(variableId, o);
    }

    // Envoyer une requête qui recherche automatiquement la fonction à utiliser selon le type de méthode
    public void request(String methodType, String args) throws InvocationTargetException, IllegalAccessException {
        for (Method method : getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodType) && method.isAnnotationPresent(CommandMethod.class)) {
                method.invoke(this, args);
                break;
            }
        }
    }

    // Essayer de se connecter à un autre client
    public Channel connectToClient(InetAddress host, int port) throws IOException {
        return new ChannelWithBuffer(new Socket(host, port));
    }

    // Écouter les messages des clients
    public void listenForClientMessages() {
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("lfcm démarré");
                    Channel localChannel = new ChannelWithBuffer(super.getServerSocket().accept());
                    System.out.println("Début de la lecture");
                    SendDataMessage recv = (SendDataMessage) localChannel.recv();
                    System.out.println("Message reçu par lfcm : " + recv.toString());
                    Object result = localHeap.get(recv.getVariableId());
                    localChannel.send(new SendDataMessage(recv.getVariableId(), result));
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Traiter le message dmalloc du client
    // Envoyer un message au serveur pour vérifier si la donnée existe, si non présente, ajouter cette donnée au tableau
    @CommandMethod
    private void dMalloc(String id) throws IOException, SecurityException, IllegalArgumentException, ClassNotFoundException {
        ClientMessage message = new ClientMessage("dMalloc", getId(), id, super.getPort());
        sendMessage(message, id);
    }

    // Traiter le message daccesswrite du client
    // Envoyer une requête d'écriture au serveur, (si la donnée existe et n'est pas verrouillée) recevoir un message de confirmation, retourner l'adresse de la donnée dans son propre tableau, si un message d'erreur est reçu, retourner null
    @CommandMethod
    private void dAccessWrite(String id) throws IOException, ClassNotFoundException {
        ClientMessage message = new ClientMessage("dAccessWrite", getId(), id, super.getPort());
        sendMessage(message, id);
    }

    // Traiter le message daccessread du client
    // Envoyer une requête de lecture au serveur, (si la donnée existe et n'est pas verrouillée) recevoir un message de confirmation, décider si lire directement sa propre donnée ou transférer la requête de lecture à un autre client, si la lecture échoue, envoyer un message d'erreur au serveur, si réussie, modifier son propre tableau et retourner l'adresse
    @CommandMethod
    private void dAccessRead(String id) throws IOException, ClassNotFoundException {
        ClientMessage message = new ClientMessage("dAccessRead", getId(), id, super.getPort());
        sendMessage(message, id);
    }

    // Traiter le message drelease du client
    // Répondre avec un message de confirmation de modification, (rendre la donnée non modifiable ?)
    @CommandMethod
    private void dRelease(String variableId) throws IOException, ClassNotFoundException {
        ClientMessage message = new ClientMessage("dRelease", getId(), variableId, super.getPort());
        sendMessage(message, variableId);
    }

    // Traiter le message dfree du client
    // Envoyer un signal de suppression, attendre une réponse
    @CommandMethod
    private void dFree(String id) throws IOException, ClassNotFoundException {
        ClientMessage message = new ClientMessage("dFree", getId(), id, super.getPort());
        sendMessage(message, id);
    }

    // Envoyer un message au serveur
    private void sendMessage(Message message, String id) throws IOException, ClassNotFoundException {
        try {
            channel.send(message);
        } catch (IOException e) {
            reconnectToServer();
            channel.send(message);
        }
        processor.process(channel, id, message);
    }

    // Définir l'état du client
    private void setClientState(ClientState clientState1) {
        clientState = clientState1;
    }

    // Définir l'état d'erreur du client RMI
    @Override
    public void setClientError(ClientState clientState) throws RemoteException {
        setClientState(clientState);
    }

    // Enregistrer les informations du client RMI
    private void registerRmiClient() throws RemoteException {
        try {
            String name = "ClientControl_" + getId(); // Identificateur unique
            ClientErrorSet stub = (ClientErrorSet) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry("localhost", 1099); // Obtenir le registre par défaut ou créer un nouveau
            registry.rebind(name, stub); // Se lier ou relier au registre
            System.out.println("Client bound in registry as " + name);
        } catch (RemoteException e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
