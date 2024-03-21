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

        Message message = (Message) channel.recv();

        System.out.println(message.toString()+"zzz");  //正常

        if (message instanceof SendDataMessage) {
            System.out.println("message instanceof SendDataMessage 正常？");  //正常
            System.out.println(((SendDataMessage) message).getHost() + "   host");
            System.out.println(((SendDataMessage) message).getPort() + "   port");
            System.out.println(client.getPort() + "   client port");
            System.out.println(client.getHost() + "  client host");
            Channel distanceChannel = client.connectToClient(((SendDataMessage) message).getHost(), ((SendDataMessage) message).getPort());
            distanceChannel.send(new SendDataMessage(variableId, client.getHost(), client.getPort()));

            //自己发自己收！！！！


            SendDataMessage sendDataMessage = (SendDataMessage) distanceChannel.recv();

            System.out.println(sendDataMessage.toString() + "   sendDataMessage");

            client.modifyHeap(sendDataMessage.getVariableId(), sendDataMessage.getValue());

        }
//        else {
//
//            if (message.getMessageType() == MessageType.DAR) {
//                Channel distanceChannel = client.connectToClient(message.getHost(), message.getPort());
//                distanceChannel.send(new SendDataMessage(variableId, message.getClientHost(), client.getPort()));
//                SendDataMessage sendDataMessage = (SendDataMessage) distanceChannel.recv();
//                client.modifyHeap(sendDataMessage.getVariableId(), sendDataMessage.getValue());
//            } else if (false) {
//                //
//            } else {
//                System.out.println("\nRecived Message");
//                System.out.println(message + "\n");
//            }
//            return message;
//        }
        return message;
    }
}
///