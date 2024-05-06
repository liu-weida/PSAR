package utils.channel;

import utils.message.ClientMessage;
import utils.message.Message;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class ChannelWithBuffer implements Channel {
    // Ce channelBasic implémente un buffer en surface
    // Ce buffer joue en fait uniquement un rôle d'enregistrement, il peut stocker tous les messages non encore lus (mais en réalité, les messages sont toujours dans le tampon TCP et non dans le buffer)
    // Les messages sont retirés du buffer après traitement par le serveur
    // Si un message est bloqué, il est placé dans lockedMessageMap, en attente de libération ultérieure

    // lockedMessageMap joue également un rôle d'enregistrement
    // Les messages bloqués sont renvoyés par le client après trois secondes
    // Processus approximatif : Client --(demande)--> Serveur --(bloqué) --(marqué comme bloqué)--> Client --(renvoi après trois secondes)--> Serveur --(si toujours bloqué, boucler ce processus) -->
    // J'ai fait de mon mieux, hahaha

    // Le comptage du buffer et le comptage de la map bloquée sont effectués dans le serveur

    // Utilisé pour stocker les messages spécifiques à cette instance de canal
    protected Queue<Message> ownQueue;
    // Stocke la file d'attente de messages de toutes les instances de canal pour un traitement centralisé
    private static final List<Queue<Message>> sharedMessageQueues = Collections.synchronizedList(new ArrayList<>());
    // Lit le variableid et le message à partir de sharedMessageQueues, stockant une liste de messages classés par ID de variable pour faciliter le traitement et la récupération.
    private static final ConcurrentHashMap<String, List<Message>> messageMap = new ConcurrentHashMap<>();
    // Stocke les messages qui n'ont pas pu être traités en raison d'un verrouillage
    private static final ConcurrentHashMap<String, List<Message>> lockedMessageMap = new ConcurrentHashMap<>();
    // Utilise un seul thread pour exécuter toutes les tâches de traitement de messages des canaux
    private static final ExecutorService sharedExecutor = Executors.newSingleThreadExecutor();
    protected final Socket socket;
    // Interface de sortie
    protected ObjectOutputStream oos;
    // Interface d'entrée
    protected ObjectInputStream ois;
    private static final Logger LOGGER = Logger.getLogger(ChannelWithBuffer.class.getName());

    // Constructeur
    public ChannelWithBuffer(Socket socket) throws IOException {
        this.socket = socket;
        ownQueue = new ConcurrentLinkedQueue<>();
        sharedMessageQueues.add(ownQueue);
        startSharedProcessing(); // Assure que le thread de traitement est en cours d'exécution
    }

    // Envoyer un message, cette méthode send est en fait identique à l'originale
    public void send(Message message) throws IOException {
        if (oos == null) {
            oos = new ObjectOutputStream(socket.getOutputStream());
        }
        oos.writeObject(message);
        oos.flush();
    }

    // Recevoir un message, cette méthode recv est aussi identique à l'originale
    public Message recv() throws IOException, ClassNotFoundException {
        if (ois == null) {
            ois = new ObjectInputStream(socket.getInputStream());
        }
        Message message = (Message) ois.readObject();
        if (message instanceof ClientMessage) {
            ownQueueOffer(message);
            // System.out.println(message.toString());
        }
        return message;
    }

    // Ajoute un message à la propre file d'attente de ce canal
    public void ownQueueOffer(Message message) {
        ownQueue.offer(message);
        // System.out.println(Arrays.toString(ownQueue.toArray()));
    }

    // Supprime et traite un message de ownQueue
    public void ownQueuePOP() {
        System.out.println("Déclenchement de pop");
        // System.out.println(Arrays.toString(ownQueue.toArray()));
        Message message = ownQueue.poll();
        removeMessageFromMap(message);
    }

    // Supprime un message spécifique de la map globale des messages, messageMap
    public static void removeMessageFromMap(Message message) {
        if (message instanceof ClientMessage) {
            ClientMessage clientMessage = (ClientMessage) message;
            String variableId = clientMessage.getVariableId(); // Obtient l'ID de variable du message
            // Obtient la liste des messages correspondante
            List<Message> messages = messageMap.get(variableId);
            if (messages != null) {
                synchronized (messages) { // Bloc synchronisé pour garantir la sécurité des threads
                    boolean isRemoved = messages.remove(message); // Essaie de supprimer le message
                    if (isRemoved) {
                        System.out.println("Message supprimé avec succès de messageMap.");
                    } else {
                        System.out.println("Message non trouvé dans messageMap.");
                    }
                }
            } else {
                System.out.println("Aucun message trouvé pour l'ID de variable : " + variableId);
            }
        } else {
            System.out.println("Type de message invalide pour la suppression.");
        }
    }

    // Ajoute un message à lockedMessageMap
    public static void addMessageToLockedMap(Message message) {
        ClientMessage clientMessage = (ClientMessage) message;
        String variableId = clientMessage.getVariableId();
        List<Message> messages = lockedMessageMap.computeIfAbsent(variableId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (messages) {
            if (!messages.contains(message)) {
                messages.add(message);
            }
        }
    }

    // Enlève tous les messages liés à un ID de variable de lockedMessageMap
    public static void removeMessagesByVariableId(String variableId) {
        List<Message> removed = lockedMessageMap.remove(variableId);
        // if (removed != null) {}
    }

    // Imprime le nombre de messages non verrouillés
    public static void printMessageCounts() {
        for (Map.Entry<String, List<Message>> entry : messageMap.entrySet()) {
            String variableId = entry.getKey();
            int count = getMessagesNum(variableId);
            if (count != 0) {
                System.out.print("Nombre de messages actuel : ");
                System.out.println("ID de Variable : " + variableId + " - Messages : " + count);
            } else {
                System.out.print("Nombre de messages actuel : ");
                System.out.println("ID de Variable : " + variableId + " - Messages : " + "0");
            }
        }
    }

    // Imprime le nombre de messages verrouillés
    public static void printLockedMessageCounts() {
        for (Map.Entry<String, List<Message>> entry : lockedMessageMap.entrySet()) {
            String variableId = entry.getKey();
            int count = getLockedMessagesNum(variableId);
            if (count != 0) {
                System.out.print("Nombre de messages verrouillés actuel : ");
                System.out.println("ID de Variable : " + variableId + " - Messages verrouillés : " + count);
            } else {
                System.out.print("Nombre de messages verrouillés actuel : ");
                System.out.println("ID de Variable : " + variableId + " - Messages verrouillés : " + "0");
            }
        }
    }

    // Obtient le nombre de messages
    private static int getMessagesNum(String variableId) {
        int num = -1;
        List<Message> messages = messageMap.get(variableId);
        if (messages == null) {
            num = 0;
        } else {
            if (messages.size() > 1000) { // Prévient la corruption des données quand le nombre est nul
                num = 0;
            } else {
                num = messages.size();
            }
        }
        return num;
    }

    // Obtient le nombre de messages verrouillés
    private static int getLockedMessagesNum(String variableId) {
        int num = -1;
        List<Message> messages = lockedMessageMap.get(variableId);
        if (messages == null) {
            num = 0;
        } else {
            if (messages.size() > 1000) { // Prévient la corruption des données quand le nombre est nul
                num = 0;
            } else {
                num = messages.size();
            }
        }
        return num;
    }

    // Reçoit un message avec un délai d'expiration. Si aucun donnée n'est reçue dans le délai spécifié, une exception est lancée
    @Override
    public Object recvWithTimeout(int timeout) throws IOException, ClassNotFoundException {  // Ceci est un recv avec un délai d'expiration
        if (timeout > 0) {
            socket.setSoTimeout(timeout);
        }
        try {
            if (ois == null) {
                ois = new ObjectInputStream(socket.getInputStream());
            }
            return ois.readObject();
        } catch (SocketTimeoutException e) {
            throw new IOException("Délai d'attente dépassé pour la réception : " + e.getMessage(), e); // En cas de dépassement de délai, SocketTimeoutException est lancée
        }
    }

    @Override
    public InetAddress getRemoteHost() {
        return socket.getInetAddress();
    }

    @Override
    public int getRemotePort() {
        return socket.getPort();
    }

    @Override
    public InetAddress getLocalHost() {
        return socket.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    // Lance le thread de traitement singleton
    private static void startSharedProcessing() {
        System.out.println("Thread démarré");
        sharedExecutor.submit(() -> {
            while (true) {
                Iterator<Queue<Message>> it = sharedMessageQueues.iterator();
                while (it.hasNext()) {
                    Queue<Message> queue = it.next();
                    Message message = queue.peek();
                    if (message != null && message instanceof ClientMessage) {
                        processClientMessage((ClientMessage) message);
                    }
                }
                // try {
                //     Thread.sleep(100); // Pour éviter une boucle excessive
                // } catch (InterruptedException ie) {
                //     Thread.currentThread().interrupt();
                // }
            }
        });
    }

    // Traite les messages reçus du client et les ajoute à messageMap
    private static void processClientMessage(ClientMessage message) {
        String variableId = message.getVariableId();
        messageMap.computeIfAbsent(variableId, k -> Collections.synchronizedList(new ArrayList<>())).add(message);
    }

    // Recherche et retourne le nombre de messages pour un ID de variable spécifique
    public static int getMessageCountByVariableId(String variableId) {
        List<Message> messages = messageMap.get(variableId);
        return messages != null ? messages.size() : 0;
    }

    // Ferme le canal, y compris les flux et le Socket
    public void close() throws IOException {
        try {
            if (ois != null) {
                ois.close();
            }
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } finally {
                socket.close();
            }
        }
    }
}
