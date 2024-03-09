package utils.processor;

import machine.Server;
import utils.exception.ServerException;
import utils.message.ClientMessage;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.Message;
import utils.message.ServerMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

public class ServerProcessor implements Processor{
    private Server server;

    public ServerProcessor(Server server){
        this.server = server;
    }

    public Message process(Socket socket){

        Channel channel = new ChannelBasic(socket);
        try {
            ClientMessage clientMessage = (ClientMessage)channel.recv();
            String clientId = clientMessage.getClientId();
            String variableId = clientMessage.getVaribaleId();

            int clientPort = channel.getRemotePort();
            InetAddress clienthost = channel.getRemoteHost();
            // Class<?> clazz = clientMessage.getClazz();
            Object obj = clientMessage.getObj();

            try {
                switch (clientMessage.getCommand()) {
                    case "dMalloc":
                        handleDMalloc(variableId, clientId, channel);
                        break;
                    case "dAccessWrite":
                        handleDAccessWrite(variableId,clientId,channel);
                        break;
                    case "dAccessRead":
                        handleDAccessRead(variableId,clientId,channel);
                        break;
                    case "dRelease":
                        handleDRelease(channel);
                        break;
                    case "dFree":
                        handleDFree(variableId, clientId, channel);
                        break;
                    default:
                        ServerMessage message = new ServerMessage("respond",false,"Commande error");
                        channel.send(message);
                }
            }catch(IOException e){
                //传输错误
            }


            //les restes a faire
            ServerMessage serverMessage = null;
            channel.send(serverMessage);

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return null;

    }

    private void handleDMalloc(String variableId, String clientId, Channel channel) throws IOException {
        System.out.println("收到初始化数据信息");
        ServerMessage message = null;
        if(server.variableExistsHeap(variableId)){
            message = new ServerMessage("respondDMalloc",false,"dMalloc fail(data already exists)");
            channel.send(message);
        }else{
            try{
                server.insertData(clientId, variableId);
            }catch(ServerException e){
                message = new ServerMessage("respondDMalloc",false,"dMalloc fail(hashmap is full)");
                channel.send(message);
                return;
            }
            message = new ServerMessage("respondDMalloc",true,"dMalloc success"); // 确保成功消息在try块内部
            channel.send(message);
        }
    }

    private void handleDAccessWrite(String variableId, String clientId, Channel channel) throws IOException, ClassNotFoundException {
        System.out.println("收到写入请求");
        ServerMessage message = null;
        //数据锁
        if(!server.variableExistsHeap(variableId)){
            message = new ServerMessage("respondDAccessWrite",false,"dAccessWrite fail(data not exists)");
            channel.send(message);
            return;
        }
        message = new ServerMessage("respondDAccessWrite",true,"dAccessWrite waiting for dRelease");
        channel.send(message);
        ClientMessage clientMessageWrite= (ClientMessage)channel.recv();
        if(clientMessageWrite.getCommand() != "dRelease"){
            message = new ServerMessage("respondDAccessWrite",false,"dAccessWrite fail(command error)");
            channel.send(message);
            //解锁下一个notifyone
            return;
        }else{
            try{//更新数据到list首位
                server.deleteData(clientId, variableId);
                server.insertData(clientId, variableId);
            }catch(ServerException e){
                message = new ServerMessage("respondDAccessWrite",false,"dAccessWrite fail(hashmap is full)");
                channel.send(message);
                //解锁下一个notifyone
                return;
            }
            message = new ServerMessage("respondDAccessWrite",true,"dAccessWrite success");
            channel.send(message);
            //解锁下一个notifyone
        }
    }
    private void handleDAccessRead(String variableId, String clientId, Channel channel) throws IOException, ClassNotFoundException {
        System.out.println("收到客户端阅读请求");
        ServerMessage message = null;
        //数据锁
        if (!server.variableExistsHeap(variableId)) {
            message = new ServerMessage("respondDAccessRead", false, "dAccessRead fail(data not exists)");
            channel.send(message);
            //解锁下一个notifyone
            return;
        }
        message = new ServerMessage("respondDAccessRead", true, "dAccessRead waiting for dRelease");
        channel.send(message);
        ClientMessage clientMessageRead = (ClientMessage) channel.recv();
        if (clientMessageRead.getCommand() != "dRelease") {
            message = new ServerMessage("respondDAccessRead", false, "dAccessRead fail(command error)");
            channel.send(message);
            //解锁下一个notifyone
        } else {
            try {
                server.deleteData(clientId, variableId);
                server.insertData(clientId, variableId);
            } catch (ServerException e) {
                channel.send(e);
                //解锁下一个notifyone
                return;
            }
            message = new ServerMessage("respondDAccessRead", true, "dAccessRead success");
            channel.send(message);
            //解锁下一个notifyone
        }
    }
    private void handleDRelease(Channel channel) throws IOException {
        System.out.println("数据使用完毕信息");
        ServerMessage message = null;
        message = new ServerMessage("respondDRelease", false, "command error");
        channel.send(message);
    }
    private void handleDFree(String variableId, String clientId, Channel channel) throws IOException {
        System.out.println("收到客户端删除数据请求");
        ServerMessage message = null;
        //数据锁
        if (!server.variableExistsHeap(variableId)) {
            message = new ServerMessage("respondDFree", false, "Dfree fail(data not exists");
            channel.send(message);
        } else {//如果存在
            server.deleteVariable(variableId);
            message = new ServerMessage("respondDFree", true, "Dfree success");
            channel.send(message);
            //notifyall
        }
    }
}
