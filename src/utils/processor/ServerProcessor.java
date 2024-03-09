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
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerProcessor implements Processor{
    private final Server server;
    private ServerMessage message;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ServerProcessor(Server server){
        this.server = server;
    }

    public Message process(Socket socket){

        Channel channel = new ChannelBasic(socket);
        try {
            ClientMessage clientMessage = (ClientMessage)channel.recv();
            String clientId = clientMessage.getClientId();
            String variableId = clientMessage.getVariableId();

            int clientPort = channel.getRemotePort();
            InetAddress clienthost = channel.getRemoteHost();
            // Class<?> clazz = clientMessage.getClazz();

            try {
                switch (clientMessage.getCommand()) {
                    case "dMalloc" -> handleDMalloc(variableId, clientId, channel);
                    case "dAccessWrite" -> handleDAccessWrite(variableId, clientId, channel);
                    case "dAccessRead" -> handleDAccessRead(variableId, clientId, channel);
                    case "dRelease" -> handleDRelease(channel);
                    case "dFree" -> handleDFree(variableId, channel);
                    default -> {
                        message = new ServerMessage("respond", false, "Commande error");

                    }
                }
            }catch(IOException e){
                //传输错误
                throw new ServerException("传输错误",e);
            }

        } catch (IOException | ClassNotFoundException | ServerException e) {
            throw new RuntimeException(e);
        }

        return message;

    }

    private void handleDMalloc(String variableId, String clientId, Channel channel) throws IOException {
        System.out.println("收到初始化数据信息");
        if(server.variableExistsHeap(variableId)){
            message = new ServerMessage("respondDMalloc",false,"dMalloc fail(data already exists)");
        }else{
            try{
                server.insertData(clientId, variableId);
            }catch(ServerException e){
                ServerMessage message = new ServerMessage("respondDMalloc",false,"dMalloc fail(insert error)");
                channel.send(message);
                return;
            }
            message = new ServerMessage("respondDMalloc",true,"dMalloc success"); // 确保成功消息在try块内部

        }
    }

    private void handleDAccessWrite(String variableId, String clientId, Channel channel) throws IOException, ClassNotFoundException {
        System.out.println("收到写入请求");
        lock.writeLock().lock();
        //数据锁
        if(!server.variableExistsHeap(variableId)){
            message = new ServerMessage("respondDAccessWrite",false,"dAccessWrite fail(data not exists)");
            return;
        }
        ServerMessage message = new ServerMessage("respondDAccessWrite",true,"dAccessWrite waiting for dRelease");
        channel.send(message);
        ClientMessage clientMessageWrite= (ClientMessage)channel.recv();
        if(!Objects.equals(clientMessageWrite.getCommand(), "dRelease")){
            message = new ServerMessage("respondDAccessWrite",false,"dAccessWrite fail(command error)");
            //解锁下一个notifyone
        }else{
            try{//更新数据到list首位
                server.deleteData(clientId, variableId);
                server.insertData(clientId, variableId);
            }catch(ServerException e){
                message = new ServerMessage("respondDAccessWrite",false,"dAccessWrite fail(insert error)");
                //解锁下一个notifyone
                return;
            }
            message = new ServerMessage("respondDAccessWrite",true,"dAccessWrite success");

            //解锁下一个notifyone
            lock.writeLock().unlock();
        }
    }
    private void handleDAccessRead(String variableId, String clientId, Channel channel) throws IOException, ClassNotFoundException {
        System.out.println("收到客户端阅读请求");
        //数据锁
        lock.readLock().lock();
        if (!server.variableExistsHeap(variableId)) {
            message = new ServerMessage("respondDAccessRead", false, "dAccessRead fail(data not exists)");

            //解锁下一个notifyone
            return;
        }
        ServerMessage message = new ServerMessage("respondDAccessRead", true, "dAccessRead waiting for dRelease");
        channel.send(message);
        ClientMessage clientMessageRead = (ClientMessage) channel.recv();
        if (!Objects.equals(clientMessageRead.getCommand(), "dRelease")) {
            message = new ServerMessage("respondDAccessRead", false, "dAccessRead fail(command error)");

            //解锁下一个notifyone
        } else {
            try {
                server.deleteData(clientId, variableId);
                server.insertData(clientId, variableId);
            } catch (ServerException e) {
                message = new ServerMessage("respondDAccessRead",false,"dAccessRead fail(insert error)");

                //解锁下一个notifyone
                return;
            }
            message = new ServerMessage("respondDAccessRead", true, "dAccessRead success");

            //解锁下一个notifyone
            lock.readLock().unlock();
        }
    }
    private void handleDRelease(Channel channel) throws IOException {
        System.out.println("数据使用完毕信息");
        message = new ServerMessage("respondDRelease", false, "command error");
    }
    private void handleDFree(String variableId, Channel channel) throws IOException {
        System.out.println("收到客户端删除数据请求");
        //数据锁
        if (!server.variableExistsHeap(variableId)) {
            message = new ServerMessage("respondDFree", false, "Dfree fail(data not exists");

        } else {//如果存在
            server.deleteVariable(variableId);
            message = new ServerMessage("respondDFree", true, "Dfree success");

            //notifyall
        }
    }
}
