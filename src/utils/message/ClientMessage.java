package utils.message;

import java.io.Serializable;

public class ClientMessage implements Message, Serializable {
    private final String command;
    private String clientId = null;
    private String variableId = null;
    private Object obj = null;

    public ClientMessage(String command, String clientId, String variableId) {  //for dMalloc,dAccessWrite,
                                                                                        // dAccessRead,dFree
        this.command = command;
        this.clientId = clientId;
        this.variableId = variableId;
    }

    public ClientMessage(String command, String variableId) {  // pour dRelease
        this.command = command;
        this.variableId = variableId;
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

    public Object getObj(){ return obj; }
}
