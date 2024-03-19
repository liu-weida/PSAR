package utils.processor;

import machine.Client;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.Message;
import utils.message.MessageType;
import utils.message.SendDataMessage;
import utils.message.ServerMessage;

import java.io.IOException;
import java.net.ServerSocket;

public class ClientProcessor implements Processor{
    private Client client = null;
    
    public void setCLient(Client client){
        this.client = client;
    }

    @Override
    public Message process(Channel channel, String variableId) throws IOException, ClassNotFoundException {
        ServerMessage message = (ServerMessage) channel.recv();
        if (message.getMessageType() == MessageType.DAR){
            Channel distanceChannel = client.connectToClient(message.getClientHost(), message.getClientPort());
            distanceChannel.send(new SendDataMessage(variableId, message.getClientHost(), client.getPort()));
            SendDataMessage sendDataMessage = (SendDataMessage) distanceChannel.recv();
            client.modifyHeap(sendDataMessage.getVariableId(), sendDataMessage.getValue());
        } else if ( false) {
            //
        } else {
            System.out.println("\nRecived Message");
            System.out.println(message + "\n");
        }
        return message;
    }
}
///