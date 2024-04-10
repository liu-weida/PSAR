package utils.message;

import utils.tools.Pair;

import java.io.Serializable;
import java.net.InetAddress;

public class HeartbeatMessage implements Message, Serializable {

    public enum Source {
        MIRROR, SERVER, CLIENT
    }

    private Source source;
    private OperationStatus operationStatus;
    private Long serverPid;
    private Pair pair;
    public HeartbeatMessage(Source source, OperationStatus op) {
        this.source = source;
        this.operationStatus = op;
    }

    public HeartbeatMessage(Source source, OperationStatus operationStatus, Long serverPid) {
        this.source = source;
        this.operationStatus = operationStatus;
        this.serverPid = serverPid;
    }

    public HeartbeatMessage(Source source, OperationStatus operationStatus, InetAddress host, int port) {
        this.source = source;
        this.operationStatus = operationStatus;
        this.pair = new Pair(host,port);
    }

    public Pair getPair() {
        return pair;
    }

    public Source getSource() {
        return source;
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public Long getServerPid() {
        return serverPid;
    }

    @Override
    public String toString() {
        return "HeartbeatMessage{" +
                "source=" + source +
                ", operationStatus=" + operationStatus +
                ", serverPid=" + serverPid +
                ", pair=" + pair +
                '}';
    }
}
