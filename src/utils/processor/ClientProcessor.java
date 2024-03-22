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

        if (message instanceof SendDataMessage) {
            SendDataMessage recvMessage = (SendDataMessage) message;
            Channel distanceChannel = client.connectToClient(recvMessage.getHost(), recvMessage.getPort());

            SendDataMessage sendDataMessage = new SendDataMessage(variableId, client.getHost(), client.getPort());

            distanceChannel.send(sendDataMessage);


            SendDataMessage replyMessage = (SendDataMessage) distanceChannel.recv();

            client.modifyHeap(sendDataMessage.getVariableId(), replyMessage.getValue());
        }

        return message;
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