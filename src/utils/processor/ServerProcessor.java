package utils.processor;

import machine.Server;
import utils.channel.ChannelWithBuffer;
import utils.enums.HeartSource;
import utils.enums.HeartState;
import utils.enums.OperationStatus;
import utils.enums.ServerState;
import utils.tools.Pair;
import utils.message.*;
import utils.channel.Channel;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static utils.enums.OperationStatus.*;

// Processeur d'informations du serveur
public class ServerProcessor implements Processor {
    // Connecter au serveur
    private Server server = null;
    // Tableau de port et hôte
    private Pair pair;
    // Pool de threads pour gérer les tâches
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // Temps du dernier battement de cœur
    private volatile long lastHeartbeatTime = System.currentTimeMillis();
    // Enregistrer l'état du serveur
    private static ServerState serverState = ServerState.normal;
    // Booléen pour tester l'absence de battement de cœur
    private boolean socketCloseOrno = false;

    // Vérifier la réception du battement de cœur
    private void checkHeartbeat(HeartbeatMessage heartbeatMessage) {
        if (System.currentTimeMillis() - lastHeartbeatTime > 30000  || heartbeatMessage.getSource() != HeartSource.CLIENT || heartbeatMessage.getOperationStatus() != HeartState.HEART || heartbeatMessage == null || socketCloseOrno) { //3s
            //System.out.println("erreur de connexion avec le client");
            InetAddress host = (InetAddress) pair.first();
            int port = (int)pair.second();
            System.out.println("Erreur du client (" + host + "   " + port + "), connexion perdue !");
            System.out.println("Nettoyage des traces du client");
            //LinkedList<String> keysToRemove = new LinkedList<>();
            // Parcourir HashMap
            for (String key : server.getHeap().keySet()) {
                LinkedList<Pair> pairs = server.getHeap().get(key);
                boolean pairFound = false;
                Iterator<Pair> iterator = pairs.iterator();

                while (iterator.hasNext()) {
                    Pair currentPair = iterator.next();
                    if (currentPair.equals(pair)) {
                        iterator.remove();  // Supprimer cette Pair
                        pairFound = true;
                        System.out.println("Removed Pair " + currentPair + " from list under key '" + key + "'");
                    }
                }
//                // Si la paire est trouvée et que la liste est vide, marquez la clé comme Supprimer
//                if (pairFound && pairs.isEmpty()) {
//                    keysToRemove.add(key);
//                    System.out.println("List under key '" + key + "' is now empty and will be removed from the heap.");
//                }
            }
//            // Supprimez toutes les clés marquées
//            for (String key : keysToRemove) {
//                server.getHeap().remove(key);
//                System.out.println("Removed key '" + key + "' from the heap.");
//            }
            scheduler.shutdownNow();
       }
    }

    // Configurer le serveur de connexion
    public void setServer(Server server) {
        this.server = server;
    }

