package utils.message;

import java.io.Serializable;
import java.net.InetAddress;

public class ServerMessage implements Message, Serializable {

    private String messageType;
    private boolean successes;
    private final String message;
    int clientPort = Integer.parseInt(null);
    InetAddress clientHost = null;

    public ServerMessage(String messageType, boolean successes, String message){
        this.messageType = messageType;
        this.successes = successes;
        this.message = message;
    }
    public ServerMessage(String messageType, boolean successes, String message,int clientPort,InetAddress clientHost){
        this.messageType = messageType;
        this.successes = successes;
        this.message = message;
        this.clientPort = clientPort;
        this.clientHost = clientHost;
    }

    public String getMessageType() {
        return messageType;
    }

    public boolean getSuccesses() {
        return successes;
    }

    public String getMessage(){
        return message;
    }

    public int getClientPort() { return clientPort; }

    public InetAddress getClientHost() { return clientHost; }
}
