package utils.message;

import java.io.Serializable;
import java.net.InetAddress;

public class SendDataMessage implements Message, Serializable {
    private String variableId;
    private Object value;
    private InetAddress host;
    private int port;

    public SendDataMessage(String variableId, InetAddress host, int port){
        this.variableId = variableId;
        this.host = host;
        this.port = port;
    }

    public SendDataMessage(String variableId, Object value){
        this.variableId = variableId;
        this.value = value;
    }

    public String getVariableId(){
        return variableId;
    }

    public Object getValue(){
        return value;
    }

    public InetAddress getHost(){
        return host;
    }

    public int getPort(){
        return port;
    }
}
