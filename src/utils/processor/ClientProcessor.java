package utils.processor;

import machine.Client;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.Message;
import utils.message.MessageType;
import utils.message.ServerMessage;

import java.io.IOException;
import java.net.ServerSocket;

public class ClientProcessor implements Processor{
    private Client client = null;
    
    public void setCLient(Client client){
        this.client = client;
    }

    @Override
    public Message process(Channel channel) throws IOException, ClassNotFoundException {
        ServerMessage message = (ServerMessage) channel.recv();
        if (message.getMessageType() == MessageType.DAR){
            client.connectToClient(message.getClientHost(), message.getClientPort(), "compter");
        } else if ( false) {
            //
        } else {
            System.out.println(message);
        }
        return message;
    }
}
//