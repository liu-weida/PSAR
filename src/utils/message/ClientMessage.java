package utils.message;

import java.io.Serializable;

public class ClientMessage extends Message implements Serializable {
    private final String command;
    private String clientId = null;
    private final int clientPort;

    public ClientMessage(String command, String clientId, String variableId, int clientPort) {  //for dMalloc,dAccessWrite, // dAccessRead,dFree
        super(variableId);
        this.command = command;
        this.clientId = clientId;
        this.clientPort = clientPort;
    }

    public ClientMessage(String command, String variableId, int clientPort) {  // pour dRelease
        super(variableId);
        this.command = command;
        this.clientPort = clientPort;
    }

    public String getCommand() {
        return command;
    }

    public String getClientId() {
        return clientId;
    }

    public int getClientPort() {
        return  clientPort;
    }

    public String toString() {
        return "Command :" + getCommand() + "\n" +
                "Client id :" + getClientId() + "\n" +
                super.toString() +
                "Client port :" + getClientId();
    }
}
