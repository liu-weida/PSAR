package machine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class Machine {
    //机器名字
    private final String id;
    //机器socket
    private ServerSocket serverSocket;
    //机器port
    private final int port;
    //机器host
    private final InetAddress host = InetAddress.getLocalHost();

    public Machine(String id, int port) throws IOException {
        this.id = id;
        this.port = port;
        serverSocket = new ServerSocket(port);

    }

    public String getId() {
        return id;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getHost() {
        return host;
    }
}
