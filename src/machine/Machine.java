package machine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class Machine {
    // Nom de la machine
    private final String id;
    // Socket de la machine
    private ServerSocket serverSocket;
    // Port de la machine
    private final int port;
    // Hôte de la machine
    private final InetAddress host = InetAddress.getLocalHost();

    // Constructeur
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