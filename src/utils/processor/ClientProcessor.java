package utils.processor;

import machine.Client;
import utils.channel.Channel;
import utils.enums.OperationStatus;
import utils.message.*;

import java.io.IOException;

// Le processeur du client
public class ClientProcessor implements Processor {
    // Le client lié
    private Client client = null;

    // Définir le client
    public void setClient(Client client) {
        this.client = client;
    }

    // Recevoir un message du serveur
    @Override
    public void process(Channel channel, String variableId, Message clientMessage) throws IOException, ClassNotFoundException {
        ServerMessage message = (ServerMessage) channel.recv();
        handleServerMessage(message, channel, variableId, clientMessage);
    }

    // Choisir le contenu à imprimer en fonction du contenu
    private void handleServerMessage(ServerMessage message, Channel channel, String variableId, Message clientMessage) throws IOException, ClassNotFoundException {
        switch (message.getOperationStatus()) {
            case SUCCESS -> System.out.println("La requête a réussi !");
            case DATA_NOT_EXISTS -> System.out.println("Les données demandées n'existent pas !");
            case DATA_EXISTS -> System.out.println("Les données allouées existent déjà !");
            case UNWRITTEN -> System.out.println("Les données demandées n'ont pas encore été écrites par un utilisateur.");
        }
        if (message.getMessageType() == MessageType.DAR) {
            switch (message.getOperationStatus()) {
                case SUCCESS -> processDataAccessRequest(message, variableId);
                case LOCKED -> handleLockedState(channel, variableId, clientMessage);
            }
        }
    }

    // Si un message de verrouillage des données est reçu, créer un nouveau thread pour retenter l'envoi
    private void handleLockedState(Channel channel, String variableId, Message clientMessage) {
        new Thread(() -> {
            boolean run = true;
            while (run) {
                try {
                    System.out.println("Le message souhaité est verrouillé, nouvelle tentative d'envoi de la demande de lecture dans 3 secondes");
                    Thread.sleep(3000);
                    channel.send(clientMessage);
                    System.out.println("Renvoi terminé");
                    ServerMessage serverMessage = (ServerMessage) channel.recv();
                    if (serverMessage.getOperationStatus() == OperationStatus.SUCCESS && serverMessage.getMessageType() == MessageType.DAR) {
                        run = false;
                        processDataAccessRequest(serverMessage, variableId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Le thread a été interrompu");
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("Erreur pendant la gestion du message", e);
                }
            }
        }).start();
    }

    // Après avoir reçu l'accord du serveur pour lire les données, aller à la fonction cible pour lire les données
    private void processDataAccessRequest(ServerMessage message, String variableId) throws IOException, ClassNotFoundException {
        Channel distanceChannel = client.connectToClient(message.getClientHost(), message.getClientPort());
        SendDataMessage sendDataMessage = new SendDataMessage(variableId, client.getHost(), client.getPort());
        distanceChannel.send(sendDataMessage);
        SendDataMessage replyMessage = (SendDataMessage) distanceChannel.recv();
        client.setObject(sendDataMessage.getVariableId(), replyMessage.getValue());
        System.out.println("Lecture réussie !");
    }
}