package utils.processor;

import machine.Client;
import machine.Machine;
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
    public void process(Channel channel, String variableId) throws IOException, ClassNotFoundException {
        ServerMessage message = (ServerMessage) channel.recv();

        if (message.getMessageType() == MessageType.DAR) {

            Channel distanceChannel = client.connectToClient(message.getClientHost(), message.getClientPort());

            SendDataMessage sendDataMessage = new SendDataMessage(variableId, client.getHost(), client.getPort());

            System.out.println(sendDataMessage.toString() + " read message ！！！！！！！！！！");

            distanceChannel.send(sendDataMessage);


            SendDataMessage replyMessage = (SendDataMessage) distanceChannel.recv();

            client.modifyHeap(sendDataMessage.getVariableId(), replyMessage.getValue());
        }

    }


}
