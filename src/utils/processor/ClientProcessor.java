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
    public Void process(Channel channel, String variableId) throws IOException, ClassNotFoundException {
        ServerMessage message = (ServerMessage) channel.recv();

        if (message.getMessageType() == MessageType.DAR) {

            Channel distanceChannel = client.connectToClient(message.getClientHost(), message.getClientPort());

            SendDataMessage sendDataMessage = new SendDataMessage(variableId, client.getHost(), client.getPort());

            System.out.println(sendDataMessage.toString() + " read message ！！！！！！！！！！");

            distanceChannel.send(sendDataMessage);


            SendDataMessage replyMessage = (SendDataMessage) distanceChannel.recv();

            client.modifyHeap(sendDataMessage.getVariableId(), replyMessage.getValue());
        }

        return null;
    }


    //自己发自己收！！！！

//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }

//            SendDataMessage sendDataMessage1 = (SendDataMessage) distanceChannel.recv();
//
//            System.out.println(sendDataMessage.toString() + "   sendDataMessage");
//
//            client.modifyHeap(sendDataMessage.getVariableId(), sendDataMessage.getValue());
//
//        }
////        else {
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
//        return message;
//    }
}
///