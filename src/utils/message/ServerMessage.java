package utils.message;

import java.io.Serializable;

public class ServerMessage implements Message, Serializable {

    private String messageType;
    private boolean successes;
    private final String message;
    private int infoClient;

    public ServerMessage(String messageType, boolean successes, String message){
        this.messageType = messageType;
        this.successes = successes;
        this.message = message;
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

}
