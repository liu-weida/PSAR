package utils.message;

import java.io.Serializable;
import java.net.InetAddress;

public class ServerMessage implements Message, Serializable {

    private MessageType messageType;
    private boolean successes;
    private String message;
    int clientPort = -1; // 修改默认值为-1，表示未设置
    InetAddress clientHost = null;

    public ServerMessage(MessageType messageType, OperationStatus status) {
        this.messageType = messageType;
        this.successes = status == OperationStatus.SUCCESS;
        this.message = generateMessage(status); // 根据状态生成消息文本
    }

    public ServerMessage(MessageType messageType, OperationStatus status, InetAddress clientHost, int clientPort) {
        this(messageType, status);
        this.clientPort = clientPort;
        this.clientHost = clientHost;
    }

    private String generateMessage(OperationStatus status) {
        // 根据不同的状态返回不同的消息文本
        switch (status) {
            case INSERT_ERROR:
                return "Insert error";
            case DATA_NOT_EXISTS:
                return "Data not exists";
            case COMMAND_ERROR:
                return "Command error";
            case SUCCESS:
                return "Success";
            case DATA_EXISTS:
                return "Data exists";
            case WAIT_FOR_DRE:
                return "waiting for dRelease";
            case EXCEPTION:
                return "EXCEPTION";
            default:
                return "";
        }
    }

    public MessageType getMessageType() {
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

    public String toString() {
        return "Message Type :" + getMessageType() + "\n" +
                "State :" + getSuccesses() + "\n" +
                "Message :" + getMessage() + "\n" +
                "Client Port :" + getClientPort() + "\n" +
                "Client Host :" + getClientHost();
    }
}
