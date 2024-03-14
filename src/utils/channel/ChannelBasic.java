package utils.channel;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;


public class ChannelBasic implements Channel {
    private final Socket socket;

    public ChannelBasic(Socket socket){
        this.socket = socket;
    }

    @Override
    public void send(Object object) throws IOException{
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(object);
        oos.flush();
    }

    @Override
    public Object recv() throws IOException, ClassNotFoundException  {
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        return ois.readObject();
    }

    @Override
    public InetAddress getRemoteHost() {
        return socket.getInetAddress();
    }

    @Override
    public int getRemotePort() {
        return socket.getPort();
    }

    @Override
    public InetAddress getLocalHost() {
        return socket.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public Socket getSocket() {
        return socket;
    }
}