package utils.channel;

import utils.message.Message;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ChannelBasic implements Channel {
    private final Socket socket;
    // Interface de sortie
    ObjectOutputStream oos;
    // Interface d'entrée
    ObjectInputStream ois;

    // Constructeur
    public ChannelBasic(Socket socket) {
        this.socket = socket;
    }

    // Envoyer un message
    @Override
    public void send(Message message) throws IOException {
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(message);
        oos.flush();
    }

    // Recevoir un message
    @Override
    public Object recv() throws IOException, ClassNotFoundException {
        ois = new ObjectInputStream(socket.getInputStream());
        return ois.readObject();
    }

    @Override
    public void ownQueueOffer(Message message) {
    }

    @Override
    public void ownQueuePOP() {
    }

    // Recevoir un message avec délai d'attente. Lève une exception si aucun message n'est reçu dans le délai imparti
    @Override
    public Object recvWithTimeout(int timeout) throws IOException, ClassNotFoundException {
        if (timeout > 0) {
            socket.setSoTimeout(timeout);
        }
        try {
            if (ois == null) {
                ois = new ObjectInputStream(socket.getInputStream());
            }
            return ois.readObject();
        } catch (SocketTimeoutException e) {
            throw new IOException("Délai de réception dépassé : " + e.getMessage(), e);
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

    // Fermer le canal
    @Override
    public void close() throws IOException {
        try {
            if (ois != null) {
                ois.close(); // Fermer le flux d'entrée
            }
        } finally {
            try {
                if (oos != null) {
                    oos.close(); // Fermer le flux de sortie
                }
            } finally {
                if (socket != null) {
                    socket.close(); // Fermer la connexion socket
                }
            }
        }
        System.out.println("Channel closed successfully.");
    }
}