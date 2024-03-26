package utils.message;

import java.io.Serializable;
import java.net.InetAddress;

public class SendDataMessage extends Message implements Serializable {
    private Object value;
    private InetAddress host;
    private int port;

    public SendDataMessage(String variableId, InetAddress host, int port){
        super(variableId);
        this.host = host;
        this.port = port;
    }

    public SendDataMessage(String variableId, Object value){
        super(variableId);
        this.value = value;
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
        return "SendDataMessage{ " +
                super.toString() +
                ", value=" + value +
                ", host=" + host +
                ", port=" + port +
                " }";
    }
}
