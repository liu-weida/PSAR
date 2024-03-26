package utils.processor;

import machine.Client;
import utils.channel.Channel;
import utils.message.Message;
import utils.message.MessageType;
import utils.message.SendDataMessage;
import utils.message.ServerMessage;

import java.io.IOException;

public class ClientProcessor {
    private Client client = null;

    public void setCLient(Client client) {
        this.client = client;
    }

    public Message process(Channel channel, String variableId) throws IOException, ClassNotFoundException {
        ServerMessage message = (ServerMessage) channel.recv();
        if (message.getMessageType() == MessageType.DAR) {
            Channel distanceChannel = client.connectToClient(message.getClientHost(), message.getClientPort());
            SendDataMessage sendDataMessage = new SendDataMessage(variableId, client.getHost(), client.getPort());
            distanceChannel.send(sendDataMessage);

            SendDataMessage replyMessage = (SendDataMessage) distanceChannel.recv();
            client.modifyHeap(sendDataMessage.getVariableId(), replyMessage.getValue());
        }
        System.out.println(message);
        return message;
    }
}