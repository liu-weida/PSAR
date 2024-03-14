package test;

import machine.Client;
import utils.channel.Channel;
import utils.channel.ChannelBasic;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Test {
   public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        Client client1 = new Client( 7070,"client1", channel);
       /* client1.setObject("compter", 123);

        List<Object> req1 = new ArrayList<>();
        req1.add("compter");
        client1.request("dMalloc", req1);
        client1.respond();

        client1.setChannel(new ChannelBasic(new Socket("localhost", 8080)));
        client1.request("dAccessWrite", req1);
        client1.respond();

        Client client2 = new Client(6060,"client2", new ChannelBasic(new Socket("localhost", 8080)));
        client2.request("dAccessRead", req1);
        client2.respond();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // client1.setChannel(new ChannelBasic(new Socket("localhost", 6060)));
        Channel c = new ChannelBasic(new Socket("localhost", 6060));
        c.send(1);


        */
    }

}
