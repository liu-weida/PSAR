package utils.processor;

import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.Message;
import utils.message.MessageType;
import utils.message.ServerMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientProcessor implements Processor{
    @Override
    public Message process(Socket socket) throws IOException, ClassNotFoundException {
        Channel channel = new ChannelBasic(socket);
        ServerMessage message = (ServerMessage) channel.recv();
        if (message.getMessageType() == MessageType.DAR){
            Thread t = new Thread(() -> {
                try {
                    ServerSocket ss = new ServerSocket(6060);
                    Channel c = new ChannelBasic(ss.accept());
                    c.send("1");
                    System.out.println(c.recv());
                } catch (IOException | ClassNotFoundException e){ }
            });
            t.start();
        } else {
            System.out.println(message);
        }
        socket.close();
        return message;
    }
}
