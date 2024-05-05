package utils.message;

import java.io.Serializable;

//Comme son nom, ce type de message est utilisé pour les messages de demande envoyés par le client au serveur.
public class ClientMessage implements Message, Serializable {
    private final String command;   //Type de demande
    private final String clientId;    //L'identifiant du client qui a initié la demande
    private final String variableId;  //Identifiant des données demandées
    private final int clientPort;  //Port local du client (pour dAccessRead)

    public ClientMessage(String command, String clientId, String variableId, int clientPort) {  //for dMalloc,dAccessWrite, // dAccessRead,dFree
        this.command = command;
        this.clientId = clientId;
        this.variableId = variableId;
        this.clientPort = clientPort;
    }

    public String getCommand() {
        return command;
    }

    public String getClientId() {
        return clientId;
    }

    public String getVariableId() {
        return variableId;
    }

    public int getClientPort() {
        return  clientPort;
    }

    public String toString() {
        return "Command :" + getCommand() + "\n" +
                "Client id :" + getClientId() + "\n" +
                "Var id :" + getVariableId() + "\n" +
                "Client port :" + getClientId();
    }
}