package utils.message;

import java.io.Serializable;
import java.net.InetAddress;

//Comme son nom l'indique, ce type de message est utilisé pour dAccessRead (messagerie de client à client).
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

    public SendDataMessage(InetAddress host, int port) {
        this.host = host;
        this.port = port;
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

    @Override
    public String toString() {
        return "SendDataMessage{" +
                "variableId='" + variableId + '\'' +
                ", value=" + value +
                ", host=" + host +
                ", port=" + port +
                '}';
    }
}