package test;

import machine.Client;
import machine.Server;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SampleTest {
    /*public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, IOException, ClassNotFoundException, NoSuchMethodException {
        ServerProcessor serverProcessor = new ServerProcessor();
        Server server = new Server(8080, "Server", serverProcessor);
        Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        Client client1 = new Client(server.getPort(), "client1", channel);
        Client client2 = new Client(server.getPort(), "client2", channel);

        server.start();
        List<Object> req1 = new ArrayList<>();
        req1.add(int.class);
        req1.add("compter");
        client1.request("dmalloc", req1);
        // client.write
        // client2.read(
        // Sot(compter)
    }

     */
}