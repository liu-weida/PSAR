package utils.processor;

import machine.Client;
import utils.channel.Channel;
import utils.enums.OperationStatus;
import utils.message.*;

import java.io.IOException;

public class ClientProcessor implements Processor{
    private Client client = null;

    public void setClient(Client client){
        this.client = client;
    }

    @Override
    public void process(Channel channel, String variableId, Message clientMessage) throws IOException, ClassNotFoundException {
        ServerMessage message = (ServerMessage) channel.recv();
        handleServerMessage(message, channel, variableId, clientMessage);
    }


    private void handleServerMessage(ServerMessage message, Channel channel, String variableId, Message clientMessage) throws IOException, ClassNotFoundException {
        switch (message.getOperationStatus()){
            case SUCCESS ->  System.out.println("请求成功！");
            case DATA_NOT_EXISTS -> System.out.println("所请求的数据不存在！");
            case DATA_EXISTS -> System.out.println("所Malloc的数据已存在！");
            case UNWRITTEN ->  System.out.println("所请求的数据暂时没有用户写入。");

        }

        if (message.getMessageType() == MessageType.DAR) {

            switch (message.getOperationStatus()){
                case SUCCESS -> processDataAccessRequest(message, variableId);
                case LOCKED -> handleLockedState(channel,variableId,clientMessage);
            }

        }

    }
    private void handleLockedState(Channel channel, String variableId, Message clientMessage) {
        new Thread(() -> {
            boolean run = true;
            while (run) {
                try {
                    System.out.println("所希望读取的消息已被锁定，将在3s后重新发送阅读请求");
                    Thread.sleep(3000);
                    channel.send(clientMessage);
                    System.out.println("重发完毕");
                    ServerMessage serverMessage = (ServerMessage) channel.recv();
                    if (serverMessage.getOperationStatus() == OperationStatus.SUCCESS && serverMessage.getMessageType() == MessageType.DAR) {
                        run = false;
                        processDataAccessRequest(serverMessage, variableId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread was interrupted");
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("Error during message handling", e);
                }
            }
        }).start();
    }

    private void processDataAccessRequest(ServerMessage message, String variableId) throws IOException, ClassNotFoundException {
        Channel distanceChannel = client.connectToClient(message.getClientHost(), message.getClientPort());
        SendDataMessage sendDataMessage = new SendDataMessage(variableId, client.getHost(), client.getPort());
        distanceChannel.send(sendDataMessage);
        SendDataMessage replyMessage = (SendDataMessage) distanceChannel.recv();
        client.modifyHeap(sendDataMessage.getVariableId(), replyMessage.getValue());
        System.out.println("阅读成功！");
    }
}







