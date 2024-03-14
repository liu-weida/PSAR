package test;

import machine.Client;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.processor.ClientProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {
    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InterruptedException {
        Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        Client client1 = new Client( 6060,"client1", channel, new ClientProcessor());
        client1.setObject("compter", 123);

        client1.listenForClientMessages();


        String req1 = "compter";
        client1.request("dMalloc", req1);
        client1.respond();
        System.out.println(1);

        client1.setChannel(new ChannelBasic(new Socket("localhost", 8080)));
        client1.request("dAccessWrite", req1);
        client1.respond();

        Client client2 = new Client(7070,"client2", new ChannelBasic(new Socket("localhost", 8080)), new ClientProcessor());
        client2.request("dAccessRead", req1);
        client2.respond();


//        // client1.setChannel(new ChannelBasic(new Socket("localhost", 6060)));
//        Channel c = new ChannelBasic(new Socket("localhost", 6060));
//        c.send(1);
    }
}
