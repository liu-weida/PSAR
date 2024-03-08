package utils.message;

import java.io.Serializable;

public class ClientMessage implements Message, Serializable {
    private final String command;
    private String clientId = null;
    private String varibaleId = null;
    private Object obj = null;

    public ClientMessage(String command, String clientId, String variableId) {  //for dMalloc,dAccessWrite,
                                                                                        // dAccessRead,dFree
        this.command = command;
        this.clientId = clientId;
        this.varibaleId = variableId;
    }

    public ClientMessage(String command, Object obj) {  // pour dRelease
        this.command = command;
        this.obj = obj;
    }

    public String getCommand() {
        return command;
    }

    public String getClientId() {
        return clientId;
    }

    public String getVaribaleId() {
        return varibaleId;
    }

    public Object getObj() {
        return obj;
    }

}