    // Traiter un message client
    public void process(Channel channel, String id, Message message) throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("En attente de message sur le port : " + channel.getRemotePort());
        Message messageRecy = (Message) channel.recv();  // Réception du message
        if (messageRecy instanceof HeartbeatMessage) {  // C'est un message de battement de cœur
            switch (((HeartbeatMessage) messageRecy).getSource()){
                case MIRROR -> handlingHBMirror(channel);
                case CLIENT ->  handlingHBClient((HeartbeatMessage) messageRecy);
            }
        } else if (messageRecy instanceof ClientMessage){  // C'est un message client
            handlingClientMessage((ClientMessage) messageRecy, channel);
        }
    }

    // Traitement du message de battement de cœur du miroir
    private void handlingHBMirror(Channel channel) throws IOException, InterruptedException {
        //System.out.println("recu mirror message");
        switch (serverState){
            case timeout -> {
                Thread.sleep(100000); //100s
                channel.send(null);
            }
            case errorSource -> {
                Random random = new Random(3);
                int index = random.nextInt();
                switch (index){
                    case 0 -> channel.send(new HeartbeatMessage(HeartSource.CLIENT, HeartState.HEARTNORMAL));
                    case 1 -> channel.send(new HeartbeatMessage(HeartSource.MIRROR,HeartState.HEARTNORMAL));
                    case 2 -> channel.send(new HeartbeatMessage(null,HeartState.HEARTNORMAL));
                }
            }
            case errorState -> {
                Random random = new Random(2);
                int index = random.nextInt();
                switch (index){
                    case 0 -> channel.send(new HeartbeatMessage(HeartSource.SERVER,HeartState.HEART));
                    case 1 -> channel.send(new HeartbeatMessage(HeartSource.SERVER,null));
                }
            }
            case errorNull -> {
                channel.send(null);
            }
            case normal -> {
                channel.send(new HeartbeatMessage(HeartSource.SERVER,HeartState.HEARTNORMAL));
            }
        }
    }

    // Gérer le battement de cœur client
    private void handlingHBClient(HeartbeatMessage heartbeatMessage) {
        scheduler.scheduleWithFixedDelay(() -> checkHeartbeat(heartbeatMessage), 0, 1, TimeUnit.SECONDS);
        lastHeartbeatTime = System.currentTimeMillis();
        pair = heartbeatMessage.getPair();
    }

    // Gérer le message de tâche client
    private void handlingClientMessage(ClientMessage clientMessage,Channel channel) throws IOException {
        ServerMessage message;
        String clientId = clientMessage.getClientId();
        String variableId = clientMessage.getVariableId();

        int clientPort = clientMessage.getClientPort();
        System.out.println("message recv from client : " + clientId);
        System.out.println("client port : " + clientPort);
        System.out.println("variableId: " + variableId);
        System.out.println("Traitement de la demande du client");
        switch (clientMessage.getCommand()) {
            case "dMalloc" -> message = handleDMalloc(variableId);
            case "dAccessWrite" -> message = handleDAccessWrite(variableId, channel.getRemoteHost(), clientPort);
            case "dAccessRead" -> message = handleDAccessRead(variableId);
            case "dRelease" -> message = handleDRelease(variableId);
            case "dFree" -> message = handleDFree(variableId);
            default -> message = new ServerMessage(MessageType.EXP, OperationStatus.COMMAND_ERROR);
        }
        System.out.println("heap: " + server.getHeap());
        if (message.getOperationStatus() == LOCKED){
            ChannelWithBuffer.addMessageToLockedMap(clientMessage);
        }
        channel.ownQueuePOP();
        channel.send(message);
    }

    // Définir l'état normal ou non du serveur
    public static void setNormalorNot(ServerState serverState1){
        serverState = serverState1;
    }

    //Recevoir un message DM
    private ServerMessage handleDMalloc(String variableId) {
        System.out.println("Recevoir un message DM");
        if (server.variableExistsHeap(variableId)) {
            return new ServerMessage(MessageType.DMA, OperationStatus.DATA_EXISTS);
        } else {
            server.modifyHeapDMalloc(variableId);
            return new ServerMessage(MessageType.DMA, SUCCESS);
        }
    }

    //Recevoir un message DAW
    private ServerMessage handleDAccessWrite(String variableId, InetAddress host, int port) {
        System.out.println("Recevoir un message DAW");
        if (!server.variableExistsHeap(variableId)) {
            return new ServerMessage(MessageType.DAW, OperationStatus.DATA_NOT_EXISTS);
        }
        switch (server.modifyHeapDAccessWrite(variableId, host, port)) {
            case SUCCESS -> {
                return new ServerMessage(MessageType.DAW, SUCCESS); //没锁
            }
            case LOCKED -> {
                return new ServerMessage(MessageType.DAW, LOCKED); //锁了
            }
            default -> {
                return new ServerMessage(MessageType.DRE, OperationStatus.ERROR);
            }
        }
    }

    //Recevoir un message DAR
    private ServerMessage handleDAccessRead(String variableId) {
        System.out.println("Recevoir un message DAR");
        if (!server.variableExistsHeap(variableId)) {
            return new ServerMessage(MessageType.DAR, OperationStatus.DATA_NOT_EXISTS);
        }
        if (server.getHeap().get(variableId).isEmpty()){
            return new ServerMessage(MessageType.DAR, OperationStatus.UNWRITTEN);
        }else {
            System.out.println("envoyer");
            Object obj = server.modifyHeapDAccessRead(variableId).first();
            switch ((OperationStatus) obj) {
                case SUCCESS -> {
                    Pair p = (Pair) server.modifyHeapDAccessRead(variableId).second();
                    // System.out.println(p.first().toString() + "  p1");
                    // System.out.println(p.second().toString() + "  p2");
                    ServerMessage s = new ServerMessage(MessageType.DAR, SUCCESS,(InetAddress) p.first(), (Integer) p.second());
                    //System.out.println(s.toString() + "  s");
                    return s;
                }
                case LOCKED -> {
                    return new ServerMessage(MessageType.DAR, LOCKED);
                }
                default -> {
                    return new ServerMessage(MessageType.DAR, OperationStatus.ERROR);
                }
            }
        }
    }

    //Recevoir un message DR
    private ServerMessage handleDRelease(String variableId) throws IOException {
        System.out.println("Recevoir un message DR");
        if (!server.variableExistsHeap(variableId)) {
            return new ServerMessage(MessageType.DAR, OperationStatus.DATA_NOT_EXISTS);
        }
        switch (server.modifyHeapDRelease(variableId)) {
            case SUCCESS -> {
                ChannelWithBuffer.removeMessagesByVariableId(variableId);
                return new ServerMessage(MessageType.DRE, SUCCESS);
            }
            case ERROR -> {
                return new ServerMessage(MessageType.DRE, OperationStatus.ERROR);
            }
        }
        return null;
    }

    //Recevoir un message DF
    private ServerMessage handleDFree(String variableId) throws IOException {
        System.out.println("Recevoir un message DF");

        if (!server.variableExistsHeap(variableId)) {
            return new ServerMessage(MessageType.DFR, OperationStatus.DATA_NOT_EXISTS);
        }
        switch (server.modifyHeapDFree(variableId)) {
            case SUCCESS -> {
                return new ServerMessage(MessageType.DFR, SUCCESS);
            }
            case LOCKED -> {
                return new ServerMessage(MessageType.DFR, LOCKED);
            }
        }
        return new ServerMessage(MessageType.DRE, OperationStatus.ERROR);
    }
}