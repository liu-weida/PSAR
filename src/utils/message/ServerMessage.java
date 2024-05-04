package utils.message;

import utils.enums.OperationStatus;

import java.io.Serializable;
import java.net.InetAddress;

//Comme son nom, ce type de message est utilisé par le serveur pour répondre au client
public class ServerMessage implements Message, Serializable {

    private MessageType messageType;
    private boolean successes;

    private OperationStatus message;
    int clientPort = -1; 
    InetAddress clientHost = null;

    public ServerMessage(MessageType messageType, OperationStatus status) {
        this.messageType = messageType;
        this.successes = status == OperationStatus.SUCCESS;  // SUCCESS -> true
        this.message = status; 
    }

    public ServerMessage(MessageType messageType, OperationStatus status, InetAddress clientHost, int clientPort) {
        this(messageType, status);
        this.clientPort = clientPort;
        this.clientHost = clientHost;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public boolean getSuccesses() {
        return successes;
    }

    public OperationStatus getOperationStatus(){
        return message;
    }

    public int getClientPort() { return clientPort; }

    public InetAddress getClientHost() { return clientHost; }



    public String toString() {
        return "Message Type :" + getMessageType() + "\n" +
                "State :" + getSuccesses() + "\n" +
                "Message :" + getOperationStatus() + "\n"+
                "Client Port :" + getClientPort() + "\n" +
                "Client Host :" + getClientHost();
    }
}
