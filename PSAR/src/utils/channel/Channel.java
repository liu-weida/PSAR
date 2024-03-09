package utils.channel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public interface Channel {

    void send(Object object) throws IOException;

    Object recv() throws IOException, ClassNotFoundException;

    InetAddress getRemoteHost();
    int getRemotePort();

    InetAddress getLocalHost();
    int getLocalPort();
    Socket getSocket();
}