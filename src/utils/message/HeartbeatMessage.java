package utils.message;

import java.io.Serializable;

public class HeartbeatMessage implements Message, Serializable {

    private MessageType mt;

    private OperationStatus op;

    private Long serverPid;
    public HeartbeatMessage(MessageType mt, OperationStatus op) {
        this.mt = mt;
        this.op = op;
    }

    public HeartbeatMessage(MessageType mt, OperationStatus op, Long serverPid) {
        this.mt = mt;
        this.op = op;
        this.serverPid = serverPid;
    }

    public MessageType getMt() {
        return mt;
    }

    public OperationStatus getOp() {
        return op;
    }

    public Long getServerPid() {
        return serverPid;
    }

    @Override
    public String toString() {
        return "HeartbeatMessage{" +
                "mt=" + mt +
                ", op=" + op +
                ", serverPid=" + serverPid +
                '}';
    }
}
