package utils.channel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import utils.message.ClientMessage;
import utils.message.Message;

public interface Channel {
    // Méthode de communication S2C (Serveur à Client)
    void send(Message message) throws IOException;  // Envoyer un message du serveur au client
    Object recv() throws IOException, ClassNotFoundException;  // Recevoir un message du serveur au client

    public void ownQueueOffer(Message message);

    public void ownQueuePOP();

    // Recevoir un message avec un délai spécifié
    Object recvWithTimeout(int timeout) throws IOException, ClassNotFoundException;

    // Obtenir l'adresse de l'hôte distant
    InetAddress getRemoteHost();

    // Obtenir le numéro de port distant
    int getRemotePort();

    // Obtenir l'adresse de l'hôte local
    InetAddress getLocalHost();

    // Obtenir le numéro de port local
    int getLocalPort();

    // Obtenir la connexion Socket
    Socket getSocket();

    // Fermer la connexion
    void close() throws IOException;
}
